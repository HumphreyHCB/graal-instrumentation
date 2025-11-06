package jdk.graal.compiler.lir.constopt;

import static jdk.graal.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.List;

import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.hotspot.amd64.AMD64HotSpotReturnOp;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeBuffers;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInsertionBuffer;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.VirtualStackSlot;
import jdk.graal.compiler.lir.amd64.AMD64GraphStartOp;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboIncActivationOp;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDPMCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDPMC;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.graal.compiler.lir.phases.PreAllocationOptimizationPhase;
import jdk.graal.compiler.options.NestedBooleanOptionKey;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionType;
import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.TargetDescription;

/**
 * Inserts:
 *  (1) A single start RDTSC capture (to stack) immediately before the first AMD64ReadTimestampCounter marker.
 *  (2) An inline end+delta+accumulate op before every AMD64HotSpotReturnOp.
 */
public final class BuboLIRPhase extends PreAllocationOptimizationPhase {


    public static class Options {
        @Option(help = "Enable Bubo Lir Phase.", type = OptionType.Debug)
        public static final NestedBooleanOptionKey BuboLIRPhase =
                new NestedBooleanOptionKey(LIROptimization, false);
    }


    /**
     * allocates spill slots, finds the first marker, inserts the start,
     * and then instruments every return with an inline end+delta write.
     */
    @Override
    protected void run(TargetDescription target,
                       LIRGenerationResult lirGenRes,
                       PreAllocationOptimizationContext context) {
        if (shouldSkip(lirGenRes)) {
            return;
        }
                final LIR lir = lirGenRes.getLIR();
        final LIRGeneratorTool lirGen = context.lirGen;

        //markAndCountMethodBounds(lir, lirGen, lirGenRes);

        // One start slot per compilation unit.
        final VirtualStackSlot tscStartSlot =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));

        final long baseAddress = BuboNativeBuffers.activationPtr();
        final int compilationId = lirGenRes.getCompilationId();

        // 1) Find the first AMD64GraphStartOp marker.
        MarkerPos marker = findGraphStartMarker(lir);

        //jdk.graal.compiler.hotspot.meta.Bubo.BuboPmcSetup.ensureInitialized();
        //BuboPmcBridge.hostInitFromAgentProperty();
        //System.out.println("Found Index : " + BuboNativeBuffers.pmcIndexPtr());

        // If no marker exists, nothing to do for this method.
        if (!marker.found()) {
            return;
        }

        // 2) Insert the start capture immediately before that marker.
        insertStartBeforeMarker(lir, context.lirGen, tscStartSlot, marker);

        // 3) Insert an inline end+delta+accumulate before every return.
        instrumentAllReturns(lir, context.lirGen, tscStartSlot, baseAddress, compilationId);
    }


private static void markAndCountMethodBounds(LIR lir,
                                             LIRGeneratorTool lirGen,
                                             LIRGenerationResult lirGenRes) {

    BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();

    for (BasicBlock<?> block : blocks) {
        final List<LIRInstruction> insns = lir.getLIRforBlock(block);
        if (insns.isEmpty()) {
            continue;
        }

        LIRInsertionBuffer buf = new LIRInsertionBuffer();
        buf.init(insns);

        NodeSourcePosition prevPos = insns.get(0).getPosition();

        for (int i = 1; i < insns.size(); i++) {
            LIRInstruction op = insns.get(i);
            NodeSourcePosition curPos = op.getPosition();

            boolean changed = false;

            if (prevPos == null && curPos != null) {
                // we were in "synthetic" land, now we hit a real source
                changed = true;
            } else if (prevPos != null && curPos != null) {
                // both real, compare them
                changed = !prevPos.equals(curPos);
            } else if (prevPos != null && curPos == null) {
                // real → null: treat as same, do NOT mark
                changed = false;
            } else {
                // null → null: no change
                changed = false;
            }

            if (changed) {
                AMD64BuboIncActivationOp incOp =
                        new AMD64BuboIncActivationOp(lirGen, lirGenRes.getCompilationId());
                buf.append(i, incOp);
                prevPos = curPos;
            } else {
                // still update prevPos when curPos is non-null,
                // so future comparisons use the newest real source
                if (curPos != null) {
                    prevPos = curPos;
                }
            }
        }

        buf.finish();
    }
}





    /** Skip stubs; they don’t guarantee extra registers and often aren’t worth instrumenting. */
    private static boolean shouldSkip(LIRGenerationResult lirGenRes) {
        return lirGenRes.getCompilationUnitName().contains("Stub");
    }

    /** Location of the first AMD64ReadTimestampCounter in LIR. */
    private static final class MarkerPos {
        final int blockIndex;
        final int insnIndex;

        MarkerPos(int blockIndex, int insnIndex) {
            this.blockIndex = blockIndex;
            this.insnIndex = insnIndex;
        }
        boolean found() { return blockIndex >= 0; }

        static MarkerPos notFound() { return new MarkerPos(-1, -1); }
    }

    /**
     * Scans blocks in order and returns the first (block, insn) at which an
     * {@link AMD64GraphStartOp} appears.
     */
    private static MarkerPos findGraphStartMarker(LIR lir) {
        BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();
        for (int b = 0; b < blocks.length; b++) {
            List<LIRInstruction> insns = lir.getLIRforBlock(blocks[b]);
            for (int i = 0; i < insns.size(); i++) {
                if (insns.get(i) instanceof AMD64GraphStartOp) {
                    return new MarkerPos(b, i);
                }
            }
        }
        return MarkerPos.notFound();
    }

    /**
     * Inserts a single {@link AMD64BuboRDTSCToSlot} immediately before the given marker
     * so that all paths after the marker can read the same start timestamp from the stack.
     */
    private static void insertStartBeforeMarker(LIR lir,
                                                LIRGeneratorTool lirGen,
                                                VirtualStackSlot tscStartSlot,
                                                MarkerPos marker) {
        BasicBlock<?> block = lir.getControlFlowGraph().getBlocks()[marker.blockIndex];
        List<LIRInstruction> insns = lir.getLIRforBlock(block);

        LIRInsertionBuffer buf = new LIRInsertionBuffer();
        buf.init(insns);
        buf.append(marker.insnIndex, new AMD64BuboRDTSCToSlot(lirGen, tscStartSlot));
        buf.finish();
    }

    /**
     * For every {@link AMD64HotSpotReturnOp}, insert an inline end+delta+accumulate op that:
     *   - reads the start from {@code tscStartSlot},
     *   - reads the end via RDTSC inside the op,
     *   - computes (end - start),
     *   - atomically adds into the native buffer slot for this compilation unit.
     */
    private static void instrumentAllReturns(LIR lir,
                                             LIRGeneratorTool lirGen,
                                             VirtualStackSlot tscStartSlot,
                                             long baseAddress,
                                             int compilationId) {
        BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();

        for (BasicBlock<?> block : blocks) {
            List<LIRInstruction> insns = lir.getLIRforBlock(block);
            LIRInsertionBuffer buf = new LIRInsertionBuffer();
            buf.init(insns);

            for (int i = 0; i < insns.size(); i++) {
                if (insns.get(i) instanceof AMD64HotSpotReturnOp) {
                    //Fresh instance per return
                    AMD64BuboWriteDeltaRDTSC endDelta =
                            new AMD64BuboWriteDeltaRDTSC(
                                    lirGen,
                                    tscStartSlot,
                                    baseAddress,
                                    compilationId,
                                    /* atomic = */ true);

                   // Insert before the return.
                   buf.append(i, endDelta);
                }
            }

            buf.finish();
        }
    }
}
