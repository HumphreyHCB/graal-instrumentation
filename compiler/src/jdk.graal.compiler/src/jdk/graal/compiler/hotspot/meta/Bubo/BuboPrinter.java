package jdk.graal.compiler.hotspot.meta.Bubo;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BuboPrinter
 */
public class BuboPrinter {

    public static void main(String[] args) {

        // HashMap<Integer, Long> data = BuboDataReader.readData("compiler/output.csv");
        // printPercentageBar(orderDataByTime(data));

    }

    public static void convertBrick(long brick) {
        long Id = brick / 100_000_0000;
        long time = brick % 100_000_000;
        System.out.println("For the value : " + brick);
        System.out.println("ID : " + Id);
        System.out.println("time : " + time);
    }

    public static HashMap<Integer, Long> orderDataByTime(HashMap<Integer, Long> data) {

        return data.entrySet()
                .stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));

    }

    public static void printPercentageBar(long[] data, HashMap<Integer, String> methods, Long TotalSpenttime) {

        System.out.println("\n\n");
        System.out.println("Bubo Agent collected the following metrics: \n");
        long sum = 0;
        HashMap<Integer, Long> timmings = new HashMap<>();
        for (int index : methods.keySet()) {
            sum += data[index];
            timmings.put(index, data[index]);
        }

        timmings = orderDataByTime(timmings);
        int counter = 0;
        String bars = "";
        String spaces = "";
        long fraction = 0;
        for (int index : timmings.keySet()) {
            if (counter >= 10) {
                System.out.println("...");
                System.out.println(
                        "There is " + (timmings.size() - 10) + " More ( We have Not Displyed the rest for simplicity)");
                break;
            }
            fraction = (long) (((float) data[index] / sum) * 50);
            bars = "";
            spaces = "";

            for (int i = 0; i < fraction; i++) {
                bars += "|";
            }
            for (int i = 0; i < 50 - fraction; i++) {
                spaces += " ";
            }

            System.out.print(
                    "\n Percentage {" + bars + spaces + "} " + (((float) data[index] / sum) * 100) + "% ");
            System.err.print("Method : " + methods.get(index));
            counter++;
        }
        // sum is cycles and TotalSpenttime is time
        // System.out.println("We Captured " + ((sum / TotalSpenttime) * 100) + " % of
        // the total Runtime with Instrumentation");

    }

    public static void printPercentageBar(HashMap<Integer, Long> data, HashMap<Integer, String> methods) {

        long sum = 0;
        for (Long vars : data.values()) {
            sum += vars;
        }

        System.out.println("\n\n");
        System.out.println("Bubo Agent collected the following metrics: \n");

        int counter = 0;

        for (int key : data.keySet()) {
            if (counter >= 10) {
                System.out.println("...");
                System.out.println(
                        "There is " + (data.size() - 10) + " More ( We have Not Displyed the rest for simplicity)");
                break;
            }
            long fraction = (long) (((float) data.get(key) / sum) * 50);
            String bars = "";
            String spaces = "";
            for (int i = 0; i < fraction; i++) {
                bars += "|";
            }
            for (int i = 0; i < 50 - fraction; i++) {
                spaces += " ";
            }
            if (methods.containsKey(key)) {
                System.out
                        .print("\n Percentage {" + bars + spaces + "} " + (((float) data.get(key) / sum) * 100) + "% ");
                System.out.print("Method : " + methods.get(key));
            } else {
                // we cant find an Method ID name
                System.out
                        .print("\n Percentage {" + bars + spaces + "} " + (((float) data.get(key) / sum) * 100) + "% ");
                System.out.print("Method(ID) : " + key);
            }

            counter++;
        }

    }

    public static BigDecimal round(float d, int decimalPlace) {
        if (d == 0 || d == 0.0f) {
            return BigDecimal.ZERO; // Return zero if the input is exactly 0
        }
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
        return bd;
    }

    public static void addToFile(String line) {
        String filename = "CompiledMethodCount.txt";
        String newline = System.getProperty("line.separator"); // Get the system's newline character

        try {
            // Create a FileWriter object with append mode
            FileWriter writer = new FileWriter(filename, true);

            // Append a newline to the file
            writer.write(newline);
            writer.write(line);

            // Close the FileWriter
            writer.close();

            System.out.println("Newline appended to the file successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }

    }

    public static void printMultiBufferDebug(long[] TimeBuffer, long[] ActivationCountBuffer, long[] CyclesBuffer,
            HashMap<Integer, String> methods, String filename) {

        System.out.println("\n\n");
        System.out.println("Bubo Agent collected the following metrics: \n");
        long sum = 0;
        HashMap<Integer, Long> timmings = new HashMap<>();
        for (int index : methods.keySet()) {
            if (TimeBuffer[index] != 0) {
                sum += TimeBuffer[index];
                timmings.put(index, TimeBuffer[index]);
            } else if (CyclesBuffer[index] != 0) {
                sum += CyclesBuffer[index];
                timmings.put(index, CyclesBuffer[index]);
            } else {
                // method was comppiled but we have no infor it it
                // maybe we look at this some point
            }

        }

        timmings = orderDataByTime(timmings);
        int counter = 0;
        String bars = "";
        String spaces = "";
        long fraction = 0;
        for (int index : timmings.keySet()) {
            if (counter > 10) {
                // System.out.println("...");
                // System.out.println(
                // "There is " + (timmings.size() - 10) + " More ( We have Not Displyed the rest
                // for simplicity)");
                break;
            }
            fraction = (long) (((float) timmings.get(index) / sum) * 50);
            bars = "";
            spaces = "";

            for (int i = 0; i < fraction; i++) {
                bars += "|";
            }
            for (int i = 0; i < 50 - fraction; i++) {
                spaces += " ";
            }

            // System.out.print("\n Percentage {" + bars + spaces + "} " + (((float)
            // timmings.get(index) / sum) * 100) + "% ");
            // System.out.print("\n " + (((float) timmings.get(index) / sum) * 100) + "% ");
            // System.out.print("@ ActivationCountBuffer : " +
            // ActivationCountBuffer[index]);
            // System.out.print("@ TimeBuffer : " + TimeBuffer[index]);
            // System.out.print("@ CyclesEstBuffer : " + CyclesBuffer[index]);
            // System.out.print("@ Method : " + methods.get(index));
            counter++;
            addToFile(((((float) timmings.get(index) / sum) * 100) + "% ") + methods.get(index), filename);

        }
        // sum is cycles and TotalSpenttime is time
        // System.out.println("We Captured " + ((sum / TotalSpenttime) * 100) + " % of
        // the total Runtime with Instrumentation");

    }

    public static void printCompUnit(HashMap<Integer, String> methods, HashMap<Integer, List<CompUnitInfo>> CompUnits) {
        System.out.println("\n\n");
        System.out.println("Bubo Agent collected the following metrics: \n");

        long sum = 0;
        HashMap<Integer, Long> timmings = new HashMap<>();
        for (int index : methods.keySet()) {
            if (BuboNativeBuffers.readTimeAt(index) != 0) {
                long adjusted = BuboNativeBuffers.readTimeAt(index) - BuboNativeBuffers.readCallSiteAt(index);
                ;
                sum += adjusted;
                timmings.put(index, adjusted);
            } else if (BuboNativeBuffers.readCyclesAt(index) != 0) {
                sum += BuboNativeBuffers.readCyclesAt(index);
                timmings.put(index, BuboNativeBuffers.readCyclesAt(index));
            } else {
                // method was compiled but we have no information on it
            }
        }

        timmings = orderDataByTime(timmings);
        int counter = 0;
        System.out.println("");
        System.out.println("The following is the Top 10 hottest Compilation Units");
        int[] top10Indexes = new int[10];
        for (int index : timmings.keySet()) {
            if (counter >= 10) {
                break;
            }
            System.out.println(methods.get(index) + " : " + (((float) timmings.get(index) / sum) * 100) + "% ");
            top10Indexes[counter] = index;
            counter++;
        }

        List<Map<String, Double>> listOfInlinedNodePercentage = new ArrayList<>();
        for (int key : timmings.keySet()) {
            double maxPercentage = ((double) timmings.get(key) / sum) * 100;
            listOfInlinedNodePercentage.add(findInlinedNodePercentage(maxPercentage, CompUnits.get(key)));
        }

        Map<String, Double> combinedMap = combinedMap(listOfInlinedNodePercentage);
        // combinedMap = aggregateReComps(combinedMap);

        if (combinedMap.isEmpty()) {
            return;
        }

        Map<String, Double> sortedMap = combinedMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        System.out.println("\n\n Inlined Estimation : \n");

        counter = 0;
        for (String key : sortedMap.keySet()) {
            if (counter >= 10) {
                break;
            }
            System.out.println(key + ": " + sortedMap.get(key));
            counter++;
        }
    }

    public static void printCompUnitandDump(long[] TimeBuffer, long[] ActivationCountBuffer, long[] CyclesBuffer,
            long[] CallSiteBuffer, HashMap<Integer, String> methods, HashMap<Integer, List<CompUnitInfo>> CompUnits,
            String filename) {
        System.out.println("\n\n");
        System.out.println("Bubo Agent collected the following metrics: \n");

        long sum = 0;
        HashMap<Integer, Long> timmings = new HashMap<>();
        for (int index : methods.keySet()) {
            if (TimeBuffer[index] != 0) {
                long adjusted = TimeBuffer[index] - CallSiteBuffer[index];
                sum += adjusted;
                timmings.put(index, adjusted);
            } else if (CyclesBuffer[index] != 0) {
                sum += CyclesBuffer[index];
                timmings.put(index, CyclesBuffer[index]);
            } else {
                // method was compiled but we have no information on it
            }
        }

        timmings = orderDataByTime(timmings);
        int counter = 0;
        System.out.println("");
        System.out.println("The following is the Top 10 hottest Compilation Units");
        int[] top10Indexes = new int[10];
        for (int index : timmings.keySet()) {
            if (counter >= 10) {
                break;
            }
            System.out.println(methods.get(index) + " : " + (((float) timmings.get(index) / sum) * 100) + "% ");
            top10Indexes[counter] = index;
            counter++;
        }

        List<Map<String, Double>> listOfInlinedNodePercentage = new ArrayList<>();
        for (int key : timmings.keySet()) {
            double maxPercentage = ((double) timmings.get(key) / sum) * 100;
            listOfInlinedNodePercentage.add(findInlinedNodePercentage(maxPercentage, CompUnits.get(key)));
        }

        Map<String, Double> combinedMap = combinedMap(listOfInlinedNodePercentage);
        // combinedMap = aggregateReComps(combinedMap);

        if (combinedMap.isEmpty()) {
            return;
        }

        Map<String, Double> sortedMap = combinedMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        System.out.println("\n\n Inlined Estimation : \n");

        counter = 0;
        for (String key : sortedMap.keySet()) {
            if (counter >= 10) {
                break;
            }
            System.out.println(key + ": " + sortedMap.get(key));
            addToFile(key + ": " + sortedMap.get(key), filename);
            counter++;
        }
    }

    public static void printCompUnit(long[] TimeBuffer, long[] ActivationCountBuffer, long[] CyclesBuffer,
            long[] CallSiteBuffer, HashMap<Integer, String> methods, HashMap<Integer, List<CompUnitInfo>> CompUnits) {
        System.out.println("\n\n");
        System.out.println("Bubo Agent collected the following metrics: \n");

        long sum = 0;
        HashMap<Integer, Long> timmings = new HashMap<>();
        // for each method compiled
        // check if we have timmings for it in ever the time buffer or the est buffer
        for (int index : methods.keySet()) {
            if (TimeBuffer[index] != 0) {
                // if its timed, we need to minus the call site buffer
                long adjusted = TimeBuffer[index] - CallSiteBuffer[index];
                sum += adjusted;
                timmings.put(index, adjusted);
            } else if (CyclesBuffer[index] != 0) {
                sum += CyclesBuffer[index];
                timmings.put(index, CyclesBuffer[index]);
            } else {
                // method was compiled but we have no information on it
            }
        }
        // sort by largest time
        timmings = orderDataByTime(timmings);
        int counter = 0;
        System.out.println("");
        System.out.println("The following is the Top 10 hottest Compilation Units");
        int[] top10Indexes = new int[10];
        for (int index : timmings.keySet()) {
            if (counter >= 10) {
                break;
            }
            System.out.println(methods.get(index) + " : " + (((float) timmings.get(index) / sum) * 100) + "% ");
            top10Indexes[counter] = index;
            counter++;
        }

        List<Map<String, Double>> listOfInlinedNodePercentage = new ArrayList<>();
        for (int key : timmings.keySet()) {
            double maxPercentage = ((double) timmings.get(key) / sum) * 100;
            listOfInlinedNodePercentage.add(findInlinedNodePercentage(maxPercentage, CompUnits.get(key)));
        }

        Map<String, Double> combinedMap = combinedMap(listOfInlinedNodePercentage);
        // combinedMap = aggregateReComps(combinedMap);

        if (combinedMap.isEmpty()) {
            return;
        }

        Map<String, Double> sortedMap = combinedMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        System.out.println("\n\n Inlined Estimation : \n");

        counter = 0;
        for (String key : sortedMap.keySet()) {
            if (counter >= 10) {
                break;
            }
            System.out.println(key + ": " + sortedMap.get(key));
        }
    }

    public static void printHotMethodsTop10() {
        // Ensure both native components are ready
        BuboNativeMethodCache.ensureInitialized();
        BuboNativeBuffers.ensureInitialized();

        System.out.println("\n\n");
        System.out.println("Bubo Agent collected the following metrics:\n");

        // ID -> method name (e.g., "HotSpotCompilation-96
        // Towers$TowersDisk.setNext(...)")
        Map<Integer, String> methods = BuboNativeMethodCache.getBuffer();
        if (methods.isEmpty()) {
            System.out.println("No methods recorded.");
            return;
        }

        long sum = 0L;
        Map<Integer, Long> timings = new HashMap<>();

        for (int id : methods.keySet()) {
            long t = BuboNativeBuffers.readTimeAt(id);
            long cs = BuboNativeBuffers.readCallSiteAt(id);
            long cy = BuboNativeBuffers.readCyclesAt(id);

            if (t != 0L) {
                long adjusted = t - cs;
                if (adjusted < 0)
                    adjusted = 0; // clamp just in case
                sum += adjusted;
                timings.put(id, adjusted);
            } else if (cy != 0L) {
                sum += cy;
                timings.put(id, cy);
            } else {
                // compiled but no timing info; skip
            }
        }

        if (timings.isEmpty() || sum == 0L) {
            System.out.println("No non-zero timing data.");
            return;
        }

        // Sort by time descending
        LinkedHashMap<Integer, Long> sorted = timings.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));

        System.out.println();
        System.out.println("The following is the Top 10 hottest Compilation Units");

        int shown = 0;
        for (Map.Entry<Integer, Long> e : sorted.entrySet()) {
            if (shown >= 10)
                break;
            int id = e.getKey();
            long v = e.getValue();
            String name = methods.getOrDefault(id, ("<unknown-" + id + ">"));
            float pct = (float) v * 100.0f / (float) sum;
            System.out.println(name + " : " + pct + "% ");
            shown++;
        }
    }

    /** If you want the sorted map back instead of printing. */
    public static LinkedHashMap<Integer, Long> getHotMethodsSorted() {
        BuboNativeMethodCache.ensureInitialized();
        BuboNativeBuffers.ensureInitialized();

        Map<Integer, String> methods = BuboNativeMethodCache.getBuffer();
        Map<Integer, Long> timings = new HashMap<>();
        long sum = 0L;

        for (int id : methods.keySet()) {
            long t = BuboNativeBuffers.readTimeAt(id);
            long cs = BuboNativeBuffers.readCallSiteAt(id);
            long cy = BuboNativeBuffers.readCyclesAt(id);

            if (t != 0L) {
                long adjusted = Math.max(0L, t - cs);
                sum += adjusted;
                timings.put(id, adjusted);
            } else if (cy != 0L) {
                sum += cy;
                timings.put(id, cy);
            }
        }

        if (timings.isEmpty())
            return new LinkedHashMap<>();

        return timings.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    public static void BuboLIRPrint() {
        BuboNativeMethodCache.ensureInitialized();
        BuboNativeBuffers.ensureInitialized();

        // compId -> methodName
        var methodMap = BuboNativeMethodCache.getBuffer();

        Map<Integer, Map<Integer, String>> loopsourceMap = BuboNativeLoopSourceCache.snapshot();

        final int maxLoops = BuboNativeBuffers.MAX_LOOPS_PER_COMP;
        final int capacity = BuboNativeBuffers.capacity();

        // how many compilation "rows" can we store?
        final int maxComps = capacity / maxLoops;
        for (int compId = 0; compId < maxComps; compId++) {
            String name = methodMap.get(compId);
            boolean printedHeader = false;

            for (int loopId = 0; loopId < maxLoops; loopId++) {
                // flat index = compId * maxLoops + loopId
                int flat = compId * maxLoops + loopId;
                long val = BuboNativeBuffers.readCyclesAt(flat);
                if (val != 0L) {
                    if (!printedHeader) {
                        System.out.println("Comp " + compId + " (" + (name != null ? name : "<unknown>") + ") loops:");
                        System.out.println("Found Encoding : " + BuboNativeLoopNestingCache.getEncoding(compId));
                        printLoopNestingPretty(BuboNativeLoopNestingCache.getEncoding(compId));
                        printedHeader = true;
                    }

                    // System.out.println(" loop " + loopId + " = " + val );
                    System.out.println(
                            "  loop " + loopId + " = " + val + " Source: " + loopsourceMap.get(compId).get(loopId));
                }
            }
        }

    }

    public static void printLoopNestingPretty(String encoding) {
        if (encoding == null || encoding.isEmpty()) {
            System.out.println("No loops");
            return;
        }

        // child -> parent
        Map<Integer, Integer> parentMap = new HashMap<>();
        // keep all nodes we see (children + parents)
        Set<Integer> nodes = new LinkedHashSet<>();

        for (String p : encoding.split(",")) {
            if (p.isEmpty()) {
                continue;
            }
            String[] parts = p.split(":");
            if (parts.length != 2) {
                continue;
            }

            int child = Integer.parseInt(parts[0]);
            int parent = Integer.parseInt(parts[1]);

            parentMap.put(child, parent);
            nodes.add(child);
            if (parent != -1) {
                nodes.add(parent);
            }
        }

        // find roots = loops whose parent is -1 or missing
        List<Integer> roots = new ArrayList<>();
        for (int n : nodes) {
            int parent = parentMap.getOrDefault(n, -1);
            if (parent == -1 || !nodes.contains(parent)) {
                roots.add(n);
            }
        }

        if (roots.isEmpty()) {
            System.out.println("No top-level loops");
            return;
        }

        for (int i = 0; i < roots.size(); i++) {
            boolean last = (i == roots.size() - 1);
            printNode(parentMap, nodes, roots.get(i), "", last);
        }
    }

    private static void printNode(Map<Integer, Integer> parentMap,
            Set<Integer> nodes,
            int node,
            String prefix,
            boolean isLast) {
        String branch = isLast ? "└── " : "├── ";
        System.out.println(prefix + branch + "Loop " + node);

        // find children of this node (scan parentMap)
        List<Integer> children = new ArrayList<>();
        for (int n : nodes) {
            int parent = parentMap.getOrDefault(n, -1);
            if (parent == node) {
                children.add(n);
            }
        }

        if (children.isEmpty()) {
            return;
        }

        String newPrefix = prefix + (isLast ? "    " : "│   ");

        for (int i = 0; i < children.size(); i++) {
            boolean lastChild = (i == children.size() - 1);
            printNode(parentMap, nodes, children.get(i), newPrefix, lastChild);
        }
    }

    public static void addToFile(String line, String Filename) {
        String filename = Filename;
        String newline = System.getProperty("line.separator"); // Get the system's newline character

        try {
            // Create a FileWriter object with append mode
            FileWriter writer = new FileWriter(filename, true);

            // Append a newline to the file
            writer.write(newline);
            writer.write(line);

            // Close the FileWriter
            writer.close();

            // System.out.println("Newline appended to the file successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }

    }

    public static Map<String, Double> findInlinedNodePercentage(double max, List<CompUnitInfo> compUnitInfos) {
        Map<String, Double> counts = new HashMap<>();
        for (CompUnitInfo info : compUnitInfos) {
            String name = info.getMethodName().replace("/", ".").replace(";", "");
            if (!"Null".equals(name)) {
                counts.put(name, info.getRatio());
            }
        }

        double totalSum = counts.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> returnMap = new HashMap<>();
        for (String key : counts.keySet()) {
            double percentage = (counts.get(key) * max) / totalSum;
            returnMap.put(key, percentage);
        }

        return returnMap;
    }

    public static Map<String, Double> combinedMap(List<Map<String, Double>> listOfMaps) {

        // Resultant map to combine all entries
        Map<String, Double> combinedMap = new HashMap<>();

        // Process each map in the list
        for (Map<String, Double> map : listOfMaps) {
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                combinedMap.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        // Output the combined results
        // for (Map.Entry<String, Double> entry : combinedMap.entrySet()) {
        // System.out.println(entry.getKey() + ": " + entry.getValue());
        // }

        return combinedMap;

    }

    /// if there are recompliations we need to aggrate them
    public static Map<String, Double> aggregateReComps(Map<String, Double> map) {
        Map<String, Double> originalMap = new HashMap<>(map);
        Map<String, Double> toAdd = new HashMap<>();

        for (String compName : originalMap.keySet()) {
            if (compName.endsWith("-Re-Comp")) {
                String OGComp = removeSuffix(compName, "-Re-Comp");

                toAdd.put(OGComp, originalMap.get(OGComp) + originalMap.get(compName));
                map.remove(compName);
            }
        }

        for (Map.Entry<String, Double> entry : toAdd.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    public static String removeSuffix(String str, String suffix) {
        if (str != null && str.endsWith(suffix)) {
            return str.substring(0, str.length() - suffix.length());
        }
        return str;
    }
}
