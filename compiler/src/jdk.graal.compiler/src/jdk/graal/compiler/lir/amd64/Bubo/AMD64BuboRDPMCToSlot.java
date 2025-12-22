package jdk.graal.compiler.lir.amd64.Bubo;

import jdk.graal.compiler.asm.amd64.AMD64Address;
import jdk.graal.compiler.asm.amd64.AMD64MacroAssembler;
import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeBuffers;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboPmcSetup;
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

import static jdk.vm.ci.amd64.AMD64.rax;
import static jdk.vm.ci.code.ValueUtil.asRegister;

@Opcode("BUBO_RDPMC_TO_SLOT")
public final class AMD64BuboRDPMCToSlot extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64BuboRDPMCToSlot> TYPE =
            LIRInstructionClass.create(AMD64BuboRDPMCToSlot.class);



    // Model architectural clobbers as fixed TEMP regs, 64-bit width.
    @Temp({OperandFlag.REG}) private AllocatableValue raxTmp;
    @Temp({OperandFlag.REG}) private AllocatableValue rdxTmp;
    @Temp({OperandFlag.REG}) private AllocatableValue rcxTmp;
    @Temp({OperandFlag.REG}) private AllocatableValue rcxFixed;

    @Use({OperandFlag.STACK}) private AllocatableValue dstSlot;

    public AMD64BuboRDPMCToSlot(LIRGeneratorTool lirGen, VirtualStackSlot dstSlot) {
        super(TYPE);
        this.dstSlot = dstSlot;
        this.raxTmp = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.rdxTmp = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.rcxTmp = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));

        // tells the compiler we need RCX as a fixed register, so dont allocate to it
        this.rcxFixed = AMD64.rcx.asValue(LIRKind.value(AMD64Kind.QWORD));
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        
        masm.movq(asRegister(raxTmp), AMD64.rax);
        masm.movq(asRegister(rdxTmp), AMD64.rdx);
        masm.movq(asRegister(rcxTmp), AMD64.rcx);

        //masm.movq(AMD64.rax, BuboPmcSetup.pmcIndexPtr());

        //AMD64Address pmcAddr = new AMD64Address(AMD64.rax, 0);
        masm.movl(AMD64.rcx, 0x40000001);

        
        masm.rdpmc();

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
        masm.movq(AMD64.rcx, asRegister(rcxTmp));
    }
}
