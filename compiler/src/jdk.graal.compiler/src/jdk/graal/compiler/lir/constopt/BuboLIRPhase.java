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
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes,
            PreAllocationOptimizationContext context) {
        if (lirGenRes.getCompilationUnitName()
                .contains("Stub")) {
            // Skip stubs, they garrentee no new registers
            return;
        }

        // final LIR lir = lirGenRes.getLIR();
        // final LIRGeneratorTool lirGen = context.lirGen;

        // BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();

        // for (int b = 0; b < blocks.length; b++) {
        // final BasicBlock<?> block = blocks[b];
        // final List<LIRInstruction> insns = lir.getLIRforBlock(block);

        // final LIRInsertionBuffer buf = new LIRInsertionBuffer();
        // buf.init(insns);

        // for (int i = 0; i < insns.size(); i++) {
        // LIRInstruction op = insns.get(i);

        // if (op instanceof AMD64ReadTimestampCounter) {
        // // Try to get a stable block id if available; fall back to block.toString()
        // String blockId;
        // try {
        // blockId = "id=" + block.getId();
        // } catch (Throwable t) {
        // blockId = block.toString(); // safe fallback
        // }

        // System.out.printf(
        // "Found AMD64ReadTimestampCounter in method %s | block %d/%d (%s) | insn %d/%d
        // | op=%s%n",
        // lirGenRes.getCompilationUnitName(),
        // b + 1, blocks.length,
        // blockId,
        // i + 1, insns.size(),
        // op
        // );
        // }
        // }

        // buf.finish();
        // }

        runTest(target, lirGenRes, context);

    }

    protected void runTest(TargetDescription target,
            LIRGenerationResult lirGenRes,
            PreAllocationOptimizationContext context) {

        final LIR lir = lirGenRes.getLIR();
        final LIRGeneratorTool lirGen = context.lirGen;

        final VirtualStackSlot tscStartSlot = lirGenRes.getFrameMapBuilder()
                .allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        final VirtualStackSlot tscEndSlot = lirGenRes.getFrameMapBuilder()
                .allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));

        final long baseAddress = BuboNativeBuffers.activationPtr();
        final int compilationId = lirGenRes.getCompilationId();

        // 1) First pass: find the FIRST AMD64ReadTimestampCounter (your start marker)
        BasicBlock<?>[] blocks = lir.getControlFlowGraph().getBlocks();
        int startBlockIdx = -1;
        int startInsnIdx = -1;

        for (int b = 0; b < blocks.length; b++) {
            BasicBlock<?> block = blocks[b];
            List<LIRInstruction> insns = lir.getLIRforBlock(block);
            for (int i = 0; i < insns.size(); i++) {
                if (insns.get(i) instanceof AMD64ReadTimestampCounter) {
                    startBlockIdx = b;
                    startInsnIdx = i;
                    break;
                }
            }
            if (startBlockIdx >= 0)
                break;
        }

        //If no start found, do nothing for this method
        if (startBlockIdx < 0) {
            return;
        }

        //2) Insert START just before that AMD64ReadTimestampCounter
        {
            BasicBlock<?> sb = blocks[startBlockIdx];
            List<LIRInstruction> sin = lir.getLIRforBlock(sb);
            LIRInsertionBuffer sbuf = new LIRInsertionBuffer();
            sbuf.init(sin);

            AMD64BuboRDTSCToSlot tscStart = new AMD64BuboRDTSCToSlot(lirGen, tscStartSlot);
            sbuf.append(startInsnIdx, tscStart); // insert directly before the marker

            sbuf.finish();
        }

        // 3) Insert END+DELTA before the first Return op (keep terminator last)
        boolean endInserted = false;

        for (int b = 0; b < blocks.length && !endInserted; b++) {
            BasicBlock<?> block = blocks[b];
            List<LIRInstruction> insns = lir.getLIRforBlock(block);

            LIRInsertionBuffer buf = new LIRInsertionBuffer();
            buf.init(insns);

            for (int i = 0; i < insns.size(); i++) {
                LIRInstruction op = insns.get(i);

                if (op instanceof AMD64HotSpotReturnOp) {
                    //AMD64BuboRDTSCToSlot tscStart = new AMD64BuboRDTSCToSlot(lirGen, tscStartSlot);

                    AMD64BuboRDTSCToSlot tscEnd = new AMD64BuboRDTSCToSlot(lirGen, tscEndSlot);

                    AMD64BuboWriteDeltaRDTSC tscDeltaWrite = new AMD64BuboWriteDeltaRDTSC(
                            lirGen,
                            tscStartSlot, tscEndSlot,
                            baseAddress,
                            compilationId,
                            /* atomic = */ false // safer single-instruction update
                    );

                    // Insert BEFORE the return (return must remain last)
                    buf.append(i, tscDeltaWrite); // will end up closest to the return
                    buf.append(i, tscEnd);
                    //buf.append(i, tscStart);      // will execute first of the three
                    endInserted = true;
                    break;
                }
            }

            buf.finish();
        }

    }

}
