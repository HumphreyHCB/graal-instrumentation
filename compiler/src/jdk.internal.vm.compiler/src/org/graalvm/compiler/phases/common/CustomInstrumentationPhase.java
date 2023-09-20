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


import java.util.Optional;

import org.graalvm.compiler.debug.DebugCloseable;
import org.graalvm.compiler.nodes.GraphState;
import org.graalvm.compiler.nodes.IfNode;
import org.graalvm.compiler.nodes.GraphState.StageFlag;
import org.graalvm.compiler.nodes.java.MethodCallTargetNode;
import org.graalvm.compiler.nodes.LoopBeginNode;
import org.graalvm.compiler.nodes.LoopEndNode;
import org.graalvm.compiler.nodes.SafepointNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.phases.tiers.MidTierContext;

import jdk.vm.ci.meta.Value;

import org.graalvm.compiler.debug.DebugContext;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.NodeFlood;
import org.graalvm.compiler.nodes.AbstractEndNode;
import org.graalvm.compiler.nodes.BeginNode; 
import org.graalvm.compiler.nodes.CustomInstrumentationNode;
import org.graalvm.compiler.nodes.FixedNode;
import org.graalvm.compiler.options.Option;
import org.graalvm.compiler.options.OptionKey;
import org.graalvm.compiler.options.OptionType;
import org.graalvm.compiler.phases.Phase;
import org.graalvm.compiler.phases.tiers.HighTierContext;

/**
 * Adds safepoints to loops.
 */
public class CustomInstrumentationPhase extends BasePhase<MidTierContext> {

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
        //this.optional = optionality == Optionality.Optional;
        optional = true;
    }


    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, MidTierContext context) {
        //if (optional && Options.DisablCIP.getValue(graph.getOptions())) {
        //    return;
        //}
        // keep it simple, come back to this 

    
            for (LoopBeginNode loopBeginNode : graph.getNodes(LoopBeginNode.TYPE)) {

                for (FixedNode node :  loopBeginNode.getBlockNodes()) {
                    if (node.getClass().equals(IfNode.class)) {
                         IfNode ifnode = ((IfNode)node);
                         CustomInstrumentationNode CustomInstrumentationNode = graph.add(new CustomInstrumentationNode());
                         graph.addAfterFixed(ifnode.getSuccessor(true), CustomInstrumentationNode);
                    }
                }
               
                for (LoopEndNode loopEndNode : loopBeginNode.loopEnds()) {
                        
                        try (DebugCloseable s = loopEndNode.withNodeSourcePosition()) {
                            CustomInstrumentationNode CustomInstrumentationNode = graph.add(new CustomInstrumentationNode());
                            graph.addBeforeFixed(loopEndNode, CustomInstrumentationNode);

                        }
                    
                }
            }
        
    }

}
