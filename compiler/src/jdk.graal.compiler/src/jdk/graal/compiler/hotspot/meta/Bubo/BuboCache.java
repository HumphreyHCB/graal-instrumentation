package jdk.graal.compiler.hotspot.meta.Bubo;

import java.lang.Thread;

/**
 * BuboCache thread buffer, this is called viva a Forgien call
 * see Custom Instrumentation Phase to see where its called.
 */

 public class BuboCache extends Thread {
        
        public static long[] Buffer;
        public static int pointer;

        public BuboCache() {
                Buffer = new long[200_000];
                pointer = 1;
        }

        public static void TestPrint(char Unused) {
                System.out.print("Test Print From Static Class");
        }


}