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
import jdk.graal.compiler.hotspot.amd64.LIRInstructionCostMultiLookup;
import jdk.graal.compiler.hotspot.meta.GT.GTCacheDebug;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRFrameState;
import jdk.graal.compiler.lir.LIRInsertionBuffer;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.StandardOp;
import jdk.graal.compiler.lir.Variable;
import jdk.graal.compiler.lir.LIRInstruction.State;
import jdk.graal.compiler.lir.amd64.AMD64FNop;
import jdk.graal.compiler.lir.amd64.AMD64Nop;
import jdk.graal.compiler.lir.amd64.AMD64Nops;
import jdk.graal.compiler.lir.amd64.AMD64PauseOp;
import jdk.graal.compiler.lir.amd64.AMD64PointLess;
import jdk.graal.compiler.lir.amd64.AMD64ReadTimestampCounter;
import jdk.graal.compiler.lir.amd64.AMD64TempNode;
import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.graal.compiler.lir.gen.LIRGeneratorTool;
import jdk.graal.compiler.lir.gen.MoveFactory;
import jdk.graal.compiler.lir.phases.PreAllocationOptimizationPhase;
import jdk.graal.compiler.lir.util.RegisterMap;
import jdk.graal.compiler.nodes.FrameState;
import jdk.graal.compiler.phases.common.LoweringPhase.Frame;
import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.PlatformKind;

public class DontUse extends PreAllocationOptimizationPhase {

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes,
    PreAllocationOptimizationContext context) {
        if (!lirGenRes.getCompilationUnitName().toLowerCase().contains("graal")) {

            for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {

                ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);
                int vectorCost = 0;
                int nopCost = 0;
                int fnopCost = 0;
                LIRFrameState stolenState = null; 
                for (LIRInstruction instruction : instructions) {
                    if (stolenState == null && instruction.hasState()) {
                        stolenState = instruction.getFrameState();
                    }
                    nopCost += LIRInstructionCostMultiLookup.getNormalCost(instruction.getClass().toString());
                    vectorCost += LIRInstructionCostMultiLookup.getVCost(instruction.getClass().toString());

                }

                // if (stolenState != null) {
                //     System.out.println("Found a Frame State");
                // }
                
                LIRInstruction referenceInstruction = instructions.get(0);
                
                //lirGenRes.getFrameMap().newReferenceMapBuilder().addLiveValue(null);
                //lirGenRes.getLIR().getDebug();
                if (!instructions.isEmpty()) {
                    
                    // instructions.add(1, new AMD64PointLess());
                    // for (int index = 0; index < Math.round(vectorCost / 8); index++) {
                    //     // instructions.add(1, new AMD64SFence());
                    //     AMD64PointLess node = new AMD64PointLess();
                    //     node.setPosition(instructions.get(1).getPosition());
                    //     instructions.add(instructions.size()-1, node);
                    //     nopCost -= 2;
                    // }
                    
                    // for (int index = 0; index < vectorCost; index++) {
                    // instructions.add(1, new AMD64Nop());
                    // //instructions.add(1, new AMD64PointLess());
                    // }
                    for (int index = 0; index < nopCost + vectorCost; index++) {
                        if (stolenState != null) {
                            //AMD64Nop node = new AMD64Nop(stolenState);
                            //instructions.add(instructions.size()-1, node);
                        }
                        else
                        {
                            AMD64Nops node = new AMD64Nops(1);
                            instructions.add(instructions.size()-1, node);
                        }
                        //node.setPosition(instructions.get(1).getPosition());
                        //instructions.add(instructions.size()-1, node);
                        // instructions.add(1, new AMD64PointLess());
                    }

                }
            }
        }
    }

    
}
