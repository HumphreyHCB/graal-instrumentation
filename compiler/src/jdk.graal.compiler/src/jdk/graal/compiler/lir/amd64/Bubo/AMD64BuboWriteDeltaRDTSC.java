package jdk.graal.compiler.lir.amd64.Bubo;

import jdk.graal.compiler.asm.amd64.AMD64Address;
import jdk.graal.compiler.asm.amd64.AMD64MacroAssembler;
import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.lir.ConstantValue;
import jdk.graal.compiler.lir.LIRInstructionClass;
import jdk.graal.compiler.lir.Opcode;
import jdk.graal.compiler.lir.VirtualStackSlot;
import jdk.graal.compiler.lir.amd64.AMD64LIRInstruction;
import jdk.graal.compiler.lir.asm.CompilationResultBuilder;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

import static jdk.vm.ci.code.ValueUtil.asRegister;

@Opcode("BUBO_RDTSC_DELTA_WRITE")
public final class AMD64BuboWriteDeltaRDTSC extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64BuboWriteDeltaRDTSC> TYPE =
            LIRInstructionClass.create(AMD64BuboWriteDeltaRDTSC.class);

    @Temp({OperandFlag.REG}) private AllocatableValue startV;
    @Temp({OperandFlag.REG}) private AllocatableValue endV;
    @Temp({OperandFlag.REG}) private AllocatableValue delta;
    @Temp({OperandFlag.REG}) private AllocatableValue addrTmp;
    @Temp({OperandFlag.REG}) private AllocatableValue tmp;

     @Use({OperandFlag.STACK}) private AllocatableValue startSlot;
    @Use({OperandFlag.STACK}) private AllocatableValue endSlot;
    private final JavaConstant addrConst;
    private final boolean atomic;

    public AMD64BuboWriteDeltaRDTSC(
            LIRGeneratorTool lirGen,
            VirtualStackSlot startSlot,
            VirtualStackSlot endSlot,
            long baseAddress,
            int compilationId,
            boolean atomic) {
        super(TYPE);
        this.startSlot = startSlot;
        this.endSlot   = endSlot;
        this.atomic    = atomic;

        long addr = baseAddress + (((long) compilationId) << 3);
        this.addrConst = JavaConstant.forLong(addr);

        this.startV  = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.endV    = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.delta   = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.addrTmp = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
        this.tmp     = lirGen.newVariable(LIRKind.value(AMD64Kind.QWORD));
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        AMD64Address sAddr = (AMD64Address) crb.asAddress(startSlot);
        AMD64Address eAddr = (AMD64Address) crb.asAddress(endSlot);

        // load start/end
        masm.movq(asRegister(startV), sAddr);
        masm.movq(asRegister(endV),   eAddr);

        // delta = end - start
        masm.movq(asRegister(delta), asRegister(endV));
        masm.subq(asRegister(delta), asRegister(startV));

        
        AMD64Address literalAddr = (AMD64Address) crb.asLongConstRef(addrConst);
        masm.movq(asRegister(addrTmp), literalAddr);

        // create the address
        AMD64Address mem = new AMD64Address(asRegister(addrTmp));


        if (atomic) {
            masm.lock();
            masm.addq(mem, asRegister(delta));
        } else {
            masm.movq(asRegister(tmp), mem);
            masm.addq(asRegister(tmp), asRegister(delta));
            masm.movq(mem, asRegister(tmp));
        }
    }
}
