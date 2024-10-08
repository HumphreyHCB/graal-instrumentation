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
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionCostMultiLookup;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionVectorLookup;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.amd64.AMD64Nop;
import jdk.graal.compiler.lir.amd64.AMD64PointLess;
import jdk.graal.compiler.lir.amd64.AMD64SFence;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.options.OptionType;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.vm.ci.code.TargetDescription;

public class LIRGTSlowdownPhasePost extends PostAllocationOptimizationPhase {

    public static class Options {
        // @formatter:off
        @Option(help = "", type = OptionType.Debug)
        public static final OptionKey<String> LIRNubers = new OptionKey<>("");
        // @formatter:on
    }

    private OptionValues options;

    LIRGTSlowdownPhasePost(OptionValues poptions) {
        options = poptions;
    }

    public static List<Integer> analyzeVectorInstructions(LIRGenerationResult lirGenRes) {
        // Use a Set to avoid duplicate block IDs
        Set<Integer> resultBlockIds = new HashSet<>();

        // Iterate through all blocks in the control flow graph
        for (BasicBlock<?> block : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {

            // Check if the block contains any "vector" instruction
            boolean hasVectorInstruction = false;
            for (LIRInstruction instruction : lirGenRes.getLIR().getLIRforBlock(block)) {
                // if (instruction.getClass().toString().toLowerCase().contains("vector")) {
                if (LIRInstructionVectorLookup.containsClassName(instruction.getClass().toString())) {
                    hasVectorInstruction = true;
                    break;
                }
            }

            // If the block contains a "vector" instruction, collect its ID and those of its
            // successors and predecessors
            if (hasVectorInstruction) {
                // Add the block's ID
                resultBlockIds.add(block.getId());

                // // Add IDs of predecessors
                // for (int i = 0; i < block.getPredecessorCount(); i++) {
                // BasicBlock<?> predecessor = block.getPredecessorAt(i);
                // resultBlockIds.add(predecessor.getId());
                // }

                // // Add IDs of successors
                // for (int i = 0; i < block.getSuccessorCount(); i++) {
                // BasicBlock<?> successor = block.getSuccessorAt(i);
                // resultBlockIds.add(successor.getId());
                // }
            }
        }

        // Convert the Set to a List and return
        return new ArrayList<>(resultBlockIds);
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes,
            PostAllocationOptimizationContext context) {
        if (!lirGenRes.getCompilationUnitName().toLowerCase().contains("graal")) {
           for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {

                ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);
                int vectorCost = 0;
                int nopCost = 0;

                for (LIRInstruction instruction : instructions) {

                    nopCost += LIRInstructionCostMultiLookup.getNormalCost(instruction.getClass().toString());
                    vectorCost += LIRInstructionCostMultiLookup.getVCost(instruction.getClass().toString());

                }

                if (!instructions.isEmpty()) {

                    int originalSize = instructions.size();
                    int nopCount = 0;
                    int sfenceCount = 0;
                    int pointLessCount = 0;
                
                    int real = Math.round(vectorCost / 2);
                    int remainder = vectorCost % 2;
                
                    // Continue looping until all nops, sfences, and PointLess nodes are inserted
                    int i = 1;
                    while (nopCount < nopCost || sfenceCount < remainder || pointLessCount < real) {
                        // Use modulo to wrap around the index to the list size
                        int currentIndex = ((i - 1) % (originalSize - 1)) + 1 + nopCount + sfenceCount + pointLessCount;
                
                        // Insert a Nop node if we haven't reached the Nop count limit
                        if (nopCount < nopCost) {
                            AMD64Nop nopNode = new AMD64Nop();
                            instructions.add(currentIndex, nopNode);
                            nopCount++;
                        }
                
                        // Insert a SFence node if we haven't reached the SFence count limit
                        if (sfenceCount < remainder) {
                            AMD64SFence sfenceNode = new AMD64SFence();
                            instructions.add(currentIndex, sfenceNode);
                            sfenceCount++;
                        }
                
                        // Insert a PointLess node if we haven't reached the PointLess count limit
                        if (pointLessCount < real) {
                            AMD64PointLess pointLessNode = new AMD64PointLess();
                            instructions.add(currentIndex, pointLessNode);
                            pointLessCount++;
                        }
                
                        i++;
        }}}}
    }

    /**
     * Utility method to copy the state from one LIR instruction to another.
     */
    // private void copyStateAndDebugInfo(LIRInstruction from, LIRInstruction to,
    // LIRGenerationResult lirGenRes) {
    // // Copy the state
    // from.forEachState((stateProcedure) -> {
    // to.visitEachState((instruction, value, mode, flags) -> {
    // // Copy the state to the new instruction
    // lirGenRes.getLIR().setState(to, value);
    // });
    // });
    // from.setPosition(null);
    // // Copy the debug info
    // LIRFrameState frameState = from.getFrameState();
    // if (frameState != null && frameState.hasDebugInfo()) {
    // // Initialize debug info for the new instruction if necessary
    // if (!frameState.hasDebugInfo()) {
    // frameState.initDebugInfo();
    // }

    // // Create a new reference map builder

    // ReferenceMapBuilder refMap =
    // lirGenRes.getFrameMap().newReferenceMapBuilder();
    // RegStackValueSet values = new RegStackValueSet(lirGenRes.getFrameMap());

    // // Add live values from the original instruction to the reference map
    // values.addLiveValues(refMap);

    // // Set the reference map for the new instruction's debug info
    // LIRFrameState newFrameState = new LIRFrameState(frameState.topFrame,
    // frameState.virtualObjects, frameState.exceptionEdge,
    // frameState.validForDeoptimization);
    // newFrameState.debugInfo().setReferenceMap(refMap.finish(newFrameState));

    // // Assign the new frame state to the new instruction
    // lirGenRes.getLIR().setState(to, newFrameState);
    // }

}
