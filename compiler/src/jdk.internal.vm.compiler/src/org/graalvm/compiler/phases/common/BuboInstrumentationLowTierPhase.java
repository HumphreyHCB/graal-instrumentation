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
package org.graalvm.compiler.phases.common;

import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.AddtoInstrumentationCache;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.JAVA_TIME_NANOS;
import java.util.Optional;

import org.graalvm.compiler.core.common.CompilationIdentifier.Verbosity;
import org.graalvm.compiler.core.common.type.BuboVoidStamp;
import org.graalvm.compiler.core.common.type.IntegerStamp;
import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.debug.DebugCloseable;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.iterators.NodeIterable;
import org.graalvm.compiler.hotspot.meta.Bubo.BuboCache;
import org.graalvm.compiler.hotspot.nodes.LoadIndexedPointerNode;
import org.graalvm.compiler.nodes.GraphState;
import org.graalvm.compiler.nodes.IfNode;
import org.graalvm.compiler.nodes.LogicNode;
import org.graalvm.compiler.nodes.MergeNode;
import org.graalvm.compiler.nodes.NamedLocationIdentity;
import org.graalvm.compiler.nodes.NodeView;
import org.graalvm.compiler.nodes.PiNode;
import org.graalvm.compiler.nodes.calc.AddNode;
import org.graalvm.compiler.nodes.calc.IntegerBelowNode;
import org.graalvm.compiler.nodes.calc.IntegerEqualsNode;
import org.graalvm.compiler.nodes.calc.LeftShiftNode;
import org.graalvm.compiler.nodes.calc.NarrowNode;
import org.graalvm.compiler.nodes.calc.SignExtendNode;
import org.graalvm.compiler.nodes.calc.SubNode;
import org.graalvm.compiler.nodes.debug.BlackholeNode;
import org.graalvm.compiler.nodes.debug.ControlFlowAnchorNode;
import org.graalvm.compiler.nodes.extended.BranchProbabilityNode;
import org.graalvm.compiler.nodes.extended.ForeignCallNode;
import org.graalvm.compiler.nodes.extended.GuardingNode;
import org.graalvm.compiler.nodes.extended.JavaReadNode;
import org.graalvm.compiler.nodes.extended.JavaWriteNode;
import org.graalvm.compiler.nodes.extended.UnsafeAccessNode;
import org.graalvm.compiler.nodes.java.AccessIndexedNode;
import org.graalvm.compiler.nodes.java.LoadFieldNode;
import org.graalvm.compiler.nodes.java.LoadIndexedNode;
import org.graalvm.compiler.nodes.java.NewArrayNode;
import org.graalvm.compiler.nodes.java.StoreFieldNode;
import org.graalvm.compiler.nodes.java.StoreIndexedNode;
import org.graalvm.compiler.nodes.memory.FloatingAccessNode;
import org.graalvm.compiler.nodes.memory.FloatingReadNode;
import org.graalvm.compiler.nodes.memory.MemoryKill;
import org.graalvm.compiler.nodes.memory.ReadNode;
import org.graalvm.compiler.nodes.memory.SideEffectFreeWriteNode;
import org.graalvm.compiler.nodes.memory.WriteNode;
import org.graalvm.compiler.nodes.memory.address.AddressNode;
import org.graalvm.compiler.nodes.memory.address.IndexAddressNode;
import org.graalvm.compiler.nodes.memory.address.OffsetAddressNode;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.ReturnNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.CompressionNode.CompressionOp;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.phases.common.LoweringPhase.LoweringToolImpl;
import org.graalvm.compiler.replacements.SnippetCounter;
import org.graalvm.compiler.replacements.SnippetCounter.Group;
import org.graalvm.compiler.replacements.arraycopy.ArrayCopyCallNode;
import org.graalvm.compiler.replacements.arraycopy.ArrayCopyNode;
import org.graalvm.compiler.replacements.nodes.ArrayIndexOfNode;
import org.graalvm.compiler.replacements.nodes.LogNode;

import org.graalvm.compiler.nodes.AbstractBeginNode;
import org.graalvm.compiler.nodes.AbstractMergeNode;
import org.graalvm.compiler.nodes.BeginNode;
import org.graalvm.compiler.nodes.ConstantNode;
import org.graalvm.compiler.nodes.CustomClockLogNode;
import org.graalvm.compiler.nodes.EndNode;
import org.graalvm.compiler.nodes.FixedNode;
import org.graalvm.compiler.options.Option;
import org.graalvm.compiler.options.OptionKey;
import org.graalvm.compiler.options.OptionType;
import org.graalvm.compiler.phases.tiers.HighTierContext;
import org.graalvm.compiler.phases.tiers.LowTierContext;
import org.graalvm.compiler.phases.tiers.MidTierContext;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import org.graalvm.compiler.debug.GraalError;

import org.graalvm.compiler.nodes.util.GraphUtil;

import org.graalvm.compiler.core.common.memory.BarrierType;
import org.graalvm.compiler.core.common.memory.MemoryOrderMode;

import org.graalvm.compiler.replacements.SnippetCounter;
import org.graalvm.compiler.replacements.SnippetCounterNode;
import org.graalvm.compiler.replacements.SnippetCounter.Group;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaConstant;

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
                ForeignCallNode startTime = graph.add(new ForeignCallNode(JAVA_TIME_NANOS,
                        ValueNode.EMPTY_ARRAY));
                graph.addAfterFixed(graph.start(), startTime);

                // for each return node 
                for (ReturnNode returnNode : graph.getNodes(ReturnNode.TYPE)) {

                    try (DebugCloseable s = returnNode.asFixedNode().withNodeSourcePosition()) {


                        // add the end time call
                        ForeignCallNode endTime = graph
                                .add(new ForeignCallNode(JAVA_TIME_NANOS, ValueNode.EMPTY_ARRAY));
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
