/*
 * Copyright (c) 2025, Oracle…
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package jdk.graal.compiler.lir.amd64.Bubo;

import static jdk.vm.ci.code.ValueUtil.asRegister;

import jdk.graal.compiler.asm.amd64.AMD64Address;
import jdk.graal.compiler.asm.amd64.AMD64MacroAssembler;
import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeBuffers;
import jdk.graal.compiler.lir.ConstantValue;
import jdk.graal.compiler.lir.LIRInstructionClass;
import jdk.graal.compiler.lir.Opcode;
import jdk.graal.compiler.lir.SyncPort;
import jdk.graal.compiler.lir.amd64.AMD64LIRInstruction;
import jdk.graal.compiler.lir.amd64.AMD64Move;
import jdk.graal.compiler.lir.asm.CompilationResultBuilder;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

/**
 * For a given compilation id, writes a given code to a given address in the Bubo native buffer.
 */
@SyncPort(from = "", sha1 = "")
@Opcode("AMD64_BUBO_WRITE")
public final class AMD64BuboWrite extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64BuboWrite> TYPE =
            LIRInstructionClass.create(AMD64BuboWrite.class);

    @Alive({OperandFlag.REG, OperandFlag.CONST}) private Value AddrValue;
    @Temp({OperandFlag.REG}) private Value addrTmp;
    @Temp({OperandFlag.REG}) private Value tmp;

    private int amount;

    public AMD64BuboWrite(LIRGeneratorTool tool,long address, int CompilationId, int amount) {
        super(TYPE);

        this.AddrValue = new ConstantValue(LIRKind.value(AMD64Kind.QWORD), JavaConstant.forLong(address + (((long) CompilationId) << 3)));
        // temps are 64-bit GPRs
        this.addrTmp = tool.newVariable(LIRKind.value(jdk.vm.ci.amd64.AMD64Kind.QWORD));
        this.tmp    = tool.newVariable(LIRKind.value(jdk.vm.ci.amd64.AMD64Kind.QWORD));
        this.amount = amount;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        // move the pointer into a register
        AMD64Move.move(crb, masm, addrTmp, AddrValue);

        // create a temporary AMD64Address for value
        AMD64Address mem = new AMD64Address(asRegister(addrTmp));

        //  load current 64-bit counter value
        masm.movq(asRegister(tmp), mem);

        //  add x the loaded value (non-atomic RMW)
        masm.addq(asRegister(tmp), amount);
        
        //  store the incremented value back
        masm.movq(mem, asRegister(tmp));
    }
}
