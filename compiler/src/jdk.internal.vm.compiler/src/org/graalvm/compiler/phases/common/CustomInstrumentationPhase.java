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


import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.BUBU_CACHE_DESCRIPTOR;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.JAVA_TIME_NANOS;
import java.util.Optional;

import org.graalvm.compiler.core.common.CompilationIdentifier.Verbosity;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.debug.DebugCloseable;
import org.graalvm.compiler.hotspot.meta.Bubo.BuboCache;
import org.graalvm.compiler.nodes.GraphState;
import org.graalvm.compiler.nodes.calc.AddNode;
import org.graalvm.compiler.nodes.calc.SubNode;
import org.graalvm.compiler.nodes.extended.ForeignCallNode;
import org.graalvm.compiler.nodes.java.LoadFieldNode;
import org.graalvm.compiler.nodes.java.NewArrayNode;
import org.graalvm.compiler.nodes.java.StoreFieldNode;
import org.graalvm.compiler.nodes.java.StoreIndexedNode;
import org.graalvm.compiler.nodes.ReturnNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.replacements.SnippetCounter;
import org.graalvm.compiler.replacements.nodes.LogNode;
import org.graalvm.compiler.nodes.ConstantNode;
import org.graalvm.compiler.nodes.CustomClockLogNode;
import org.graalvm.compiler.options.Option;
import org.graalvm.compiler.options.OptionKey;
import org.graalvm.compiler.options.OptionType;
import org.graalvm.compiler.phases.tiers.HighTierContext;
import org.graalvm.compiler.phases.tiers.LowTierContext;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.JavaConstant;
/**
 * Adds CustomInstrumentation to loops.
 */
public class CustomInstrumentationPhase extends BasePhase<HighTierContext>  {

    public static class Options {

        // @formatter:off
        @Option(help = "Disable Custom Instrumentation Phase", type = OptionType.Debug)
        public static final OptionKey<Boolean> DisablCIP = new OptionKey<>(true);
        // @formatter:on
    }

    public enum Optionality {
        Optional,
        Required;
    }



    @Override
    public boolean checkContract() {
        // the size / cost after is highly dynamic and dependent on the graph, thus we do not verify
        // costs for this phase
        return false;
    }

    @Override
    public Optional<NotApplicable> notApplicableTo(GraphState graphState) {
        return ALWAYS_APPLICABLE;
    }

    private final boolean optional;


    public CustomInstrumentationPhase() {
        optional = true;
    }


    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, HighTierContext context) {
            ForeignCallNode[] returnNodesTime =  new ForeignCallNode[graph.getNodes(ReturnNode.TYPE).count()];
            ForeignCallNode startTime = graph.add(new ForeignCallNode(JAVA_TIME_NANOS, ValueNode.EMPTY_ARRAY));
            graph.addAfterFixed(graph.start(), startTime);

            int pointer = 0;
            for (ReturnNode returnNode : graph.getNodes(ReturnNode.TYPE)) {
                
                try (DebugCloseable s = returnNode.asFixedNode().withNodeSourcePosition()) {
                ForeignCallNode javaCurrentCPUtime = graph.add(new ForeignCallNode(JAVA_TIME_NANOS, ValueNode.EMPTY_ARRAY));
                graph.addBeforeFixed(returnNode, javaCurrentCPUtime);
                returnNodesTime[pointer] = javaCurrentCPUtime;
                pointer++;
                }          
            }
            // get comp ID
            Long id = Long.parseLong(graph.compilationId().toString(Verbosity.ID).split("-")[1]);
            ValueNode ID = graph.addWithoutUnique(new ConstantNode(JavaConstant.forLong(id), StampFactory.forKind(JavaKind.Long)));

            for (ForeignCallNode returnNode : returnNodesTime) {
                
                SubNode Time = graph.addWithoutUnique(new SubNode(returnNode,startTime));

                 try {
                    //Read the buffer form the static class
                LoadFieldNode readBuffer = graph.add(LoadFieldNode.create(null, null, context.getMetaAccess().lookupJavaField(BuboCache.class.getField("Buffer"))));
                graph.addAfterFixed(returnNode, readBuffer);

                // read the pointer from the static class
                LoadFieldNode readPointer = graph.add(LoadFieldNode.create(null, null, context.getMetaAccess().lookupJavaField(BuboCache.class.getField("pointer"))));
                graph.addAfterFixed(readBuffer, readPointer);

                // write to the read buffer the ID using the pointer as the index
                StoreIndexedNode writeToBufferID = graph.add(new StoreIndexedNode(readBuffer, readPointer.asNode(), null, null, JavaKind.Long, ID));
                graph.addAfterFixed(readPointer,writeToBufferID);

                // add one to the pointer
                ValueNode one = graph.addWithoutUnique(new ConstantNode(JavaConstant.forInt(1), StampFactory.forKind(JavaKind.Int)));
                AddNode pointerPlus1 = graph.addWithoutUnique(new AddNode(readPointer, one));

                // write to the buffer the time using the incremented pointer
                StoreIndexedNode writeToBufferTime = graph.add(new StoreIndexedNode(readBuffer, pointerPlus1, null, null, JavaKind.Long, Time));
                graph.addAfterFixed(writeToBufferID,writeToBufferTime);

                // write the changed buffer back to the static class
                StoreFieldNode WriteBufferBack = graph.add(new StoreFieldNode(null, context.getMetaAccess().lookupJavaField(BuboCache.class.getField("Buffer")), readBuffer));
                graph.addAfterFixed(writeToBufferTime,WriteBufferBack);

                    // add one for the pointer again
                AddNode pointerPlus2 = graph.addWithoutUnique(new AddNode(pointerPlus1, one));

                    // write the pointer back to the static class
                StoreFieldNode WritePointerBack =  graph.add(new StoreFieldNode(null, context.getMetaAccess().lookupJavaField(BuboCache.class.getField("pointer")), pointerPlus2));
                graph.addAfterFixed(WriteBufferBack, WritePointerBack);

                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO: handle exception
                }



                //CustomClockLogNode logClock = graph.add(new CustomClockLogNode(Time,returnNode));
                //graph.addAfterFixed(returnNode, logClock);
           }
    }


}
