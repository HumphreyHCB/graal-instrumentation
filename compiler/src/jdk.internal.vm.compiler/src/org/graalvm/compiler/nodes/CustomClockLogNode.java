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
import org.graalvm.compiler.hotspot.meta.Bubo.BuboCache;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.calc.AddNode;
import org.graalvm.compiler.nodes.calc.IntegerEqualsNode;
import org.graalvm.compiler.nodes.calc.SubNode;
import org.graalvm.compiler.nodes.calc.IntegerLessThanNode.LessThanOp;
import org.graalvm.compiler.nodes.extended.BranchProbabilityNode;
import org.graalvm.compiler.nodes.extended.ForeignCallNode;
import org.graalvm.compiler.nodes.java.LoadFieldNode;
import org.graalvm.compiler.nodes.java.NewArrayNode;
import org.graalvm.compiler.nodes.java.StoreFieldNode;
import org.graalvm.compiler.nodes.java.StoreIndexedNode;
import org.graalvm.compiler.nodes.memory.MemoryAccess;
import org.graalvm.compiler.nodes.memory.MemoryKill;
import org.graalvm.compiler.nodes.spi.LIRLowerable;
import org.graalvm.compiler.nodes.spi.Lowerable;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.*;

// import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.BUBU_CACHE_DESCRIPTOR;
// import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.BUBU_CACHE_ROTATEBUFFER;

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

                try {
                    

                LoadFieldNode readBuffer = graph().add(LoadFieldNode.create(null, null, tool.getMetaAccess().lookupJavaField(BuboCache.class.getField("Buffer"))));
                graph().addAfterFixed(returnNode, readBuffer);

                LoadFieldNode readPointer = graph().add(LoadFieldNode.create(null, null, tool.getMetaAccess().lookupJavaField(BuboCache.class.getField("pointer"))));
                graph().addAfterFixed(readBuffer, readPointer);
                    
                StoreIndexedNode writeToBufferID = graph().add(new StoreIndexedNode(readBuffer, readPointer, null, null, JavaKind.Long, ID));
                graph().addAfterFixed(readPointer,writeToBufferID);

                ValueNode one = graph().addWithoutUnique(new ConstantNode(JavaConstant.forInt(1), StampFactory.forKind(JavaKind.Int)));
                AddNode pointerPlus1 = graph().addWithoutUnique(new AddNode(readPointer, one));

                StoreIndexedNode writeToBufferTime = graph().add(new StoreIndexedNode(readBuffer, pointerPlus1, null, null, JavaKind.Long, Time));
                graph().addAfterFixed(writeToBufferID,writeToBufferTime);

                StoreFieldNode WriteBufferBack = graph().add(new StoreFieldNode(null, tool.getMetaAccess().lookupJavaField(BuboCache.class.getField("Buffer")), readBuffer));
                graph().addAfterFixed(writeToBufferTime,WriteBufferBack);


                AddNode pointerPlus2 = graph().addWithoutUnique(new AddNode(pointerPlus1, one));


                StoreFieldNode WritePointerBack =  graph().add(new StoreFieldNode(null, tool.getMetaAccess().lookupJavaField(BuboCache.class.getField("pointer")), pointerPlus2));
                graph().addAfterFixed(WriteBufferBack, WritePointerBack);

                graph().replaceFixed(this, WritePointerBack);
                

                // EndNode trueEnd = graph().addWithoutUnique(new EndNode());
                // EndNode falseEnd = graph().addWithoutUnique(new EndNode());
        
                // BeginNode trueBegin = graph().addWithoutUnique(new BeginNode());
                // trueBegin.setNext(trueEnd);
                // BeginNode falseBegin = graph().addWithoutUnique(new BeginNode());
                // falseBegin.setNext(falseEnd);

                // ValueNode pointerMax = graph().addWithoutUnique(new ConstantNode(JavaConstant.forInt(250_000_000), StampFactory.forKind(JavaKind.Int)));
                // IntegerEqualsNode doesPointerEquallMax = graph().add(new IntegerEqualsNode(pointerPlus2, pointerMax));
    
                // // condition is currently null, hence the crash
                // IfNode shouldBufferRotate = graph().add(new IfNode(doesPointerEquallMax, trueBegin, falseBegin, BranchProbabilityNode.NOT_FREQUENT_PROFILE));
                
    
                // MergeNode merge = graph().add(new MergeNode());
                // merge.addForwardEnd(trueEnd);
                // merge.addForwardEnd(falseEnd);
                
                // writeToBufferTime.setNext(shouldBufferRotate);
                // merge.setNext(WritePointerBack);



               
                //graph().addAfterFixed(WritePointerBack, doesPointerEquallMax);
                
                //ForeignCallNode rotateBufferCall = graph().add(new ForeignCallNode(BUBU_CACHE_ROTATEBUFFER, ValueNode.EMPTY_ARRAY));
                //graph().addAfterFixed(WritePointerBack, rotateBufferCall);
                
                
                
                //graph().addAfterFixed(WritePointerBack, rotateBufferCall);

                
                // readBuffer.lower(tool);
                // readPointer.lower(tool);
                
                // writeToBufferID.lower(tool);
                // writeToBufferTime.lower(tool);
                
                // WriteBufferBack.lower(tool);
                // WritePointerBack.lower(tool);


            } catch (Exception e) {

                e.printStackTrace();
                // TODO: handle exception
            }





    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
    }


}
