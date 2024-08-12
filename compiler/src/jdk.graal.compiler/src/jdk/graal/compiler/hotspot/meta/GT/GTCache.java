package jdk.graal.compiler.hotspot.meta.GT;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * BuboCache thread buffer, this is called viva a Forgien call
 * see Custom Instrumentation Phase to see where its called.
 */

public class GTCache extends Thread {

        public static long[] ActivationCountBuffer; // stores activaation of Comp units
        private static Map<String, Set<String>> LIRInstructionsByteCode;

        public GTCache() {
                ActivationCountBuffer = new long[200_000];
                LIRInstructionsByteCode = new HashMap<>();
        }

        // Method to add a string to a specific ID's set
        public static void addStringToID(String id, String value) {
                // Get the set associated with the ID, or create a new one if it doesn't exist
                Set<String> strings = LIRInstructionsByteCode.getOrDefault(id, new HashSet<>());
                if (strings.contains(value)) {
                        return;
                }
                strings.add(value); // Add the string to the set
                LIRInstructionsByteCode.put(id, strings); // Put the updated set back into the map
        }

        // Method to get the set of strings associated with a specific ID
        public Set<String> getStringsByID(String id) {
                return LIRInstructionsByteCode.getOrDefault(id, new HashSet<>()); // Return the set or an empty set if ID doesn't
                                                                     // exist
        }

        // Method to check if a string exists for a specific ID
        public boolean containsStringForID(String id, String value) {
                Set<String> strings = LIRInstructionsByteCode.get(id);
                return strings != null && strings.contains(value);
        }

        public static void dumpLIRInstructionsToJSON() {
        Map<String, Set<String>> snapshot;

        // Create a defensive copy of the map
        synchronized (LIRInstructionsByteCode) {
            snapshot = new HashMap<>(LIRInstructionsByteCode);
        }

        // Now, safely iterate over the snapshot
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");

        int mapSize = snapshot.size();
        int mapIndex = 0;
        for (Map.Entry<String, Set<String>> entry : snapshot.entrySet()) {
            jsonBuilder.append("  \"").append(entry.getKey()).append("\": [\n");
            int setSize = entry.getValue().size();
            int setIndex = 0;
            for (String value : entry.getValue()) {
                jsonBuilder.append("    \"").append(value).append("\"");
                if (++setIndex < setSize) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\n");
            }
            jsonBuilder.append("  ]");
            if (++mapIndex < mapSize) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }

        jsonBuilder.append("}\n");
        
                // Write the JSON string to a file
                try (FileWriter writer = new FileWriter("LIRInstructionsByteCodeDump.json")) {
                    writer.write(jsonBuilder.toString());
                    System.out.println("LIRInstructionsByteCode has been successfully dumped to LIRInstructionsByteCodeDump.json");
                } catch (IOException e) {
                    System.err.println("Error while dumping LIRInstructionsByteCode to JSON: " + e.getMessage());
                }
            }

}