package jdk.graal.compiler.lir.constopt;

import static jdk.graal.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeBuffers;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInsertionBuffer;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.VirtualStackSlot;
import jdk.graal.compiler.lir.amd64.AMD64LoopEndOp;
import jdk.graal.compiler.lir.amd64.AMD64LoopStartOp;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.graal.compiler.lir.phases.PreAllocationOptimizationPhase;
import jdk.graal.compiler.options.NestedBooleanOptionKey;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionType;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.TargetDescription;

public final class BuboLIRPhase extends PreAllocationOptimizationPhase {

    public static class Options {
        @Option(help = "Enable Bubo Lir Phase.", type = OptionType.Debug)
        public static final NestedBooleanOptionKey BuboLIRPhase =
                new NestedBooleanOptionKey(LIROptimization, false);
    }

    private static final class MarkerPos {
        final int blockIndex;
        final int insnIndex;
        final int loopId;
        MarkerPos(int blockIndex, int insnIndex, int loopId) {
            this.blockIndex = blockIndex;
            this.insnIndex = insnIndex;
            this.loopId = loopId;
        }
        @Override
        public String toString() {
            return "b=" + blockIndex + ", insn=" + insnIndex;
        }
    }

    @Override
    protected void run(TargetDescription target,
                       LIRGenerationResult lirGenRes,
                       PreAllocationOptimizationContext context) {
        if (shouldSkip(lirGenRes)) {
            return;
        }

        final LIR lir = lirGenRes.getLIR();
        final LIRGeneratorTool lirGen = context.lirGen;
        final long baseAddress = BuboNativeBuffers.activationPtr();
        final int compilationId = lirGenRes.getCompilationId();

        //  collect ALL start/end markers, grouped by loopId
        Map<Integer, List<MarkerPos>> startsById = new HashMap<>();
        Map<Integer, List<MarkerPos>> endsById   = new HashMap<>();
        BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();
        for (int b = 0; b < blocks.length; b++) {
            List<LIRInstruction> insns = lir.getLIRforBlock(blocks[b]);
            for (int i = 0; i < insns.size(); i++) {
                LIRInstruction op = insns.get(i);
                if (op instanceof AMD64LoopStartOp) {
                    int id = ((AMD64LoopStartOp) op).loopId;
                    startsById.computeIfAbsent(id, k -> new ArrayList<>())
                              .add(new MarkerPos(b, i, id));
                } else if (op instanceof AMD64LoopEndOp) {
                    int id = ((AMD64LoopEndOp) op).loopId;
                    endsById.computeIfAbsent(id, k -> new ArrayList<>())
                            .add(new MarkerPos(b, i, id));
                }
            }
        }


        // loopIds that have BOTH starts and ends
        HashSet<Integer> instrumentableIds = new HashSet<>();
        instrumentableIds.addAll(startsById.keySet());
        instrumentableIds.retainAll(endsById.keySet());

        //  drop loopIds whose earliest end is before earliest start
        removeBadOrderIds(instrumentableIds, startsById, endsById, lirGenRes);

        if (instrumentableIds.isEmpty()) {return;}

        // allocate spill for each instrumentable loopId
        Map<Integer, VirtualStackSlot> loopSlots = new HashMap<>();
        for (int id : instrumentableIds) {
            VirtualStackSlot slot = lirGenRes.getFrameMapBuilder()
                    .allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
            loopSlots.put(id, slot);
        }

        // insert starts only for instrumentable ids
        for (int id : instrumentableIds) {
            for (MarkerPos m : startsById.get(id)) {
                insertStartBeforeMarker(lir, lirGen, loopSlots.get(id), m);
            }
        }

        // insert ends only for instrumentable ids
        instrumentLoopEnds(lir, lirGen, loopSlots, baseAddress, compilationId, instrumentableIds);
    }

    private static void removeBadOrderIds(HashSet<Integer> instrumentableIds,
                                          Map<Integer, List<MarkerPos>> startsById,
                                          Map<Integer, List<MarkerPos>> endsById,
                                          LIRGenerationResult lirGenRes) {
                //  drop loopIds whose earliest end is before earliest start
        HashSet<Integer> badOrderIds = new HashSet<>();
        for (int id : instrumentableIds) {
            List<MarkerPos> sList = startsById.get(id);
            List<MarkerPos> eList = endsById.get(id);

            int minStartBlock = Integer.MAX_VALUE;
            for (MarkerPos mp : sList) {
                if (mp.blockIndex < minStartBlock) {
                    minStartBlock = mp.blockIndex;
                }
            }

            int minEndBlock = Integer.MAX_VALUE;
            for (MarkerPos mp : eList) {
                if (mp.blockIndex < minEndBlock) {
                    minEndBlock = mp.blockIndex;
                }
            }

            if (minEndBlock < minStartBlock) {
                // skip this loop entirely
                System.out.println("[BUBO]   skip loopId=" + id +" comp="+ lirGenRes.getCompilationUnitName() + " because earliest end(b=" + minEndBlock +
                                   ") < earliest start(b=" + minStartBlock + ")");
                badOrderIds.add(id);
            }
        }
        instrumentableIds.removeAll(badOrderIds);
    }

    private static boolean shouldSkip(LIRGenerationResult lirGenRes) {
        String name = lirGenRes.getCompilationUnitName();
        return name.contains("Stub") || name.contains("HotSpotOSRCompilation");
    }

    private static void insertStartBeforeMarker(LIR lir,
                                                LIRGeneratorTool lirGen,
                                                VirtualStackSlot slot,
                                                MarkerPos marker) {
        BasicBlock<?> block = lir.getControlFlowGraph().getBlocks()[marker.blockIndex];
        List<LIRInstruction> insns = lir.getLIRforBlock(block);

        LIRInsertionBuffer buf = new LIRInsertionBuffer();
        buf.init(insns);
        buf.append(marker.insnIndex, new AMD64BuboRDTSCToSlot(lirGen, slot));
        buf.finish();
    }

    private static void instrumentLoopEnds(LIR lir,
                                           LIRGeneratorTool lirGen,
                                           Map<Integer, VirtualStackSlot> loopSlots,
                                           long baseAddress,
                                           int compilationId,
                                           HashSet<Integer> instrumentableIds) {

        BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();

        for (BasicBlock<?> block : blocks) {
            List<LIRInstruction> insns = lir.getLIRforBlock(block);
            LIRInsertionBuffer buf = new LIRInsertionBuffer();
            buf.init(insns);

            for (int i = 0; i < insns.size(); i++) {
                LIRInstruction op = insns.get(i);
                if (op instanceof AMD64LoopEndOp) {
                    int loopId = ((AMD64LoopEndOp) op).loopId;
                    if (!instrumentableIds.contains(loopId)) {
                        // no start
                        continue;
                    }
                    VirtualStackSlot slot = loopSlots.get(loopId);
                    if (slot == null) {
                        continue;
                    }

                    AMD64BuboWriteDeltaRDTSC endDelta =
                            new AMD64BuboWriteDeltaRDTSC(
                                    lirGen,
                                    slot,
                                    baseAddress,
                                    compilationId,
                                    loopId,
                                    true);

                    buf.append(i, endDelta);
                }
            }

            buf.finish();
        }
    }
}
