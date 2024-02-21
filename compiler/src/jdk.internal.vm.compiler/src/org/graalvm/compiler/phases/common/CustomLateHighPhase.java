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
 * Adds CustomInstrumentation to loops.
 */
public class CustomLateHighPhase extends BasePhase<HighTierContext> {

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

    public CustomLateHighPhase(Group group) {
        this.group = group;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, HighTierContext context) {

        try {
            
            int id = Integer.parseInt(graph.compilationId().toString(Verbosity.ID).split("-")[1]);
            ValueNode ID = graph
                    .addWithoutUnique(new ConstantNode(JavaConstant.forInt(id), StampFactory.forKind(JavaKind.Int)));

            // Read the buffer form the static class
            LoadFieldNode readBuffer = graph.add(LoadFieldNode.create(null, null,
                    context.getMetaAccess().lookupJavaField(BuboCache.class.getField("Buffer"))));
            graph.addAfterFixed(graph.start(), readBuffer);

            AddressNode address = createArrayAddress(graph, readBuffer,
                    context.getMetaAccess().getArrayBaseOffset(JavaKind.Long), JavaKind.Long, ID,
                    context.getMetaAccess());
            address.setStamp(StampFactory.forBuboVoid());


            JavaReadNode memoryWrite = graph.add(new JavaReadNode(JavaKind.Long, address,
            NamedLocationIdentity.getArrayLocation(JavaKind.Long), BarrierType.ARRAY, null, false));
            
            graph.addAfterFixed(readBuffer, memoryWrite);
            
            ValueNode[] list = new ValueNode[]{address};
            ReachabilityFenceNode fenceNode = graph.add(ReachabilityFenceNode.create(list));
            graph.addAfterFixed(memoryWrite, fenceNode);


        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }

    }

    public AddressNode createArrayAddress(StructuredGraph graph, ValueNode array, int arrayBaseOffset,
            JavaKind elementKind, ValueNode index, MetaAccessProvider metaAccess) {
        ValueNode wordIndex;
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

    protected void lowerLoadFieldNode(LoadFieldNode loadField, HighTierContext context) {
        // assert loadField.getStackKind() != JavaKind.Illegal : loadField;
        StructuredGraph graph = loadField.graph();
        ResolvedJavaField field = loadField.field();
        ValueNode object = loadField.isStatic() ? staticFieldBase(graph, field, context) : loadField.object();
        // object = createNullCheckedValue(object, loadField, tool);
        Stamp loadStamp = loadStamp(loadField.stamp(NodeView.DEFAULT), getStorageKind(field, context));

        AddressNode address = createFieldAddress(graph, object, field);

        // BarrierType barrierType =
        // context.getPlatformConfigurationProvider().getBarrierSet().fieldReadBarrierType(field,
        // getStorageKind(field, context));
        ReadNode memoryRead = graph
                .add(new ReadNode(address, overrideFieldLocationIdentity(loadField.getLocationIdentity()), loadStamp,
                        null, loadField.getMemoryOrder()));
        ValueNode readValue = implicitLoadConvert(graph, getStorageKind(field, context), memoryRead);
        loadField.replaceAtUsages(readValue);
        graph.replaceFixed(loadField, memoryRead);
    }

    public final JavaKind getStorageKind(ResolvedJavaField field, HighTierContext context) {
        return getStorageKind(field.getType(), context);
    }

    public final JavaKind getStorageKind(JavaType type, HighTierContext context) {
        return context.getMetaAccessExtensionProvider().getStorageKind(type);
    }

    public FieldLocationIdentity overrideFieldLocationIdentity(FieldLocationIdentity fieldIdentity) {
        return fieldIdentity;
    }

    public ValueNode staticFieldBase(StructuredGraph graph, ResolvedJavaField f, HighTierContext context) {
        HotSpotResolvedJavaField field = (HotSpotResolvedJavaField) f;

        JavaConstant base = context.getProviders().getConstantReflection().asJavaClass(field.getDeclaringClass());
        return ConstantNode.forConstant(base, context.getMetaAccess(), graph);
    }

    public AddressNode createOffsetAddress(StructuredGraph graph, ValueNode object, long offset) {
        ValueNode o = ConstantNode.forIntegerKind(JavaKind.Long, offset, graph);
        return graph.unique(new OffsetAddressNode(object, o));
    }

    public AddressNode createFieldAddress(StructuredGraph graph, ValueNode object, ResolvedJavaField field) {
        int offset = field.getOffset();
        if (offset >= 0) {
            return createOffsetAddress(graph, object, offset);
        } else {
            throw GraalError.shouldNotReachHere(
                    "Field is missing: " + field.getDeclaringClass().toJavaName(true) + "." + field.getName());
        }
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

    public Stamp loadStamp(Stamp stamp, JavaKind kind) {
        return loadStamp(stamp, kind, true);
    }

    protected Stamp loadStamp(Stamp stamp, JavaKind kind, boolean compressible) {

        switch (kind) {
            case Boolean:
            case Byte:
                return IntegerStamp.OPS.getNarrow().foldStamp(32, 8, stamp);
            case Char:
            case Short:
                return IntegerStamp.OPS.getNarrow().foldStamp(32, 16, stamp);
        }
        return stamp;
    }

    public final ValueNode implicitLoadConvert(StructuredGraph graph, JavaKind kind, ValueNode value) {
        return implicitLoadConvert(graph, kind, value, true);
    }

    public ValueNode implicitLoadConvert(JavaKind kind, ValueNode value) {
        return implicitLoadConvert(kind, value, true);
    }

    protected final ValueNode implicitLoadConvert(StructuredGraph graph, JavaKind kind, ValueNode value,
            boolean compressible) {
        ValueNode ret = implicitLoadConvert(kind, value, compressible);
        if (!ret.isAlive()) {
            ret = graph.addOrUnique(ret);
        }
        return ret;
    }

    protected ValueNode implicitLoadConvert(JavaKind kind, ValueNode value, boolean compressible) {
        // if (useCompressedOops(kind, compressible)) {
        // return newCompressionNode(CompressionOp.Uncompress, value);
        // }

        switch (kind) {
            case Byte:
            case Short:
                return new SignExtendNode(value, 32);
            case Boolean:
                // case Char:
                // return new ZeroExtendNode(value, 32);
        }
        return value;
    }
    // protected void lowerIndexAddressNode(IndexAddressNode indexAddress,
    // HighTierContext context) {
    // AddressNode lowered = createArrayAddress(indexAddress.graph(),
    // indexAddress.getArray(), indexAddress.getArrayKind(),
    // indexAddress.getElementKind(), indexAddress.getIndex(), context);
    // indexAddress.replaceAndDelete(lowered);
    // }

    // public AddressNode createArrayAddress(StructuredGraph graph, ValueNode array,
    // JavaKind arrayKind, JavaKind elementKind, ValueNode index, HighTierContext
    // context) {
    // int base = context.getMetaAccess().getArrayBaseOffset(arrayKind);
    // return createArrayAddress(graph, array, base, elementKind, index,
    // context.getTarget(),context.getMetaAccess());
    // }
}