package jdk.graal.compiler.hotspot.meta.GT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.graalvm.collections.EconomicMap;

//import org.native4j.capstone.Capstone;
import capstone.Capstone;
import jdk.graal.compiler.core.common.GraalOptions;
import jdk.graal.compiler.lir.asm.CompilationResultBuilder;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.graal.compiler.options.OptionType;
import jdk.graal.compiler.util.json.JsonBuilder;
import jdk.graal.compiler.util.json.JsonPrettyWriter;
import jdk.graal.compiler.util.json.JsonWriter;
import jdk.graal.compiler.util.json.JsonBuilder.ObjectBuilder;
import jdk.graal.compiler.util.json.JsonParser;
import jdk.graal.compiler.options.OptionValues;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public static class Options {
        @Option(help = "The name of the file you woul like the information to be dumped too.", type = OptionType.Debug) //
        public static final OptionKey<String> LIRCostInformationFile = new OptionKey<>("LIRInstructionsCost.json");
    }

    //public static long[] ActivationCountBuffer; // stores activaation of Comp units
    private static Map<String, Set<String>> LIRInstructionsByteCode;
    private static Map<String, Integer> opcodeMap;
    private static Capstone capstoneParser;
    private static OptionValues OptionValues;
    private static Set<String> uniqueBytes;
    

    public GTCache(OptionValues optionValues) {
        //capstoneParser = new Capstone(Capstone.CS_ARCH_X86, Capstone.CS_MODE_64);
        //ActivationCountBuffer = new long[200_000];
        LIRInstructionsByteCode = new ConcurrentHashMap<>();
        opcodeMap = new HashMap<>();
        OptionValues = optionValues;
        uniqueBytes = new HashSet<>();

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

    public static List<String> disassembleOPCode(String input) {
        // Convert the input string to a byte array
        List<String> mnemonics = new ArrayList<>();
        String[] byteStrings = input.trim().split(" ");
        byte[] code = new byte[byteStrings.length];

        for (int i = 0; i < byteStrings.length; i++) {
            code[i] = (byte) Integer.parseInt(byteStrings[i], 16);
        }

        Capstone.CsInsn[] allInsn = capstoneParser.disasm(code, 0x1000);

        for (Capstone.CsInsn insn : allInsn) {
            mnemonics.add(insn.mnemonic);
        }

        return mnemonics;
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


    public static void LoadInstructionMapCost() {
        String jsonFile = "MandelbrotInstructions.json";
        opcodeMap = new HashMap<>();
    
        try (FileReader reader = new FileReader(jsonFile)) {
            // Use the JsonParser to parse the JSON file
            JsonParser parser = new JsonParser(reader);
            List<EconomicMap<String, Object>> instructions = (List<EconomicMap<String, Object>>) parser.parse();
    
            // Iterate over the parsed instructions
            for (EconomicMap<String, Object> instruction : instructions) {
                String name = (String) instruction.get("name");
                int ops = Integer.parseInt(instruction.get("ops").toString());
                int latency = Integer.parseInt(instruction.get("latency").toString());
                // Store in the opcodeMap
                opcodeMap.put(name, ops + latency);
            }
        } catch (IOException e) {
            System.out.println(
                    "Error occurred when parsing the JSON instruction list, it's either missing or we had a problem parsing it.");
            e.printStackTrace();
        }
    }
    

    public static Integer containsOPCODE(String nameToCheck) {
        Integer value = opcodeMap.get(nameToCheck.toUpperCase());
        if (value != null) {
            return value;
        } else {
            if (GraalOptions.LIRGTSlowDownDebugMode.getValue(OptionValues)) {
                System.out.println("Could not find opcode " + nameToCheck.toUpperCase());
            }
            return 1; // Assuming 1 cycle cost if not found
        }
    }
    

    public static int caculateCostForInstructions(List<String> bytes) {
        int cost = 0;
        for (String string : bytes) {
            uniqueBytes.add(string);
            cost += containsOPCODE(string);
        }
        return cost;
    }

    // Method to deep copy a map with sets as values
    private static Map<String, Set<String>> deepCopy(Map<String, Set<String>> original) {
        Map<String, Set<String>> copy = new ConcurrentHashMap<>();
        for (Map.Entry<String, Set<String>> entry : original.entrySet()) {
            copy.put(entry.getKey(), new CopyOnWriteArraySet<>(entry.getValue()));
        }
        return copy;
    }

    public static void postProcessingShutdown() {
        capstoneParser = new Capstone(Capstone.CS_ARCH_X86, Capstone.CS_MODE_64);
        LoadInstructionMapCost(); // Adjusted method name

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
                tempSum += caculateCostForInstructions((disassembleOPCode(value)));
                counter++;
            }
            LIRCostMap.put(entry.getKey(), Math.round(tempSum / counter));
            counter = 0;
            tempSum = 0;
        }
        if (GraalOptions.LIRGTSlowDownDebugMode.getValue(OptionValues)) {
            System.out.println("Each uniqueByte ");

            for (String string : uniqueBytes) {
                System.out.println(string);
            }
        }
        

        String fileName = Options.LIRCostInformationFile.getValue(OptionValues);

        // Writing the JSON data to the specified file
        try (JsonWriter jsonWriter = new JsonWriter(Path.of(fileName))) {
            jsonWriter.appendObjectStart().newline().indent();

            int mapSize = LIRCostMap.size();
            int mapIndex = 0;

            for (Map.Entry<String, Integer> entry : LIRCostMap.entrySet()) {
                jsonWriter.appendKeyValue(entry.getKey(), entry.getValue());

                if (++mapIndex < mapSize) {
                    jsonWriter.appendSeparator();
                }

                jsonWriter.newline();
            }

            jsonWriter.unindent().appendObjectEnd().newline();
            jsonWriter.flush();
            System.out.println("LIRCostMap has been successfully dumped to " + fileName);
        } catch (IOException e) {
            System.err.println("Error while dumping LIRCostMap to JSON: " + e.getMessage());
        }

    }

    /*
     * Th method just dumps all the uqinly found bytecode for each LIRInstructions, it should not be use oputside of debug 
     */
    public static void dumpLIRInstructionsToJSON() {

        Map<String, Set<String>> snapshot;

        // Create a defensive copy of the map
        synchronized (LIRInstructionsByteCode) {
            snapshot = deepCopy(LIRInstructionsByteCode);
        }
        String file = "BytecodeForeachLir.json";
        // Write the JSON object to a file using JsonWriter
        try (JsonWriter jsonWriter = new JsonWriter(Path.of(file))) {
            jsonWriter.appendObjectStart().newline().indent();

            int mapSize = snapshot.size();
            int mapIndex = 0;

            for (Map.Entry<String, Set<String>> entry : snapshot.entrySet()) {
                jsonWriter.quote(entry.getKey())
                        .appendFieldSeparator()
                        .appendArrayStart();

                int setSize = entry.getValue().size();
                int setIndex = 0;

                for (String value : entry.getValue()) {
                    jsonWriter.quote(value);
                    if (++setIndex < setSize) {
                        jsonWriter.appendSeparator();
                    }
                }

                jsonWriter.appendArrayEnd();

                if (++mapIndex < mapSize) {
                    jsonWriter.appendSeparator();
                }

                jsonWriter.newline();
            }

            jsonWriter.unindent().appendObjectEnd().newline();
            jsonWriter.flush();
            System.out.println(
                file + " has been successfully dumped to LIRInstructionsByteCodeDump.json");
        } catch (IOException e) {
            System.err.println("Error while dumping "+file+" to JSON: " + e.getMessage());
        }
    }

}