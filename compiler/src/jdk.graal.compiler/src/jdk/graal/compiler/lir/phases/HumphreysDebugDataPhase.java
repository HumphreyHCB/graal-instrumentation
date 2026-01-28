package jdk.graal.compiler.lir.phases;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import jdk.graal.compiler.core.common.cfg.AbstractControlFlowGraph;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.core.common.cfg.CFGLoop;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.phases.PostAllocationOptimizationPhase;
import jdk.graal.compiler.phases.common.BuboInstrumentationGraphMarkersLowTierPhase;
import jdk.graal.compiler.phases.common.HumphreyDebugDataInstrumentationGraphMarkersLowTierPhase;
import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.TargetDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jdk.graal.compiler.lir.amd64.AMD64LoopEndOp;
import jdk.graal.compiler.lir.amd64.AMD64LoopStartOp;

public final class HumphreysDebugDataPhase extends PostAllocationOptimizationPhase {

    @Override
    protected void run(TargetDescription target,
            LIRGenerationResult lirGenRes,
            PostAllocationOptimizationContext context) {

        // Skip stubs / OSR compilations.
        String compName = lirGenRes.getCompilationUnitName();
        if (compName.contains("Stub") || compName.contains("HotSpotOSRCompilation")) {
            return;
        }
        LIR lir = lirGenRes.getLIR();
        AbstractControlFlowGraph<?> cfg = lir.getControlFlowGraph();

        Map<Integer, CFGLoop<?>> loopIdToLoop = mapLoopIdsToLoops(cfg, lir);
        Map<CFGLoop<?>, Integer> loopToLoopId = new HashMap<>();
        for (Map.Entry<Integer, CFGLoop<?>> e : loopIdToLoop.entrySet()) {
            loopToLoopId.put(e.getValue(), e.getKey());
        }



        System.out.println("=== HumphreysDebugDataPhase ===");
        System.out.println("Compilation: " + lirGenRes.getCompilationId() + "-" + compName);
        System.out.println("Number of loops: " + cfg.getNumberOfLoops());
        System.out.println();

        // ---- Per-block information ----
        for (BasicBlock<?> block : cfg.getBlocks()) {
            System.out.println("Block " + block.getId());

            // Successors
            System.out.println("  Successors:");
            for (int i = 0; i < block.getSuccessorCount(); i++) {
                BasicBlock<?> succ = block.getSuccessorAt(i);
                System.out.println("    -> " + succ.getId());
            }

            // Predecessors
            System.out.println("  Predecessors:");
            for (int i = 0; i < block.getPredecessorCount(); i++) {
                BasicBlock<?> pred = block.getPredecessorAt(i);
                System.out.println("    <- " + pred.getId());
            }

            // Block loop membership
            if (block.getLoop() != null) {
                //CFGLoop<?> loop = block.getLoop();
                //Integer loopId = loopToLoopId.get(loop);
                // if (loopId != null) {
                //     System.out.println("  In loop: L" + loopId);
                // } else {
                //     System.out.println("  In loop: <unknown>");
                // }
                //BuboInstrumentationGraphMarkersLowTierPhase.CFG_ID_TO_IDA.get(block.getLoop().getIndex());
                System.out.println("  In loop: L" + HumphreyDebugDataInstrumentationGraphMarkersLowTierPhase.CFG_ID_TO_IDA.get(block.getLoop().getIndex()));
                //System.out.println("  In loop: L" + block.getLoop().index);
            } else {
                System.out.println("  In loop: <none>");
            }

            // Collect unique source positions seen in this block.
            Set<String> srcEncodings = new LinkedHashSet<>();

            List<LIRInstruction> instrs = lir.getLIRforBlock(block);
            for (LIRInstruction instr : instrs) {
                NodeSourcePosition pos = instr.getPosition();
                if (pos != null) {
                    String enc = pos.getMethod().toString();
                    srcEncodings.add(enc);
                }
            }

            System.out.println("  Source positions in block:");
            if (srcEncodings.isEmpty()) {
                System.out.println("    <none>");
            } else {
                for (String enc : srcEncodings) {
                    System.out.println("    " + enc);
                }
            }

            System.out.println(" BuboLoopMakers: ");
            for (LIRInstruction instr : instrs) {
                if (instr instanceof AMD64BuboRDTSCToSlot || instr instanceof AMD64BuboWriteDeltaRDTSC) {
                    System.out.println(" Found in this block : " + instr.getClass());
                    if (instr instanceof AMD64BuboWriteDeltaRDTSC) {
                        AMD64BuboWriteDeltaRDTSC a = (AMD64BuboWriteDeltaRDTSC) instr;
                        System.out.print(" LoopID: " + a.loopId);

                    }
                    if (instr instanceof AMD64BuboRDTSCToSlot) {
                        AMD64BuboRDTSCToSlot a = (AMD64BuboRDTSCToSlot) instr;
                        System.out.print(" LoopID: " + a.loopID);

                    }

                }
            }

            System.out.println();
        }

        System.out.println("=== End HumphreysDebugDataPhase ===");
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

}
