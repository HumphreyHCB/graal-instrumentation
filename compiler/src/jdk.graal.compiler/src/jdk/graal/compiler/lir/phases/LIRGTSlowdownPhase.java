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

import jdk.graal.compiler.core.common.LIRKind;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.hotspot.meta.GT.GTCacheDebug;
import jdk.graal.compiler.lir.LIR;
import jdk.graal.compiler.lir.LIRInsertionBuffer;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.StandardOp;
import jdk.graal.compiler.lir.Variable;
import jdk.graal.compiler.lir.amd64.AMD64PauseOp;
import jdk.graal.compiler.lir.amd64.AMD64ReadTimestampCounter;
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
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PreAllocationOptimizationContext context) {
        // try a LFence incase
        for (BasicBlock<?> b : lirGenRes.getLIR().getControlFlowGraph().getBlocks()) {

            // ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);
            // int loopAmount =  (int) Math.floor((instructions.size() - 2)/ 2);
            // if (!instructions.isEmpty()) {
            //     for (LIRInstruction iterable_element : instructions) {
            //         GTCacheDebug.add(iterable_element.getClass().getName());
            //     }
                //for (int index = 0; index < loopAmount; index++) {
                //    instructions.add(1, new AMD64PauseOp());
                //}
                
            //} 

        }

    }

}
