package jdk.graal.compiler.phases.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jdk.graal.compiler.core.common.CompilationIdentifier;
import jdk.graal.compiler.core.common.CompilationIdentifier.Verbosity;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.core.common.cfg.CFGLoop;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.LoopBeginNode;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.cfg.ControlFlowGraph;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.tiers.LowTierContext;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeLoopNestingCache;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeMethodCache;

public class HumphreyDebugDataInstrumentationGraphMarkersLowTierPhase extends BasePhase<LowTierContext> {

    private final OptionValues options;

    public HumphreyDebugDataInstrumentationGraphMarkersLowTierPhase(OptionValues options) {
        this.options = options;
    }

    @Override
    public boolean checkContract() {
        return false;
    }

    @Override
    public Optional<NotApplicable> notApplicableTo(GraphState graphState) {
        return ALWAYS_APPLICABLE;
    }

    /**
     * IMPORTANT:
     * This version is "graph side effect free":
     * - creates no new nodes
     * - inserts no new nodes
     * - does not rewrite edges
     *
     * It still updates:
     * - BuboNativeMethodCache
     * - BuboNativeLoopNestingCache
     * - CFG_ID_TO_IDA
     */
    @Override
    protected void run(StructuredGraph graph, LowTierContext context) {
        String name = graph.compilationId().toString(Verbosity.NAME);
        if (name.contains("Stub") || name.contains("HotSpotOSRCompilation")) {
            return;
        }

        // Keep your existing method cache side effect
        BuboNativeMethodCache.add(
                graph.compilationId().toString(CompilationIdentifier.Verbosity.ID) + " " +
                graph.compilationId().toString(CompilationIdentifier.Verbosity.NAME)
        );

        // Read LoopBeginNodes, but do not tag them with new marker nodes
        List<LoopBeginNode> begins = graph.getNodes().filter(LoopBeginNode.class).snapshot();
        if (begins.isEmpty()) {
            return;
        }

        // Deterministic ida assignment
        List<LoopBeginNode> beginsSorted = new ArrayList<>(begins);
        beginsSorted.sort(Comparator.comparingInt(LoopBeginNode::getId));

        Map<LoopBeginNode, Integer> beginToIda = new IdentityHashMap<>();
        for (int ida = 0; ida < beginsSorted.size(); ida++) {
            beginToIda.put(beginsSorted.get(ida), ida);
        }

        storeLoopNestingToNative(graph, beginsSorted, beginToIda);
    }

    public static final Map<Integer, Integer> CFG_ID_TO_IDA = new HashMap<>();

    private void storeLoopNestingToNative(
            StructuredGraph graph,
            List<LoopBeginNode> beginsSorted,
            Map<LoopBeginNode, Integer> beginToIda) {

        ControlFlowGraph cfg = graph.getLastCFG();
        if (cfg == null) {
            return;
        }

        List<? extends CFGLoop<?>> loops = cfg.getLoops();
        if (loops == null || loops.isEmpty()) {
            return;
        }

        int compId = parseCompilationNumericId(graph.compilationId().toString(Verbosity.ID));

        // Map CFGLoop -> ida using LoopBeginNode -> BasicBlock -> CFGLoop
        IdentityHashMap<CFGLoop<?>, Integer> loopToIda = new IdentityHashMap<>();

        // Clear per compilation if you want to avoid stale mappings across comps
        // If you rely on it being global across the whole VM lifetime, remove this line.
        CFG_ID_TO_IDA.clear();

        for (LoopBeginNode begin : beginsSorted) {
            BasicBlock<?> block = cfg.blockFor(begin);
            if (block == null) {
                continue;
            }

            CFGLoop<?> loop = block.getLoop();
            if (loop == null) {
                continue;
            }

            Integer ida = beginToIda.get(begin);
            if (ida == null) {
                continue;
            }

            loopToIda.put(loop, ida);
            CFG_ID_TO_IDA.put(loop.index, ida);
        }

        // Build "child:parent,child:parent,..." in CFG loop iteration order
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (CFGLoop<?> loop : loops) {
            Integer childId = loopToIda.get(loop);
            if (childId == null) {
                System.out.println("We have found a loop that was not tagged (no LoopBeginNode mapping).");
                // You can choose to skip it instead of emitting "null"
                // continue;
            }

            CFGLoop<?> parent = loop.getParent();
            Integer parentId = (parent != null) ? loopToIda.get(parent) : -1;

            if (!first) {
                sb.append(',');
            }
            first = false;

            sb.append(childId).append(':').append(parentId);
        }

        BuboNativeLoopNestingCache.putEncoding(compId, sb.toString());
    }

    private static int parseCompilationNumericId(String verbosityId) {
        // Your old code assumed something like "...-123"
        // This keeps the same behaviour but is a bit safer.
        int dash = verbosityId.lastIndexOf('-');
        if (dash >= 0 && dash + 1 < verbosityId.length()) {
            try {
                return Integer.parseInt(verbosityId.substring(dash + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        // Fallback, keep deterministic, but you may prefer to throw instead.
        return 0;
    }
}
