package jdk.graal.compiler.hotspot.meta.GT;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap;
/**
 * BuboCache thread buffer, this is called viva a Forgien call
 * see Custom Instrumentation Phase to see where its called.
 */

public class GTCache extends Thread {

    public static long[] ActivationCountBuffer; // stores activaation of Comp units
    private static Map<String, Set<String>> LIRInstructionsByteCode;
    private static List<Map.Entry<String, Integer>> instructionList;

    public GTCache() {
        ActivationCountBuffer = new long[200_000];
        LIRInstructionsByteCode = new HashMap<>();
        instructionList = new ArrayList<>();

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
        return LIRInstructionsByteCode.getOrDefault(id, new HashSet<>()); // Return the set or an empty set if ID
                                                                          // doesn't
        // exist
    }

    // Method to check if a string exists for a specific ID
    public boolean containsStringForID(String id, String value) {
        Set<String> strings = LIRInstructionsByteCode.get(id);
        return strings != null && strings.contains(value);
    }

    public static String disassembleOPCode(String input) {
        // Define the command with the input string
        String[] command = { "/bin/bash", "-c", "echo \"" + input + "\" | udcli -64 -x" };

        StringBuilder output = new StringBuilder();

        // Create a ProcessBuilder instance
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        try {
            // Start the process
            Process process = processBuilder.start();

            // Capture the output from the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Process exited with error code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return output.toString().trim();
    }

    public static List<String> extractCommands(String input) {
        List<String> commands = new ArrayList<>();

        // Split the input by lines
        String[] lines = input.split("\\n");

        for (String line : lines) {
            // Split each line by spaces, trim and filter out empty segments
            String[] parts = line.trim().split("\\s+");

            // The command part is typically the first non-address, non-opcode segment
            if (parts.length >= 3) {
                commands.add(parts[2]);
            }
        }

        return commands;
    }

    public static List<Map.Entry<String, Integer>> parseCsvToInstructionList() {
        String csvFile = "instructions.csv";
        //List<Map.Entry<String, Integer>> instructionList = new ArrayList<>();

        try {
            // Read all lines from the CSV file
            List<String> lines = Files.readAllLines(Paths.get(csvFile));

            // Skip the header line
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] values = line.split(",");

                // Assuming the CSV structure is: name, opcode, ops
                String name = values[0];
                int ops = Integer.parseInt(values[values.length - 1]); // "ops" is the last column

                // Store in a Map.Entry
                Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>(name, ops);
                instructionList.add(entry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return instructionList;
    }

    public static Integer containsOPCODE(String nameToCheck) {
        for (Map.Entry<String, Integer> entry : instructionList) {
            if (entry.getKey().equals(nameToCheck.toUpperCase())) {
                return entry.getValue();
            }
        }
        System.out.println("Could not find opcode " + nameToCheck);
        return 1; // we cant find the comand, in 90 % case thats fine as they are only 1cycle cost
    }

    public static int caculateCostForInstructions(List<String> bytes) {
        int cost = 0;
        for (String string : bytes) {
            cost += containsOPCODE(string);
        }
        return cost;
    }
        // Method to deep copy a map with sets as values
        private static Map<String, Set<String>> deepCopy(Map<String, Set<String>> original) {
            Map<String, Set<String>> copy = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : original.entrySet()) {
                copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            return copy;
        }
    public static void postProcessingShutdown() {
        parseCsvToInstructionList();

        Map<String, Set<String>> snapshot;

        // Create a defensive copy of the map
        synchronized (LIRInstructionsByteCode) {
            snapshot = deepCopy(LIRInstructionsByteCode);
        }
        Map<String, Integer> LIRCostMap = new HashMap<>(); 
        int counter = 0;
        int tempSum = 0;
        for (Map.Entry<String, Set<String>> entry : snapshot.entrySet()) {
            for (String value : entry.getValue()) {
                // a LIR may have many differrent versions so we take aveage cost
                tempSum += caculateCostForInstructions(extractCommands(disassembleOPCode(value)));
                counter++;
            }
            LIRCostMap.put(entry.getKey(), Math.round(tempSum/counter));
            counter = 0;
            tempSum = 0;
        }

        System.out.println("\n\n\n\n\n Dump:");
        for (String Lir : LIRCostMap.keySet()) {
            System.out.println("For LIR : " + Lir + " Cost: " +  LIRCostMap.get(Lir));
        }

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
            System.out.println(
                    "LIRInstructionsByteCode has been successfully dumped to LIRInstructionsByteCodeDump.json");
        } catch (IOException e) {
            System.err.println("Error while dumping LIRInstructionsByteCode to JSON: " + e.getMessage());
        }
    }

}