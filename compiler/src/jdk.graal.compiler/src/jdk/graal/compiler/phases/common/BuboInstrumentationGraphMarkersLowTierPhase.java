package jdk.graal.compiler.phases.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import jdk.graal.compiler.core.common.CompilationIdentifier;
import jdk.graal.compiler.core.common.CompilationIdentifier.Verbosity;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.nodes.BeginNode;
import jdk.graal.compiler.nodes.EndofLoopNode;
import jdk.graal.compiler.nodes.FixedNode;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.LoopBeginNode;
import jdk.graal.compiler.nodes.LoopExitNode;
import jdk.graal.compiler.nodes.ReturnNode;
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
        // for (int i = 0; i < 3; i++) {

        //     StartofLoopNode start = graph.add(new StartofLoopNode(ida, graph.start().getNodeSourcePosition()));
        //     graph.addAfterFixed(graph.start(), start);

        //     for (Node node : graph.getNodes()) {
        //         if (node instanceof ReturnNode) {
        //             EndofLoopNode end = graph.add(new EndofLoopNode(ida, node.getNodeSourcePosition()));
        //             graph.addBeforeFixed((ReturnNode) node, end);
        //         }
        //     }
        //     ida++;
        // }

        // collect all loop exits
        List<LoopBeginNode> begins = graph.getNodes().filter(LoopBeginNode.class).snapshot();

        for (LoopBeginNode begin : begins) {
            //if (exit.loopBegin().isSimpleLoop() || exit.loopBegin().isMainLoop()) {
                StartofLoopNode start = graph.add(new StartofLoopNode(ida, graph.start().getNodeSourcePosition()));
                graph.addBeforeFixed( (FixedNode) begin.inputs().first(), start);

                for (LoopExitNode exit : begin.loopExits()) {
                     EndofLoopNode end = graph.add(new EndofLoopNode(ida, exit.getNodeSourcePosition()));
                    graph.addAfterFixed((LoopExitNode) exit, end);
                }
            
             
                // EndofLoopNode end = graph.add(new EndofLoopNode(ida, exit.getNodeSourcePosition()));
                // graph.addAfterFixed((LoopExitNode) exit, end);
                //  StartofLoopNode start = graph.add(new StartofLoopNode(ida, graph.start().getNodeSourcePosition()));
                //  graph.addBeforeFixed( (FixedNode) exit.loopBegin().inputs().first(), start);
                //    System.out.println("intpurts " +exit.loopBegin().inputs());
                   
                 //System.out.println("end's begin : " + exit.loopBegin());
                //exit.loopBegin().setLoopId(ida);
                ida++;
            //}

        }

        //  List<LoopBeginNode> starts = graph.getNodes().filter(LoopBeginNode.class).snapshot();

        //  for (LoopBeginNode start : starts) {
        //       System.out.println("sTART : " + start);
        //     System.out.println("Starts pred : " + start.predecessor());

        //     for (Node input : start.inputs()) {
        //         System.out.println("sTART : " + input);
        //     }
          
        //  }

        // if (exits.isEmpty()) {
        //     return;
        // }

        // id per loop begin we see in exits
        HashMap<LoopBeginNode, Integer> beginToId = new HashMap<>();
        int nextId = 0;

        // for (LoopExitNode exit : exits) {
        // LoopBeginNode begin = exit.loopBegin();
        // if (begin == null) {continue;}

        // Integer id = beginToId.get(begin);
        // if (id == null) {
        // id = nextId++;
        // beginToId.put(begin, id);
        // StartofLoopNode start = graph.add(new StartofLoopNode(id,
        // exit.getNodeSourcePosition()));
        // graph.addAfterFixed(begin, start);
        // }
        // else{
        // StartofLoopNode start = graph.add(new StartofLoopNode(id,
        // exit.getNodeSourcePosition()));
        // graph.addAfterFixed(begin, start);
        // }

        // // insert our marker right after the exit
        // EndofLoopNode end = graph.add(new EndofLoopNode(id,
        // exit.getNodeSourcePosition()));
        // graph.addAfterFixed(exit, end);
        // }
    }
}
