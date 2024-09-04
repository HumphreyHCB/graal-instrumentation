package jdk.graal.compiler.hotspot.meta.GT;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.collections.EconomicMap;

import capstone.Capstone;
import jdk.graal.compiler.core.common.GraalOptions;
import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.graal.compiler.options.OptionType;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.util.json.JsonParser;
import jdk.graal.compiler.util.json.JsonWriter;

public class GTCache extends Thread {

    public static class Options {
        @Option(help = "The name of the file you would like the information to be dumped to.", type = OptionType.Debug)
        public static final OptionKey<String> LIRCostInformationFile = new OptionKey<>("LIRInstructionsCost.json");
    }

    private static Map<String, Set<String>> LIRInstructionsByteCode;
    private static Map<String, LIRInstruction> opcodeMap;
    private static Capstone capstoneParser;
    private static OptionValues OptionValues;
    private static Set<String> uniqueBytes;

    public GTCache(OptionValues optionValues) {
        LIRInstructionsByteCode = new ConcurrentHashMap<>();
        opcodeMap = new HashMap<>();
        OptionValues = optionValues;
        uniqueBytes = new HashSet<>();
    }

    public static class LIRInstruction {
        public final String name;
        public final int totalCost; // Total cycles excluding "v" type instructions
        public final int vCost;     // Cycles for "v" type instructions

        public LIRInstruction(String name, int totalCost, int vCost) {
            this.name = name;
            this.totalCost = totalCost;
            this.vCost = vCost;
        }
    }

    public static class LIRCost {
        public int normalCost; // Average normal cost
        public int vCost;      // Average "v" cost

        public LIRCost(int normalCost, int vCost) {
            this.normalCost = normalCost;
            this.vCost = vCost;
        }
    }

    public static void addStringToID(String id, String value) {
        Set<String> strings = LIRInstructionsByteCode.getOrDefault(id, new HashSet<>());
        strings.add(value);
        LIRInstructionsByteCode.put(id, strings);
    }

    public static List<String> disassembleOPCode(String input) {
        if (input == null) {
            return null;
        }
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

    public static void LoadInstructionMapCost() {
        String jsonFile = "instructionsLatency.json";
        opcodeMap = new HashMap<>();

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonParser parser = new JsonParser(reader);
            List<EconomicMap<String, Object>> instructions = (List<EconomicMap<String, Object>>) parser.parse();

            for (EconomicMap<String, Object> instruction : instructions) {
                String name = (String) instruction.get("name");
                int ops = Integer.parseInt(instruction.get("ops").toString());
                int latency = Integer.parseInt(instruction.get("latency").toString());
                String type = (String) instruction.get("type");

                int totalCost = "v".equals(type) ? 0 : ops + latency;
                int vCost = "v".equals(type) ? ops + latency : 0;

                opcodeMap.put(name, new LIRInstruction(name, totalCost, vCost));
            }
        } catch (IOException e) {
            System.out.println(
                    "Error occurred when parsing the JSON instruction list, it's either missing or we had a problem parsing it.");
            e.printStackTrace();
        }
    }


    public static LIRInstruction containsOPCODE(String nameToCheck) {
        LIRInstruction instruction = opcodeMap.get(nameToCheck.toUpperCase());
        if (instruction != null) {
            return instruction;
        } else {
            if (GraalOptions.LIRGTSlowDownDebugMode.getValue(OptionValues)) {
                System.out.println("Could not find opcode " + nameToCheck.toUpperCase());
            }
            return new LIRInstruction(nameToCheck, 1, 0); // Assuming 1 cycle cost if not found, and no vCost
        }
    }

    public static List<LIRInstruction> calculateCostForInstructions(List<String> bytes) {
        List<LIRInstruction> instructionList = new ArrayList<>();
        if (bytes != null) {
            for (String mnemonic : bytes) {
                uniqueBytes.add(mnemonic);
                LIRInstruction instruction = containsOPCODE(mnemonic);
                instructionList.add(instruction);
            }
        }
        return instructionList;
    }

    private static Map<String, Set<String>> deepCopy(Map<String, Set<String>> original) {
        Map<String, Set<String>> copy = new ConcurrentHashMap<>();

        synchronized (original) {
            for (Map.Entry<String, Set<String>> entry : original.entrySet()) {
                // Create an immutable copy of the set
                Set<String> newSet = Collections.unmodifiableSet(new HashSet<>(entry.getValue()));
                
                copy.put(entry.getKey(), newSet); // Add the copied set to the map
            }
        }

        return copy;
    }
    

    public static void postProcessingShutdown() {
        capstoneParser = new Capstone(Capstone.CS_ARCH_X86, Capstone.CS_MODE_64);
        LoadInstructionMapCost();
    
        Map<String, Set<String>> snapshot;
        try {
			sleep(5000L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        synchronized (LIRInstructionsByteCode) {
            snapshot = deepCopy(LIRInstructionsByteCode);
        }
        Map<String, LIRCost> LIRCostMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : snapshot.entrySet()) {
            if (entry.getKey().contains("class jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$MemoryMROp")) {
                System.out.println("class jdk.graal.compiler.lir.amd64.AMD64BinaryConsumer$MemoryMROp");
            }
            int entryCounter = 0;
            int entryTempSum = 0;
            int entryTempVSum = 0;
        
            for (String value : entry.getValue()) {
                List<String> mnemonics = disassembleOPCode(value);
                List<LIRInstruction> instructions = calculateCostForInstructions(mnemonics);
        
                for (LIRInstruction instruction : instructions) {
                    entryTempSum += instruction.totalCost;
                    entryTempVSum += instruction.vCost;
                }
                entryCounter++;
            }
        
            // Calculate the average cost per entry
            int averageCost = entryCounter > 0 ? (int) Math.ceil((float) entryTempSum / entryCounter) : 0;
            int averageVCost = entryCounter > 0 ? (int) Math.ceil((float) entryTempVSum / entryCounter) : 0;
            
        
            if (averageVCost > 0  && averageVCost < 1 ) {
                averageVCost = 1; // bias to 1 for when it someimtes a vector
            }
            
            LIRCostMap.put(entry.getKey(), new LIRCost(averageCost, averageVCost));
    
            if (averageVCost > 0) {
                System.out.println("LIR Instruction with 'v' cost: " + entry.getKey());
            }
        }
    
        String fileName = Options.LIRCostInformationFile.getValue(OptionValues);
    
        try (JsonWriter jsonWriter = new JsonWriter(Path.of(fileName))) {
            jsonWriter.appendArrayStart().newline().indent(); // Start the JSON array
    
            int mapSize = LIRCostMap.size();
            int mapIndex = 0;
    
            for (Map.Entry<String, LIRCost> entry : LIRCostMap.entrySet()) {
                jsonWriter.appendObjectStart(); // Start the object for this entry
                jsonWriter.appendKeyValue("Class", entry.getKey().replace("class ", ""));
                jsonWriter.appendSeparator(); // Add a separator between key-value pairs
                jsonWriter.appendKeyValue("normalCost", entry.getValue().normalCost);
                jsonWriter.appendSeparator(); // Add a separator between key-value pairs
                jsonWriter.appendKeyValue("vCost", entry.getValue().vCost);
                jsonWriter.appendObjectEnd(); // End the object for this entry
    
                if (++mapIndex < mapSize) {
                    jsonWriter.appendSeparator(); // Separate entries with a comma
                }
    
                jsonWriter.newline();
            }
    
            jsonWriter.unindent().appendArrayEnd().newline(); // End the JSON array
            jsonWriter.flush();
            System.out.println("LIRCostMap has been successfully dumped to " + fileName);
            //ystem.out.println("The vCount " + vCount);
        } catch (IOException e) {
            System.err.println("Error while dumping LIRCostMap to JSON: " + e.getMessage());
        }
    }
    
    

    public static void dumpLIRInstructionsToJSON() {
        Map<String, Set<String>> snapshot;

        synchronized (LIRInstructionsByteCode) {
            snapshot = deepCopy(LIRInstructionsByteCode);
        }
        String file = "BytecodeForeachLir.json";
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
            System.out.println(file + " has been successfully dumped to LIRInstructionsByteCodeDump.json");
        } catch (IOException e) {
            System.err.println("Error while dumping " + file + " to JSON: " + e.getMessage());
        }
    }
}
