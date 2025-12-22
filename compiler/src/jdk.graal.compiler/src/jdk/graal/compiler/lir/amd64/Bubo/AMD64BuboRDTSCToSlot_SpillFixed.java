package jdk.graal.compiler.lir.amd64.Bubo;

import jdk.graal.compiler.asm.amd64.AMD64Address;
import jdk.graal.compiler.asm.amd64.AMD64MacroAssembler;
import jdk.graal.compiler.core.common.LIRKind;
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

@Opcode("BUBO_RDTSC_TO_SLOT_SPILLFIXED")
public final class AMD64BuboRDTSCToSlot_SpillFixed extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64BuboRDTSCToSlot_SpillFixed> TYPE =
            LIRInstructionClass.create(AMD64BuboRDTSCToSlot_SpillFixed.class);

    // Destination stack slot to store the 64-bit TSC
    @Use({OperandFlag.STACK}) private AllocatableValue dstSlot;

    // We WILL touch RAX/RDX because RDTSC writes them.
    // Declare them as temps so Graal knows they are clobbered in this instruction.
    @Temp({OperandFlag.REG}) private AllocatableValue fixedRax;
    @Temp({OperandFlag.REG}) private AllocatableValue fixedRdx;

    // Fresh spill slots to preserve incoming RAX/RDX values
    @Temp({OperandFlag.STACK}) private AllocatableValue saveRaxSlot;
    @Temp({OperandFlag.STACK}) private AllocatableValue saveRdxSlot;

    public final int loopID;

public AMD64BuboRDTSCToSlot_SpillFixed(VirtualStackSlot dstSlot,
                                       VirtualStackSlot saveRaxSlot,
                                       VirtualStackSlot saveRdxSlot,
                                       int loopID)
 {
        super(TYPE);
        this.dstSlot = dstSlot;
        this.loopID = loopID;

        // Fixed architectural regs
        this.fixedRax = AMD64.rax.asValue(LIRKind.value(AMD64Kind.QWORD));
        this.fixedRdx = AMD64.rdx.asValue(LIRKind.value(AMD64Kind.QWORD));

        // Fresh spill slots
        this.saveRaxSlot = saveRaxSlot;
        this.saveRdxSlot = saveRdxSlot;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        // Save incoming RAX/RDX
        AMD64Address saveRaxAddr = (AMD64Address) crb.asAddress(saveRaxSlot);
        AMD64Address saveRdxAddr = (AMD64Address) crb.asAddress(saveRdxSlot);
        masm.movq(saveRaxAddr, AMD64.rax);
        masm.movq(saveRdxAddr, AMD64.rdx);

        // Serialize before RDTSC (your original order was lfence then rdtsc)
        masm.lfence();
        masm.rdtsc();   // EDX:EAX

        // Build 64-bit TSC in RAX
        // (movl rax, eax) zero-extends low 32 into RAX; masm.movl(rax, rax) is fine for that
        masm.movl(AMD64.rax, AMD64.rax);
        masm.movl(AMD64.rdx, AMD64.rdx);
        masm.shlq(AMD64.rdx, 32);
        masm.orq(AMD64.rax, AMD64.rdx);

        // Store to dst stack slot
        AMD64Address dstAddr = (AMD64Address) crb.asAddress(dstSlot);
        masm.movq(dstAddr, AMD64.rax);

        // Restore incoming RAX/RDX
        masm.movq(AMD64.rdx, saveRdxAddr);
        masm.movq(AMD64.rax, saveRaxAddr);
    }
}
