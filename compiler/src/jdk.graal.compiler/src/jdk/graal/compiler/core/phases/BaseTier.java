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

import java.util.Comparator;
import java.util.ListIterator;

import jdk.graal.compiler.core.common.GraalOptions;
import jdk.graal.compiler.debug.DebugCloseable;
import jdk.graal.compiler.debug.DebugContext;
import jdk.graal.compiler.debug.TimerKey;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.loop.DefaultLoopPolicies;
import jdk.graal.compiler.nodes.loop.LoopPolicies;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.PhaseSuite;
import jdk.graal.compiler.serviceprovider.GraalServices;
import jdk.graal.compiler.phases.common.GTDebugInfoLoggerHighTierPhase;

public class BaseTier<C> extends PhaseSuite<C> {

    /**
     * Time spent in hinted GC in frontend.
     */
    public static final TimerKey HIRHintedGC = DebugContext.timer("HIRHintedGC").doc("Time spent in hinted GC performed before each HIR phase.");

    public LoopPolicies createLoopPolicies(@SuppressWarnings("unused") OptionValues options) {
        return new DefaultLoopPolicies();
    }

    @SuppressWarnings({"try"})
    @Override
    protected void run(StructuredGraph graph, C context) {
        if (GraalOptions.GTDebugInfoLog.getValue(graph.getOptions())) {
            // 1) Locate the first GTDebugInfoLoggerHighTierPhase in the *underlying* list
            @SuppressWarnings("unchecked")
            Class<? extends BasePhase<? super C>> debugClass =
                    (Class<? extends BasePhase<? super C>>) (Class<?>) GTDebugInfoLoggerHighTierPhase.class;
            
            ListIterator<BasePhase<? super C>> it = findPhase(debugClass, false);
            if (it != null) {
                // iterator is just past the matching phase
                BasePhase<? super C> debugPhase = it.previous();
                it.remove();              // remove it from its old position
                prependPhase(debugPhase); // stick it back at index 0
            }
        }
        for (BasePhase<? super C> phase : getPhases()) {
            // Notify the runtime that most objects allocated in previous HIR phase are dead and can
            // be reclaimed. This will lower the chance of allocation failure in the next HIR phase.
            try (DebugCloseable timer = HIRHintedGC.start(graph.getDebug())) {
                GraalServices.notifyLowMemoryPoint();
            }
            phase.apply(graph, context);
        }
    }
}
