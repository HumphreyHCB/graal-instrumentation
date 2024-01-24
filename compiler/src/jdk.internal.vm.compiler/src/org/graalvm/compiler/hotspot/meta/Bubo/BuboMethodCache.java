package org.graalvm.compiler.hotspot.meta.Bubo;

import java.util.HashMap;
import java.lang.Thread;

public class BuboMethodCache extends Thread  {

    public static HashMap<Integer, String> Buffer;


    public BuboMethodCache() {
        Buffer = new HashMap<Integer, String>();

    }
    
    public static void add(int id, String method)
    {
        Buffer.put(id, method);
    }

    
}
