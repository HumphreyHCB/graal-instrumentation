package jdk.graal.compiler.hotspot.meta.Bubo;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        //System.out.println("We Captured " + ((sum / TotalSpenttime) * 100) + " % of the total Runtime with Instrumentation");

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

    public static void printMultiBufferDebug(long[] TimeBuffer,long[] ActivationCountBuffer,long[] CyclesBuffer, HashMap<Integer, String> methods, String filename) {

        System.out.println("\n\n");
        System.out.println("Bubo Agent collected the following metrics: \n");
        long sum = 0;
        HashMap<Integer, Long> timmings = new HashMap<>();
        for (int index : methods.keySet()) {
            if (TimeBuffer[index] != 0) {
                sum += TimeBuffer[index];
                timmings.put(index, TimeBuffer[index]);
            }
            else if (CyclesBuffer[index] != 0) {
                sum += CyclesBuffer[index];
                timmings.put(index, CyclesBuffer[index]);
            }
            else{
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
                //         "There is " + (timmings.size() - 10) + " More ( We have Not Displyed the rest for simplicity)");
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

            //System.out.print("\n Percentage {" + bars + spaces + "} " + (((float) timmings.get(index) / sum) * 100) + "% ");
            // System.out.print("\n  " + (((float) timmings.get(index) / sum) * 100) + "% ");
            // System.out.print("@ ActivationCountBuffer : " + ActivationCountBuffer[index]);
            // System.out.print("@ TimeBuffer : " + TimeBuffer[index]);
            // System.out.print("@ CyclesEstBuffer : " + CyclesBuffer[index]);
            // System.out.print("@ Method : " + methods.get(index));
            counter++;
            addToFile(( (((float) timmings.get(index) / sum) * 100) + "% ")+ methods.get(index), filename);

        }
        // sum is cycles and TotalSpenttime is time
        //System.out.println("We Captured " + ((sum / TotalSpenttime) * 100) + " % of the total Runtime with Instrumentation");

    }

    public static void printCompUnit(long[] TimeBuffer,long[] ActivationCountBuffer,long[] CyclesBuffer, HashMap<Integer, String> methods, HashMap<Integer, String> CompUnits) {

        System.out.println("\n\n");
        System.out.println("Bubo Agent collected the following metrics: \n");
        long sum = 0;
        HashMap<Integer, Long> timmings = new HashMap<>();
        for (int index : methods.keySet()) {
            if (TimeBuffer[index] != 0) {
                sum += TimeBuffer[index];
                timmings.put(index, TimeBuffer[index]);
            }
            else if (CyclesBuffer[index] != 0) {
                sum += CyclesBuffer[index];
                timmings.put(index, CyclesBuffer[index]);
            }
            else{
                // method was comppiled but we have no infor it it
                // maybe we look at this some point
            }

        }
        
        timmings = orderDataByTime(timmings);
        int counter = 0;
        String bars = "";
        String spaces = "";
        long fraction = 0;
        System.out.println("");
        System.out.println("The following is the Top 10 hottest Compilation Units");
        int[] top10Indexs = new int[10];
        for (int index : timmings.keySet()) {
            if (counter >= 10) {
                break;
            }

            System.out.println(methods.get(index) + " : " + (((float) timmings.get(index) / sum) * 100) + "% ");
            top10Indexs[counter] = index;
            counter++;
            // addToFile(( (((float) timmings.get(index) / sum) * 100) + "% ")+ methods.get(index) , filename);
            // addToFile("\t " + CompUnits.get(index), filename);
        }

        List<Map<String, Double>> listOfInlinedNodePercentage = new ArrayList<>();
        for (int i = 0; i < top10Indexs.length; i++) {
            double maxPercentage = ((double) timmings.get(top10Indexs[i]) / sum) * 100;
            listOfInlinedNodePercentage.add(findInlinedNodePercentage(maxPercentage,CompUnits.get(top10Indexs[i])));
        }

        Map<String, Double> combinedMap = combinedMap(listOfInlinedNodePercentage);

        if (combinedMap.size() <= 0) {
            return;
        }
        // Sort the map by values in descending order
        Map<String, Double> sortedMap = combinedMap.entrySet()
        .stream()
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        // Display the sorted map
        System.out.println("\n\n Inlinded Estimation : \n");

        counter = 0;
        for (String key : sortedMap.keySet()) {
            if (counter >= 10) {
                break;
            }

            System.out.println(key + ": " + sortedMap.get(key));
            //addToFile(key + ": " + sortedMap.get(key), filename);
            counter++;
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
           
           //System.out.println("Newline appended to the file successfully.");
       } catch (IOException e) {
           System.out.println("An error occurred: " + e.getMessage());
       }
       
   }

   public static Map<String, Double> findInlinedNodePercentage(Double max, String info) {
        // Parse the input string into a map
        Map<String, Integer> counts = new HashMap<>();
        String[] entries = info.trim().split(" ");
        for (String entry : entries) {
            String[] parts = entry.split(",");
            String name = parts[0];
            if (name == "Null") {
                continue;
             }
             name = name.replace("/", ".").replace(";", ""); 
            int count = Integer.parseInt(parts[1]);
            counts.put(name, count);
        }

        int totalSum = counts.values().stream().mapToInt(Integer::intValue).sum();
        
        Map<String, Double> returnMap = new HashMap<>();

        for (String key : counts.keySet()) {
            double percentage = (counts.get(key) * max) / totalSum;
            returnMap.put(key, percentage);
        }

     return returnMap;
    
   }

   public static Map<String, Double> combinedMap(List<Map<String, Double>> listOfMaps ) {
    
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
    //     System.out.println(entry.getKey() + ": " + entry.getValue());
    // }

    return combinedMap;

   }
}
