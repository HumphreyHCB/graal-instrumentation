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
    @Temp({OperandFlag.REG}) private AllocatableValue end64;    // end timestamp (64-bit)
    @Temp({OperandFlag.REG}) private AllocatableValue start64;  // start timestamp (64-bit)
    @Temp({OperandFlag.REG}) private AllocatableValue delta;    // delta = end - start
    @Temp({OperandFlag.REG}) private AllocatableValue addrTmp;  // buffer address

    // inputs
    @Use({OperandFlag.STACK}) private AllocatableValue startSlot;

    private final JavaConstant addrConst;
    private final boolean atomic;
    private final int loopId;

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

        // allocate temps
        this.raxTmp = AMD64.rax.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.rdxTmp = AMD64.rdx.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.end64   = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.start64 = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.delta   = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.addrTmp = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
    }

@Override
public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
    // Save regs we clobber
    masm.movq(asRegister(raxTmp), AMD64.rax);
    masm.movq(asRegister(rdxTmp), AMD64.rdx);

     // load start from stack
    AMD64Address sAddr = (AMD64Address) crb.asAddress(startSlot);
    masm.movq(asRegister(start64), sAddr);

    // end = rdtscp
    masm.rdtsc();
    masm.lfence();

    // build 64-bit end
    masm.movl(asRegister(end64), AMD64.rax);  // end = low
    masm.movl(AMD64.rdx, AMD64.rdx);          // zero-extend high
    masm.shlq(AMD64.rdx, 32);
    masm.orq(asRegister(end64), AMD64.rdx);

    // delta = end - start
    masm.movq(asRegister(delta), asRegister(end64));
    masm.subq(asRegister(delta), asRegister(start64));

    // ---- branchless validity check ----
    // if (end < start)   delta = 0
    // (this catches “garbage start”, “slot never set”, and genuine underflow)
    masm.xorq(AMD64.rdx, AMD64.rdx);  // rdx = 0
    masm.cmpq(asRegister(end64), asRegister(start64));
    masm.cmovq(ConditionFlag.Below, asRegister(delta), AMD64.rdx);
    // -----------------------------------

    // load buffer address
    AMD64Address literalAddr = (AMD64Address) crb.asLongConstRef(addrConst);
    masm.movq(asRegister(addrTmp), literalAddr);

    AMD64Address mem = new AMD64Address(asRegister(addrTmp));
    if (atomic) {
        masm.lock();
        masm.addq(mem, asRegister(delta));
    } else {
        masm.movq(AMD64.rax, mem);
        masm.addq(AMD64.rax, asRegister(delta));
        masm.movq(mem, AMD64.rax);
    }

    // restore
    masm.movq(AMD64.rdx, asRegister(rdxTmp));
    masm.movq(AMD64.rax, asRegister(raxTmp));
}

}
