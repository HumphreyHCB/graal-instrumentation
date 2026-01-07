package jdk.graal.compiler.phases.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jdk.graal.compiler.core.common.CompilationIdentifier.Verbosity;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.core.common.cfg.CFGLoop;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.Invoke;
import jdk.graal.compiler.nodes.LoopBeginNode;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.StructuredGraph.ScheduleResult;
import jdk.graal.compiler.nodes.cfg.ControlFlowGraph;
import jdk.graal.compiler.nodes.cfg.HIRBlock;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.schedule.SchedulePhase;
import jdk.graal.compiler.phases.tiers.LowTierContext;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Helper class that collects the marker methods defined in {@link BuboAgentCompilerMarkers}
 * and stores them in a static array for use by compiler phases.  At runtime,
 * call {@link #initializeMarkers(HotSpotResolvedObjectType)} once to populate
 * the {@link #MARKERS} array.  The array indices correspond to the marker
 * numbers: Marker0 is stored at index {@code 0}, Marker1 at index {@code 1},
 * …​, Marker20 at index {@code 20}, and the special delimiter method
 * {@code MarkerDelimiter} is stored at index {@link #MAX_MARKERS} – 1.
 */
public final class GTCollectCompilerMarkers {

    /**
     * Total number of marker slots: there are 21 numbered markers (0–20) plus
     * one delimiter marker at the end.  Adjust this value if you add more
     * markers to {@link BuboAgentCompilerMarkers}.
     */
    public static final int MAX_MARKERS = 22;

    /**
     * Array holding references to the marker methods.  Index {@code i}
     * contains the {@link ResolvedJavaMethod} corresponding to {@code Marker<i>}
     * for {@code i} in [0..20], and index {@code MAX_MARKERS – 1} contains the
     * delimiter method {@code MarkerDelimiter}.  The array is populated by
     * {@link #initializeMarkers(HotSpotResolvedObjectType)}.
     */
    public static final ResolvedJavaMethod[] MARKERS = new ResolvedJavaMethod[MAX_MARKERS];

    /**
     * Initialize the {@link #MARKERS} array by resolving the methods of
     * {@link BuboAgentCompilerMarkers}.  This method is idempotent: if the
     * array is already populated (MARKERS[0] != null), it returns without
     * performing work.  If a marker method cannot be resolved, the
     * corresponding array entry will remain {@code null}.
     *
     * @param accessingType the type used to perform the class lookup.  This
     *                      should be the non-snippet HotSpot type associated
     *                      with the calling context; passing {@code null} is
     *                      allowed but may restrict access depending on the
     *                      module system.  See {@link HotSpotJVMCIRuntime#lookupType}.
     */
    public static void initializeMarkers(HotSpotResolvedObjectType accessingType) {
        // Avoid re-initialisation.
        if (MARKERS[0] != null) {
            return;
        }

        HotSpotJVMCIRuntime rt = HotSpotJVMCIRuntime.runtime();
        // Resolve the custom marker class.
        ResolvedJavaType markerType = (ResolvedJavaType) rt.lookupType("Lmy/custom/BuboAgentCompilerMarkers;", accessingType, true);

        for (ResolvedJavaMethod m : markerType.getDeclaredMethods()) {
            String name = m.getName();
            if (name.startsWith("Marker")) {
                String suffix = name.substring("Marker".length());
                int idx = -1;
                if ("Delimiter".equals(suffix)) {
                    idx = MAX_MARKERS - 1;
                } else {
                    try {
                        idx = Integer.parseInt(suffix);
                    } catch (NumberFormatException nfe) {
                        // Ignore methods that do not follow the MarkerN naming convention.
                    }
                }
                if (idx >= 0 && idx < MAX_MARKERS) {
                    MARKERS[idx] = m;
                }
            }
        }
    }

    // This class is not meant to be instantiated.
    private GTCollectCompilerMarkers() {}
}
