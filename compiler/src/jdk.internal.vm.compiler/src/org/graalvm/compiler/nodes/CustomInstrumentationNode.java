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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.graalvm.compiler.core.common.GraalOptions;
import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.hotspot.HotSpotGraalRuntime;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.spi.LIRLowerable;
import org.graalvm.compiler.nodes.spi.Lowerable;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.compiler.replacements.SnippetCounter;
import org.graalvm.compiler.replacements.SnippetCounterNode;
import org.graalvm.compiler.replacements.SnippetCounter.Group;
import org.graalvm.compiler.replacements.nodes.LogNode;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.*;

/**
 * Marks a position in the graph where a node should be emitted.
 */
// @formatter:off
@NodeInfo(cycles = CYCLES_2,
          cyclesRationale = "",
          size = SIZE_1)
// @formatter:on
public final class CustomInstrumentationNode extends FixedWithNextNode implements Lowerable, LIRLowerable{

    public static final NodeClass<CustomInstrumentationNode> TYPE = NodeClass.create(CustomInstrumentationNode.class);

    private final String Method;

    private Group group;

    public CustomInstrumentationNode(String methodName, Group group) {
        super(TYPE, StampFactory.forVoid());
        Method = methodName;
        this.group = group;
    }


    @Override
    public void lower(LoweringTool tool) {

        ConstantNode constNextInt = graph().addWithoutUnique(new ConstantNode(JavaConstant.forInt(1), StampFactory.forUnsignedInteger(32)));
        SnippetCounter counter = new SnippetCounter(group, ("Humphrey Method: " + Method), "Humphrey: This is my counter ...");
        SnippetCounterNode snippetCounter = graph().add(new SnippetCounterNode(counter, constNextInt));
        graph().replaceFixed(this, snippetCounter);
        
        //look at log node sniipets
        LogNode log = graph().add(new LogNode("The Current Value of the Counter is: " , new ConstantNode(JavaConstant.forLong(snippetCounter.getCounter().value()), StampFactory.forUnsignedInteger(32)) ,null,null));

        graph().addBeforeFixed(snippetCounter.asFixedNode(), log);  
        
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generate'");     
    }

    
}
