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


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.graalvm.compiler.replacements.SnippetCounter.Group;
import org.graalvm.compiler.core.CompilationWrapper.ExceptionAction;
import org.graalvm.compiler.core.Instrumentation;
import org.graalvm.compiler.core.common.CompilationIdentifier;
import org.graalvm.compiler.core.common.GraalOptions;
import org.graalvm.compiler.core.target.Backend;
import org.graalvm.compiler.debug.DebugCloseable;
import org.graalvm.compiler.nodes.GraphState;
import org.graalvm.compiler.nodes.IfNode;
import org.graalvm.compiler.nodes.Invoke;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.GraphState.StageFlag;
import org.graalvm.compiler.nodes.extended.OSRStartNode;
import org.graalvm.compiler.nodes.java.MethodCallTargetNode;
import org.graalvm.compiler.nodes.LoopBeginNode;
import org.graalvm.compiler.nodes.LoopEndNode;
import org.graalvm.compiler.nodes.SafepointNode;
import org.graalvm.compiler.nodes.StartNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.phases.tiers.MidTierContext;
import org.graalvm.compiler.replacements.SnippetCounter;
import org.graalvm.compiler.replacements.nodes.MethodHandleNode;
import org.graalvm.compiler.replacements.nodes.ResolvedMethodHandleCallTargetNode;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

import org.graalvm.compiler.debug.DebugContext;
import org.graalvm.compiler.debug.DebugHandlersFactory;
import org.graalvm.compiler.debug.DiagnosticsOutputDirectory;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.NodeFlood;
import org.graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import org.graalvm.compiler.hotspot.HotSpotBackend;
import org.graalvm.compiler.hotspot.HotSpotGraalRuntime.HotSpotGC;
import org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;
import org.graalvm.compiler.hotspot.meta.HotSpotProviders;
import org.graalvm.compiler.nodes.AbstractEndNode;
import org.graalvm.compiler.nodes.BeginNode;
import org.graalvm.compiler.nodes.CallTargetNode;
import org.graalvm.compiler.nodes.CustomInstrumentationNode;
import org.graalvm.compiler.nodes.FixedNode;
import org.graalvm.compiler.nodes.FixedWithNextNode;
import org.graalvm.compiler.options.Option;
import org.graalvm.compiler.options.OptionKey;
import org.graalvm.compiler.options.OptionType;
import org.graalvm.compiler.options.OptionValues;
import org.graalvm.compiler.phases.Phase;
import org.graalvm.compiler.phases.tiers.HighTierContext;

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
    private final SnippetCounter.Group group;


    public CustomInstrumentationPhase(SnippetCounter.Group group) {
        this.group = group;
        optional = true;
    }


    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, HighTierContext context) {

            // for (ResolvedJavaMethod method : graph.getMethods()) {               
            //     System.out.println("ResolvedJavaMethod  "+method.getName());
            // }


            // for (Node node : graph.getNodes()) {
            //     System.out.println("Node  " + node.toString());
            //     //System.out.println("Node  asJavaConstant " + methodcall.asNode().asJavaConstant().toString());
            //     //System.out.println("Node  getNodeSourcePosition " + methodcall.getNodeSourcePosition().toString());
            //     //System.out.println("Node  DebugProperties" + node.getDebugProperties());

            // }


            // for (StartNode methodcall : graph.getNodes(StartNode.TYPE)) {
            //     System.out.println("Node  " + methodcall.getDebugProperties());
            //     System.out.println("Node  getCurrentScopeName " + methodcall.getDebug().getCurrentScopeName());
            //     System.out.println("Node  toString "+methodcall.toString());
            // }


            for (Invoke invokes : graph.getInvokes()) {
                //System.out.println(invokes.callTarget().targetName());
                try (DebugCloseable s = invokes.asFixedNode().withNodeSourcePosition()) {
                CustomInstrumentationNode CustomInstrumentationNode = graph.add(new CustomInstrumentationNode(invokes.callTarget().targetName(),group));
                graph.addBeforeFixed(invokes.asFixedNode(), CustomInstrumentationNode);  
                }             
            }

            // for (LoopBeginNode loopBeginNode : graph.getNodes(LoopBeginNode.TYPE)) {

            //     for (FixedNode node :  loopBeginNode.getBlockNodes()) {
            //         //System.out.println("First loop: " + node.toString());
            //         // find all if nodes follwoing the loop begiun
            //         if (node.getClass().equals(IfNode.class)) {
            //              //System.out.println("in loop loop: " + node.toString());
            //              IfNode ifnode = ((IfNode)node);
            //              // find all of the begin nodes that follow the if
            //              for (Node sucnode  : ifnode.cfgSuccessors()) {
            //                 CustomInstrumentationNode CustomInstrumentationNode = graph.add(new CustomInstrumentationNode(sucnode.toString() + sucnode.getDebug().toString(),group));
            //                 graph.addAfterFixed((FixedWithNextNode) sucnode, CustomInstrumentationNode);
            //              }
            //         }
            //     }
               
            //     for (LoopEndNode loopEndNode : loopBeginNode.loopEnds()) {
                        
            //             try (DebugCloseable s = loopEndNode.withNodeSourcePosition()) {
            //                 CustomInstrumentationNode CustomInstrumentationNode = graph.add(new CustomInstrumentationNode(loopEndNode.toString(),group));
            //                 graph.addBeforeFixed(loopEndNode, CustomInstrumentationNode);

            //             }
                    
            //     }
            // }
            
        
    }



}
