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


import static org.graalvm.compiler.core.common.GraalOptions.OptEliminateGuards;
import static org.graalvm.compiler.hotspot.meta.HotSpotHostForeignCallsProvider.JAVA_TIME_NANOS;
import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_IGNORED;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_IGNORED;
import static org.graalvm.compiler.nodes.memory.MemoryKill.NO_LOCATION;
import static org.graalvm.compiler.phases.common.LoweringPhase.ProcessBlockState.ST_ENTER;
import static org.graalvm.compiler.phases.common.LoweringPhase.ProcessBlockState.ST_ENTER_ALWAYS_REACHED;
import static org.graalvm.compiler.phases.common.LoweringPhase.ProcessBlockState.ST_LEAVE;
import static org.graalvm.compiler.phases.common.LoweringPhase.ProcessBlockState.ST_PROCESS;
import static org.graalvm.compiler.phases.common.LoweringPhase.ProcessBlockState.ST_PROCESS_ALWAYS_REACHED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.graalvm.collections.EconomicSet;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.debug.CounterKey;
import org.graalvm.compiler.debug.DebugCloseable;
import org.graalvm.compiler.debug.DebugContext;
import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.debug.TTY;
import org.graalvm.compiler.debug.TimerKey;
import org.graalvm.compiler.graph.Graph;
import org.graalvm.compiler.graph.Graph.Mark;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.NodeBitMap;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.graph.NodeMap;
import org.graalvm.compiler.graph.NodeSourcePosition;
import org.graalvm.compiler.graph.iterators.NodeIterable;
import org.graalvm.compiler.nodeinfo.InputType;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.AbstractBeginNode;
import org.graalvm.compiler.nodes.BeginNode;
import org.graalvm.compiler.nodes.ControlSinkNode;
import org.graalvm.compiler.nodes.CustomClockLogNode;
import org.graalvm.compiler.nodes.FixedGuardNode;
import org.graalvm.compiler.nodes.FixedNode;
import org.graalvm.compiler.nodes.FixedWithNextNode;
import org.graalvm.compiler.nodes.GraphState;
import org.graalvm.compiler.nodes.GraphState.StageFlag;
import org.graalvm.compiler.nodes.GuardNode;
import org.graalvm.compiler.nodes.LogicNode;
import org.graalvm.compiler.nodes.LoopExitNode;
import org.graalvm.compiler.nodes.PhiNode;
import org.graalvm.compiler.nodes.ProxyNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import org.graalvm.compiler.nodes.UnreachableBeginNode;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.WithExceptionNode;
import org.graalvm.compiler.nodes.calc.FloatingNode;
import org.graalvm.compiler.nodes.cfg.HIRBlock;
import org.graalvm.compiler.nodes.extended.AnchoringNode;
import org.graalvm.compiler.nodes.extended.ForeignCall;
import org.graalvm.compiler.nodes.extended.GuardedNode;
import org.graalvm.compiler.nodes.extended.GuardingNode;
import org.graalvm.compiler.nodes.memory.MemoryAccess;
import org.graalvm.compiler.nodes.memory.MemoryKill;
import org.graalvm.compiler.nodes.memory.MemoryMapNode;
import org.graalvm.compiler.nodes.memory.MultiMemoryKill;
import org.graalvm.compiler.nodes.memory.SideEffectFreeWriteNode;
import org.graalvm.compiler.nodes.memory.SingleMemoryKill;
import org.graalvm.compiler.nodes.spi.CoreProviders;
import org.graalvm.compiler.nodes.spi.CoreProvidersDelegate;
import org.graalvm.compiler.nodes.spi.Lowerable;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.nodes.virtual.CommitAllocationNode;
import org.graalvm.compiler.options.Option;
import org.graalvm.compiler.options.OptionKey;
import org.graalvm.compiler.options.OptionType;
import org.graalvm.compiler.options.OptionValues;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.phases.common.util.EconomicSetNodeEventListener;
import org.graalvm.compiler.phases.schedule.SchedulePhase;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.SpeculationLog;
import jdk.vm.ci.meta.SpeculationLog.Speculation;

/**
 * Adds CustomInstrumentation to loops.
 */
public class CustomInstrumentationLoweringPhase extends LoweringPhase  {



    public CustomInstrumentationLoweringPhase(CanonicalizerPhase canonicalizer, boolean lowerOptimizableMacroNodes) {
        super(canonicalizer, LoweringTool.StandardLoweringStage.HIGH_TIER, lowerOptimizableMacroNodes, StageFlag.CUSTOMINSTRUMENTATION_TIER);
    }

    public CustomInstrumentationLoweringPhase(CanonicalizerPhase canonicalizer) {
        super(canonicalizer, LoweringTool.StandardLoweringStage.HIGH_TIER, StageFlag.CUSTOMINSTRUMENTATION_TIER);
    }

    private enum LoweringMode {
        LOWERING,
        VERIFY_LOWERING
    }

    @Override
    protected void run(StructuredGraph graph, CoreProviders context) {
        lower(graph, context, LoweringMode.LOWERING);
        //assert checkPostLowering(graph, context);
        
    }

    @SuppressWarnings("try")
    private void lower(StructuredGraph graph, CoreProviders context, LoweringMode mode) {
        final LoweringToolImpl loweringTool = new LoweringToolImpl(context, null, null, null, null);
            
        for (Node node : graph.getNodes()) {
            if (node instanceof CustomClockLogNode) {
                CustomClockLogNode clocknode = (CustomClockLogNode) node;
                clocknode.lower(loweringTool);
            }
         }          
        }


        // boolean immutableSchedule = mode == LoweringMode.VERIFY_LOWERING;
        // OptionValues options = graph.getOptions();
        // new SchedulePhase(immutableSchedule, options).apply(graph, context);

        // if (Options.PrintLoweringScheduleToTTY.getValue(graph.getOptions())) {
        //     TTY.printf("%s%n", graph.getLastSchedule().print());
        // }

        // EconomicSetNodeEventListener listener = new EconomicSetNodeEventListener();
        // try (Graph.NodeEventScope nes = graph.trackNodeEvents(listener)) {
        //     ScheduleResult schedule = graph.getLastSchedule();
        //     schedule.getCFG().computePostdominators();
        //     HIRBlock startBlock = schedule.getCFG().getStartBlock();
        //     ProcessFrame rootFrame = new ProcessFrame(context, startBlock, graph.createNodeBitMap(), startBlock.getBeginNode(), null, schedule);
        //     LoweringPhase.processBlock(rootFrame);
        // }

        // if (!listener.getNodes().isEmpty()) {
        //     //canonicalizer.applyIncremental(graph, context, listener.getNodes()); // lets hope this is not important
        // }
        // assert graph.verify();
    //}


    /**
     * Checks that lowering of a given node did not introduce any new {@link Lowerable} nodes that
     * could be lowered in the current {@link LoweringPhase}. Such nodes must be recursively lowered
     * as part of lowering {@code node}.
     *
     * @param node a node that was just lowered
     * @param preLoweringMark the graph mark before {@code node} was lowered
     * @param unscheduledUsages set of {@code node}'s usages that were unscheduled before it was
     *            lowered
     * @throws AssertionError if the check fails
     */
    // private boolean checkPostLowering(StructuredGraph graph, CoreProviders context) {
    //     Mark expectedMark = graph.getMark();
    //     lower(graph, context, LoweringMode.VERIFY_LOWERING);
    //     Mark mark = graph.getMark();
    //     assert mark.equals(expectedMark) || graph.getNewNodes(mark).count() == 0 : graph + ": a second round in the current lowering phase introduced these new nodes: " +
    //                     graph.getNewNodes(expectedMark).snapshot();
    //     return true;
    // }

    // private class ProcessFrame extends Frame<ProcessFrame> {
    //     private final NodeBitMap activeGuards;
    //     private AnchoringNode anchor;
    //     private final ScheduleResult schedule;
    //     private final CoreProviders context;

    //     ProcessFrame(CoreProviders context, HIRBlock block, NodeBitMap activeGuards, AnchoringNode anchor, ProcessFrame parent, ScheduleResult schedule) {
    //         super(block, parent);
    //         this.context = context;
    //         this.activeGuards = activeGuards;
    //         this.anchor = anchor;
    //         this.schedule = schedule;
    //     }

    //     @Override
    //     public void preprocess() {
    //         this.anchor = process(context, block, activeGuards, anchor, schedule);
    //     }

    //     @Override
    //     public ProcessFrame enter(HIRBlock b) {
    //         return new ProcessFrame(context, b, activeGuards, b.getBeginNode(), this, schedule);
    //     }

    //     @Override
    //     public Frame<?> enterAlwaysReached(HIRBlock b) {
    //         AnchoringNode newAnchor = anchor;
    //         if (parent != null && b.getLoop() != parent.block.getLoop() && !b.isLoopHeader()) {
    //             // We are exiting a loop => cannot reuse the anchor without inserting loop
    //             // proxies.
    //             newAnchor = b.getBeginNode();
    //         }
    //         return new ProcessFrame(context, b, activeGuards, newAnchor, this, schedule);
    //     }

    //     @Override
    //     public void postprocess() {
    //         if (anchor == block.getBeginNode() && OptEliminateGuards.getValue(activeGuards.graph().getOptions())) {
    //             for (GuardNode guard : anchor.asNode().usages().filter(GuardNode.class)) {
    //                 if (activeGuards.isMarkedAndGrow(guard)) {
    //                     activeGuards.clear(guard);
    //                 }
    //             }
    //         }
    //     }
    // }

    


}
