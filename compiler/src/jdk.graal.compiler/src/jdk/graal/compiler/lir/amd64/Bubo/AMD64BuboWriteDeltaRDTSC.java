package jdk.graal.compiler.lir.amd64.Bubo;

import jdk.graal.compiler.asm.amd64.AMD64Address;
import jdk.graal.compiler.asm.amd64.AMD64MacroAssembler;
import jdk.graal.compiler.core.common.LIRKind;
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
    @Temp({OperandFlag.REG}) private AllocatableValue rcxTmp;   // save/restore RCX
    @Temp({OperandFlag.REG}) private AllocatableValue end64;    // end timestamp (64-bit)
    @Temp({OperandFlag.REG}) private AllocatableValue start64;  // start timestamp (64-bit)
    @Temp({OperandFlag.REG}) private AllocatableValue delta;    // delta = end - start
    @Temp({OperandFlag.REG}) private AllocatableValue addrTmp;  // buffer address

    // inputs
    @Use({OperandFlag.STACK}) private AllocatableValue startSlot;

    private final JavaConstant addrConst;
    private final boolean atomic;

    public AMD64BuboWriteDeltaRDTSC(
            LIRGeneratorTool lirGen,
            VirtualStackSlot startSlot,
            long baseAddress,
            int compilationId,
            boolean atomic) {
        super(TYPE);
        this.startSlot = startSlot;
        this.atomic = atomic;

        long addr = baseAddress + ((long) compilationId) * 8L;
        this.addrConst = JavaConstant.forLong(addr);

        // allocate temps
        this.raxTmp  = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.rdxTmp  = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.rcxTmp  = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.end64   = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.start64 = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.delta   = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.addrTmp = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        // Save caller-visible regs we’ll clobber
        masm.movq(asRegister(raxTmp), AMD64.rax);
        masm.movq(asRegister(rdxTmp), AMD64.rdx);
        masm.movq(asRegister(rcxTmp), AMD64.rcx);

        // === End timestamp: RDTSCP; LFENCE ===
        masm.rdtscp();          // EDX:EAX, ECX=TSC_AUX
        masm.lfence();

        // end64 = ((uint64)EDX << 32) | (uint32)EAX
        masm.movl(asRegister(end64), AMD64.rax);   // zero-extend EAX -> end64
        masm.movl(AMD64.rdx, AMD64.rdx);           // zero-extend EDX
        masm.shlq(AMD64.rdx, 32);
        masm.orq(asRegister(end64), AMD64.rdx);

        // start64 = [startSlot]
        AMD64Address sAddr = (AMD64Address) crb.asAddress(startSlot);
        masm.movq(asRegister(start64), sAddr);

        // delta = end64 - start64   (unsigned wrap if ever needed)
        masm.movq(asRegister(delta), asRegister(end64));
        masm.subq(asRegister(delta), asRegister(start64));

        // addrTmp = *(constptr addrConst)
        AMD64Address literalAddr = (AMD64Address) crb.asLongConstRef(addrConst);
        masm.movq(asRegister(addrTmp), literalAddr);

        // *(addrTmp) += delta
        AMD64Address mem = new AMD64Address(asRegister(addrTmp));
        if (atomic) {
            masm.lock();
            masm.addq(mem, asRegister(delta));
        } else {
            // non-atomic: read-modify-write
            masm.movq(AMD64.rax, mem);
            masm.addq(AMD64.rax, asRegister(delta));
            masm.movq(mem, AMD64.rax);
        }

        // Restore regs
        masm.movq(AMD64.rcx, asRegister(rcxTmp));
        masm.movq(AMD64.rdx, asRegister(rdxTmp));
        masm.movq(AMD64.rax, asRegister(raxTmp));
    }
}
