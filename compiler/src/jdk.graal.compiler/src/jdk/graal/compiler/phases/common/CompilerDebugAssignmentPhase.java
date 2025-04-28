/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

import jdk.graal.compiler.bytecode.ResolvedJavaMethodBytecodeProvider;
import jdk.graal.compiler.core.common.GraalOptions;
import jdk.graal.compiler.debug.Markers.CompilerMarkers;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.schedule.SchedulePhase;
import jdk.graal.compiler.phases.tiers.LowTierContext;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public class CompilerDebugAssignmentPhase extends BasePhase<LowTierContext> {

    @Override
    public Optional<NotApplicable> notApplicableTo(GraphState graphState) {
        return Optional.empty();
    }

    public CompilerDebugAssignmentPhase() {
    }

    @Override
    protected void run(StructuredGraph graph, LowTierContext context) {
        MetaAccessProvider meta = context.getMetaAccess();

        if (graph.method() != null) {
            ResolvedJavaType markerType = meta.lookupJavaType(CompilerMarkers.class);

            ResolvedJavaMethod stubMethod = markerType.getDeclaredMethods()[0];

            //ResolvedJavaMethod stubMethod = CompilerMarkers.stubMethod;

            NodeSourcePosition oldPos = null;
            for (Node n : graph.getNodes()) {
                if (n.getNodeSourcePosition() != null) {
                    oldPos = n.getNodeSourcePosition();
                    break;
                }
            }

            if (oldPos == null) {
                //System.out.println("oldPos is null, in " + graph.method().format("%H.%n(%p)"));
                return;
            }
            for (Node n : graph.getNodes()) {

                
                //NodeSourcePosition oldPos = n.getNodeSourcePosition();

                if (n.getNodeSourcePosition() == null) {
                    //System.out.println("Method " + graph.method().format("%H.%n(%p)"));
                    //System.out.println("Node " + n.getClass().getName());
                    NodeSourcePosition newPos = new NodeSourcePosition(
                            null,
                            null,
                            stubMethod,
                            /* you can choose an appropriate BCI; -1 is common for synthetic */
                            -1);
                    n.setNodeSourcePosition(newPos);

                }

            }
        }

    }
}
