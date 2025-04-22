package jdk.graal.compiler.phases.common;

import java.lang.reflect.Method;
import java.util.Optional;

import jdk.graal.compiler.debug.GraalError;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.hotspot.HotSpotGraalCompiler;
import jdk.graal.compiler.nodes.GraphState;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.phases.BasePhase;
import jdk.graal.compiler.phases.tiers.LowTierContext;
import jdk.graal.compiler.serviceprovider.GraalServices.LibgraalConfig;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.graal.compiler.MyMarkers;

public final class GTChangeDebugInfoLowTierPhase extends BasePhase<LowTierContext> {
    /**
     * Cache the host‐side Method object once at class‑load time.
     * (No ResolvedJavaMethod here—that comes later via MetaAccessProvider.)
     */
    // private static final Method STUB_HOST_METHOD;

    // static {
    // try {
    // STUB_HOST_METHOD = MyMarkers.class.getDeclaredMethod("GT_debug_stub");
    // } catch (NoSuchMethodException e) {
    // throw GraalError.shouldNotReachHere(
    // "MyMarkers.GT_debug_stub not found on host classpath");
    // }
    // }

    @Override
    public Optional<NotApplicable> notApplicableTo(GraphState graphState) {
        return Optional.empty();
    }

    @Override
    protected void run(StructuredGraph graph, LowTierContext ctx) {
         MetaAccessProvider meta = ctx.getMetaAccess();

         if (graph.method() != null) {
            // System.out.println("Method " + graph.method().getName());
            // HotSpotGraalCompiler compiler = (HotSpotGraalCompiler) HotSpotJVMCIRuntime.runtime().getCompiler();
            // if (compiler.GT_debug_stub == null) {
            //     //throw GraalError.shouldNotReachHere("GT_debug_stub not found on host classpath");
                
            // }else{
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            //     System.out.println("GT_debug_stub found on host classpath");
            // }
        //     // Turn the MyMarkers.class into a ResolvedJavaType if you need it.
         ResolvedJavaType markerType = meta.lookupJavaType(MyMarkers.class);
         ResolvedJavaMethod stubMethod = markerType.getDeclaredMethods()[0];

        //System.out.println("In GTChangeDebugInfoLowTierPhase we Found: " + stubMethod.getName());
        //     // LibgraalConfig
        //     // Convert the host Method into a ResolvedJavaMethod
        //     // ResolvedJavaMethod stubMethod = meta.lookupJavaMethod(STUB_HOST_METHOD);

            // Now walk the graph and replace every position with your stub
            for (Node n : graph.getNodes()) {
                NodeSourcePosition oldPos = n.getNodeSourcePosition();
                if (oldPos != null) {
                    NodeSourcePosition newPos = new NodeSourcePosition(
                            oldPos.getSourceLanguage(),
                            oldPos.getCaller(),
                            stubMethod,
                            /* you can choose an appropriate BCI; -1 is common for synthetic */
                            -1);
                    n.setNodeSourcePosition(newPos);
                }
             }
         }
    }
}
