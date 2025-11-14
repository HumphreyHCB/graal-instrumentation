package jdk.graal.compiler.phases.common;

import java.util.List;
import java.util.Optional;

import jdk.graal.compiler.core.common.CompilationIdentifier;
import jdk.graal.compiler.core.common.CompilationIdentifier.Verbosity;
import jdk.graal.compiler.nodes.EndofLoopNode;
import jdk.graal.compiler.nodes.FixedNode;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.LoopBeginNode;
import jdk.graal.compiler.nodes.LoopExitNode;
import jdk.graal.compiler.nodes.StartofLoopNode;
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
        if (graph.compilationId().toString(Verbosity.NAME).contains("Stub")
                || graph.compilationId().toString(Verbosity.NAME).contains("HotSpotOSRCompilation")) {
            return;
        }

        BuboNativeMethodCache.add(graph.compilationId().toString(CompilationIdentifier.Verbosity.ID) + " "
                + graph.compilationId().toString(CompilationIdentifier.Verbosity.NAME));

        int ida = 0;
        List<LoopBeginNode> begins = graph.getNodes().filter(LoopBeginNode.class).snapshot();

        for (LoopBeginNode begin : begins) {
            StartofLoopNode start = graph.add(new StartofLoopNode(ida, begin.inputs().first().getNodeSourcePosition()));
            graph.addBeforeFixed((FixedNode) begin.inputs().first(), start);

            for (LoopExitNode exit : begin.loopExits()) {
                EndofLoopNode end = graph.add(new EndofLoopNode(ida, exit.getNodeSourcePosition()));
                graph.addAfterFixed((LoopExitNode) exit, end);
            }

            ida++;
        }

    }
}
