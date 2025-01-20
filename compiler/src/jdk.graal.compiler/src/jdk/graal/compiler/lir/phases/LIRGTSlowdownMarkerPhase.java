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

import jdk.graal.compiler.lir.amd64.AMD64PrefetchOp;
import jdk.graal.compiler.core.common.CompilationIdentifier;
import jdk.graal.compiler.core.common.cfg.AbstractControlFlowGraph;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.hotspot.amd64.AMD64HotSpotSafepointOp;
import jdk.graal.compiler.hotspot.amd64.AMD64HotSpotStrategySwitchOp;
import jdk.graal.compiler.hotspot.amd64.GTBlockSlowDownLookUp;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionCostMultiLookup;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionVectorLookup;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.amd64.AMD64Call.DirectCallOp;
import jdk.graal.compiler.lir.amd64.AMD64ControlFlow.CmpBranchOp;
import jdk.graal.compiler.lir.amd64.AMD64ControlFlow.CmpConstBranchOp;
import jdk.graal.compiler.lir.amd64.AMD64ControlFlow.FloatBranchOp;
import jdk.graal.compiler.lir.amd64.AMD64ControlFlow.TestBranchOp;
import jdk.graal.compiler.lir.amd64.AMD64ControlFlow.TestByteBranchOp;
import jdk.graal.compiler.lir.amd64.AMD64Move.CompareAndSwapOp;
import jdk.graal.compiler.lir.amd64.AMD64Move.CompressPointerOp;
import jdk.graal.compiler.lir.amd64.AMD64Move.NullCheckOp;
import jdk.graal.compiler.lir.amd64.g1.AMD64G1PostWriteBarrierOp;
import jdk.graal.compiler.lir.amd64.g1.AMD64G1PreWriteBarrierOp;
import jdk.graal.compiler.lir.amd64.AMD64GTBackendMarkerOp;
import jdk.graal.compiler.lir.amd64.AMD64GTMarkerOp;
import jdk.graal.compiler.lir.amd64.AMD64Move;
import jdk.graal.compiler.lir.amd64.AMD64Move.UncompressPointerOp;
import jdk.graal.compiler.lir.amd64.AMD64Nop;
import jdk.graal.compiler.hotspot.amd64.AMD64HotSpotReturnOp;
import jdk.graal.compiler.lir.amd64.AMD64Nops;
import jdk.graal.compiler.lir.amd64.AMD64PointLess;
import jdk.graal.compiler.lir.amd64.AMD64SFence;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.nodeinfo.Verbosity;
import jdk.graal.compiler.nodes.calc.CompareNode.CompareOp;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.options.OptionType;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

public class LIRGTSlowdownMarkerPhase extends PostAllocationOptimizationPhase {

    private OptionValues options;

    LIRGTSlowdownMarkerPhase(OptionValues options) {
        this.options = options;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes,
            PostAllocationOptimizationContext context) {
        if (lirGenRes.getCompilationUnitName(CompilationIdentifier.Verbosity.DETAILED)
                .contains("HotSpotOSRCompilation")) {
            return;
        }

        outerLoop: for (int blockId : lirGenRes.getLIR().codeEmittingOrder()) {

            // if (lirGenRes.getCompilationUnitName(CompilationIdentifier.Verbosity.DETAILED)
            // .contains("moveDisks")) {
            //     System.out.println("Found moveDisks");
            // }

            BasicBlock<?> b = lirGenRes.getLIR().getBlockById(blockId);
            if (b == null) {
                continue;
            }
            ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);

            boolean ShouldWeSkipBlock = ShouldWeSkipBlock(instructions);

            if (ShouldWeSkipBlock) {
                continue;
            }

            AMD64GTMarkerOp markerOp = new AMD64GTMarkerOp(b.getId(), lirGenRes.getCompilationUnitName());
            instructions.add(1, markerOp);

            int counter = 1;
            for (int i = 0; i < instructions.size(); i++) {
                if (instructions.get(i) instanceof CompressPointerOp 
                    || instructions.get(i) instanceof DirectCallOp
                    || instructions.get(i) instanceof AMD64G1PostWriteBarrierOp 
                    || instructions.get(i) instanceof UncompressPointerOp 
                    || instructions.get(i) instanceof AMD64G1PreWriteBarrierOp
                    || instructions.get(i) instanceof AMD64HotSpotSafepointOp
                    || instructions.get(i) instanceof AMD64PrefetchOp ){


                    if (instructions.get(i) instanceof CompressPointerOp) {
                        CompressPointerOp toTest = (CompressPointerOp) instructions.get(i);
            
                        // ATM we dont do anything specific with this instruction
                    }
    
                    if (instructions.get(i) instanceof AMD64G1PostWriteBarrierOp) {
                        AMD64G1PostWriteBarrierOp toTest = (AMD64G1PostWriteBarrierOp) instructions.get(i);
                        // ATM we dont do anything specific with this instruction
                    }


                    if (instructions.get(i) instanceof AMD64G1PreWriteBarrierOp) {
                        AMD64G1PreWriteBarrierOp toTest = (AMD64G1PreWriteBarrierOp) instructions.get(i);
                        // ATM we dont do anything specific with this instruction
                    }

                    if (instructions.get(i) instanceof UncompressPointerOp) {
                        UncompressPointerOp toTest = (UncompressPointerOp) instructions.get(i);
                        // ATM we dont do anything specific with this instruction
                    }


                    // If we reach here, it's one of the other instruction types (e.g., AMD64G1PreWriteBarrierOp)
                    // Insert the marker anyway.
                    instructions.add(i,
                            new AMD64GTBackendMarkerOp(b.getId(), counter, lirGenRes.getCompilationUnitName()));
                    counter++;
                    i++;



                    LIRInstruction ins = instructions.get(i + 1);
                    if (ins instanceof DirectCallOp ||
                        ins instanceof CompressPointerOp ||
                        ins instanceof AMD64G1PostWriteBarrierOp ||
                        ins instanceof UncompressPointerOp ||
                        ins instanceof AMD64G1PreWriteBarrierOp ||
                        ins instanceof TestByteBranchOp ||
                        ins instanceof AMD64HotSpotSafepointOp ||
                        ins instanceof AMD64HotSpotReturnOp ||
                         i + 1 == instructions.size() - 1) {

                        }
                    else{
                        instructions.add(i + 1,
                        new AMD64GTBackendMarkerOp(b.getId(), counter, lirGenRes.getCompilationUnitName()));
                        counter++;
                    }

                }
            }

            

            
            
        }
    }

    /*
     * Contains Checks for blocks we should check and should we skip
     * 
     */
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
