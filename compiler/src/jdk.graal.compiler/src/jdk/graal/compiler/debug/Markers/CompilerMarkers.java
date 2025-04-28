package jdk.graal.compiler.debug.Markers;

import jdk.graal.compiler.api.runtime.GraalRuntime;
import jdk.graal.compiler.core.GraalCompiler;
import jdk.graal.compiler.hotspot.HotSpotGraalCompiler;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public class CompilerMarkers {
    // public static ResolvedJavaMethod stubMethod;
    // static{
    //     HotSpotGraalCompiler compiler = (HotSpotGraalCompiler) HotSpotJVMCIRuntime.runtime().getCompiler();

    //     ResolvedJavaType markerType = compiler.getGraalRuntime().getHostProviders().getMetaAccess().lookupJavaType(CompilerMarkers.class);

    //     stubMethod = markerType.getDeclaredMethods()[0];
    // }
    public static void UnknownCompilerLIR() {
        
    }
    public static void UnknownCompilerHIR() {
        
    }
}
