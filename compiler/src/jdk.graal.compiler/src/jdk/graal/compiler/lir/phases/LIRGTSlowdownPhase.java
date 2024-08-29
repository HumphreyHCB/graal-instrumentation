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
import java.util.List;

import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionCostLookup;
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
import jdk.graal.compiler.lir.amd64.AMD64ReadTimestampCounter;
import jdk.graal.compiler.lir.amd64.AMD64TempNode;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.graal.compiler.lir.gen.MoveFactory;
import jdk.graal.compiler.lir.phases.PreAllocationOptimizationPhase;
import jdk.graal.compiler.lir.util.RegisterMap;
import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.PlatformKind;

public class LIRGTSlowdownPhase extends PreAllocationOptimizationPhase {

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes,
            PreAllocationOptimizationContext context) {
        // try a LFence incase
        if (lirGenRes.getCompilationUnitName().equals("Mandelbrot.mandelbrot(int)")) {
            System.out.println(lirGenRes.getCompilationUnitName());

            int blockIndex = 0; // Initialize a counter for the block index
            List<Integer> ids = new ArrayList<>(Arrays.asList(8, 10));

            for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {

                ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);
                int blockCost = 0;
                for (LIRInstruction instruction : instructions) {
                    blockCost += LIRInstructionCostLookup.getCost(instruction.getClass().toString());
                }

                boolean vector = false;
                int memoryFences = 0;
                for (LIRInstruction instruction : instructions) {
                    if (instruction.getClass().toString().contains("AMD64VectorBinary")) {
                        memoryFences += LIRInstructionCostLookup.getCost(instruction.getClass().toString());
                        vector = true;
                    }
                }
                System.out.println(memoryFences);

                if (!instructions.isEmpty()) {
                    if (vector || blockIndex == 8 || blockIndex == 10) {

                        for (int i = 0; i < blockCost; i++) {
                            instructions.add(1, new AMD64TempNode());
                        }
                    } else {
                        for (int index = 0; index < blockCost; index++) {

                            instructions.add(1, new AMD64Nop());

                            // for (int i = 1; i < instructions.size() - 1; i++) {
                            // int insertionCount =
                            // LIRInstructionCostLookup.getCost(instructions.get(i).getClass().toString());

                            // // Insert new elements based on the cost
                            // for (int index = 0; index < insertionCount; index++) {
                            // instructions.add(i + 1, new AMD64TempNode());
                            // i++; // Increment `i` to skip the newly added element
                            // }
                            // }
                            // instructions.add(1, new AMD64TempNode());
                            // for (int index = 0; index < PauseAmount; index++) {
                            // instructions.add(instructions.size() -1, new AMD64PauseOp());
                            // }

                        }

                    }
                }
                blockIndex++; // Increment the block index counter after processing each block
            }
            // }
            // else {

            // for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks())
            // {

            // ArrayList<LIRInstruction> instructions =
            // lirGenRes.getLIR().getLIRforBlock(b);
            // int blockCost = 0;
            // for (LIRInstruction instruction : instructions) {
            // blockCost +=
            // LIRInstructionCostLookup.getCost(instruction.getClass().toString());
            // }
            // if (!instructions.isEmpty()) {
            // for (int index = 0; index < blockCost; index++) {
            // instructions.add(1, new AMD64Nop());
            // }

            // }

            // }
            // }

        }

    }
}
