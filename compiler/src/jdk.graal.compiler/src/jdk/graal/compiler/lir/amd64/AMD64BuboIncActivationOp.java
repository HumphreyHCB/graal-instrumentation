/*
 * Copyright (c) 2025, Oracle…
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package jdk.graal.compiler.lir.amd64;

import static jdk.vm.ci.code.ValueUtil.asRegister;

import jdk.graal.compiler.asm.amd64.AMD64Address;
import jdk.graal.compiler.asm.amd64.AMD64MacroAssembler;
import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.lir.LIRInstructionClass;
import jdk.graal.compiler.lir.Opcode;
import jdk.graal.compiler.lir.SyncPort;
import jdk.graal.compiler.lir.asm.CompilationResultBuilder;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

/**
 * *(activationAddress) += 1L  (non-atomic)
 */
@SyncPort(from = "", sha1 = "")
@Opcode("AMD64_BUBO_INC_ACT")
public final class AMD64BuboIncActivationOp extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64BuboIncActivationOp> TYPE =
            LIRInstructionClass.create(AMD64BuboIncActivationOp.class);

    @Alive({OperandFlag.REG, OperandFlag.CONST}) private Value activationAddrValue;
    @Temp({OperandFlag.REG}) private Value addrTmp;
    @Temp({OperandFlag.REG}) private Value tmp;

    public AMD64BuboIncActivationOp(LIRGeneratorTool tool, Value activationAddr) {
        super(TYPE);
        this.activationAddrValue = activationAddr;
        // temps are 64-bit GPRs
        this.addrTmp = tool.newVariable(LIRKind.value(jdk.vm.ci.amd64.AMD64Kind.QWORD));
        this.tmp    = tool.newVariable(LIRKind.value(jdk.vm.ci.amd64.AMD64Kind.QWORD));
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        // move the pointer into a register
        AMD64Move.move(crb, masm, addrTmp, activationAddrValue);

        // create a temporary AMD64Address for value
        AMD64Address mem = new AMD64Address(asRegister(addrTmp));

        //  load current 64-bit counter value
        masm.movq(asRegister(tmp), mem);

        //  increment the loaded value (non-atomic RMW)
        masm.addq(asRegister(tmp), 1);

        //  store the incremented value back
        masm.movq(mem, asRegister(tmp));
    }
}
