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
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

import static jdk.vm.ci.code.ValueUtil.asRegister;

@Opcode("BUBO_RDTSC_DELTA_INLINE_END")
public final class AMD64BuboWriteDeltaRDTSC extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64BuboWriteDeltaRDTSC> TYPE =
            LIRInstructionClass.create(AMD64BuboWriteDeltaRDTSC.class);

    // temps / clobbers
    @Temp({OperandFlag.REG}) private AllocatableValue raxTmp;   // save/restore RAX
    @Temp({OperandFlag.REG}) private AllocatableValue rdxTmp;   // save/restore RDX
    //@Temp({OperandFlag.REG}) private AllocatableValue end64;    // end timestamp (64-bit)
    @Temp({OperandFlag.REG}) private AllocatableValue start64;  // start timestamp (64-bit)
    @Temp({OperandFlag.REG}) private AllocatableValue delta;    // delta = end - start
    @Temp({OperandFlag.REG}) private AllocatableValue addrTmp;  // buffer address

    // inputs
    @Use({OperandFlag.STACK}) private AllocatableValue startSlot;

    private final JavaConstant addrConst;
    private final boolean atomic;
    public final int loopId;

    public AMD64BuboWriteDeltaRDTSC(
            LIRGeneratorTool lirGen,
            VirtualStackSlot startSlot,
            long baseAddress,
            int compilationId,  int loopId,
            boolean atomic) {
        super(TYPE);
        this.startSlot = startSlot;
        this.atomic = atomic;
        this.loopId = loopId;

        long addr = BuboNativeBuffers.cyclesLoopAddr(compilationId, loopId);
        //long addr = baseAddress + ((long) compilationId) * 8L;
        this.addrConst = JavaConstant.forLong(addr);

        LIRKind qword = LIRKind.value(AMD64Kind.QWORD);
        // allocate temps
        this.raxTmp = AMD64.rax.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.rdxTmp = AMD64.rdx.asValue(LIRKind.value(AMD64Kind.QWORD));
        //this.end64   = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.start64 = AMD64.r9.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.delta   = AMD64.r10.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.addrTmp = AMD64.r11.asValue(LIRKind.value(AMD64Kind.QWORD));
    }

@Override
public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
    // // Save regs we clobber



    // end = rdtscp
    masm.lfence();
    masm.rdtsc();
    masm.lfence();

     // load start from stack
    AMD64Address sAddr = (AMD64Address) crb.asAddress(startSlot);
    masm.movq(asRegister(start64), sAddr);

    // r10 = end64 (low in eax, high in edx)
    masm.movl(AMD64.r10, AMD64.rax);   // r10 = zero-extended eax
    masm.shlq(AMD64.rdx, 32);
    masm.orq(AMD64.r10, AMD64.rdx);    // r10 = end64

    // validity check needs end vs start BEFORE we destroy end
    // We'll set rdx=0 and use cmovb to later clear delta if end<start.
    masm.xorq(AMD64.rdx, AMD64.rdx);           // rdx = 0 (also our cmov source)
    masm.cmpq(AMD64.r10, AMD64.r9);            // compare end vs start
    // Now compute delta = end - start
    masm.subq(AMD64.r10, AMD64.r9);            // r10 = delta
    // If end < start (Below), clear delta
    masm.cmovq(ConditionFlag.Below, AMD64.r10, AMD64.rdx);

    // r11 = &counter
    AMD64Address literalAddr = (AMD64Address) crb.asLongConstRef(addrConst);
    masm.movq(AMD64.r11, literalAddr);

    // atomic add [r11] += r10
    AMD64Address mem = new AMD64Address(AMD64.r11);
    masm.lock();
    masm.addq(mem, AMD64.r10);
}

}
