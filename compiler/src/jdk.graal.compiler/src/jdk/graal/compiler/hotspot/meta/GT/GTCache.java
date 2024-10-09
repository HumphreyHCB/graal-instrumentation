package jdk.graal.compiler.hotspot.meta.GT;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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
import java.util.concurrent.ConcurrentSkipListSet;

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

    public static long[] ActivationCountBuffer; // stores activation of Comp units
    private static Map<String, Set<String>> LIRInstructionsByteCode;
    private static Map<String, LIRInstruction> opcodeMap;
    private static Capstone capstoneParser;
    private static OptionValues OptionValues;
    private static Set<String> uniqueBytes;

    public GTCache(OptionValues optionValues) {
        ActivationCountBuffer = new long[200_000];
        LIRInstructionsByteCode = new ConcurrentHashMap<>();
        opcodeMap = new HashMap<>();
        OptionValues = optionValues;
        uniqueBytes = new HashSet<>();
    }

    public static class LIRInstruction {
        public final String name;
        public final int nopCost; // Total cycles excluding "v" type instructions
        public final int vCost; // Cycles for "v" type instructions

        public LIRInstruction(String name, int nopCost, int vCost) {
            this.name = name;
            this.nopCost = nopCost;
            this.vCost = vCost;
        }
    }

    public static class LIRCost {
        public int normalCost; // Average normal cost
        public int vCost; // Average "v" cost

        public LIRCost(int normalCost, int vCost) {
            this.normalCost = normalCost;
            this.vCost = vCost;
        }
    }

    public static void addStringToID(String id, String value) {
        Set<String> strings = LIRInstructionsByteCode.getOrDefault(id, new ConcurrentSkipListSet<>());
        strings.add(value);
        LIRInstructionsByteCode.put(id, strings);
    }

    public static int[] computeCycleCostForGivenString(String value) {
        if (opcodeMap == null || opcodeMap.isEmpty()) {
            LoadInstructionMapCost();
            capstoneParser = new Capstone(Capstone.CS_ARCH_X86, Capstone.CS_MODE_64);
        }
    
        // Disassemble the given input string to get a list of mnemonics.
        List<String> mnemonics = disassembleOPCode(value);
        if (mnemonics == null || mnemonics.isEmpty()) {
            System.out.println("Found no mnemonics");
            return new int[]{0, 0};
        }
    
        // Calculate the total nop cost and total v cost for the given mnemonics.
        int totalNopCost = 0;
        int totalVCost = 0;
        for (String mnemonic : mnemonics) {
            LIRInstruction instruction = containsOPCODE(mnemonic);
            totalNopCost += instruction.nopCost;
            totalVCost += instruction.vCost;
        }
    
        return new int[]{totalNopCost, totalVCost};
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
        String jsonFile = "UopsInfoEddited.json";
        opcodeMap = new HashMap<>();

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonParser parser = new JsonParser(reader);
            @SuppressWarnings("unchecked")
            List<EconomicMap<String, Object>> instructions = (List<EconomicMap<String, Object>>) parser.parse();

            for (EconomicMap<String, Object> instruction : instructions) {
                String name = (String) instruction.get("name");
                if (instruction.get("ops").toString().equals(""))
                    continue;
                int ops = Integer.parseInt(instruction.get("ops").toString());
                int latency = Integer.parseInt(instruction.get("latency").toString());
                String type = (String) instruction.get("type");

                int nopCost = "v".equals(type) ? 0 : ops + latency;
                int vCost = "v".equals(type) ? ops + latency : 0;

                opcodeMap.put(name, new LIRInstruction(name, nopCost, vCost));
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
                uniqueBytes.add(nameToCheck.toUpperCase());
            }
            return new LIRInstruction(nameToCheck, 1, 0); // Assuming 1 cycle cost if not found, and no vCost
        }
    }

    public static List<LIRInstruction> calculateCostForInstructions(List<String> bytes) {
        List<LIRInstruction> instructionList = new ArrayList<>();
        if (bytes != null) {
            for (String mnemonic : bytes) {
                LIRInstruction instruction = containsOPCODE(mnemonic);
                instructionList.add(instruction);
            }
        }
        return instructionList;
    }

    private static Map<String, Set<String>> deepCopy(Map<String, Set<String>> original) {
        Map<String, Set<String>> copy = new ConcurrentHashMap<>();

        original.forEach((key, value) -> {
            Set<String> newSet = Collections.unmodifiableSet(new HashSet<>(value));
            copy.put(key, newSet);
        });

        return copy;
    }

    public static void postProcessingShutdown() {
        capstoneParser = new Capstone(Capstone.CS_ARCH_X86, Capstone.CS_MODE_64);
        LoadInstructionMapCost();

        Map<String, Set<String>> snapshot;
        try {
            sleep(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (LIRInstructionsByteCode) {
            snapshot = deepCopy(LIRInstructionsByteCode);
        }
        Map<String, LIRCost> LIRCostMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : snapshot.entrySet()) {
            int entryCounter = 0;
            int entryTempSum = 0;
            int entryTempVSum = 0;

            for (String value : entry.getValue()) {
                List<String> mnemonics = disassembleOPCode(value);
                List<LIRInstruction> instructions = calculateCostForInstructions(mnemonics);

                for (LIRInstruction instruction : instructions) {
                    entryTempSum += instruction.nopCost;
                    entryTempVSum += instruction.vCost;
                }
                entryCounter++;
            }

            int averageCost = entryCounter > 0 ? (int) Math.ceil((float) entryTempSum / entryCounter) : 0;
            int averageVCost = entryCounter > 0 ? (int) Math.ceil((float) entryTempVSum / entryCounter) : 0;

            if (averageVCost > 0 && averageVCost < 1) {
                averageVCost = 1;
            }

            LIRCostMap.put(entry.getKey(), new LIRCost(averageCost, averageVCost));
        }
        String fileName = Options.LIRCostInformationFile.getValue(OptionValues);
        if (GraalOptions.LIRGTSlowDownDebugMode.getValue(OptionValues)) {
            if (!uniqueBytes.isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(fileName.replace(".json", "_NotFound.txt")))) {
                    for (String string : uniqueBytes) {
                        writer.write(string);
                        writer.newLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try (JsonWriter jsonWriter = new JsonWriter(Path.of(fileName))) {
            jsonWriter.appendArrayStart().newline().indent();

            int mapSize = LIRCostMap.size();
            int mapIndex = 0;

            for (Map.Entry<String, LIRCost> entry : LIRCostMap.entrySet()) {
                jsonWriter.appendObjectStart();
                jsonWriter.appendKeyValue("Class", entry.getKey().replace("class ", ""));
                jsonWriter.appendSeparator();
                jsonWriter.appendKeyValue("normalCost", entry.getValue().normalCost);
                jsonWriter.appendSeparator();
                jsonWriter.appendKeyValue("vCost", entry.getValue().vCost);
                jsonWriter.appendObjectEnd();

                if (++mapIndex < mapSize) {
                    jsonWriter.appendSeparator();
                }

                jsonWriter.newline();
            }

            jsonWriter.unindent().appendArrayEnd().newline();
            jsonWriter.flush();
            System.out.println("LIRCostMap has been successfully dumped to " + fileName);
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
