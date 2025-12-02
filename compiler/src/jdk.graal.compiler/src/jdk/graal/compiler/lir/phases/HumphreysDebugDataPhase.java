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
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.phases.PostAllocationOptimizationPhase;
import jdk.vm.ci.code.TargetDescription;

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

        System.out.println("=== HumphreysDebugDataPhase ===");
        System.out.println("Compilation: " + compName);
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
                System.out.println("  In loop: L" + block.getLoop().getIndex());
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

            System.out.println();
        }
        
        System.out.println("=== End HumphreysDebugDataPhase ===");
    }

}
