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
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.JAVA_TIME_MILLIS;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.JAVA_TIME_NANOS;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.TEST_PRINT_DESCRIPTOR;
import static org.graalvm.compiler.hotspot.replacements.Log.LOG_OBJECT;
import static org.graalvm.compiler.hotspot.replacements.Log.LOG_PRIMITIVE;
import static org.graalvm.compiler.hotspot.replacements.Log.LOG_PRINTF;
import static org.graalvm.compiler.nodeinfo.InputType.Anchor;

import java.util.Optional;

import org.graalvm.compiler.asm.amd64.AMD64Address;
import org.graalvm.compiler.core.amd64.AMD64AddressLowering;
import org.graalvm.compiler.core.amd64.AMD64AddressNode;
import org.graalvm.compiler.core.common.CompilationIdentifier.Verbosity;
import org.graalvm.compiler.core.common.memory.BarrierType;
import org.graalvm.compiler.core.common.memory.MemoryOrderMode;
import org.graalvm.compiler.core.common.spi.JavaConstantFieldProvider;
import org.graalvm.compiler.core.common.type.BuboVoidStamp;
import org.graalvm.compiler.core.common.type.IntegerStamp;
import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.debug.DebugCloseable;
import org.graalvm.compiler.graph.iterators.NodeIterable;
import org.graalvm.compiler.hotspot.meta.Bubo.BuboCache;
import org.graalvm.compiler.lir.amd64.AMD64AddressValue;
import org.graalvm.compiler.nodes.GraphState;
import org.graalvm.compiler.nodes.IfNode;
import org.graalvm.compiler.nodes.MergeNode;
import org.graalvm.compiler.nodes.NamedLocationIdentity;
import org.graalvm.compiler.nodes.NodeView;
import org.graalvm.compiler.nodes.PiNode;
import org.graalvm.compiler.nodes.calc.AddNode;
import org.graalvm.compiler.nodes.calc.IntegerEqualsNode;
import org.graalvm.compiler.nodes.calc.LeftShiftNode;
import org.graalvm.compiler.nodes.calc.NarrowNode;
import org.graalvm.compiler.nodes.calc.SignExtendNode;
import org.graalvm.compiler.nodes.calc.SubNode;
import org.graalvm.compiler.nodes.debug.BlackholeNode;
import org.graalvm.compiler.nodes.debug.ControlFlowAnchorNode;
import org.graalvm.compiler.nodes.extended.AnchoringNode;
import org.graalvm.compiler.nodes.extended.BranchProbabilityNode;
import org.graalvm.compiler.nodes.extended.ForeignCallNode;
import org.graalvm.compiler.nodes.extended.GuardingNode;
import org.graalvm.compiler.nodes.extended.JavaReadNode;
import org.graalvm.compiler.nodes.extended.JavaWriteNode;
import org.graalvm.compiler.nodes.gc.BarrierSet;
import org.graalvm.compiler.nodes.gc.G1PostWriteBarrier;
import org.graalvm.compiler.nodes.gc.WriteBarrier;
import org.graalvm.compiler.nodes.java.LoadFieldNode;
import org.graalvm.compiler.nodes.java.LoadIndexedNode;
import org.graalvm.compiler.nodes.java.NewArrayNode;
import org.graalvm.compiler.nodes.java.ReachabilityFenceNode;
import org.graalvm.compiler.nodes.java.StoreFieldNode;
import org.graalvm.compiler.nodes.java.StoreIndexedNode;
import org.graalvm.compiler.nodes.memory.ReadNode;
import org.graalvm.compiler.nodes.memory.SideEffectFreeWriteNode;
import org.graalvm.compiler.nodes.memory.WriteNode;
import org.graalvm.compiler.nodes.memory.address.AddressNode;
import org.graalvm.compiler.nodes.memory.address.IndexAddressNode;
import org.graalvm.compiler.nodes.memory.address.OffsetAddressNode;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.util.GraphUtil;
import org.graalvm.compiler.nodes.ReturnNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.phases.common.LoweringPhase.LoweringToolImpl;
import org.graalvm.compiler.replacements.SnippetCounter;
import org.graalvm.compiler.replacements.SnippetCounterNode;
import org.graalvm.compiler.replacements.SnippetCounter.Group;
import org.graalvm.compiler.replacements.nodes.LogNode;
import org.graalvm.compiler.word.WordCastNode;
import org.graalvm.word.impl.WordBoxFactory;
import org.graalvm.compiler.nodes.AbstractBeginNode;
import org.graalvm.compiler.nodes.AbstractMergeNode;
import org.graalvm.compiler.nodes.BeginNode;
import org.graalvm.compiler.nodes.ConstantNode;
import org.graalvm.compiler.nodes.CustomClockLogNode;
import org.graalvm.compiler.nodes.EndNode;
import org.graalvm.compiler.nodes.FixedNode;
import org.graalvm.compiler.nodes.FloatingAnchoredNode;
import org.graalvm.compiler.options.Option;
import org.graalvm.compiler.options.OptionKey;
import org.graalvm.compiler.options.OptionType;
import org.graalvm.compiler.phases.tiers.HighTierContext;
import org.graalvm.compiler.phases.tiers.LowTierContext;
import org.graalvm.compiler.phases.tiers.MidTierContext;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaField;
import org.graalvm.compiler.core.common.type.ObjectStamp;
import org.graalvm.compiler.nodes.FieldLocationIdentity;

import org.graalvm.compiler.debug.GraalError;
import jdk.vm.ci.meta.JavaConstant;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaField;
import jdk.vm.ci.meta.JavaConstant;

/**
 * Adds ReadNode & Addres to Start of the graphg, this will be use in the Low Ter Instrumentation phase .
 */
public class BuboInstrumentationHighTierPhase extends BasePhase<HighTierContext> {

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

    public BuboInstrumentationHighTierPhase() {
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, HighTierContext context) {

        try {
            // the ID for this Compilation
            int id = Integer.parseInt(graph.compilationId().toString(Verbosity.ID).split("-")[1]);
            ValueNode ID = graph
                    .addWithoutUnique(new ConstantNode(JavaConstant.forInt(id), StampFactory.forKind(JavaKind.Int)));

            // Read the buffer form the static class
            LoadFieldNode readBuffer = graph.add(LoadFieldNode.create(null, null,
                    context.getMetaAccess().lookupJavaField(BuboCache.class.getField("Buffer"))));
            graph.addAfterFixed(graph.start(), readBuffer);

            // create the address, and give it our unique stamp for identifaction later on
            AddressNode address = createArrayAddress(graph, readBuffer,
                    context.getMetaAccess().getArrayBaseOffset(JavaKind.Long), JavaKind.Long, ID,
                    context.getMetaAccess());
            address.setStamp(StampFactory.forBuboVoid());

            // add a dummy read node, this node is not used beyond the adress it use will be use later on in the low teir instrumentation phase
            JavaReadNode memoryRead = graph.add(new JavaReadNode(JavaKind.Long, address,
            NamedLocationIdentity.getArrayLocation(JavaKind.Long), BarrierType.ARRAY, null, false));
            
            graph.addAfterFixed(readBuffer, memoryRead);
            
            // add a ReachabilityFenceNode this should stop our address from being optmised out
            ValueNode[] list = new ValueNode[]{address};
            ReachabilityFenceNode fenceNode = graph.add(ReachabilityFenceNode.create(list));
            graph.addAfterFixed(memoryRead, fenceNode);


        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }

    }

    public AddressNode createArrayAddress(StructuredGraph graph, ValueNode array, int arrayBaseOffset,
            JavaKind elementKind, ValueNode index, MetaAccessProvider metaAccess) {
        ValueNode wordIndex;
        // this temproy work around the value 8 should not be hard codes
        if (8 > 4) {
            wordIndex = graph.unique(new SignExtendNode(index, 8 * 8));
        } else {
            assert 8 == 4 : "unsupported word size";
            wordIndex = index;
        }
        int shift = CodeUtil.log2(metaAccess.getArrayIndexScale(elementKind));
        ValueNode scaledIndex = graph.unique(new LeftShiftNode(wordIndex, ConstantNode.forInt(shift, graph)));
        ValueNode offset = graph
                .unique(new AddNode(scaledIndex, ConstantNode.forIntegerKind(JavaKind.Long, arrayBaseOffset, graph)));
        return graph.unique(new OffsetAddressNode(array, offset));
    }



}