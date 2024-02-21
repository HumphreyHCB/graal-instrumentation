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
 * Adds CustomInstrumentation to loops.
 */
public class CustomLateLowPhase extends BasePhase<LowTierContext> {

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

    Group group;

    public CustomLateLowPhase(Group group) {
        this.group = group;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, LowTierContext context) {
        try {
            // WriteNode writeToRemove = null;
            // for (WriteNode node : graph.getNodes().filter(WriteNode.class)) {
            //     if (node.getAddress().stamp(NodeView.DEFAULT) == StampFactory.forBuboVoid()) {
            //         writeToRemove = node;
            //         continue;
            //     }
            // }

            // FloatingReadNode writeToRemove = null;
            // for (FloatingReadNode node : graph.getNodes().filter(FloatingReadNode.class)) {
            //     if (node.getAddress().stamp(NodeView.DEFAULT) == StampFactory.forBuboVoid()) {
            //         writeToRemove = node;
            //         continue;
            //     }
            // }

            OffsetAddressNode addressNode = null;
            for (OffsetAddressNode node : graph.getNodes().filter(OffsetAddressNode.class)) {
                if (node.stamp(NodeView.DEFAULT) == StampFactory.forBuboVoid()) {
                    addressNode = node;
                    continue;
                }
            }

            if (addressNode != null) {

                //graph.start().removeUsage(writeToRemove);

                
                ForeignCallNode startTime = graph.add(new ForeignCallNode(JAVA_TIME_NANOS,
                        ValueNode.EMPTY_ARRAY));
                graph.addAfterFixed(graph.start(), startTime);


                for (ReturnNode returnNode : graph.getNodes(ReturnNode.TYPE)) {

                    try (DebugCloseable s = returnNode.asFixedNode().withNodeSourcePosition()) {

                        ForeignCallNode endTime = graph
                                .add(new ForeignCallNode(JAVA_TIME_NANOS, ValueNode.EMPTY_ARRAY));
                        graph.addBeforeFixed(returnNode, endTime);

                        SubNode Time = graph.addWithoutUnique(new SubNode(endTime, startTime));



                        JavaReadNode readCurrentValue = graph
                                .add(new JavaReadNode(JavaKind.Long, addressNode,
                                        NamedLocationIdentity.getArrayLocation(JavaKind.Long), null, null, false));
                        graph.addAfterFixed(endTime, readCurrentValue);
                        
                        AddNode aggregate = graph.addWithoutUnique(new AddNode(readCurrentValue, Time));

                        JavaWriteNode memoryWrite = graph.add(new JavaWriteNode(JavaKind.Long,
                        addressNode,
                                NamedLocationIdentity.getArrayLocation(JavaKind.Long), aggregate, BarrierType.ARRAY,
                                false));
                        graph.addAfterFixed(readCurrentValue, memoryWrite);

                    }

                }

                // for (ConstantNode node : writeToRemove.inputs().filter(ConstantNode.class)) {
                //     node.safeDelete();                    
                //     // node.removeUsage(writeToRemove);
                // } 
                // System.out.println("the method is " + graph.compilationId().toString(Verbosity.NAME));

                // for (Node iterable_element : graph.start().usages()) {
                //     System.out.println("StartNode uses 1 : " + iterable_element.getClass());
                // }

                // for (Node iterable_element : writeToRemove.usages().snapshot()) {
                //     //System.out.println("Trying to remove " + iterable_element.getClass());
                //     iterable_element.removeUsage(writeToRemove);
                // }
                // for (Node iterable_element : writeToRemove.usages().snapshot()) {
                //     System.out.println("Trying to remove 2" + iterable_element.getClass());
                //     iterable_element.removeUsage(writeToRemove);
                // }

                // for (Node iterable_element : graph.getNodes()) {
                //     if (iterable_element.removeUsage(writeToRemove)) {
                //         System.out.println("Found a another node to rmove" + iterable_element.getClass());
                //     }
                // }
                // for (Node iterable_element : graph.start().usages()) {
                //     System.out.println("StartNode uses 2 : " + iterable_element.getClass());
                // }

                

                //graph.removeFixed(writeToRemove);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("ERROR: Custom Instruments Failure");
            System.out.print("---------------------------------------------------------------------------");
            System.out.print("---------------------------------------------------------------------------");
            System.out.print("---------------------------------------------------------------------------");
        }

    }

    public AddressNode createArrayAddress(StructuredGraph graph, ValueNode array, int arrayBaseOffset,
            JavaKind elementKind, ValueNode index, TargetDescription target, MetaAccessProvider metaAccess) {
        ValueNode wordIndex;
        if (target.wordSize > 4) {
            wordIndex = graph.unique(new SignExtendNode(index, target.wordSize * 8));
        } else {
            assert target.wordSize == 4 : "unsupported word size";
            wordIndex = index;
        }
        int shift = CodeUtil.log2(metaAccess.getArrayIndexScale(elementKind));
        ValueNode scaledIndex = graph.unique(new LeftShiftNode(wordIndex, ConstantNode.forInt(shift, graph)));
        ValueNode offset = graph.unique(
                new AddNode(scaledIndex, ConstantNode.forIntegerKind(target.wordJavaKind, arrayBaseOffset, graph)));
        return graph.unique(new OffsetAddressNode(array, offset));
    }

    public AddressNode createOffsetAddress(StructuredGraph graph, ValueNode object, long offset,
            TargetDescription target) {
        ValueNode o = ConstantNode.forIntegerKind(target.wordJavaKind, offset, graph);
        return graph.unique(new OffsetAddressNode(object, o));
    }

    public static final IntegerStamp POSITIVE_ARRAY_INDEX_STAMP = IntegerStamp.create(32, 0, Integer.MAX_VALUE - 1);

    /**
     * Create a PiNode on the index proving that the index is positive. On some
     * platforms this is
     * important to allow the index to be used as an int in the address mode.
     */
    protected ValueNode createPositiveIndex(StructuredGraph graph, ValueNode index, GuardingNode boundsCheck) {
        return graph.addOrUnique(
                PiNode.create(index, POSITIVE_ARRAY_INDEX_STAMP, boundsCheck != null ? boundsCheck.asNode() : null));
    }

    protected void lowerJavaWriteNode(JavaWriteNode write) {
        StructuredGraph graph = write.graph();
        ValueNode value = implicitStoreConvert(graph, write.getWriteKind(), write.value(), write.isCompressible());
        WriteNode memoryWrite;
        if (write.hasSideEffect()) {
            memoryWrite = graph.add(new WriteNode(write.getAddress(), write.getKilledLocationIdentity(), value,
                    write.getBarrierType(), write.getMemoryOrder()));
        } else {
            assert !write.ordersMemoryAccesses();
            memoryWrite = graph.add(new SideEffectFreeWriteNode(write.getAddress(), write.getKilledLocationIdentity(),
                    value, write.getBarrierType()));
        }
        memoryWrite.setStateAfter(write.stateAfter());
        graph.replaceFixedWithFixed(write, memoryWrite);
        memoryWrite.setGuard(write.getGuard());
    }

    public final ValueNode implicitStoreConvert(StructuredGraph graph, JavaKind kind, ValueNode value) {
        return implicitStoreConvert(graph, kind, value, true);
    }

    public ValueNode implicitStoreConvert(JavaKind kind, ValueNode value) {
        return implicitStoreConvert(kind, value, true);
    }

    protected final ValueNode implicitStoreConvert(StructuredGraph graph, JavaKind kind, ValueNode value,
            boolean compressible) {
        ValueNode ret = implicitStoreConvert(kind, value, compressible);
        if (!ret.isAlive()) {
            ret = graph.addOrUnique(ret);
        }
        return ret;
    }

    /**
     * @param compressible whether the covert should be compressible
     */
    protected ValueNode implicitStoreConvert(JavaKind kind, ValueNode value, boolean compressible) {
        // if (useCompressedOops(kind, compressible)) {
        // return newCompressionNode(CompressionOp.Compress, value);
        // }
        switch (kind) {
            case Boolean:
            case Byte:
                return new NarrowNode(value, 8);
            case Char:
            case Short:
                return new NarrowNode(value, 16);
        }
        return value;
    }

    protected void lowerIndexAddressNode(IndexAddressNode indexAddress, LowTierContext context) {
        AddressNode lowered = createArrayAddress(indexAddress.graph(), indexAddress.getArray(),
                indexAddress.getArrayKind(), indexAddress.getElementKind(), indexAddress.getIndex(), context);
        indexAddress.replaceAndDelete(lowered);
    }

    public AddressNode createArrayAddress(StructuredGraph graph, ValueNode array, JavaKind arrayKind,
            JavaKind elementKind, ValueNode index, LowTierContext context) {
        int base = context.getMetaAccess().getArrayBaseOffset(arrayKind);
        return createArrayAddress(graph, array, base, elementKind, index, context.getTarget(), context.getMetaAccess());
    }

}
