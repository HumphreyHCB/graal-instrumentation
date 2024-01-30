/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.nodes;

import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import org.graalvm.compiler.core.common.CompilationIdentifier.Verbosity;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.graph.Node.Input;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.calc.SubNode;
import org.graalvm.compiler.nodes.extended.ForeignCallNode;
import org.graalvm.compiler.nodes.java.NewArrayNode;
import org.graalvm.compiler.nodes.java.StoreIndexedNode;
import org.graalvm.compiler.nodes.memory.MemoryAccess;
import org.graalvm.compiler.nodes.memory.MemoryKill;
import org.graalvm.compiler.nodes.spi.LIRLowerable;
import org.graalvm.compiler.nodes.spi.Lowerable;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.*;

import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.BUBU_CACHE_DESCRIPTOR;

/**
 * Marks a position in the graph() where a node should be emitted.
 */
// @formatter:off
@NodeInfo(cycles = CYCLES_2,
          cyclesRationale = "",
          size = SIZE_1)
// @formatter:on
public final class CustomClockLogNode extends FixedWithNextNode implements Lowerable, LIRLowerable {

    public static final NodeClass<CustomClockLogNode> TYPE = NodeClass.create(CustomClockLogNode.class);
    
    @Input
    private SubNode Time;

    @Input
    private ForeignCallNode returnNode;

    public CustomClockLogNode(SubNode Time, ForeignCallNode returnNode) {
        super(TYPE, StampFactory.forVoid());
        this.Time = Time;
        this.returnNode = returnNode;
    }

    @Override
    public void lower(LoweringTool tool) {
        Long id = Long.parseLong(graph().compilationId().toString(Verbosity.ID).split("-")[1]);
        ValueNode ID = graph().addWithoutUnique(new ConstantNode(JavaConstant.forLong(id), StampFactory.forKind(JavaKind.Long)));


                //create and array and add to the graph
                ValueNode length = graph().addWithoutUnique(new ConstantNode(JavaConstant.forInt(2), StampFactory.forKind(JavaKind.Int)));
                NewArrayNode array = graph().add(new NewArrayNode( tool.getMetaAccess().lookupJavaType(Long.TYPE), length, true));
                graph().addBeforeFixed(returnNode, array);

                // add the ID to the first index in the array
                ValueNode IDindex = graph().addWithoutUnique(new ConstantNode(JavaConstant.forInt(0), StampFactory.forKind(JavaKind.Int)));
                StoreIndexedNode storeID = graph().add(new StoreIndexedNode(array, IDindex, null, null, JavaKind.Long, ID));
                graph().addAfterFixed(array, storeID);

                // add the time to the array 
                ValueNode Timeindex = graph().addWithoutUnique(new ConstantNode(JavaConstant.forInt(1), StampFactory.forKind(JavaKind.Int)));
                StoreIndexedNode storeTime = graph().add(new StoreIndexedNode(array, Timeindex, null, null, JavaKind.Long, Time));
                graph().addAfterFixed(storeID, storeTime);

                // send the array off to be added to the cache
                ForeignCallNode node = graph().add(new ForeignCallNode(BUBU_CACHE_DESCRIPTOR, array));
                graph().addAfterFixed(returnNode, node);
                graph().replaceFixed(this, node);

                storeID.lower(tool);
                storeTime.lower(tool);

    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
    }


}
