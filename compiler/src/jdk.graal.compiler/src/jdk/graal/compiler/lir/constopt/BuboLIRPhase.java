/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.graal.compiler.lir.constopt;

import static jdk.graal.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.List;

import jdk.graal.compiler.asm.amd64.AMD64Assembler.AMD64Op;
import jdk.graal.compiler.core.common.CompilationIdentifier;
import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.hotspot.amd64.AMD64HotSpotReturnOp;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeBuffers;
import jdk.graal.compiler.lir.ConstantValue;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInsertionBuffer;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.VirtualStackSlot;
import jdk.graal.compiler.lir.amd64.AMD64Call;
import jdk.graal.compiler.lir.amd64.AMD64ReadTimestampCounter;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboExitLog;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboIncActivationOp;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWrite;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.graal.compiler.lir.phases.PreAllocationOptimizationPhase;
import jdk.graal.compiler.options.NestedBooleanOptionKey;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionType;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

/**
 */
public final class BuboLIRPhase extends PreAllocationOptimizationPhase {

    public static class Options {
        // @formatter:off
        @Option(help = "Enable Bubo Lir Phase.", type = OptionType.Debug)
        public static final NestedBooleanOptionKey BuboLIRPhase = new NestedBooleanOptionKey(LIROptimization, true);
        // @formatter:on
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PreAllocationOptimizationContext context) {
        if (lirGenRes.getCompilationUnitName()
                .contains("Stub")) {
                    // Skip stubs, they garrentee no new registers
            return;
        }

        runTest(target, lirGenRes, context);
        
        
        // final LIR lir = lirGenRes.getLIR();
        // final LIRGeneratorTool lirGen = context.lirGen;
        // //lir.getControlFlowGraph().getBlocks()
        //  for (BasicBlock<?> block : lir.getControlFlowGraph().getBlocks()) {
        //     final List<LIRInstruction> insns = lir.getLIRforBlock((block));

        //     final LIRInsertionBuffer buf = new LIRInsertionBuffer();
        //     buf.init(insns);

        //     for (int i = 0; i < insns.size(); i++) {
        //         LIRInstruction op = insns.get(i);
        //         if (op instanceof AMD64HotSpotReturnOp) {

        //             AMD64BuboExitLog exitLogInstr = new AMD64BuboExitLog(
        //                 lirGen,
        //                 BuboNativeBuffers.activationPtr(), // baseAddress
        //                 lirGenRes.getCompilationId()       // compilationId
        //             );

        //             // Insert *before* the return
        //             buf.append(i, exitLogInstr);

                    
        //         }
        //     }
        //     buf.finish();
        // }
    }


    
    protected void runTest(TargetDescription target,
                    LIRGenerationResult lirGenRes,
                    PreAllocationOptimizationContext context) {

        // Skip stubs (no new regs guaranteed)
        if (lirGenRes.getCompilationUnitName().contains("Stub")) {
            return;
        }

        final LIR lir = lirGenRes.getLIR();
        final LIRGeneratorTool lirGen = context.lirGen;

        // allocate one 64-bit spill slot for the first RDTSC value
        final VirtualStackSlot tscStartSlot =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));

        final VirtualStackSlot tscEndSlot =
                lirGenRes.getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));

        final long baseAddress = BuboNativeBuffers.activationPtr();
        final int compilationId = lirGenRes.getCompilationId();

        boolean insertedForMethod = false; // only once per method

        for (BasicBlock<?> block : lir.getControlFlowGraph().getBlocks()) {
            final List<LIRInstruction> insns = lir.getLIRforBlock(block);

            final LIRInsertionBuffer buf = new LIRInsertionBuffer();
            buf.init(insns);

            for (int i = 0; i < insns.size(); i++) {
                if (insertedForMethod) {
                    break; // we already instrumented one return, done
                }

                LIRInstruction op = insns.get(i);
                if (op instanceof AMD64HotSpotReturnOp) {

                    // 1) start timestamp -> spill slot
                    AMD64BuboRDTSCToSlot tscStart =
                            new AMD64BuboRDTSCToSlot(lirGen, tscStartSlot);

                    AMD64BuboRDTSCToSlot tscEnd =
                            new AMD64BuboRDTSCToSlot(lirGen, tscEndSlot);

                    // 2) end timestamp, delta = end - [spill], add to native buffer

                    AMD64BuboWriteDeltaRDTSC tscDeltaWrite =
                            new AMD64BuboWriteDeltaRDTSC(
                                    lirGen,
                                    tscStartSlot, tscEndSlot,
                                    baseAddress,
                                    compilationId, false
                            );

                    // Insert *before* the return in this precise order.
                    // LIRInsertionBuffer.append(i, ...) inserts before insns[i];
                    // appending twice at the same index preserves order (first appended ends up earlier).
                    buf.append(i, tscStart);
                    buf.append(i, tscEnd);
                    buf.append(i, tscDeltaWrite);

                    insertedForMethod = true;
                    // no "i++" adjustment needed; both were inserted before the return
                }
            }
            buf.finish();

            if (insertedForMethod) {
                break; // we’re done for this method
            }
        }
    }

    // inside your AMD64 LIR generator subclass (so getArithmetic()/emitConstant exist)
    private static Value getrdtscValue(LIRGeneratorTool lirGen, AMD64ReadTimestampCounter rdtsc) {
        // lo32 = EAX, hi32 = EDX
        AllocatableValue lo32 = rdtsc.getLowResult();
        AllocatableValue hi32 = rdtsc.getHighResult();

        // Zero-extend both to 64-bit
        Value lo64 = lirGen.getArithmetic().emitZeroExtend(lo32, 32, 64);
        Value hi64 = lirGen.getArithmetic().emitZeroExtend(hi32, 32, 64);

        // Shift high by 32
        Value shift32 = new ConstantValue(LIRKind.value(AMD64Kind.DWORD), JavaConstant.forInt(32));
        Value hiShifted = lirGen.getArithmetic().emitShl(hi64, shift32);

        // (hi << 32) | lo
        return lirGen.getArithmetic().emitOr(hiShifted, lo64);
    }


}
