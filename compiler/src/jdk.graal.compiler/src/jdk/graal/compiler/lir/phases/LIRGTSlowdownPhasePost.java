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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionCostLookup;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionCostMultiLookup;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionVectorLookup;
import jdk.graal.compiler.hotspot.meta.GT.GTCacheDebug;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInsertionBuffer;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.StandardOp;
import jdk.graal.compiler.lir.Variable;
import jdk.graal.compiler.lir.amd64.AMD64FNop;
import jdk.graal.compiler.lir.amd64.AMD64Nop;
import jdk.graal.compiler.lir.amd64.AMD64Nops;
import jdk.graal.compiler.lir.amd64.AMD64PauseOp;
import jdk.graal.compiler.lir.amd64.AMD64PointLess;
import jdk.graal.compiler.lir.amd64.AMD64ReadTimestampCounter;
import jdk.graal.compiler.lir.amd64.AMD64SFence;
import jdk.graal.compiler.lir.amd64.AMD64TempNode;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.graal.compiler.lir.gen.MoveFactory;
import jdk.graal.compiler.lir.phases.PreAllocationOptimizationPhase;
import jdk.graal.compiler.lir.util.RegisterMap;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.options.OptionType;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.PlatformKind;

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

        for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {

            ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);
            int vectorCost = 0;
            int nopCost = 0;
            int fnopCost = 0;
            for (LIRInstruction instruction : instructions) {
                // if (instruction.getClass().toString().toLowerCase().contains("float")) {
                //     fnopCost += LIRInstructionCostMultiLookup.getNormalCost(instruction.getClass().toString());
                //     fnopCost += LIRInstructionCostMultiLookup.getVCost(instruction.getClass().toString());
                // }
                // else{
                    nopCost += LIRInstructionCostMultiLookup.getNormalCost(instruction.getClass().toString());
                    vectorCost += LIRInstructionCostMultiLookup.getVCost(instruction.getClass().toString());
                //}
                //
                
                
            }


            if (!instructions.isEmpty()) {
                    //instructions.add(1, new AMD64PointLess());
                for (int index = 0; index < Math.round(vectorCost/2); index++) {
                    //instructions.add(1, new AMD64SFence());
                    instructions.add(1, new AMD64PointLess());
                }
                for (int index = 0; index < Math.round(nopCost/2); index++) {
                    instructions.add(1, new AMD64Nop());
                    //instructions.add(1, new AMD64PointLess());
                }

            }
        }
    }

}
