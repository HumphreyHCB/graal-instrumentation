package jdk.graal.compiler.lir.phases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.VirtualStackSlot;
import jdk.graal.compiler.lir.amd64.AMD64LoopEndOp;
import jdk.graal.compiler.lir.amd64.AMD64LoopStartOp;
import jdk.graal.compiler.lir.amd64.Bubo.BuboProbeFrameInfo;
import jdk.graal.compiler.lir.asm.CompilationResultBuilder;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.options.OptionValues;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.TargetDescription;



public final class BuboLIRPhasePreAllocateFrames extends PreAllocationOptimizationPhase {

    @Override
    protected void run(TargetDescription target,
                       LIRGenerationResult lirGenRes,
                       PreAllocationOptimizationContext context) {

        if (shouldSkip(lirGenRes)) {
            return;
        }

        final LIR lir = lirGenRes.getLIR();
        final int compilationId = lirGenRes.getCompilationId();
        final BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();

        // Collect loop IDs that have markers (you can restrict this if you only want “instrumentable” loops)
        List<Integer> loopIds = new ArrayList<>();
        for (BasicBlock<?> b : blocks) {
            List<LIRInstruction> insns = lir.getLIRforBlock(b);
            if (insns == null) continue;

            for (LIRInstruction op : insns) {
                if (op instanceof AMD64LoopStartOp s) {
                    loopIds.add(s.loopId);
                } else if (op instanceof AMD64LoopEndOp e) {
                    loopIds.add(e.loopId);
                }
            }
        }

        if (loopIds.isEmpty()) {
            return;
        }

        // ---- Allocate all stack slots ONCE ----
        Map<Integer, VirtualStackSlot> loopStartSlots = new HashMap<>();

        for (Integer id : loopIds) {
            loopStartSlots.computeIfAbsent(id, k ->
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD))
            );
        }

        // Shared save slots: START probe saves only RAX/RDX
        VirtualStackSlot startSaveRax =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        VirtualStackSlot startSaveRdx =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));

        // Shared save slots: END probe saves r9/r10/r11/rax/rdx
        VirtualStackSlot endSaveR9  =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        VirtualStackSlot endSaveR10 =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        VirtualStackSlot endSaveR11 =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        VirtualStackSlot endSaveRax =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        VirtualStackSlot endSaveRdx =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));

        BuboProbeFrameInfo.put(compilationId, new BuboProbeFrameInfo(
                loopStartSlots,
                startSaveRax, startSaveRdx,
                endSaveR9, endSaveR10, endSaveR11, endSaveRax, endSaveRdx
        ));
    }

    private static boolean shouldSkip(LIRGenerationResult lirGenRes) {
        String name = lirGenRes.getCompilationUnitName();
        return name.contains("Stub") || name.contains("HotSpotOSRCompilation");
    }
}
