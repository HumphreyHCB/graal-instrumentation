package jdk.graal.compiler.phases.common;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jdk.graal.compiler.core.common.CompilationIdentifier;
import jdk.graal.compiler.core.common.CompilationIdentifier.Verbosity;
import jdk.graal.compiler.core.common.cfg.BasicBlock;
import jdk.graal.compiler.core.common.cfg.CFGLoop;
import jdk.graal.compiler.nodes.EndofLoopNode;
import jdk.graal.compiler.nodes.FixedNode;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.LoopBeginNode;
import jdk.graal.compiler.nodes.LoopExitNode;
import jdk.graal.compiler.nodes.StartofLoopNode;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.cfg.ControlFlowGraph;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.tiers.LowTierContext;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeLoopNestingCache;
import jdk.graal.compiler.hotspot.meta.Bubo.BuboNativeMethodCache;

public class BuboInstrumentationGraphMarkersLowTierPhase extends BasePhase<LowTierContext> {

    private final OptionValues options;

    public BuboInstrumentationGraphMarkersLowTierPhase(OptionValues options) {
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

    @Override
    protected void run(StructuredGraph graph, LowTierContext context) {
        if (graph.compilationId().toString(Verbosity.NAME).contains("Stub")
                || graph.compilationId().toString(Verbosity.NAME).contains("HotSpotOSRCompilation")) {
            return;
        }

        BuboNativeMethodCache.add(graph.compilationId().toString(CompilationIdentifier.Verbosity.ID) + " "
                + graph.compilationId().toString(CompilationIdentifier.Verbosity.NAME));

        int ida = 0;
        List<LoopBeginNode> begins = graph.getNodes().filter(LoopBeginNode.class).snapshot();

        Map<LoopBeginNode, Integer> beginToId = new IdentityHashMap<>();

        for (LoopBeginNode begin : begins) {
            beginToId.put(begin, ida);
            StartofLoopNode start = graph.add(new StartofLoopNode(ida, begin.inputs().first().getNodeSourcePosition()));
            graph.addBeforeFixed((FixedNode) begin.inputs().first(), start);

            for (LoopExitNode exit : begin.loopExits()) {
                EndofLoopNode end = graph.add(new EndofLoopNode(ida, exit.getNodeSourcePosition()));
                graph.addAfterFixed(exit, end);
            }

            ida++;
        }

        storeLoopNestingToNative(graph, begins, beginToId);

    }

    private static void storeLoopNestingToNative(StructuredGraph graph,
            List<LoopBeginNode> begins,
            Map<LoopBeginNode, Integer> beginToId) {

        ControlFlowGraph cfg = graph.getLastCFG();
        List<? extends CFGLoop<?>> loops = cfg.getLoops();
        if (loops == null || loops.isEmpty()) {return;}

        int compId = Integer.parseInt(graph.compilationId().toString(Verbosity.ID).split("-")[1]);

        IdentityHashMap<CFGLoop<?>, Integer> loopToIda = new IdentityHashMap<>();

        for (LoopBeginNode begin : begins) {
            BasicBlock<?> block = cfg.blockFor(begin);
            if (block == null) {
                continue;
            }
            CFGLoop<?> loop = block.getLoop();
            if (loop == null) {
                continue;
            }
            Integer ida = beginToId.get(begin);
            if (ida != null) {
                loopToIda.put(loop, ida);
            }
        }

        // Build "child:parent,child:parent,..." string
        StringBuilder sb = new StringBuilder();
        
        boolean first = true;
        for (CFGLoop<?> loop : loops) {
            Integer childId = loopToIda.get(loop);
            if (childId == null) {
                System.out.println("We have found a loop that was not tagged");
            }

            CFGLoop<?> parent = loop.getParent();
            Integer parentId = (parent != null) ? loopToIda.get(parent) : -1;

            if (!first) {
                sb.append(',');
            }
            first = false;

            sb.append(childId).append(':').append(parentId);
        }

        String nestingEncoding = sb.toString();

        BuboNativeLoopNestingCache.putEncoding(compId, nestingEncoding);
    }

}
