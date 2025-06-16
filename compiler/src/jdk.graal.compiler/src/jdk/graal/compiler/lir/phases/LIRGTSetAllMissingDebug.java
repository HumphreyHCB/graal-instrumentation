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

import jdk.graal.compiler.lir.gen.LIRGenerationResult;
import jdk.vm.ci.code.TargetDescription;

import jdk.graal.compiler.core.common.LIRKind;

import jdk.graal.compiler.core.common.cfg.BasicBlock;

import jdk.graal.compiler.graph.NodeSourcePosition;

import jdk.graal.compiler.hotspot.HotSpotGraalCompiler;

import jdk.graal.compiler.hotspot.HotSpotGraalRuntime;

import jdk.graal.compiler.lir.ConstantValue;

import jdk.graal.compiler.lir.LIR;

import jdk.graal.compiler.lir.LIRInsertionBuffer;

import jdk.graal.compiler.lir.LIRInstruction;

import jdk.graal.compiler.lir.StandardOp.BlockEndOp;

import jdk.graal.compiler.lir.StandardOp.LabelOp;
import jdk.graal.compiler.lir.amd64.AMD64PointLessReg;
import jdk.graal.compiler.lir.amd64.AMD64PointLesss;
import jdk.graal.compiler.lir.gen.DiagnosticLIRGeneratorTool;

import jdk.graal.compiler.lir.gen.LIRGenerationResult;

import jdk.graal.compiler.lir.gen.LIRGeneratorTool;

import jdk.graal.compiler.lir.phases.PostAllocationOptimizationPhase;

import jdk.vm.ci.code.TargetDescription;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;

import jdk.vm.ci.meta.JavaConstant;

import jdk.vm.ci.meta.JavaKind;

import jdk.vm.ci.meta.MetaAccessProvider;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import jdk.vm.ci.meta.ResolvedJavaType;

public class LIRGTSetAllMissingDebug extends PostAllocationOptimizationPhase {

    LIRGTSetAllMissingDebug() {
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes,
            PostAllocationOptimizationContext context) {
        HotSpotGraalCompiler compiler = (HotSpotGraalCompiler) HotSpotJVMCIRuntime.runtime().getCompiler();

        ResolvedJavaType markerType = compiler.getGraalRuntime().getHostProviders().getMetaAccess()
                .lookupJavaType(CompilerMarkers.class);

        ResolvedJavaMethod stubMethod = markerType.getDeclaredMethods()[0];

        for (int blockId : lirGenRes.getLIR().getBlocks()) {

            if (LIR.isBlockDeleted(blockId)) {

                continue;

            }

            BasicBlock<?> b = lirGenRes.getLIR().getBlockById(blockId);

            ArrayList<LIRInstruction> instructions = lirGenRes.getLIR().getLIRforBlock(b);

            for (LIRInstruction instruction : instructions) {

                if (instruction.getPosition() == null) {

                    if (instruction instanceof AMD64PointLessReg || instruction instanceof AMD64PointLesss) {
                        NodeSourcePosition position = new NodeSourcePosition(null, null, markerType.getDeclaredMethods()[1], -1);

                        instruction.setPosition(position);
                        //continue;
                    }
                    else{

                     NodeSourcePosition position = new NodeSourcePosition(null, null, stubMethod, -1);

                     instruction.setPosition(position);
                    }
                }

            }
        }
    }

}
