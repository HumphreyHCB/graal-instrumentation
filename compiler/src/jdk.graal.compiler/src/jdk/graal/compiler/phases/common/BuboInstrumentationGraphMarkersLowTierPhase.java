package jdk.graal.compiler.phases.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import jdk.graal.compiler.core.common.CompilationIdentifier;
import jdk.graal.compiler.core.common.CompilationIdentifier.Verbosity;
import jdk.graal.compiler.nodes.EndofLoopNode;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.LoopBeginNode;
import jdk.graal.compiler.nodes.LoopExitNode;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.tiers.LowTierContext;
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
        if (graph.compilationId().toString(Verbosity.NAME).contains("Stub") || graph.compilationId().toString(Verbosity.NAME).contains("HotSpotOSRCompilation")) {
            return;
        }

        BuboNativeMethodCache.add(graph.compilationId().toString(CompilationIdentifier.Verbosity.ID) + " " +graph.compilationId().toString(CompilationIdentifier.Verbosity.NAME));


        // collect all loop exits
        List<LoopExitNode> exits = graph.getNodes().filter(LoopExitNode.class).snapshot();
        if (exits.isEmpty()) {return;}

        // id per loop begin we see in exits
        HashMap<LoopBeginNode, Integer> beginToId = new HashMap<>();
        int nextId = 0;

        for (LoopExitNode exit : exits) {
            LoopBeginNode begin = exit.loopBegin();
            if (begin == null) {continue;}

            Integer id = beginToId.get(begin);
            if (id == null) {
                id = nextId++;
                beginToId.put(begin, id);
                begin.setLoopId(id); // tell the begin to print a marker with the given ID
            }

            // insert our marker right after the exit
            EndofLoopNode end = graph.add(new EndofLoopNode(id, exit.getNodeSourcePosition()));
            graph.addAfterFixed(exit, end);
        }
    }
}
