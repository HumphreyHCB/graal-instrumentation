package jdk.graal.compiler.lir.phases;

import java.lang.reflect.Method;
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
import jdk.graal.compiler.hotspot.SnippetResolvedJavaMethod;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.amd64.AMD64LoopStartOp;
import jdk.graal.compiler.lir.amd64.AMD64Call.CallOp;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.phases.PostAllocationOptimizationPhase;
import jdk.graal.compiler.nodes.StartofLoopNode;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.graal.compiler.phases.common.GTCollectCompilerMarkers;

public final class AssignDebugPostAllocPhase extends PostAllocationOptimizationPhase {

    // private static ResolvedJavaMethod
    // resolveStringHashCode(HotSpotResolvedObjectType accessingType) {

    // // Everything here runs at *runtime* (inside the isolate), not at
    // native-image build time.
    // HotSpotJVMCIRuntime rt = HotSpotJVMCIRuntime.runtime();
    // ResolvedJavaType stringType = (ResolvedJavaType)
    // rt.lookupType("Lmy/custom/BuboAgentCompilerMarkers;", accessingType, true);

    // ResolvedJavaMethod found = null;
    // for (ResolvedJavaMethod m : stringType.getDeclaredMethods()) {
    // if (m.getName().equals("Marker0")
    // && m.getSignature().getParameterCount(false) == 0) {
    // found = m;
    // break;
    // }
    // }

    // if (found == null) {
    // throw new IllegalStateException("Did not find
    // Lmy/custom/BuboAgentCompilerMarkers;");
    // }

    // STRING_HASHCODE = found;
    // return found;
    // }

    @Override
    protected void run(TargetDescription target,
            LIRGenerationResult lirGenRes,
            PostAllocationOptimizationContext context) {
        // || lirGenRes.getCompilationUnitName().contains("HotSpotOSRCompilation")
        if (lirGenRes.getCompilationUnitName().contains("Stub")) {
            return;
        }

        // context.
        // compName = lirGenRes.getCompilationUnitName();

        // System.out.println("In : " + compName + " Delmi is: " +
        // GTCollectCompilerMarkers.Delimter);

        LIR lir = lirGenRes.getLIR();
        AbstractControlFlowGraph<?> cfg = lir.getControlFlowGraph();

        Map<Integer, CFGLoop<?>> loopIdMap = orderLoopsByNesting(mapLoopIdsToLoops(cfg, lir));

        assingDebugingInformation(loopIdMap, lir, lirGenRes);

    }

    private static boolean isHotSpotOnlyChain(NodeSourcePosition pos) {
        for (NodeSourcePosition p = pos; p != null; p = p.getCaller()) {
            ResolvedJavaMethod m = p.getMethod();
            if (m == null) {
                continue;
            }
            // If ANY method in the chain is not a real HotSpotResolvedJavaMethod,
            // HotSpotCompiledCodeStream may crash when writing debug info.
            if (!(m instanceof HotSpotResolvedJavaMethod)) {
                return false;
            }
        }
        return true;
    }

    private void assingDebugingInformation(Map<Integer, CFGLoop<?>> loopIdMap,
            LIR lir,
            LIRGenerationResult lirGenRes) {

        for (Integer loopID : loopIdMap.keySet()) {
            CFGLoop<?> loop = loopIdMap.get(loopID);

            // Initialise markers once (lazily) the first time we find a usable HotSpot-only
            // chain.
            boolean markersReady = GTCollectCompilerMarkers.MARKERS != null
                    && GTCollectCompilerMarkers.MARKERS[0] != null;

            HotSpotResolvedObjectType cachedAccessingType = null;

            for (BasicBlock<?> block : loop.getBlocks()) {
                ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);

                for (LIRInstruction instr : instructions) {

                    // If markers are not ready yet, try to bootstrap them from the first valid
                    // position we see.
                    if (!markersReady) {
                        NodeSourcePosition p = instr.getPosition();
                        if (p != null && isHotSpotOnlyChain(p)) {
                            cachedAccessingType = findFirstNonSnippetHotSpotType(p);
                            if (cachedAccessingType != null) {
                                GTCollectCompilerMarkers.initializeMarkers(cachedAccessingType);
                                markersReady = GTCollectCompilerMarkers.MARKERS != null
                                        && GTCollectCompilerMarkers.MARKERS[0] != null;
                            }
                        }
                    }

                    if (!markersReady) {
                        continue;
                    }
                    instr.setPosition(buildDebugPositionChain(
                            instr.getPosition(),
                            lirGenRes.getCompilationId(),
                            loopID));
                }
            }
        }
    }

    private static HotSpotResolvedObjectType findFirstNonSnippetHotSpotType(NodeSourcePosition pos) {
        for (NodeSourcePosition p = pos; p != null; p = p.getCaller()) {
            ResolvedJavaMethod m = p.getMethod();
            if (m == null) {
                continue;
            }

            // Skip snippet wrapper methods explicitly
            if (m instanceof SnippetResolvedJavaMethod) {
                continue;
            }

            // Only accept real HotSpot methods (safe for lookupType context)
            if (m instanceof HotSpotResolvedJavaMethod) {
                return (HotSpotResolvedObjectType) ((HotSpotResolvedJavaMethod) m).getDeclaringClass();
            }

            // Any other non-HotSpot method types (wrappers) are ignored.
        }
        return null;
    }

    private static HotSpotResolvedObjectType findHotSpotAccessingType(NodeSourcePosition pos) {
        for (NodeSourcePosition p = pos; p != null; p = p.getCaller()) {
            if (p.getMethod() == null) {
                continue;
            }
            ResolvedJavaType t = p.getMethod().getDeclaringClass();
            if (t instanceof HotSpotResolvedObjectType) {
                return (HotSpotResolvedObjectType) t;
            }
        }
        return null;
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
        if (GTCollectCompilerMarkers.MARKERS[GTCollectCompilerMarkers.MAX_MARKERS -
                1] == null) {
            // System.out.println("Maxed out markers on : " + compName);
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