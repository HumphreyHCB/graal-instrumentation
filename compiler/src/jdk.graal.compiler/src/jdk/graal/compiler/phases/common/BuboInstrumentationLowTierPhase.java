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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jdk.graal.compiler.core.common.type.StampFactory;
import jdk.graal.compiler.debug.DebugCloseable;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.core.common.CompilationIdentifier.Verbosity;
import jdk.graal.compiler.nodes.ClockTimeNode;
import jdk.graal.compiler.nodes.ConstantNode;
import jdk.graal.compiler.nodes.FixedNode;
import jdk.graal.compiler.nodes.FixedWithNextNode;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.InvokeNode;
import jdk.graal.compiler.nodes.NamedLocationIdentity;
import jdk.graal.compiler.nodes.NodeView;
import jdk.graal.compiler.nodes.calc.AddNode;
import jdk.graal.compiler.nodes.calc.SubNode;
import jdk.graal.compiler.nodes.extended.JavaReadNode;
import jdk.graal.compiler.nodes.extended.JavaWriteNode;
import jdk.graal.compiler.nodes.java.ReachabilityFenceNode;
import jdk.graal.compiler.nodes.memory.address.OffsetAddressNode;
import jdk.graal.compiler.nodes.util.GraphUtil;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.nodes.ReturnNode;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.UnwindNode;
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
import jdk.graal.compiler.hotspot.meta.Bubo.CompUnitInfo;

public class BuboInstrumentationLowTierPhase extends BasePhase<LowTierContext> {

    private OptionValues options;

    public BuboInstrumentationLowTierPhase(OptionValues options) {
        this.options = options;
    }

    @Override
    public boolean checkContract() {
        return false;
    }

    @Override
    public Optional<NotApplicable> notApplicableTo(GraphState graphState) {
        return ALWAYS_APPLICABLE;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, LowTierContext context) {
        try {
            InstrumentationBuffers buffers = findInstrumentationBuffers(graph);

            if (buffers.isComplete()) {
                double graphCycleCost = NodeCostUtil.computeGraphCycles(graph, true);
                if (graphCycleCost >= GraalOptions.MinGraphSize.getValue(options)) {
                    handleFullInstrumentation(graph, buffers, graphCycleCost);
                } else {
                    handlePartialInstrumentation(graph, buffers, graphCycleCost);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("---------------------------------------------------------------------------");
            System.out.println("ERROR: Bubo Instrumentation Failure");
            System.out.print("---------------------------------------------------------------------------");
        }

        recordNodeRatios(graph);
    }

    private void handleFullInstrumentation(StructuredGraph graph, InstrumentationBuffers buffers, double graphCycleCost) {
        ClockTimeNode startTime = graph.add(new ClockTimeNode());
        graph.addAfterFixed(graph.start(), startTime);

        for (InvokeNode invokeNode : graph.getNodes().filter(InvokeNode.class)) {
            ClockTimeNode invokeStartTime = graph.add(new ClockTimeNode());
            graph.addBeforeFixed(invokeNode, invokeStartTime);

            ClockTimeNode invokeEndTime = graph.add(new ClockTimeNode());
            graph.addAfterFixed(invokeNode, invokeEndTime);

            SubNode timeDiff = graph.addWithoutUnique(new SubNode(invokeEndTime, invokeStartTime));
            aggregateAndStore(graph, invokeEndTime, buffers.callSiteBuffer, timeDiff);
        }

        for (ReturnNode returnNode : graph.getNodes(ReturnNode.TYPE)) {
            handleEndNodeInstrumentation(graph, returnNode, startTime, buffers);
        }

        for (UnwindNode unwindNode : graph.getNodes(UnwindNode.TYPE)) {
            handleEndNodeInstrumentation(graph, unwindNode, startTime, buffers);
        }
    }

    private void handleEndNodeInstrumentation(StructuredGraph graph, FixedNode endNode, ClockTimeNode startTime, InstrumentationBuffers buffers) {
        try (DebugCloseable s = endNode.withNodeSourcePosition()) {
            ClockTimeNode endTime = graph.add(new ClockTimeNode());
            graph.addBeforeFixed(endNode, endTime);

            SubNode timeDiff = graph.addWithoutUnique(new SubNode(endTime, startTime));
            aggregateAndStore(graph, endTime, buffers.timeBuffer, timeDiff);
            incrementAndStoreActivationCount(graph, buffers.activationCountBuffer);
        }
    }

    private void handlePartialInstrumentation(StructuredGraph graph, InstrumentationBuffers buffers, double graphCycleCost) {
        JavaReadNode readCurrentValue = graph.add(new JavaReadNode(JavaKind.Long, buffers.cyclesBuffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
        graph.addAfterFixed(graph.start(), readCurrentValue);

        ValueNode estimatedCost = graph.addWithoutUnique(new ConstantNode(JavaConstant.forInt((int) Math.round(graphCycleCost)), StampFactory.forKind(JavaKind.Int)));
        AddNode aggregate = graph.addWithoutUnique(new AddNode(readCurrentValue, estimatedCost));

        JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long, buffers.cyclesBuffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), aggregate, BarrierType.ARRAY, false));
        graph.addAfterFixed(readCurrentValue, memoryWrite);

        incrementAndStoreActivationCount(graph, buffers.activationCountBuffer);
    }

    private void aggregateAndStore(StructuredGraph graph, FixedWithNextNode position, OffsetAddressNode buffer, ValueNode timeDiff) {
        JavaReadNode readCurrentValue = graph.add(new JavaReadNode(JavaKind.Long, buffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
        graph.addAfterFixed(position, readCurrentValue);
        AddNode aggregate = graph.addWithoutUnique(new AddNode(readCurrentValue, timeDiff));
        JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long, buffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), aggregate, BarrierType.ARRAY, false));
        graph.addAfterFixed(readCurrentValue, memoryWrite);
    }

    private void incrementAndStoreActivationCount(StructuredGraph graph, OffsetAddressNode buffer) {
        JavaReadNode readCurrentValue = graph.add(new JavaReadNode(JavaKind.Long, buffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
        ValueNode one = graph.addWithoutUnique(new ConstantNode(JavaConstant.forInt(1), StampFactory.forKind(JavaKind.Int)));
        AddNode addOne = graph.addWithoutUnique(new AddNode(readCurrentValue, one));
        JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long, buffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), addOne, BarrierType.ARRAY, false));
        graph.addAfterFixed(readCurrentValue, memoryWrite);
    }

    private InstrumentationBuffers findInstrumentationBuffers(StructuredGraph graph) {
        OffsetAddressNode callSiteBuffer = null;
        OffsetAddressNode timeBuffer = null;
        OffsetAddressNode activationCountBuffer = null;
        OffsetAddressNode cyclesBuffer = null;

        for (ReachabilityFenceNode node : graph.getNodes().filter(ReachabilityFenceNode.class)) {
            if (node.stamp(NodeView.DEFAULT) == StampFactory.forBuboVoid()) {
                for (OffsetAddressNode element : node.getValues().filter(OffsetAddressNode.class)) {
                    if (element.stamp(NodeView.DEFAULT).equals(StampFactory.forBuboTimeRead())) {
                        timeBuffer = element;
                    }
                    if (element.stamp(NodeView.DEFAULT).equals(StampFactory.forBuboActivationCountRead())) {
                        activationCountBuffer = element;
                    }
                    if (element.stamp(NodeView.DEFAULT).equals(StampFactory.forBuboCycleRead())) {
                        cyclesBuffer = element;
                    }
                    if (element.stamp(NodeView.DEFAULT).equals(StampFactory.forBuboCallSiteRead())) {
                        callSiteBuffer = element;
                    }
                }
            }
        }

        return new InstrumentationBuffers(callSiteBuffer, timeBuffer, activationCountBuffer, cyclesBuffer);
    }

    private void recordNodeRatios(StructuredGraph graph) {
        HashMap<String, Double> nodeRatioMap = new HashMap<>();
        nodeRatioMap.put("Null", 0D);
        Map<Node, Double> graphCyclesMap = NodeCostUtil.computeGraphCyclesMap(graph, true);

        for (Node node : graphCyclesMap.keySet()) {
            NodeSourcePosition nsp = node.getNodeSourcePosition();
            if (nsp == null || nsp.getMethod().isNative() || nsp.getMethod().getDeclaringClass().getName().contains("Ljdk/graal/compiler/")) {
                continue;
            }
            String key = nsp.getMethod().getDeclaringClass().getName() + "." + nsp.getMethod().getName();
            nodeRatioMap.put(key, nodeRatioMap.getOrDefault(key, 0D) + graphCyclesMap.get(node));
        }

        List<CompUnitInfo> methodInfos = new ArrayList<>();
        for (Map.Entry<String, Double> entry : nodeRatioMap.entrySet()) {
            methodInfos.add(new CompUnitInfo(entry.getKey(), entry.getValue()));
        }

        BuboCompUnitCache.add(Integer.parseInt(graph.compilationId().toString(Verbosity.ID).split("-")[1]), methodInfos);
    }

    private static class InstrumentationBuffers {
        OffsetAddressNode callSiteBuffer;
        OffsetAddressNode timeBuffer;
        OffsetAddressNode activationCountBuffer;
        OffsetAddressNode cyclesBuffer;

        InstrumentationBuffers(OffsetAddressNode callSiteBuffer, OffsetAddressNode timeBuffer, OffsetAddressNode activationCountBuffer, OffsetAddressNode cyclesBuffer) {
            this.callSiteBuffer = callSiteBuffer;
            this.timeBuffer = timeBuffer;
            this.activationCountBuffer = activationCountBuffer;
            this.cyclesBuffer = cyclesBuffer;
        }

        boolean isComplete() {
            return timeBuffer != null && activationCountBuffer != null && cyclesBuffer != null;
        }
    }
}
