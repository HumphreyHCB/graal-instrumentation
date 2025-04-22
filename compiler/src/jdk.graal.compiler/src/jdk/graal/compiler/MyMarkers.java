// File: compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/phases/common/MyMarkers.java
package jdk.graal.compiler;

import jdk.graal.compiler.api.replacements.Snippet;

/** Holder used only for debug metadata. Never referenced at run time. */
public final class MyMarkers {
    private MyMarkers() { }

    /** Name that will show up in Graal dumps / stack traces. */
    @Snippet
    public static void GT_debug_stub() { 
        System.out.println("GT_debug_stub called");
    }
}
