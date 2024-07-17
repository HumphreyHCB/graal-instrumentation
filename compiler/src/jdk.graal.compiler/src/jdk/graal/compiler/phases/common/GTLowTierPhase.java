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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

/**
 * Adds Instrumentation to the start and end of all method compilations.
 */
public class GTLowTierPhase extends BasePhase<LowTierContext> {

    private OptionValues options;

    /**
     * Constructor for BuboInstrumentationLowTierPhase.
     * 
     * @param options Option values for the phase.
     */
    public GTLowTierPhase(OptionValues options) {
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
            System.out.println("ERROR: GT Failure");
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
        double initialCost = Math.floor(NodeCostUtil.computeGraphCycles(graph, true));
        //double targetCost = initialCost * 2; // its 2 cause we want to double the overhead
        int incrementCost = 16;
        
        int requiredIncrements = (int) Math.floor(initialCost/ incrementCost);

        
        System.out.println("In comp ID " + graph.compilationId().toString(Verbosity.NAME) );
        System.out.println("graph Cost " + initialCost);
        //System.out.println("Ceil" + Math.ceil((targetCost - initialCost)));
        System.out.println("Going to add " + requiredIncrements + " increment" + " as we currently judge the cost to be " + incrementCost);

        String filePath = "IncsAdded.txt";

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter writer = new FileWriter(file, true);
                writer.write("In comp ID " + graph.compilationId().toString(Verbosity.NAME) + "\n");
                //writer.write("Ceil" + (targetCost - initialCost) + "\n");
                writer.write("graph Cost " + initialCost + "\n");
                writer.write("Going to add " + requiredIncrements + " increment" + " as we currently judge the cost to be " + incrementCost + "\n");

            writer.close();
            //System.out.println("File has been written to " + filePath);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        for (int i = 0; i < requiredIncrements; i++) {
            incrementAndStoreActivationCount(graph, graph.start(), buffers.activationCountBuffer);
        }
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
    private void incrementAndStoreActivationCount(StructuredGraph graph, FixedWithNextNode node , OffsetAddressNode buffer) {
        JavaReadNode readCurrentValue = graph.add(new JavaReadNode(JavaKind.Long, buffer, NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
        graph.addAfterFixed(node, readCurrentValue);
        ValueNode one = graph.addWithoutUnique(new ConstantNode(JavaConstant.forInt(0), StampFactory.forKind(JavaKind.Int)));
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

        OffsetAddressNode activationCountBuffer = null;
        for (ReachabilityFenceNode node : graph.getNodes().filter(ReachabilityFenceNode.class)) {
            if (node.stamp(NodeView.DEFAULT) == StampFactory.forBuboVoid()) {
                for (OffsetAddressNode element : node.getValues().filter(OffsetAddressNode.class)) {
                    if (element.stamp(NodeView.DEFAULT).equals(StampFactory.forBuboActivationCountRead())) {
                        activationCountBuffer = element;
                    }
                }
            }
        }

        return new InstrumentationBuffers( activationCountBuffer);
    }

    /**
     * Class to hold instrumentation buffers.
     */
    private static class InstrumentationBuffers {
        OffsetAddressNode activationCountBuffer;

        InstrumentationBuffers(OffsetAddressNode activationCountBuffer) {
            this.activationCountBuffer = activationCountBuffer;
        }

        /**
         * Checks if all required buffers are present.
         * 
         * @return True if all buffers are present, false otherwise.
         */
        boolean isComplete() {
            return activationCountBuffer != null;
        }
    }
}
