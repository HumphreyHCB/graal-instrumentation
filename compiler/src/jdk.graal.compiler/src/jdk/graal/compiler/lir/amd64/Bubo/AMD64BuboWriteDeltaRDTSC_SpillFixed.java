package jdk.graal.compiler.lir.amd64.Bubo;

import jdk.graal.compiler.asm.amd64.AMD64Address;
import jdk.graal.compiler.asm.amd64.AMD64Assembler.ConditionFlag;
import jdk.graal.compiler.asm.amd64.AMD64MacroAssembler;
import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeBuffers;
import jdk.graal.compiler.lir.LIRInstructionClass;
import jdk.graal.compiler.lir.Opcode;
import jdk.graal.compiler.lir.VirtualStackSlot;
import jdk.graal.compiler.lir.amd64.AMD64LIRInstruction;
import jdk.graal.compiler.lir.asm.CompilationResultBuilder;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;

@Opcode("BUBO_RDTSC_DELTA_INLINE_END_SPILLFIXED")
public final class AMD64BuboWriteDeltaRDTSC_SpillFixed extends AMD64LIRInstruction {

    public static final LIRInstructionClass<AMD64BuboWriteDeltaRDTSC_SpillFixed> TYPE =
            LIRInstructionClass.create(AMD64BuboWriteDeltaRDTSC_SpillFixed.class);

    // Input: start timestamp lives in this stack slot (written by your "start" op)
    @Use({OperandFlag.STACK}) private AllocatableValue startSlot;

    // We will use ONLY these fixed regs inside the op:
    //   r9  = start64
    //   r10 = end64 then delta
    //   r11 = addrTmp
    //
    // And we must treat rax/rdx as clobbered because rdtsc writes them.
    @Temp({OperandFlag.REG}) private AllocatableValue fixedR9;
    @Temp({OperandFlag.REG}) private AllocatableValue fixedR10;
    @Temp({OperandFlag.REG}) private AllocatableValue fixedR11;
    @Temp({OperandFlag.REG}) private AllocatableValue fixedRax;
    @Temp({OperandFlag.REG}) private AllocatableValue fixedRdx;

    // Fresh spill slots to preserve the *incoming* values of those regs
    @Temp({OperandFlag.STACK}) private AllocatableValue saveR9Slot;
    @Temp({OperandFlag.STACK}) private AllocatableValue saveR10Slot;
    @Temp({OperandFlag.STACK}) private AllocatableValue saveR11Slot;
    @Temp({OperandFlag.STACK}) private AllocatableValue saveRaxSlot;
    @Temp({OperandFlag.STACK}) private AllocatableValue saveRdxSlot;

    private final JavaConstant addrConst;
    private final boolean atomic;
    public final int loopId;

public AMD64BuboWriteDeltaRDTSC_SpillFixed(VirtualStackSlot startSlot,
                                           VirtualStackSlot saveR9Slot,
                                           VirtualStackSlot saveR10Slot,
                                           VirtualStackSlot saveR11Slot,
                                           VirtualStackSlot saveRaxSlot,
                                           VirtualStackSlot saveRdxSlot,
                                           long baseAddress,
                                           int compilationId,
                                           int loopId,
                                           boolean atomic) {
        super(TYPE);

        this.startSlot = startSlot;
        this.atomic = atomic;
        this.loopId = loopId;

        long addr = BuboNativeBuffers.cyclesLoopAddr(compilationId, loopId);
        this.addrConst = JavaConstant.forLong(addr);

        // Fixed internal regs (deterministic)
        this.fixedR9  = AMD64.r9 .asValue(LIRKind.value(AMD64Kind.QWORD));
        this.fixedR10 = AMD64.r10.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.fixedR11 = AMD64.r11.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.fixedRax = AMD64.rax.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.fixedRdx = AMD64.rdx.asValue(LIRKind.value(AMD64Kind.QWORD));

        // Fresh spill slots (one per reg we preserve)
        this.saveR9Slot  = saveR9Slot;
        this.saveR10Slot = saveR10Slot;
        this.saveR11Slot = saveR11Slot;
        this.saveRaxSlot = saveRaxSlot;
        this.saveRdxSlot = saveRdxSlot;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        // --- Save fixed regs to spill slots ---
        AMD64Address saveR9Addr  = (AMD64Address) crb.asAddress(saveR9Slot);
        AMD64Address saveR10Addr = (AMD64Address) crb.asAddress(saveR10Slot);
        AMD64Address saveR11Addr = (AMD64Address) crb.asAddress(saveR11Slot);
        AMD64Address saveRaxAddr = (AMD64Address) crb.asAddress(saveRaxSlot);
        AMD64Address saveRdxAddr = (AMD64Address) crb.asAddress(saveRdxSlot);

        masm.movq(saveR9Addr,  AMD64.r9);
        masm.movq(saveR10Addr, AMD64.r10);
        masm.movq(saveR11Addr, AMD64.r11);
        masm.movq(saveRaxAddr, AMD64.rax);
        masm.movq(saveRdxAddr, AMD64.rdx);

        // --- r9 = start64 from stack ---
        AMD64Address sAddr = (AMD64Address) crb.asAddress(startSlot);
        masm.movq(AMD64.r9, sAddr);

        // --- rdtsc; lfence ---
        masm.rdtsc();
        masm.lfence();

        // --- Build end64 directly into r10 (no extra temp reg) ---
        // r10 = zero-extended eax
        masm.movl(AMD64.r10, AMD64.rax);
        // rdx = high, shift and OR into r10
        masm.shlq(AMD64.rdx, 32);
        masm.orq(AMD64.r10, AMD64.rdx);  // r10 = end64

        // --- validity check + delta in-place ---
        // Compare end (r10) vs start (r9) BEFORE we destroy end
        masm.xorq(AMD64.rdx, AMD64.rdx);         // rdx = 0
        masm.cmpq(AMD64.r10, AMD64.r9);          // end ? start
        masm.subq(AMD64.r10, AMD64.r9);          // r10 = delta
        masm.cmovq(ConditionFlag.Below, AMD64.r10, AMD64.rdx); // if end<start, delta=0

        // --- r11 = &counter ---
        AMD64Address literalAddr = (AMD64Address) crb.asLongConstRef(addrConst);
        masm.movq(AMD64.r11, literalAddr);

        // --- update counter ---
        AMD64Address mem = new AMD64Address(AMD64.r11);
        if (atomic) {
            masm.lock();
            masm.addq(mem, AMD64.r10); // [r11] += delta
        } else {
            // Non-atomic RMW (only correct if you accept lost updates)
            masm.movq(AMD64.rax, mem);
            masm.addq(AMD64.rax, AMD64.r10);
            masm.movq(mem, AMD64.rax);
        }

        // --- Restore fixed regs from spill slots ---
        masm.movq(AMD64.rdx, saveRdxAddr);
        masm.movq(AMD64.rax, saveRaxAddr);
        masm.movq(AMD64.r11, saveR11Addr);
        masm.movq(AMD64.r10, saveR10Addr);
        masm.movq(AMD64.r9,  saveR9Addr);
    }
}
