/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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
package jdk.graal.compiler.phases.common;

import java.util.Optional;

import jdk.graal.compiler.debug.DebugContext;
import jdk.graal.compiler.debug.TTY;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.tiers.HighTierContext;

/**
 * Adds ReadNode & Addres to Start of the graphg, this will be use in the Low Ter Instrumentation phase .
 */
public class GTDebugInfoLoggerHighTierPhase extends BasePhase<HighTierContext> {
    


    @Override
    public boolean checkContract() {
        // the size / cost after is highly dynamic and dependent on the graph, thus we
        // do not verify
        // costs for this phase
        return false;
    }

    @Override
    public Optional<NotApplicable> notApplicableTo(GraphState graphState) {
        return ALWAYS_APPLICABLE;
    }

    public GTDebugInfoLoggerHighTierPhase() {
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, HighTierContext context) {
        int NodeCount = graph.getNodeCount();
        int SourcePositionCount = 0;

            for (var node : graph.getNodes()) {
                if (node.getNodeSourcePosition() != null && node.verify()) {
                    SourcePositionCount++;
                }
            }   
                TTY.println("Start of Logging for GTDebugInfoLog for : " + graph.compilationId().toString(jdk.graal.compiler.core.common.CompilationIdentifier.Verbosity.NAME) + " : " + graph.getNodeCount() + " nodes");
                TTY.println("Source out of Nodes: " + SourcePositionCount +"/"+ NodeCount);
                TTY.println("Ratio : " + (double) SourcePositionCount / (double) NodeCount);
                TTY.println("IRStage,Phase,DebugNodeAmount,NodeCount,Ratio");
        }
            

    }