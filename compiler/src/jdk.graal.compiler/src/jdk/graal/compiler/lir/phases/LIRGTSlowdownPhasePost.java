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
import jdk.graal.compiler.lir.amd64.AMD64Call.DirectCallOp;
import jdk.graal.compiler.lir.amd64.AMD64Move.CompressPointerOp;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.amd64.AMD64Nop;
import jdk.graal.compiler.lir.amd64.AMD64Nops;
import jdk.graal.compiler.lir.amd64.AMD64PointLess;
import jdk.graal.compiler.lir.amd64.AMD64PointLesss;
import jdk.graal.compiler.lir.amd64.AMD64SFence;
import jdk.graal.compiler.lir.amd64.g1.AMD64G1PostWriteBarrierOp;
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
        // if (lirGenRes.getCompilationUnitName().toLowerCase().contains("placequeen"))
        // {

        for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {
            ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);
            int loopAmount = GTBlockSlowDownLookUp.getBlockCost(lirGenRes.getCompilationUnitName(), b.getId());
            // for (int i = 0; i < loopAmount; i++) {
            //     AMD64PointLess PointLess = new AMD64PointLess();
            //     instructions.add(1, PointLess);
            // }

            AMD64PointLesss PointLessa = new AMD64PointLesss(loopAmount);
            instructions.add(1, PointLessa);

            int counter = 1;
            for (int i = 0; i < instructions.size(); i++) {
                if (instructions.get(i) instanceof DirectCallOp ||
                        instructions.get(i) instanceof CompressPointerOp ||
                        instructions.get(i) instanceof AMD64G1PostWriteBarrierOp) {

                    // for (int X = 0; X < GTBlockSlowDownLookUp.getBackendBlockCost(lirGenRes.getCompilationUnitName(),
                    //         b.getId(), counter); X++) {
                    //     AMD64PointLess PointLess = new AMD64PointLess();
                    //     instructions.add(i + 1, PointLess);
                    // }
                    AMD64PointLesss PointLessb = new AMD64PointLesss( GTBlockSlowDownLookUp.getBackendBlockCost(lirGenRes.getCompilationUnitName(), b.getId(), counter));
                    instructions.add(i + 1, PointLessb);
                    counter++;

                    // Move the index forward to skip over the newly inserted marker
                    i++;
                }
            }

        }

    }

}
