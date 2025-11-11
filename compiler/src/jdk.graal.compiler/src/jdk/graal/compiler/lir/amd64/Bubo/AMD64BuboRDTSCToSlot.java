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
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;

import static jdk.vm.ci.code.ValueUtil.asRegister;

@Opcode("BUBO_RDTSC_TO_SLOT")
public final class AMD64BuboRDTSCToSlot extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64BuboRDTSCToSlot> TYPE =
            LIRInstructionClass.create(AMD64BuboRDTSCToSlot.class);

    // Use fixed architectural regs for reading the post-RDTSC values.

    // Model architectural clobbers as fixed TEMP regs, 64-bit width
    @Temp({OperandFlag.REG}) private AllocatableValue raxTmp;
    @Temp({OperandFlag.REG}) private AllocatableValue rdxTmp;

    @Use({OperandFlag.STACK}) private AllocatableValue dstSlot;

    public AMD64BuboRDTSCToSlot(LIRGeneratorTool lirGen, VirtualStackSlot dstSlot) {
        super(TYPE);
        this.dstSlot = dstSlot;
        this.raxTmp = AMD64.rax.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.rdxTmp = AMD64.rdx.asValue(LIRKind.value(AMD64Kind.QWORD));
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        // Preserve method return in RAX (live across return paths)
        masm.movq(asRegister(raxTmp), AMD64.rax);
         masm.movq(asRegister(rdxTmp), AMD64.rdx);

        // rdtsc -> EDX:EAX (low in EAX, high in EDX)
        masm.rdtsc();

        // Zero-extend halves to 64-bit and combine into RAX:
        // movl rax, eax  (zero-extend low 32 into RAX)
        masm.movl(AMD64.rax, AMD64.rax);
        // movl rdx, edx  (zero-extend high 32 into RDX)
        masm.movl(AMD64.rdx, AMD64.rdx);
        // RDX <<= 32; RAX |= RDX
        masm.shlq(AMD64.rdx, 32);
        masm.orq(AMD64.rax, AMD64.rdx);

        // Store 64-bit TSC (in RAX) to the stack slot
        AMD64Address addr = (AMD64Address) crb.asAddress(dstSlot);
        masm.movq(addr, AMD64.rax);

        // Restore original return value to RAX
        masm.movq(AMD64.rdx, asRegister(rdxTmp));
        masm.movq(AMD64.rax, asRegister(raxTmp));
    }
}
