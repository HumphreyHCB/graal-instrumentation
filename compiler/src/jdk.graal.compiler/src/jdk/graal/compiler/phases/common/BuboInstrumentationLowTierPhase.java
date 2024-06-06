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

import java.util.HashMap;
import java.util.Optional;

import jdk.graal.compiler.core.common.type.StampFactory;
import jdk.graal.compiler.debug.DebugCloseable;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.core.common.CompilationIdentifier.Verbosity;
import jdk.graal.compiler.nodes.ClockTimeNode;
import jdk.graal.compiler.nodes.ConstantNode;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.NamedLocationIdentity;
import jdk.graal.compiler.nodes.NodeView;
import jdk.graal.compiler.nodes.calc.AddNode;
import jdk.graal.compiler.nodes.calc.SubNode;
import jdk.graal.compiler.nodes.extended.JavaReadNode;
import jdk.graal.compiler.nodes.extended.JavaWriteNode;
import jdk.graal.compiler.nodes.java.ReachabilityFenceNode;
import jdk.graal.compiler.nodes.memory.address.OffsetAddressNode;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.nodes.ReturnNode;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.contract.NodeCostUtil;
import jdk.graal.compiler.phases.tiers.LowTierContext;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.graal.compiler.core.common.GraalOptions;
import jdk.graal.compiler.core.common.memory.BarrierType;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboCompUnitCache;

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

    private OptionValues options;

    public BuboInstrumentationLowTierPhase(OptionValues options) {
        this.options = options;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, LowTierContext context) {
        try {

            // find the address node added in the high tier phase, using the BuboVoidStamp
            OffsetAddressNode OldBufferAddress = null;
            OffsetAddressNode TimeBuffer = null;
            OffsetAddressNode ActivationCountBuffer = null;
            OffsetAddressNode CyclesBuffer = null;

            // find the ReachabilityFenceNode we inserted earlyer, and all of the address
            // nodes it has saved
            for (ReachabilityFenceNode node : graph.getNodes().filter(ReachabilityFenceNode.class)) {
                if (node.stamp(NodeView.DEFAULT) == StampFactory.forBuboVoid()) {
                    for (OffsetAddressNode element : node.getValues().filter(OffsetAddressNode.class)) {
                        // Since we don't have direct stamp checks in the iteration, we assume these
                        // nodes
                        // are distinguished by their creation conditions outside this snippet.
                        if (element.stamp(NodeView.DEFAULT).equals(StampFactory.forBuboTimeRead())) {
                            TimeBuffer = element;
                        }
                        if (element.stamp(NodeView.DEFAULT).equals(StampFactory.forBuboActivationCountRead())) {
                            ActivationCountBuffer = element;
                        }
                        if (element.stamp(NodeView.DEFAULT).equals(StampFactory.forBuboCycleRead())) {
                            CyclesBuffer = element;
                        }
                    }
                }
            }

            if (TimeBuffer != null && ActivationCountBuffer != null
                    && CyclesBuffer != null) {
                double graphCycleCost = NodeCostUtil.computeGraphCycles(graph, false);
                if (graphCycleCost >= GraalOptions.MinGraphSize.getValue(options)) {

                    // add the starting ForeignCallNode to the start of the graph
                    // ForeignCallNode startTime = graph.add(new
                    // ForeignCallNode(FAST_JAVA_TIME_MILLIS, ValueNode.EMPTY_ARRAY));
                    ClockTimeNode startTime = graph.add(new ClockTimeNode());
                    graph.addAfterFixed(graph.start(), startTime);

                    // for each return node
                    for (ReturnNode returnNode : graph.getNodes(ReturnNode.TYPE)) {

                        try (DebugCloseable s = returnNode.asFixedNode().withNodeSourcePosition()) {

                            // add the end time call
                            // ForeignCallNode endTime = graph.add(new
                            // ForeignCallNode(FAST_JAVA_TIME_MILLIS, ValueNode.EMPTY_ARRAY));
                            ClockTimeNode endTime = graph.add(new ClockTimeNode());
                            graph.addBeforeFixed(returnNode, endTime);

                            // ValueNode dummy = graph.addWithoutUnique(new
                            // ConstantNode(JavaConstant.forInt(100), StampFactory.forKind(JavaKind.Int)));

                            SubNode Time = graph.addWithoutUnique(new SubNode(endTime, startTime));

                            // read the current value store in the array index
                            JavaReadNode readCurrentValue = graph
                                    .add(new JavaReadNode(JavaKind.Long, TimeBuffer,
                                            NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
                            graph.addAfterFixed(endTime, readCurrentValue);

                            // add the store time with the new time
                            AddNode aggregate = graph.addWithoutUnique(new AddNode(readCurrentValue, Time));

                            // write this value back
                            JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long,
                                    TimeBuffer,
                                    NamedLocationIdentity.getArrayLocation(JavaKind.Long), aggregate, BarrierType.ARRAY,
                                    false));
                            graph.addAfterFixed(readCurrentValue, memoryWrite);

                            // activation writing
                            // read the current value store in the array index
                            JavaReadNode readCurrentValueinActivationCountBuffer = graph
                                    .add(new JavaReadNode(JavaKind.Long, ActivationCountBuffer,
                                            NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
                            graph.addAfterFixed(memoryWrite, readCurrentValueinActivationCountBuffer);

                            ValueNode one = graph.addWithoutUnique(
                                    new ConstantNode(JavaConstant.forInt(1), StampFactory.forKind(JavaKind.Int)));
                            // add the store time with the new time
                            AddNode add1 = graph.addWithoutUnique(new AddNode(readCurrentValue, one));

                            // write this value back
                            JavaWriteNode memoryWriteActivationCountBuffer = graph.add(new JavaWriteNode(JavaKind.Long,
                                    ActivationCountBuffer,
                                    NamedLocationIdentity.getArrayLocation(JavaKind.Long), add1, BarrierType.ARRAY,
                                    false));
                            graph.addAfterFixed(readCurrentValueinActivationCountBuffer,
                                    memoryWriteActivationCountBuffer);

                        }

                    }

                } else {

                    // the grapgh is too expensive to fully instrument collecting time
                    // so instead we will get amout of cycles everytime it actvates

                    // read the current value store in the array index
                    JavaReadNode readCurrentValue = graph.add(new JavaReadNode(JavaKind.Long, CyclesBuffer,
                            NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
                    graph.addAfterFixed(graph.start(), readCurrentValue);

                    ValueNode estimatedCost = graph.addWithoutUnique(new ConstantNode(
                            JavaConstant.forInt((int) Math.round(graphCycleCost)), StampFactory.forKind(JavaKind.Int)));

                    // add the estimatedCost curent value in the array
                    AddNode aggregate = graph.addWithoutUnique(new AddNode(readCurrentValue, estimatedCost));

                    // write this value back
                    JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long,
                            CyclesBuffer,
                            NamedLocationIdentity.getArrayLocation(JavaKind.Long), aggregate, BarrierType.ARRAY,
                            false));
                    graph.addAfterFixed(readCurrentValue, memoryWrite);

                    // activation writing
                    // read the current value store in the array index
                    JavaReadNode readCurrentValueinActivationCountBuffer = graph
                            .add(new JavaReadNode(JavaKind.Long, ActivationCountBuffer,
                                    NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
                    graph.addAfterFixed(memoryWrite, readCurrentValueinActivationCountBuffer);

                    ValueNode one = graph.addWithoutUnique(
                            new ConstantNode(JavaConstant.forInt(1), StampFactory.forKind(JavaKind.Int)));
                    // add the store time with the new time
                    AddNode add1 = graph.addWithoutUnique(new AddNode(readCurrentValue, one));

                    // write this value back
                    JavaWriteNode memoryWriteActivationCountBuffer = graph.add(new JavaWriteNode(JavaKind.Long,
                            ActivationCountBuffer,
                            NamedLocationIdentity.getArrayLocation(JavaKind.Long), add1, BarrierType.ARRAY,
                            false));
                    graph.addAfterFixed(readCurrentValueinActivationCountBuffer, memoryWriteActivationCountBuffer);

                }
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("ERROR: Custom Instruments Failure");
            System.out.print("---------------------------------------------------------------------------");
            System.out.print("---------------------------------------------------------------------------");
            System.out.print("---------------------------------------------------------------------------");
        }
        HashMap<String,Integer> nodeRatioMap = new HashMap<String,Integer>();
        nodeRatioMap.put("Null", 0); // fill null
        String graphID = graph.compilationId().toString(Verbosity.NAME);
        for (Node node : graph.getNodes()) {
            NodeSourcePosition nsp = node.getNodeSourcePosition();
            if (nsp == null) {

            } else {
                if (nsp.getMethod().isNative() || nsp.getMethod().getDeclaringClass().getName().contains("Ljdk/graal/compiler/")) {
                    continue;
                }
                //nsp.getClass().toGenericString();
                String key = nsp.getMethod().getDeclaringClass().getName()+"."+nsp.getMethod().getName();
                if (nodeRatioMap.containsKey(key)) {
                    nodeRatioMap.put(key, nodeRatioMap.get(key) + Math.max(1,node.estimatedNodeCycles().value));
                } else {
                    nodeRatioMap.put(key, Math.max(1,node.estimatedNodeCycles().value));
                }
            }
    }
    String Info = " ";
    for (String method : nodeRatioMap.keySet()) {
        Info += method + "," + nodeRatioMap.get(method) + " ";
    }
    BuboCompUnitCache.add(Integer.parseInt(graph.compilationId().toString(Verbosity.ID).split("-")[1]), Info);
}

    
}
