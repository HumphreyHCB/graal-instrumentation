/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.graal.compiler.core.phases;


import jdk.graal.compiler.core.common.GraalOptions;
import jdk.graal.compiler.core.common.LibGraalSupport;
import jdk.graal.compiler.debug.DebugCloseable;
import jdk.graal.compiler.debug.DebugContext;
import jdk.graal.compiler.debug.TimerKey;
import jdk.graal.compiler.debug.Markers.CompilerMarkers;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.hotspot.HotSpotGraalCompiler;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.loop.DefaultLoopPolicies;
import jdk.graal.compiler.nodes.loop.LoopPolicies;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.PhaseSuite;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public class BaseTier<C> extends PhaseSuite<C> {

    /**
     * Time spent in hinted GC in frontend.
     */
    public static final TimerKey HIRHintedGC = DebugContext.timer("HIRHintedGC")
            .doc("Time spent in hinted GC performed before each HIR phase.");

    public LoopPolicies createLoopPolicies(@SuppressWarnings("unused") OptionValues options) {
        return new DefaultLoopPolicies();
    }

    @SuppressWarnings({ "try" })
    @Override
    protected void run(StructuredGraph graph, C context) {
        if (GraalOptions.AdditionalCompilerDebugInformation.getValue(graph.getOptions())) {
            addMissingDebug(graph, null);
        }
        
        for (BasePhase<? super C> phase : getPhases()) {
            LibGraalSupport libgraal = LibGraalSupport.INSTANCE;
            if (libgraal != null) {
                /*
                 * Notify the libgraal runtime that most objects allocated in previous HIR phase
                 * are
                 * dead and can be reclaimed. This will lower the chance of allocation failure
                 * in
                 * the next HIR phase.
                 */
                try (DebugCloseable timer = HIRHintedGC.start(graph.getDebug())) {
                    libgraal.notifyLowMemoryPoint(false);
                    libgraal.processReferences();
                }
            }
            phase.apply(graph, context);
            if (GraalOptions.AdditionalCompilerDebugInformation.getValue(graph.getOptions())) {
                addMissingDebug(graph, phase);

            }
        }
    }

    protected void addMissingDebug(StructuredGraph graph, BasePhase<? super C> phase) {
        HotSpotGraalCompiler compiler = (HotSpotGraalCompiler) HotSpotJVMCIRuntime.runtime().getCompiler();
        ResolvedJavaMethod stubMethod =null;
        if (phase == null) {
            ResolvedJavaType markerType = compiler.getGraalRuntime().getHostProviders().getMetaAccess()
            .lookupJavaType(CompilerMarkers.class);
             stubMethod = markerType.getDeclaredMethods()[1];
            
        }
        else{
        ResolvedJavaType markerType = compiler.getGraalRuntime().getHostProviders().getMetaAccess()
                .lookupJavaType(phase.getClass());
        
        ResolvedJavaMethod[] methods = markerType.getDeclaredMethods();

         stubMethod = null;
        for (ResolvedJavaMethod m : methods) {
            if ("run".equals(m.getName())) {
                stubMethod = m;
                break;
            }
        }

        // default to the first method if “run” wasn’t present
        if (stubMethod == null && methods.length > 0) {
            stubMethod = methods[0];
        }
    }
        // ResolvedJavaMethod stubMethod = markerType.getDeclaredMethods();

        for (Node n : graph.getNodes()) {
            if (n.getNodeSourcePosition() == null) {
                NodeSourcePosition newPos = new NodeSourcePosition(
                    null,
                        null,
                        stubMethod,
                        /* you can choose an appropriate BCI; -1 is common for synthetic */
                        -1);
                n.setNodeSourcePosition(newPos);

            }

        }

    }
}
