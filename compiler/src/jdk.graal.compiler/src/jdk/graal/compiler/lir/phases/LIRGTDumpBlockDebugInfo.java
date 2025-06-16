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

public class LIRGTDumpBlockDebugInfo extends PostAllocationOptimizationPhase {

    private OptionValues options;

    LIRGTDumpBlockDebugInfo(OptionValues options) {
        this.options = options;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes,
            PostAllocationOptimizationContext context) {
        if (lirGenRes.getCompilationUnitName(CompilationIdentifier.Verbosity.DETAILED)
                .contains("HotSpotOSRCompilation")) {
            return;
        }
        System.out.println("\n\n\n");
        System.out.println("Dumping LIR block debug info for :" + lirGenRes.getCompilationUnitName(CompilationIdentifier.Verbosity.DETAILED));
        int LirCount = 0;
        int DebugCount = 0;
        for (int blockId : lirGenRes.getLIR().codeEmittingOrder()) {
            BasicBlock<?> block   = lirGenRes.getLIR().getBlockById(blockId);
            List<LIRInstruction> insns = lirGenRes.getLIR().getLIRforBlock(block);
        
            // --- Block header (blank line first) -----------------------------------
            System.out.printf("%nBlock %d%n", blockId);
        
            // --- Instructions ------------------------------------------------------
            for (LIRInstruction insn : insns) {
                String methodInfo = (insn.getPosition() == null)
                                    ? "No position"
                                    : insn.getPosition().getMethod().toString();
        
                System.out.printf("  %-28s : %s%n", insn.name(), methodInfo);
            }
        
        
        // System.out.println();

        // if (uniqueMethods.size() == 1) {
        //     System.out.println("Unique Method: " + uniqueMethods.iterator().next());
        // } else {
        //     System.out.println("Methods found: " + uniqueMethods);
        // }

        // if (uniqueRootMethods.size() == 1) {
        //     System.out.println("Unique Root Method: " + uniqueRootMethods.iterator().next());
        // } else {
        //     System.out.println("Root Methods found: " + uniqueRootMethods);
        // }

        // System.out.println("Contains Backend: " + containsBackend);

           }
           System.out.println("Total LIR Instructions: " + LirCount);
           System.out.println("Total LIR Instructions With Debug: " + DebugCount);
        
    }

}
