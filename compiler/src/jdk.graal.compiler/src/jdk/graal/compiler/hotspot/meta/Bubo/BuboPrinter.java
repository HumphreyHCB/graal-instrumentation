package jdk.graal.compiler.hotspot.meta.Bubo;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
         String filename = "Incount.txt";
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

    public static void printMultiBufferDebug(long[] TimeBuffer,long[] ActivationCountBuffer,long[] CyclesBuffer, HashMap<Integer, String> methods) {

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
            if (counter >= 100) {
                System.out.println("...");
                System.out.println(
                        "There is " + (timmings.size() - 10) + " More ( We have Not Displyed the rest for simplicity)");
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
            System.out.print("\n  " + (((float) timmings.get(index) / sum) * 100) + "% ");
            System.out.print("@ ActivationCountBuffer : " + ActivationCountBuffer[index]);
            System.out.print("@ TimeBuffer : " + TimeBuffer[index]);
            System.out.print("@ CyclesEstBuffer : " + CyclesBuffer[index]);
            System.out.print("@ Method : " + methods.get(index));
            counter++;
        }
        // sum is cycles and TotalSpenttime is time
        //System.out.println("We Captured " + ((sum / TotalSpenttime) * 100) + " % of the total Runtime with Instrumentation");

    }

}
