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
import jdk.graal.compiler.hotspot.amd64.GTBlockSlowDownLookUp;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionCostMultiLookup;
import jdk.graal.compiler.hotspot.amd64.LIRInstructionVectorLookup;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.amd64.AMD64Nop;
import jdk.graal.compiler.lir.amd64.AMD64Nops;
import jdk.graal.compiler.lir.amd64.AMD64PointLess;
import jdk.graal.compiler.lir.amd64.AMD64SFence;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.options.OptionType;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.vm.ci.code.TargetDescription;

public class LIRGTSlowdownPhasePost extends PostAllocationOptimizationPhase {

    private OptionValues options;

    LIRGTSlowdownPhasePost(OptionValues poptions) {
        options = poptions;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes,
            PostAllocationOptimizationContext context) {
    //if (lirGenRes.getCompilationUnitName().toLowerCase().contains("placequeen")) {
                
        for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {
            ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);
            //if (b.getId() == AbstractControlFlowGraph.INVALID_BLOCK_ID) {
           //    continue;
           // }
            //for (int i = 0; i < b.getId(); i++) {

            for (int i = 0; i <  GTBlockSlowDownLookUp.getBlockCost(lirGenRes.getCompilationUnitName(), b.getId()); i++) {
                AMD64PointLess PointLess = new AMD64PointLess();
                instructions.add(1, PointLess);
            }
       // }
    }
        // for (int blockId : lirGenRes.getLIR().getBlocks()) {
        // if (blockId == Integer.MAX_VALUE) {
        // // if a block id == max then its a delected block
        // continue;
        // }
        // ArrayList<LIRInstruction> instructions = lirGenRes.getLIR()
        // .getLIRforBlock(lirGenRes.getLIR().getBlockById(blockId));

        // if (instructions != null) {
        // for (int i = 0; i < 5; i++) {
        // // AMD64PointLess
        // AMD64PointLess nopNode = new AMD64PointLess();
        // instructions.add(1, nopNode);
        // }
        // }

        // }

        // ArrayList<LIRInstruction> instructions =
        // lirGenRes.getLIR().getLIRforBlock(b);
        // int vectorCost = 0;
        // int nopCost = 0;

        // for (LIRInstruction instruction : instructions) {

        // nopCost +=
        // LIRInstructionCostMultiLookup.getNormalCost(instruction.getClass().toString());
        // vectorCost +=
        // LIRInstructionCostMultiLookup.getVCost(instruction.getClass().toString());

        // }

        // if (!instructions.isEmpty()) {

        // int originalSize = instructions.size();
        // int nopCount = 0;
        // int sfenceCount = 0;
        // int pointLessCount = 0;

        // int real = Math.round(vectorCost / 1);
        // int remainder = vectorCost % 1;

        // // Continue looping until all nops, sfences, and PointLess nodes are inserted
        // int i = 1;
        // while (nopCount < nopCost || sfenceCount < remainder || pointLessCount <
        // real) {
        // // Use modulo to wrap around the index to the list size
        // int currentIndex = ((i - 1) % (originalSize - 1)) + 1 + nopCount +
        // sfenceCount + pointLessCount;

        // // Insert a Nop node if we haven't reached the Nop count limit
        // if (nopCount < nopCost) {
        // AMD64Nop nopNode = new AMD64Nop();
        // instructions.add(currentIndex, nopNode);
        // nopCount++;
        // }

        // // Insert a SFence node if we haven't reached the SFence count limit
        // if (sfenceCount < remainder) {
        // AMD64SFence sfenceNode = new AMD64SFence();
        // instructions.add(currentIndex, sfenceNode);
        // sfenceCount++;
        // }

        // // Insert a PointLess node if we haven't reached the PointLess count limit
        // if (pointLessCount < real) {
        // AMD64PointLess pointLessNode = new AMD64PointLess();
        // instructions.add(currentIndex, pointLessNode);
        // pointLessCount++;

        // }

        // i++;
        // } // }}
    }

}
