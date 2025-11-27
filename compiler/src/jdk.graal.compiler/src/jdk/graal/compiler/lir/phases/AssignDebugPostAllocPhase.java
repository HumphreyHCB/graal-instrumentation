package jdk.graal.compiler.lir.phases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.graal.compiler.core.common.cfg.AbstractControlFlowGraph;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.core.common.cfg.CFGLoop;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.amd64.AMD64LoopStartOp;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.phases.PostAllocationOptimizationPhase;
import jdk.graal.compiler.nodes.StartofLoopNode;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.graal.compiler.phases.common.GTCollectCompilerMarkers;

public final class AssignDebugPostAllocPhase extends PostAllocationOptimizationPhase {


    @Override
    protected void run(TargetDescription target,
            LIRGenerationResult lirGenRes,
            PostAllocationOptimizationContext context) {

        if (lirGenRes.getCompilationUnitName().contains("Stub")
                || lirGenRes.getCompilationUnitName().contains("HotSpotOSRCompilation")) {
            return;
        }

        LIR lir = lirGenRes.getLIR();
        AbstractControlFlowGraph<?> cfg = lir.getControlFlowGraph();

        Map<Integer, CFGLoop<?>> loopIdMap = orderLoopsByNesting(mapLoopIdsToLoops(cfg, lir));

        assingDebugingInformation(loopIdMap, lir, lirGenRes);

    }

    private void assingDebugingInformation(Map<Integer, CFGLoop<?>> loopIdMap,
            LIR lir,
            LIRGenerationResult lirGenRes) {
            
        for (Integer LoopID : loopIdMap.keySet()) {
            CFGLoop<?> loop = loopIdMap.get(LoopID);
            
            for (BasicBlock<?> block : loop.getBlocks())
            {
                ArrayList<LIRInstruction> instructions =  lir.getLIRforBlock(block);

                for (LIRInstruction instr : instructions) {
                    if (instr.getPosition() != null) {
                        instr.setPosition(buildDebugPositionChain(instr.getPosition(), lirGenRes.getCompilationId(), LoopID));
                        
                    }
                }

            }

        }


    }
 

    private Map<Integer, CFGLoop<?>> orderLoopsByNesting(Map<Integer, CFGLoop<?>> loopIdMap) {
        if (loopIdMap == null || loopIdMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // Copy entries into a list for sorting
        List<Map.Entry<Integer, CFGLoop<?>>> entries = new ArrayList<>(loopIdMap.entrySet());

        // Sort by loop depth (outer → inner).
        // Tie-breaker: loopId ascending to keep ordering stable.
        entries.sort((e1, e2) -> {
            int d1 = e1.getValue().getDepth();
            int d2 = e2.getValue().getDepth();

            if (d1 != d2) {
                return Integer.compare(d1, d2);
            }
            return Integer.compare(e1.getKey(), e2.getKey());
        });

        // Build ordered map preserving sorted order
        Map<Integer, CFGLoop<?>> ordered = new LinkedHashMap<>();
        for (Map.Entry<Integer, CFGLoop<?>> e : entries) {
            ordered.put(e.getKey(), e.getValue());
        }

        return ordered;
    }

    private Map<Integer, CFGLoop<?>> mapLoopIdsToLoops(AbstractControlFlowGraph<?> cfg, LIR lir) {
        BasicBlock<?>[] blocks = cfg.getBlocks();

        // loopId (from AMD64LoopStartOp) -> CFGLoop (from first successor)
        Map<Integer, CFGLoop<?>> loopsFromStarts = new HashMap<>();

        for (BasicBlock<?> block : blocks) {
            @SuppressWarnings("unchecked")
            List<LIRInstruction> lirList = (List<LIRInstruction>) lir.getLIRforBlock(block);
            if (lirList == null || lirList.isEmpty()) {
                continue;
            }

            AMD64LoopStartOp loopStart = null;
            for (LIRInstruction instr : lirList) {
                if (instr instanceof AMD64LoopStartOp) {
                    loopStart = (AMD64LoopStartOp) instr;
                    break;
                }
            }

            if (loopStart == null) {
                continue;
            }

            CFGLoop<?> succLoop = block.getSuccessorAt(0).getLoop();
            if (succLoop == null) {
                continue;
            }

            loopsFromStarts.put(loopStart.loopId, succLoop);
        }

        return loopsFromStarts;
    }

    private static NodeSourcePosition buildDebugPositionChain(NodeSourcePosition ogPos,
            int compID,
            int loopId) {
        // start from the original position
        NodeSourcePosition pos = null;
        if (GTCollectCompilerMarkers.MARKERS[GTCollectCompilerMarkers.MAX_MARKERS - 1] == null) {
            return ogPos;
        }

        // push each digit of compID as a marker
        int[] digits = Integer.toString(compID).chars().map(c -> c - '0').toArray();
        for (int d : digits) {
            if (d >= 0 && d < GTCollectCompilerMarkers.MARKERS.length) {
                pos = new NodeSourcePosition(
                        null,
                        pos,
                        GTCollectCompilerMarkers.MARKERS[d],
                        -1);
            }
        }

        // delimiter
        pos = new NodeSourcePosition(
                null,
                pos,
                GTCollectCompilerMarkers.MARKERS[GTCollectCompilerMarkers.MAX_MARKERS - 1],
                -1);

        // push the loop marker
        if (loopId >= 0 && loopId < GTCollectCompilerMarkers.MARKERS.length) {
            pos = new NodeSourcePosition(
                    null,
                    pos,
                    GTCollectCompilerMarkers.MARKERS[loopId],
                    -1);
        }

        return pos;
    }

}
