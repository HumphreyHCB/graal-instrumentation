package jdk.graal.compiler.hotspot.amd64;

import java.util.HashMap;
import java.util.Set;
import jdk.graal.compiler.lir.LIRInstruction;
import jdk.graal.compiler.lir.LIRInstructionClass;
import jdk.graal.compiler.lir.amd64.AMD64Move;

import java.util.Collection;

public class LIRInstructionCostLookup {

    public static HashMap<LIRInstructionClass, Integer> table = new HashMap<>();

    static {
        table.put(AMD64HotSpotDeoptimizeCallerOp.TYPE, -1);
        //table.put(AMD64Move.MembarOp.TYPE, 0);

    }


    // Retrieve the value associated with a name
    public static Object getValue(LIRInstruction name) {
        return table.get(name);
    }


    // Check if the table contains a specific name
    public static boolean containsName(LIRInstruction name) {
        return table.containsKey(name);
    }

}
