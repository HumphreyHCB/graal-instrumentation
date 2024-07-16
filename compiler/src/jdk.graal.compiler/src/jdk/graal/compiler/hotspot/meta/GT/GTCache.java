package jdk.graal.compiler.hotspot.meta.GT;

import java.lang.Thread;

/**
 * BuboCache thread buffer, this is called viva a Forgien call
 * see Custom Instrumentation Phase to see where its called.
 */

 public class GTCache extends Thread {
        
        public static long[] ActivationCountBuffer; // stores activaation of Comp units

        public GTCache() {
                ActivationCountBuffer = new long[200_000];
        }

        public static void TestPrint(char Unused) {
                System.out.print("Test Print From Static Class");
        }


}