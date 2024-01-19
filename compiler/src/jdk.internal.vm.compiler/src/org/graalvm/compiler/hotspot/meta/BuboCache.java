package org.graalvm.compiler.hotspot.meta;

import java.lang.Thread;

public class BuboCache extends Thread {
        
        public static long[] Buffer;
        public static int pointer;

        public BuboCache() {
                Buffer = new long[3_000_000];
                pointer = 0;

        }
        public static void dummyPrint(long useless){
                System.out.println("In Humphrey Cache, Dumy print");
        }

        public static void testPrint(){
                System.out.println("Dead methods");
        }

        public static void incPointer()
        {
                pointer++;
        }

        public static void add(long item)
        {
                Buffer[pointer] = item;
                pointer++;

        }

        public static void print(){
                if (pointer == 0) {
                        System.out.println("Buffer is empty! :(");
                }
                for (int i = 0; i < pointer; i++) {
                        System.out.println(Buffer[i]);
                }
        }


}
