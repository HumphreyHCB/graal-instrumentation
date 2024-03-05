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

import static jdk.graal.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.JAVA_TIME_MILLIS;
import static jdk.graal.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.FAST_JAVA_TIME_MILLIS;
import java.util.Optional;

import jdk.graal.compiler.core.common.type.StampFactory;
import jdk.graal.compiler.debug.DebugCloseable;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.NamedLocationIdentity;
import jdk.graal.compiler.nodes.NodeView;
import jdk.graal.compiler.nodes.calc.AddNode;
import jdk.graal.compiler.nodes.calc.SubNode;
import jdk.graal.compiler.nodes.extended.ForeignCallNode;
import jdk.graal.compiler.nodes.extended.JavaReadNode;
import jdk.graal.compiler.nodes.extended.JavaWriteNode;
import jdk.graal.compiler.nodes.memory.address.OffsetAddressNode;
import jdk.graal.compiler.nodes.ReturnNode;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.tiers.LowTierContext;
import jdk.vm.ci.meta.JavaKind;
import jdk.graal.compiler.core.common.memory.BarrierType;

/**
 * Adds Instrumentation to the start and end of all method compilations.
 */
public class BuboInstrumentationLowTierPhase extends BasePhase<LowTierContext> {

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


    public BuboInstrumentationLowTierPhase() {
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, LowTierContext context) {
        try {


            // find the address node added in the high tier phase, using the BuboVoidStamp 
            OffsetAddressNode addressNode = null;
            for (OffsetAddressNode node : graph.getNodes().filter(OffsetAddressNode.class)) {
                if (node.stamp(NodeView.DEFAULT) == StampFactory.forBuboVoid()) {
                    addressNode = node;
                    continue;
                }
            }

            if (addressNode != null) {

                // add the starting ForeignCallNode to the start of the graph
                ForeignCallNode startTime = graph.add(new ForeignCallNode(FAST_JAVA_TIME_MILLIS,
                        ValueNode.EMPTY_ARRAY));
                graph.addAfterFixed(graph.start(), startTime);

                // for each return node 
                for (ReturnNode returnNode : graph.getNodes(ReturnNode.TYPE)) {

                    try (DebugCloseable s = returnNode.asFixedNode().withNodeSourcePosition()) {


                        // add the end time call
                        ForeignCallNode endTime = graph
                                .add(new ForeignCallNode(FAST_JAVA_TIME_MILLIS, ValueNode.EMPTY_ARRAY));
                        graph.addBeforeFixed(returnNode, endTime);

                        SubNode Time = graph.addWithoutUnique(new SubNode(endTime, startTime));


                        // read the current value store in the array index
                        JavaReadNode readCurrentValue = graph
                                .add(new JavaReadNode(JavaKind.Long, addressNode,
                                        NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
                        graph.addAfterFixed(endTime, readCurrentValue);

                        // add the store time with the new time
                        AddNode aggregate = graph.addWithoutUnique(new AddNode(readCurrentValue, Time));

                        // write this value back
                        JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long,
                                addressNode,
                                NamedLocationIdentity.getArrayLocation(JavaKind.Long), aggregate, BarrierType.ARRAY,
                                false));
                        graph.addAfterFixed(readCurrentValue, memoryWrite);

                    }

                }

                // graph.removeFixed(writeToRemove);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("ERROR: Custom Instruments Failure");
            System.out.print("---------------------------------------------------------------------------");
            System.out.print("---------------------------------------------------------------------------");
            System.out.print("---------------------------------------------------------------------------");
        }

    }

}
