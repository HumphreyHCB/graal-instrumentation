package jdk.graal.compiler.lir.phases;

import java.util.List;

import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.phases.PostAllocationOptimizationPhase;
import jdk.vm.ci.code.TargetDescription;

public final class BuboVerifyPostAllocPhase extends PostAllocationOptimizationPhase {

    @Override
    protected void run(TargetDescription target,
                       LIRGenerationResult lirGenRes,
                       PostAllocationOptimizationContext context) {

        LIR lir = lirGenRes.getLIR();

        // have we seen ANY start-marker yet in this method?
        boolean sawStartInMethod = false;

        BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();
        for (int b = 0; b < blocks.length; b++) {
            BasicBlock<?> block = blocks[b];
            List<LIRInstruction> insns = lir.getLIRforBlock(block);

            for (int i = 0; i < insns.size(); i++) {
                LIRInstruction op = insns.get(i);

                if (op instanceof AMD64BuboRDTSCToSlot) {
                    sawStartInMethod = true;
                } else if (op instanceof AMD64BuboWriteDeltaRDTSC) {
                    if (!sawStartInMethod) {
                        // this means some LIR phase reordered / deleted our start
                        System.out.printf(
                            "[BUBO VERIFY] write-before-start in %s: block=%d/%d insn=%d/%d op=%s%n",
                            lirGenRes.getCompilationUnitName(),
                            b + 1, blocks.length,
                            i + 1, insns.size(),
                            op
                        );
                    }
                }
            }
        }
    }
}
