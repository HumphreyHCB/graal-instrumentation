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

            int rdtscIdx = -1;
            int writeDeltaIdx = -1;

            for (int i = 0; i < insns.size(); i++) {
                LIRInstruction op = insns.get(i);
                if (op instanceof AMD64BuboRDTSCToSlot) {
                    rdtscIdx = i;
                } else if (op instanceof AMD64BuboWriteDeltaRDTSC) {
                    writeDeltaIdx = i;
                }
            }

            // Move RDTSC to just before the last instruction
            if (rdtscIdx != -1 && insns.size() >= 2) {
                LIRInstruction rdtsc = insns.remove(rdtscIdx);
                insns.add(insns.size() - 1, rdtsc);
                // adjust writeDeltaIdx if it was after removed index
                if (writeDeltaIdx > rdtscIdx) {
                    writeDeltaIdx--;
                }
            }

            // Move WriteDelta to index 1 (if possible)
            if (writeDeltaIdx != -1 && insns.size() > 1) {
                LIRInstruction wd = insns.remove(writeDeltaIdx);
                insns.add(1, wd);
            }
        }

    }
}
