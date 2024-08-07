package jdk.graal.compiler.hotspot.meta.GT;

import java.lang.Thread;
import java.util.HashMap;

/**
 * BuboCache thread buffer, this is called viva a Forgien call
 * see Custom Instrumentation Phase to see where its called.
 */

 public class GTCacheDebug extends Thread {
        
        public static HashMap<String,Integer> Occurrences; // stores activaation of Comp units

        public GTCacheDebug() {
                Occurrences = new HashMap<String,Integer>();
        }

        public static void add(String name){

                if (Occurrences.containsKey(name)) {
                        Occurrences.put(name, Occurrences.get(name) + 1);
                }
                else{
                        Occurrences.put(name, 1);
                }
        }

}