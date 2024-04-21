package jdk.graal.compiler.hotspot.meta.Bubo;

import java.util.HashMap;
import java.lang.Thread;

public class BuboCompUnitCache extends Thread  {

    public static HashMap<Integer, String> Buffer;


    public BuboCompUnitCache() {
        Buffer = new HashMap<Integer, String>();
    }
    
    public static void add(Integer ID,String Info)
    {
        Buffer.put(ID, Info);
    }
    
}
