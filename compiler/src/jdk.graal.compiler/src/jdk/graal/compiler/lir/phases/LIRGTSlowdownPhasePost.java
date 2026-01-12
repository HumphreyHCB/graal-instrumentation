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
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.hotspot.amd64.AMD64HotSpotReturnOp;
import jdk.graal.compiler.hotspot.amd64.AMD64HotSpotSafepointOp;
import jdk.graal.compiler.hotspot.amd64.GTBlockSlowDownLookUp;
import jdk.graal.compiler.lir.amd64.AMD64Call.DirectCallOp;
import jdk.graal.compiler.lir.amd64.AMD64ControlFlow.TestByteBranchOp;
import jdk.graal.compiler.lir.amd64.AMD64LoopEndOp;
import jdk.graal.compiler.lir.amd64.AMD64LoopStartOp;
import jdk.graal.compiler.lir.amd64.AMD64Move;
import jdk.graal.compiler.lir.amd64.AMD64Move.CompressPointerOp;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.StandardOp;
import jdk.graal.compiler.lir.StandardOp.JumpOp;
import jdk.graal.compiler.lir.StandardOp.LabelOp;
import jdk.graal.compiler.lir.amd64.AMD64PointLesss;
import jdk.graal.compiler.lir.amd64.AMD64PrefetchOp;
import jdk.graal.compiler.lir.amd64.AMD64PointLessReg;
import jdk.graal.compiler.lir.amd64.g1.AMD64G1PostWriteBarrierOp;
import jdk.graal.compiler.lir.amd64.g1.AMD64G1PreWriteBarrierOp;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.options.OptionValues;
import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.graal.compiler.core.common.GraalOptions;
import jdk.graal.compiler.lir.amd64.AMD64Move.UncompressPointerOp;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboRDTSCToSlot;
import jdk.graal.compiler.lir.amd64.Bubo.AMD64BuboWriteDeltaRDTSC;
import jdk.graal.compiler.core.common.CompilationIdentifier;
import jdk.graal.compiler.lir.amd64.AMD64ControlFlow.RangeTableSwitchOp;

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
        NodeSourcePosition source = null;

        int slowInsertCount = 0; // Counter for how many slowdown instructions we've inserted globally
        for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {
            ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);


            boolean ShouldWeSkipBlock = ShouldWeSkipBlock(instructions);
            boolean ShouldWeSkipBlock2 = ShouldWeSkipBlockBuboOps(instructions);
            if (ShouldWeSkipBlock || ShouldWeSkipBlock2) {
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
                        ins instanceof AMD64HotSpotReturnOp ||
                        ins instanceof AMD64PrefetchOp
                    ) {
                    firstDelimiterIndex = idx;
                    break;
                }
            }

            int segmentEnd = (firstDelimiterIndex == -1) ? instructions.size() : firstDelimiterIndex;
            if (segmentEnd == instructions.size()) {
                // This means no delimiter found. Avoid inserting after the last instruction.
                segmentEnd = instructions.size() - 1;
            }
            // if the value of MixGTSlowdown is anything byt -1 ( -1 is the deafult value, it should be a boolean as we never use its value) we will Set the source
            if (GraalOptions.MixGTSlowdown.getValue(options) != -1) {   
                int position = 1;
                for (LIRInstruction instruction : instructions) {
                    if (instruction instanceof StandardOp.LabelOp) {
                        continue;
                    }
                    if (instruction.getPosition() != null) {
                        source = instruction.getPosition(); // get first Source
                        position = instructions.indexOf(instruction); // get the position of the first instruction with a source
                        break;
                    }
                }

                for (int count = 0; count < loopAmount; count++) {
                    Register reg = AMD64.cpuRegisters.get(slowInsertCount % AMD64.cpuRegisters.size());
                    AMD64PointLessReg pointLessReg = new AMD64PointLessReg(reg);
                    if (source!= null){
                        pointLessReg.setPosition(source);
                        
                    }
                    instructions.add(position, pointLessReg);
                    slowInsertCount++;
                }
                
            }
            else if (segmentEnd <= 1 ) {
                // Distribute the initial loopAmount slowdown instructions before the first
                // delimiter
                // If segmentEnd <= 1, just insert all after the first instruction
                if (instructions.size() > 1 && loopAmount > 0) {
                    for (int count = 0; count < loopAmount; count++) {
                        Register reg = AMD64.cpuRegisters.get(slowInsertCount % AMD64.cpuRegisters.size());
                        AMD64PointLessReg pointLessReg = new AMD64PointLessReg(reg);
                        if (source!= null){
                            pointLessReg.setPosition(source);
                            
                        }
                        instructions.add(1, pointLessReg);
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
                    if (remainder > 0)
                        remainder--;

                    int insertPos = idx + 1 + insertionOffset;
                    for (int k = 0; k < insertsHere; k++) {
                        
                        Register reg = AMD64.cpuRegisters.get(slowInsertCount % AMD64.cpuRegisters.size());
                        AMD64PointLessReg pointLessReg = new AMD64PointLessReg(reg);
                        if (source!= null){
                            pointLessReg.setPosition(source);
                        }
                        instructions.add(insertPos, pointLessReg);
                        slowInsertCount++;
                        insertionOffset++;
                        insertPos++;
                    }
                }
            }

            int counter = 1;
            for (int i = 0; i < instructions.size(); i++) {
                if (instructions.get(i) instanceof CompressPointerOp 
                    || instructions.get(i) instanceof DirectCallOp
                    || instructions.get(i) instanceof AMD64G1PostWriteBarrierOp 
                    || instructions.get(i) instanceof UncompressPointerOp 
                    || instructions.get(i) instanceof AMD64G1PreWriteBarrierOp
                    || instructions.get(i) instanceof AMD64HotSpotSafepointOp
                    || instructions.get(i) instanceof AMD64PrefetchOp){

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

                    AMD64PointLesss PointLessbackend = new AMD64PointLesss(GTBlockSlowDownLookUp
                            .getBackendBlockCost(lirGenRes.getCompilationUnitName(), b.getId(), counter));

                            PointLessbackend.setPosition(source);
                            if (source!= null){
                                PointLessbackend.setPosition(source);
                                
                            }
                    instructions.add(i, PointLessbackend);
                    counter++;
                    i++;

                    // there are cases where there is a set of instructions that become theier own
                    // block, but they exist inbetween delimiters
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

                    } else {

                        AMD64PointLesss PointLessHiddenBackend = new AMD64PointLesss(GTBlockSlowDownLookUp
                                .getBackendBlockCost(lirGenRes.getCompilationUnitName(), b.getId(), counter));
                                if (source!= null){
                                PointLessHiddenBackend.setPosition(source);
                                }
                        instructions.add(i + 1, PointLessHiddenBackend);
                        counter++;
                    }

                }
            }

            // if (BuboLIRPhase.Options.BuboLIRPhase.getValue(options)) {
            //     AdjustBuboProbes(b, lirGenRes);
            // }

        }

    }

    private void AdjustBuboProbes(BasicBlock<?> b, LIRGenerationResult lirGenRes) {
        ArrayList<LIRInstruction> insns = lirGenRes.getLIR().getLIRforBlock(b);

        // Find indices (first occurrences)
        int rdtscIdx = -1;
        int writeDeltaIdx = -1;
        for (int i = 0; i < insns.size(); i++) {
            LIRInstruction op = insns.get(i);
            if (rdtscIdx < 0 && op instanceof AMD64BuboRDTSCToSlot) {
                rdtscIdx = i;
            } else if (writeDeltaIdx < 0 && op instanceof AMD64BuboWriteDeltaRDTSC) {
                writeDeltaIdx = i;
            }
            if (rdtscIdx >= 0 && writeDeltaIdx >= 0) {
                break;
            }
        }

        boolean hasRdtsc = rdtscIdx >= 0;
        boolean hasWriteDelta = writeDeltaIdx >= 0;

        if (!hasRdtsc && !hasWriteDelta) {
            return;
        }

        // Helper: is this a slowdown op?
        // (Keep your types/names exactly as you use them)
        java.util.function.Predicate<LIRInstruction> isSlowdown = op -> (op instanceof AMD64PointLesss)
                || (op instanceof AMD64PointLessReg);

        // ------------------------------------------------------------------
        // Rule 1:
        // Any slowdown AFTER AMD64BuboRDTSCToSlot should be placed BEFORE it.
        // i.e. move slowdowns from (rdtscIdx+1 .. end) -> just before rdtscIdx
        // ------------------------------------------------------------------
        if (hasRdtsc) {
            java.util.ArrayList<LIRInstruction> afterRdtsc = new java.util.ArrayList<>();

            for (int i = rdtscIdx + 1; i < insns.size();) {
                LIRInstruction op = insns.get(i);
                if (isSlowdown.test(op)) {
                    afterRdtsc.add(op);
                    insns.remove(i); // don't increment i
                    if (hasWriteDelta && i <= writeDeltaIdx) {
                        writeDeltaIdx--; // shifted left
                    }
                } else {
                    i++;
                }
            }

            // Insert before RDTSC, preserving relative order of extracted ops
            if (!afterRdtsc.isEmpty()) {
                insns.addAll(rdtscIdx, afterRdtsc);
                rdtscIdx += afterRdtsc.size();
                if (hasWriteDelta && rdtscIdx <= writeDeltaIdx) {
                    writeDeltaIdx += afterRdtsc.size(); // shifted right
                }
            }
        }

        // Re-find writeDeltaIdx if needed (safe if it shifted a lot)
        if (hasWriteDelta) {
            writeDeltaIdx = -1;
            for (int i = 0; i < insns.size(); i++) {
                if (insns.get(i) instanceof AMD64BuboWriteDeltaRDTSC) {
                    writeDeltaIdx = i;
                    break;
                }
            }
            if (writeDeltaIdx < 0) {
                return;
            }
        }

        // ------------------------------------------------------------------
        // Rule 2:
        // Any slowdown BEFORE AMD64BuboWriteDeltaRDTSC should be placed AFTER it.
        // i.e. move slowdowns from (0 .. writeDeltaIdx-1) -> just after writeDeltaIdx
        // ------------------------------------------------------------------
        if (hasWriteDelta) {
            java.util.ArrayList<LIRInstruction> beforeWriteDelta = new java.util.ArrayList<>();

            for (int i = 0; i < writeDeltaIdx;) {
                LIRInstruction op = insns.get(i);
                if (isSlowdown.test(op)) {
                    beforeWriteDelta.add(op);
                    insns.remove(i);
                    writeDeltaIdx--; // marker shifts left when removing before it
                    // rdtscIdx might shift too if it is after i
                    if (hasRdtsc && i <= rdtscIdx) {
                        rdtscIdx--;
                    }
                } else {
                    i++;
                }
            }

            // Insert after writeDelta, preserving relative order of extracted ops
            if (!beforeWriteDelta.isEmpty()) {
                insns.addAll(writeDeltaIdx + 1, beforeWriteDelta);
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

        private boolean ShouldWeSkipBlockBuboOps(ArrayList<LIRInstruction> instructions) {
        boolean sawAllowedOp = false;

        for (LIRInstruction instr : instructions) {
            // Ignore structural ops
            if (instr instanceof LabelOp) {
                continue;
            }
            if (instr instanceof JumpOp) {
                continue;
            }

            // Allowed "non-real" ops
            if (instr instanceof AMD64LoopEndOp
                    || instr instanceof AMD64BuboWriteDeltaRDTSC
                    || instr instanceof AMD64LoopStartOp
                    || instr instanceof AMD64BuboRDTSCToSlot) {
                sawAllowedOp = true;
                continue;
            }

            // Anything else makes this a real block
            return false;
        }

        // Skip only if we saw at least one allowed op
        return sawAllowedOp;
    }

}
