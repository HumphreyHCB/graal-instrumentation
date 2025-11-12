package jdk.graal.compiler.lir.constopt;

import static jdk.graal.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.AbstractControlFlowGraph;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeBuffers;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeLoopSourceCache;
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
import jdk.graal.compiler.nodes.cfg.ControlFlowGraph;
import jdk.graal.compiler.options.NestedBooleanOptionKey;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionType;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.TargetDescription;

public final class BuboLIRPhase extends PreAllocationOptimizationPhase {

    public static class Options {
        @Option(help = "Enable Bubo Lir Phase.", type = OptionType.Debug)
        public static final NestedBooleanOptionKey BuboLIRPhase = new NestedBooleanOptionKey(LIROptimization, false);
    }

    private static final class MarkerPos {
        final int blockIndex;
        final int insnIndex;
        final int loopId;
        final NodeSourcePosition pos;
        final boolean LoopStart;

        MarkerPos(int blockIndex, int insnIndex, int loopId, NodeSourcePosition pos, boolean LoopStart) {
            this.blockIndex = blockIndex;
            this.insnIndex = insnIndex;
            this.loopId = loopId;
            this.pos = pos;
            this.LoopStart = LoopStart;
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

        // collect ALL start/end markers, grouped by loopId
        // Map<Integer, List<MarkerPos>> startsById = new HashMap<>();
        // Map<Integer, List<MarkerPos>> endsById = new HashMap<>();
        List<MarkerPos> markers = new ArrayList<>();
        BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();

        for (int block = 0; block < blocks.length; block++) {
            List<LIRInstruction> insns = lir.getLIRforBlock(blocks[block]);
            for (int instruction = 0; instruction < insns.size(); instruction++) {
                LIRInstruction op = insns.get(instruction);

                if (op instanceof AMD64LoopStartOp) {
                    AMD64LoopStartOp StartOp = (AMD64LoopStartOp) op;
                    markers.add(new MarkerPos(block, instruction, StartOp.loopId, StartOp.position, true));
                } else if (op instanceof AMD64LoopEndOp) {
                    AMD64LoopEndOp EndOp = (AMD64LoopEndOp) op;
                    markers.add(new MarkerPos(block, instruction, EndOp.loopId, EndOp.position, false));
                }
            }
        }

        if (markers.isEmpty()) {
            return;
        }

        // allocate spill
        Map<Integer, VirtualStackSlot> loopSlots = new HashMap<>();
        for (MarkerPos marker : markers) {
            if (marker.LoopStart && !loopSlots.containsKey(marker.loopId)) {
                VirtualStackSlot slot = lirGenRes.getFrameMapBuilder()
                        .allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
                loopSlots.put(marker.loopId, slot);
            }
        }

        // insert starts only
        for (MarkerPos marker : markers) {
            if (marker.LoopStart) {
                insertStartBeforeMarker(lir, lirGen, loopSlots.get(marker.loopId), marker);
            }
        }

        // insert ends only for instrumentable ids
        for (MarkerPos marker : markers) {
            if (!marker.LoopStart) {
                instrumentLoopEnds(lir, lirGen, loopSlots.get(marker.loopId), baseAddress, compilationId, marker);
            }
        }
        
    }

    private static void removeBadOrderIds(HashSet<Integer> instrumentableIds,
            Map<Integer, List<MarkerPos>> startsById,
            Map<Integer, List<MarkerPos>> endsById,
            LIRGenerationResult lirGenRes) {
        // drop loopIds whose earliest end is before earliest start
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
                System.out.println("[BUBO]   skip loopId=" + id + " comp=" + lirGenRes.getCompilationUnitName()
                        + " because earliest end(b=" + minEndBlock +
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

    static Map<Integer, String> loopStartSources = new HashMap<>();

    private static void insertStartBeforeMarker(LIR lir,
            LIRGeneratorTool lirGen,
            VirtualStackSlot slot,
            MarkerPos marker) {

        BasicBlock<?> block = lir.getControlFlowGraph().getBlocks()[marker.blockIndex];
        List<LIRInstruction> insns = lir.getLIRforBlock(block);

        loopStartSources.put(marker.loopId, "no source for now");

        LIRInsertionBuffer buf = new LIRInsertionBuffer();
        buf.init(insns);
        buf.append(marker.insnIndex, new AMD64BuboRDTSCToSlot(lirGen, slot));
        buf.finish();
    }

    private static void instrumentLoopEnds(LIR lir,
            LIRGeneratorTool lirGen,
            VirtualStackSlot slot,
            long baseAddress,
            int compilationId,
            MarkerPos marker) {

        BasicBlock<?> block = lir.getControlFlowGraph().getBlocks()[marker.blockIndex];
        List<LIRInstruction> insns = lir.getLIRforBlock(block);

        LIRInsertionBuffer buf = new LIRInsertionBuffer();
        buf.init(insns);

        AMD64BuboWriteDeltaRDTSC endDelta = new AMD64BuboWriteDeltaRDTSC(
                lirGen,
                slot,
                baseAddress,
                compilationId,
                marker.loopId,
                true);

        buf.append(marker.insnIndex, endDelta);

        String startSrc = loopStartSources.get(marker.loopId);
        String endSrc = "no source for now";

        String combined = startSrc + " | " + endSrc;

        BuboNativeLoopSourceCache.add(compilationId, marker.loopId, combined);

        buf.finish();

    }
}
