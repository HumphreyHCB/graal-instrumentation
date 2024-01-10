package org.graalvm.compiler.hotspot.meta;

import java.lang.Thread;

public class HumphreysCache extends Thread {
        
        public static long[] Buffer;
        public static int pointer;

        public HumphreysCache() {
                Buffer = new long[3_000_000];
                pointer = 0;

        }
        public static void dummyPrint(){
                System.out.println("In Humphrey Cache");
        }

        public static void add(long item)
        {
                Buffer[pointer] = item;
                pointer++;

        }

        public void print(){

                for (long l : Buffer) {
                        System.out.print(l);
                }
        }


}
