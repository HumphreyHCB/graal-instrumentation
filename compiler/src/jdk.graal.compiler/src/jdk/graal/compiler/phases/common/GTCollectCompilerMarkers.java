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

public class GTCollectCompilerMarkers extends BasePhase<LowTierContext> {

    private final OptionValues options;

    public GTCollectCompilerMarkers(OptionValues options) {
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

    public static final int MAX_MARKERS = 22;
    private static volatile String MARKER_TYPE_SIG = null;
    private static final ConcurrentHashMap<String, String> METHOD_SIGS = new ConcurrentHashMap<>();
    
    @Override
    protected void run(StructuredGraph graph, LowTierContext context) {
                if (graph.compilationId().toString(Verbosity.NAME).contains("Stub")
                || graph.compilationId().toString(Verbosity.NAME).contains("HotSpotOSRCompilation")) {
            return;
        }

        ResolvedJavaMethod method = graph.method();
        String name = method.getName();
        
        if (name.startsWith("Marker")) {
            // Store the full signature
            String fullSig = method.format("%H.%n(%p)%R");
            
            if (MARKER_TYPE_SIG == null) {
                MARKER_TYPE_SIG = method.getDeclaringClass().toJavaName();
            }
            
            if (name.contains("MarkerDelimiter")) {
                METHOD_SIGS.put("MarkerDelimiter", fullSig);
               // System.err.println("Stored MarkerDelimiter sig: " + fullSig);
                return;
            }
            
            try {
                int idx = Integer.parseInt(name.substring("Marker".length()));
                if (idx >= 0 && idx < MAX_MARKERS) {
                    METHOD_SIGS.put("Marker" + idx, fullSig);
                    //System.err.println("Stored Marker" + idx + " sig: " + fullSig);
                }
            } catch (NumberFormatException ignore) {
            }
        }
    }
    
    public static ResolvedJavaMethod getMarker(StructuredGraph currentGraph, int idx) {
        String methodName = (idx == MAX_MARKERS - 1) ? "MarkerDelimiter" : ("Marker" + idx);
        String targetSig = METHOD_SIGS.get(methodName);
        
        if (targetSig == null) {
            System.err.println("Signature for " + methodName + " not yet captured");
            return null;
        }
        
        // Search in the current graph's reachable methods
        for (ResolvedJavaMethod m : getAllReachableMethods(currentGraph)) {
            if (m.format("%H.%n(%p)%R").equals(targetSig)) {
                return m;
            }
        }
        
        return null;
    }
    
    private static Set<ResolvedJavaMethod> getAllReachableMethods(StructuredGraph graph) {
        Set<ResolvedJavaMethod> methods = new HashSet<>();
        for (Invoke invoke : graph.getInvokes()) {
            if (invoke.callTarget() != null && invoke.callTarget().targetMethod() != null) {
                methods.add(invoke.callTarget().targetMethod());
            }
        }
        return methods;
    }

    // private void computeNesting(StructuredGraph graph,
    //                             List<LoopBeginNode> begins,
    //                             Map<LoopBeginNode, Integer> beginToId, LowTierContext context) {

    //     ControlFlowGraph cfga = ControlFlowGraph.computeForSchedule(graph);
    //     graph.setLastCFG(cfga);

    //     ControlFlowGraph cfg = graph.getLastCFG();
    //     List<? extends CFGLoop<?>> loops = cfg.getLoops();
    //     if (loops == null || loops.isEmpty()) {
    //         return;
    //     }

    //     IdentityHashMap<CFGLoop<?>, Integer> loopToIda = new IdentityHashMap<>();

    //     // for each begin loop, find its ida, e.g the arbitrary id we gave it earlier
    //     for (LoopBeginNode begin : begins) {
    //         BasicBlock<?> block = cfg.blockFor(begin);
    //         if (block == null) {
    //             continue;
    //         }

    //         CFGLoop<?> loop = block.getLoop();
    //         if (loop == null) {
    //             continue;
    //         }

    //         Integer ida = beginToId.get(begin);
    //         if (ida != null) {
    //             loopToIda.put(loop, ida);
    //         }
    //     }

    //     if (loopToIda.isEmpty()) {
    //         return;
    //     }

    //     // Build or get a schedule so we know, for each block, all nodes that execute there
    //     ScheduleResult schedule = graph.getLastSchedule();
    //     if (schedule == null) {
    //         SchedulePhase schedulePhase = new SchedulePhase(SchedulePhase.SchedulingStrategy.LATEST);
    //         schedulePhase.apply(graph, context.getProviders());
    //         schedule = graph.getLastSchedule();
    //     }

    //     // Encode the numeric part of the compilation id once
    //     int compId = Integer.parseInt(graph.compilationId().toString(Verbosity.ID).split("-")[1]);

    //     assignLoopMarkersFromOuterToInner(graph, cfg, schedule, loopToIda, compId);
    // }

    // private static void assignLoopMarkersFromOuterToInner(StructuredGraph graph,
    //                                                       ControlFlowGraph cfg,
    //                                                       ScheduleResult schedule,
    //                                                       IdentityHashMap<CFGLoop<?>, Integer> loopToIda,
    //                                                       int compId) {

    //     if (loopToIda.isEmpty()) {
    //         return;
    //     }

    //     // Compute depth of each loop: 0 = outermost
    //     IdentityHashMap<CFGLoop<?>, Integer> depthMap = new IdentityHashMap<>();
    //     for (CFGLoop<?> loop : loopToIda.keySet()) {
    //         computeLoopDepth(loop, depthMap);
    //     }

    //     // Sort loops by depth: outermost (depth 0) first, then inner loops
    //     List<CFGLoop<?>> orderedLoops = new ArrayList<>(loopToIda.keySet());
    //     orderedLoops.sort(Comparator.comparingInt(depthMap::get));

    //     // Now assign NodeSourcePosition markers in that order
    //     for (CFGLoop<?> loop : orderedLoops) {
    //         Integer ida = loopToIda.get(loop);
    //         if (ida == null) {
    //             continue;
    //         }

    //         ResolvedJavaMethod marker = safeMarkerAt(ida);
    //         if (marker == null) {
    //             continue;
    //         }

    //         // lookup set of blocks belonging to this loop
    //         @SuppressWarnings("unchecked")
    //         List<HIRBlock> blocksInLoop = new ArrayList<>((List<HIRBlock>) loop.getBlocks());
    //         blocksInLoop.add((HIRBlock) loop.getHeader());
    //         loop.getLoopExits().forEach(exit -> {
    //             blocksInLoop.add((HIRBlock) exit);
    //         });

    //         IdentityHashMap<HIRBlock, Boolean> blockSet = new IdentityHashMap<>();
    //         for (HIRBlock b : blocksInLoop) {
    //             blockSet.put(b, Boolean.TRUE);
    //         }

    //         // For each block in the loop, use the SCHEDULE to get ALL nodes in that block
    //         for (HIRBlock b : blocksInLoop) {
    //             HIRBlock block = b;

    //             for (Node node : schedule.nodesFor(block)) {
    //                 if (!blockSet.containsKey(b)) {
    //                     continue;
    //                 }

    //                 NodeSourcePosition pos = node.getNodeSourcePosition();
    //                 if (pos != null) {
    //                     node.setNodeSourcePosition(
    //                             buildDebugPositionChain(
    //                                     pos.getCaller(),
    //                                     compId,
    //                                     ida));
    //                 }
    //             }
    //         }
    //     }
    // }

    // private static NodeSourcePosition buildDebugPositionChain(NodeSourcePosition ogPos,
    //                                                           int compID,
    //                                                           int loopId) {
    //     // start from the original position
    //     NodeSourcePosition pos = null;
    //     if (MARKERS[MAX_MARKERS - 1] == null) {
    //         return ogPos;
    //     }

    //     // push each digit of compID as a marker
    //     int[] digits = Integer.toString(compID).chars().map(c -> c - '0').toArray();
    //     for (int d : digits) {
    //         if (d >= 0 && d < MARKERS.length) {
    //             pos = new NodeSourcePosition(
    //                     null,
    //                     pos,
    //                     MARKERS[d],
    //                     -1
    //             );
    //         }
    //     }

    //     // delimiter
    //     pos = new NodeSourcePosition(
    //             null,
    //             pos,
    //             MARKERS[MAX_MARKERS - 1],
    //             -1
    //     );

    //     // push the loop marker
    //     if (loopId >= 0 && loopId < MARKERS.length) {
    //         pos = new NodeSourcePosition(
    //                 null,            
    //                 pos,               
    //                 MARKERS[loopId],   
    //                 -1                 
    //         );
    //     }

    //     return pos;
    // }

    /**
     * Recursively computes the nesting depth of a loop.
     * Depth 0 = no parent; depth N = N parents up the chain.
     */
    private static int computeLoopDepth(CFGLoop<?> loop,
                                        IdentityHashMap<CFGLoop<?>, Integer> depthMap) {
        Integer cached = depthMap.get(loop);
        if (cached != null) {
            return cached;
        }

        CFGLoop<?> parent = loop.getParent();
        int depth = (parent == null) ? 0 : computeLoopDepth(parent, depthMap) + 1;
        depthMap.put(loop, depth);
        return depth;
    }

    // private static ResolvedJavaMethod safeMarkerAt(int idx) {
    //     return (idx >= 0 && idx < MARKERS.length) ? MARKERS[idx] : null;
    // }
}
