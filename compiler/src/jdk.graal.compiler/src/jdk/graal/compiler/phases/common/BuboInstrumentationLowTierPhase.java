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

/**
 * Adds Instrumentation to the start and end of all method compilations.
 */
public class BuboInstrumentationLowTierPhase extends BasePhase<LowTierContext> {

    private OptionValues options;

    /**
     * Constructor for BuboInstrumentationLowTierPhase.
     * 
     * @param options Option values for the phase.
     */
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

    /**
     * Runs the instrumentation phase on the given graph.
     * 
     * @param graph   The structured graph to instrument.
     * @param context The low tier context.
     */
    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, LowTierContext context) {
        try {
            // Find instrumentation buffers in the graph
            InstrumentationBuffers buffers = findInstrumentationBuffers(graph);

            if (buffers.isComplete()) {

                    handleFullInstrumentation(graph, buffers);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: Bubo Instrumentation Failure");
        }

        // Record node ratios for further analysis
        //recordNodeRatios(graph);
    }

    /**
     * Handles full instrumentation of the graph.
     * 
     * @param graph          The structured graph to instrument.
     * @param buffers        The instrumentation buffers.
     * @param graphCycleCost The cycle cost of the graph.
     */
    private void handleFullInstrumentation(StructuredGraph graph, InstrumentationBuffers buffers) {

        for (Node node : graph.getNodes()) {
            if (!node.successors().isEmpty()) {
                //node.
            }
        }

    }

    /**
     * Handles instrumentation at the end of a node.
     * 
     * @param graph     The structured graph to instrument.
     * @param endNode   The end node.
     * @param startTime The start time node.
     * @param buffers   The instrumentation buffers.
     */
    private void handleEndNodeInstrumentation(StructuredGraph graph, FixedNode endNode, ClockTimeNode startTime, InstrumentationBuffers buffers) {
        try (DebugCloseable s = endNode.withNodeSourcePosition()) {
            ClockTimeNode endTime = graph.add(new ClockTimeNode());
            graph.addBeforeFixed(endNode, endTime);

            SubNode timeDiff = graph.addWithoutUnique(new SubNode(endTime, startTime));
            aggregateAndStore(graph, endTime, buffers.timeBuffer, timeDiff);
            incrementAndStoreActivationCount(graph, buffers.activationCountBuffer);
        }
    }

    /**
     * Handles partial instrumentation of the graph.
     * 
     * @param graph          The structured graph to instrument.
     * @param buffers        The instrumentation buffers.
     * @param graphCycleCost The cycle cost of the graph.
     */
    private void handlePartialInstrumentation(StructuredGraph graph, InstrumentationBuffers buffers, double graphCycleCost) {
        JavaReadNode readCurrentValue = graph.add(new JavaReadNode(JavaKind.Long, buffers.cyclesBuffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
        graph.addAfterFixed(graph.start(), readCurrentValue);

        ValueNode estimatedCost = graph.addWithoutUnique(new ConstantNode(JavaConstant.forInt((int) Math.round(graphCycleCost)), StampFactory.forKind(JavaKind.Int)));
        AddNode aggregate = graph.addWithoutUnique(new AddNode(readCurrentValue, estimatedCost));

        JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long, buffers.cyclesBuffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), aggregate, BarrierType.ARRAY, false));
        graph.addAfterFixed(readCurrentValue, memoryWrite);

        incrementAndStoreActivationCount(graph, buffers.activationCountBuffer);
    }

    /**
     * Aggregates and stores time differences.
     * 
     * @param graph     The structured graph.
     * @param position  The position node.
     * @param buffer    The buffer node.
     * @param timeDiff  The time difference node.
     */
    private void aggregateAndStore(StructuredGraph graph, FixedWithNextNode position, OffsetAddressNode buffer, ValueNode timeDiff) {
        JavaReadNode readCurrentValue = graph.add(new JavaReadNode(JavaKind.Long, buffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
        graph.addAfterFixed(position, readCurrentValue);
        AddNode aggregate = graph.addWithoutUnique(new AddNode(readCurrentValue, timeDiff));
        JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long, buffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), aggregate, BarrierType.ARRAY, false));
        graph.addAfterFixed(readCurrentValue, memoryWrite);
    }

    /**
     * Increments and stores the activation count.
     * 
     * @param graph  The structured graph.
     * @param buffer The buffer node.
     */
    private void incrementAndStoreActivationCount(StructuredGraph graph, OffsetAddressNode buffer) {
        JavaReadNode readCurrentValue = graph.add(new JavaReadNode(JavaKind.Long, buffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
        ValueNode one = graph.addWithoutUnique(new ConstantNode(JavaConstant.forInt(1), StampFactory.forKind(JavaKind.Int)));
        AddNode addOne = graph.addWithoutUnique(new AddNode(readCurrentValue, one));
        JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long, buffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), addOne, BarrierType.ARRAY, false));
        graph.addAfterFixed(readCurrentValue, memoryWrite);
    }

    /**
     * Finds instrumentation buffers in the graph.
     * 
     * @param graph The structured graph.
     * @return The found instrumentation buffers.
     */
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

    /**
     * Records node ratios for further analysis.
     * 
     * @param graph The structured graph.
     */
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

    /**
     * Class to hold instrumentation buffers.
     */
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

        /**
         * Checks if all required buffers are present.
         * 
         * @return True if all buffers are present, false otherwise.
         */
        boolean isComplete() {
            return timeBuffer != null && activationCountBuffer != null && cyclesBuffer != null;
        }
    }
}
