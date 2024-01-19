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

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.graalvm.compiler.bytecode.ResolvedJavaMethodBytecodeProvider;
import org.graalvm.compiler.core.common.GraalOptions;
import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.core.common.type.StampPair;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.hotspot.HotSpotGraalRuntime;
import org.graalvm.compiler.hotspot.meta.BuboMetaTools;
import org.graalvm.compiler.hotspot.meta.HotSpotForeignCallDescriptor;
import org.graalvm.compiler.hotspot.meta.HotSpotForeignCallsProvider;
import org.graalvm.compiler.hotspot.meta.HotSpotForeignCallsProviderImpl;
import org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider;
import org.graalvm.compiler.hotspot.meta.HotSpotForeignCallDescriptor.Reexecutability;
import org.graalvm.compiler.hotspot.meta.BuboCache;
import org.graalvm.compiler.hotspot.replacements.DigestBaseSnippets;
import org.graalvm.compiler.lir.Variable;
import org.graalvm.compiler.lir.gen.LIRGeneratorTool;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.calc.AddNode;
import org.graalvm.compiler.nodes.extended.ForeignCallNode;
import org.graalvm.compiler.nodes.java.MethodCallTargetNode;
import org.graalvm.compiler.nodes.spi.LIRLowerable;
import org.graalvm.compiler.nodes.spi.Lowerable;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.compiler.nodes.util.GraphUtil;
import org.graalvm.compiler.replacements.InvocationPluginHelper;
import org.graalvm.compiler.replacements.SnippetCounter;
import org.graalvm.compiler.replacements.SnippetCounterNode;
import org.graalvm.compiler.replacements.SnippetSubstitutionInvocationPlugin;
import org.graalvm.compiler.replacements.StringLatin1Snippets;
import org.graalvm.compiler.replacements.SnippetCounter.Group;
import org.graalvm.compiler.replacements.nodes.LogNode;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.hotspot.HotSpotCompiledNmethod;
import jdk.vm.ci.runtime.JVMCI;
import jdk.vm.ci.runtime.JVMCIRuntime;
import jdk.vm.ci.meta.*;

import static org.graalvm.compiler.hotspot.meta.HotSpotForeignCallDescriptor.Transition.SAFEPOINT;
import static org.graalvm.compiler.hotspot.meta.HotSpotForeignCallsProviderImpl.NO_LOCATIONS;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.AddtoInstrumentationCache;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.BUBU_CACHE_DESCRIPTOR;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.INVOKE_STATIC_METHOD_ONE_ARG;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.JAVA_TIME_MILLIS;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.JAVA_TIME_NANOS;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.dummyPrintdesc;

/**
 * Marks a position in the graph where a node should be emitted.
 */
// @formatter:off
@NodeInfo(cycles = CYCLES_2,
          cyclesRationale = "",
          size = SIZE_1)
// @formatter:on
public final class CustomClockLogNode extends FixedWithNextNode implements Lowerable, LIRLowerable {

    public static final NodeClass<CustomClockLogNode> TYPE = NodeClass.create(CustomClockLogNode.class);


    public CustomClockLogNode() {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public void lower(LoweringTool tool) {
        ForeignCallNode javaCurrentCPUtime = graph().add(new ForeignCallNode(JAVA_TIME_MILLIS, EMPTY_ARRAY));
        graph().replaceFixed(this, javaCurrentCPUtime);


        // have a look at /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/src/jdk.internal.vm.compiler/src/org/graalvm/compiler/hotspot/amd64/AMD64HotSpotForeignCallsProvider.java
        // MethodCallTargetNode callTarget = graph().add(new MethodCallTargetNode(CallTargetNode.InvokeKind.Special, method, new ValueNode[0], StampPair.createSingle(StampFactory.forVoid()), null));
        // InvokeNode invokeNode = graph().add(new InvokeNode(callTarget, 0));
        // graph().replaceFixed(this, invokeNode);
        // invokeNode.setStateAfter(GraphUtil.findLastFrameState(invokeNode));
         ForeignCallNode node = graph().add(new ForeignCallNode(BUBU_CACHE_DESCRIPTOR, javaCurrentCPUtime));
         // ForeignCallNode node = graph().add(new ForeignCallNode(HotSpotHostForeignCallsProvider.TestForeignCalls.createStubCallDescriptor(JavaKind.Object), javaCurrentCPUtime));
        graph().addAfterFixed(javaCurrentCPUtime, node);
        
        //ValueNode[] targetArguments = new ValueNode[3];
        //targetArguments[0] = thread; // theread , not sure whats that needs to be
        //targetArguments[1] = ConstantNode.forConstant(tool.getproviders.getStampProvider().createMethodStamp(), javaMethod.getEncoding(), providers.getMetaAccess(), kit.getGraph());
        //targetArguments[2] = ConstantNode.defaultForKind(JavaKind.Long, kit.getGraph());

        // ForeignCallNode CacheAdd = graph().add(new ForeignCallNode(AddtoInstrumentationCache, javaCurrentCPUtime));
        // graph().addAfterFixed(javaCurrentCPUtime, CacheAdd);



    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
    }

}
