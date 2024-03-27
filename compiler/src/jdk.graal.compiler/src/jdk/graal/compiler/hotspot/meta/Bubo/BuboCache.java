package jdk.graal.compiler.hotspot.meta.Bubo;

import java.lang.Thread;

/**
 * BuboCache thread buffer, this is called viva a Forgien call
 * see Custom Instrumentation Phase to see where its called.
 */

 public class BuboCache extends Thread {
        
        public static long[] TimeBuffer;
        public static int[] ActivationCountBuffer;
        public static long[] CyclesBuffer;
        public static int pointer;
        public static long[] Buffer;

        public BuboCache() {
                Buffer = new long[200_000];
                TimeBuffer = new long[200_000];
                ActivationCountBuffer = new int[200_000];
                CyclesBuffer = new long[200_000];
                pointer = 1;
        }

        public static void TestPrint(char Unused) {
                System.out.print("Test Print From Static Class");
        }


}