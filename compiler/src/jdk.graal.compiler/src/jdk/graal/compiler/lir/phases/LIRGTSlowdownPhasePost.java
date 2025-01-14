/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.graal.compiler.lir.phases;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdk.graal.compiler.core.common.cfg.AbstractControlFlowGraph;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.hotspot.amd64.AMD64HotSpotReturnOp;
import jdk.graal.compiler.hotspot.amd64.AMD64HotSpotSafepointOp;
import jdk.graal.compiler.hotspot.amd64.GTBlockSlowDownLookUp;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionCostMultiLookup;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionVectorLookup;
import jdk.graal.compiler.lir.amd64.AMD64Call.DirectCallOp;
import jdk.graal.compiler.lir.amd64.AMD64ControlFlow.TestByteBranchOp;
import jdk.graal.compiler.lir.amd64.AMD64Move;
import jdk.graal.compiler.lir.amd64.AMD64Move.CompressPointerOp;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.StandardOp.JumpOp;
import jdk.graal.compiler.lir.amd64.AMD64Nop;
import jdk.graal.compiler.lir.amd64.AMD64Nops;
import jdk.graal.compiler.lir.amd64.AMD64PointLess;
import jdk.graal.compiler.lir.amd64.AMD64PointLesss;
import jdk.graal.compiler.lir.amd64.AMD64PointLessReg;
import jdk.graal.compiler.lir.amd64.AMD64SFence;
import jdk.graal.compiler.lir.amd64.g1.AMD64G1PostWriteBarrierOp;
import jdk.graal.compiler.lir.amd64.g1.AMD64G1PreWriteBarrierOp;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.options.OptionType;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.graal.compiler.core.common.CompilationIdentifier;
import jdk.graal.compiler.lir.amd64.AMD64Move.UncompressPointerOp;

public class LIRGTSlowdownPhasePost extends PostAllocationOptimizationPhase {

    private OptionValues options;

    LIRGTSlowdownPhasePost(OptionValues poptions) {
        options = poptions;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes,
            PostAllocationOptimizationContext context) {
        if (lirGenRes.getCompilationUnitName(CompilationIdentifier.Verbosity.DETAILED)
                .contains("HotSpotOSRCompilation")) {
            return;
        }
        int slowInsertCount = 0; // Counter for how many slowdown instructions we've inserted globally
        for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {
            ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);

            boolean ShouldWeSkipBlock = ShouldWeSkipBlock(instructions);
            if (ShouldWeSkipBlock) {
                continue;
            }
        
            int loopAmount = GTBlockSlowDownLookUp.getBlockCost(lirGenRes.getCompilationUnitName(), b.getId());
        
            // Find first delimiter
            int firstDelimiterIndex = -1;
            for (int idx = 0; idx < instructions.size(); idx++) {
                LIRInstruction ins = instructions.get(idx);
                if (ins instanceof DirectCallOp ||
                    ins instanceof CompressPointerOp ||
                    ins instanceof AMD64G1PostWriteBarrierOp ||
                    ins instanceof UncompressPointerOp ||
                    ins instanceof AMD64G1PreWriteBarrierOp ||
                    ins instanceof TestByteBranchOp ||
                    ins instanceof AMD64HotSpotSafepointOp ||
                    ins instanceof AMD64HotSpotReturnOp  ) {
                    firstDelimiterIndex = idx;
                    break;
                }
            }
        
            int segmentEnd = (firstDelimiterIndex == -1) ? instructions.size() : firstDelimiterIndex;
            if (segmentEnd == instructions.size()) {
                // This means no delimiter found. Avoid inserting after the last instruction.
                segmentEnd = instructions.size() - 1;
            }
        
            // Distribute the initial loopAmount slowdown instructions before the first delimiter
            // If segmentEnd <= 1, just insert all after the first instruction
            if (segmentEnd <= 1) {
                if (instructions.size() > 1 && loopAmount > 0) {
                    for (int count = 0; count < loopAmount; count++) {
                        Register reg = AMD64.cpuRegisters[slowInsertCount % AMD64.cpuRegisters.length];
                        instructions.add(1, new AMD64PointLessReg(reg));
                        slowInsertCount++;
                    }
                }
            } else {
                // Distribute evenly
                int segmentCount = segmentEnd;
                int baseInsert = loopAmount / segmentCount;
                int remainder = loopAmount % segmentCount;
        
                int insertionOffset = 0; 
                for (int idx = 0; idx < segmentEnd; idx++) {
                    int insertsHere = baseInsert + ((remainder > 0) ? 1 : 0);
                    if (remainder > 0) remainder--;
        
                    int insertPos = idx + 1 + insertionOffset;
                    for (int k = 0; k < insertsHere; k++) {
                        Register reg = AMD64.cpuRegisters[slowInsertCount % AMD64.cpuRegisters.length];
                        instructions.add(insertPos, new AMD64PointLessReg(reg));
                        slowInsertCount++;
                        insertionOffset++;
                        insertPos++;
                    }
                }
            }

            int counter = 1;
            for (int i = 0; i < instructions.size(); i++) {
                if (instructions.get(i) instanceof DirectCallOp ||
                        instructions.get(i) instanceof CompressPointerOp ||
                        instructions.get(i) instanceof AMD64G1PostWriteBarrierOp || instructions.get(i) instanceof UncompressPointerOp || instructions.get(i) instanceof AMD64G1PreWriteBarrierOp ) {

                    if (instructions.get(i) instanceof CompressPointerOp) {
                        CompressPointerOp toTest = (CompressPointerOp) instructions.get(i);

                        // Check if no code will be emitted
                        if (!toTest.willThisEmit()) {
                            continue;
                        }
                    }
                    if (instructions.get(i) instanceof AMD64G1PostWriteBarrierOp) {
                        AMD64G1PostWriteBarrierOp toTest = (AMD64G1PostWriteBarrierOp) instructions.get(i);
            
                        if (toTest.sameReg()) {
                            continue;
                        }
            
                        //if (toTest.shouldSkipBarrier()) {
                        if (toTest.isNonNull()) {
                            continue;
                        }
                    }
                    if (instructions.get(i) instanceof AMD64G1PreWriteBarrierOp) {
                        AMD64G1PreWriteBarrierOp toTest = (AMD64G1PreWriteBarrierOp) instructions.get(i);
            
                        if (toTest.sameReg()) {
                            continue;
                        }
            
                        //if (toTest.shouldSkipBarrier()) {
                        if (toTest.isNonNull()) {
                            continue;
                        }
                    }

                    if (instructions.get(i) instanceof UncompressPointerOp) {
                        UncompressPointerOp toTest = (UncompressPointerOp) instructions.get(i);
                        if (toTest.isNonNull()) {
                            continue;
                        }
                        
                    }

                        AMD64PointLesss PointLessbackend = new AMD64PointLesss(GTBlockSlowDownLookUp
                                .getBackendBlockCost(lirGenRes.getCompilationUnitName(), b.getId(), counter));
                        instructions.add(i, PointLessbackend);
                        counter++;
                    i++;


                    // there are cases where there is a set of instructions that become theier own block, but they exist inbetween delimiters
                    LIRInstruction ins = instructions.get(i + 1);
                    if (ins instanceof DirectCallOp ||
                        ins instanceof CompressPointerOp ||
                        ins instanceof AMD64G1PostWriteBarrierOp ||
                        ins instanceof UncompressPointerOp ||
                        ins instanceof AMD64G1PreWriteBarrierOp ||
                        ins instanceof TestByteBranchOp ||
                        ins instanceof AMD64HotSpotSafepointOp ||
                        ins instanceof AMD64HotSpotReturnOp || i + 1 == instructions.size() - 1) {

                        }
                    else{

                        AMD64PointLesss PointLessHiddenBackend = new AMD64PointLesss(GTBlockSlowDownLookUp
                                .getBackendBlockCost(lirGenRes.getCompilationUnitName(), b.getId(), counter));
                        instructions.add(i + 1, PointLessHiddenBackend);
                        counter++;
                    }

                }
            }


            
        }

        

    }

    private boolean ShouldWeSkipBlock(ArrayList<LIRInstruction> instructions) {
        boolean skip = false;
        // Iterate through the instructions to find instances of AMD64Move.MoveToRegOp
        if (instructions.size() == 3) {
            for (LIRInstruction instr : instructions) {
                if (instr instanceof AMD64Move.MoveToRegOp) {
                    AMD64Move.MoveToRegOp moveOp = (AMD64Move.MoveToRegOp) instr;

                    // Check if the input and result are RegisterValue instances
                    if (moveOp.getInput() instanceof RegisterValue && moveOp.getResult() instanceof RegisterValue) {
                        RegisterValue input = (RegisterValue) moveOp.getInput();
                        RegisterValue result = (RegisterValue) moveOp.getResult();

                        // Check if the instruction is moving between the same register
                        if (input.getRegister().equals(result.getRegister())) {
                            // System.out.println("Found MoveToRegOp moving between the same register: " +
                            // input);
                            skip = true;
                        }
                    }
                }
            }
        }

        return skip;
    }

}
