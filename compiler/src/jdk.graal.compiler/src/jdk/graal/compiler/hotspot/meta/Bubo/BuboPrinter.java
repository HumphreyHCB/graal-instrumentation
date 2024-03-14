package jdk.graal.compiler.hotspot.meta.Bubo;

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
                    "\n Percentage {" + bars + spaces + "} " + round(((float) data[index] / sum) * 100, 2) + "% ");
            System.err.print("Method : " + methods.get(index));
            counter++;
        }
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
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
        return bd;
    }
}
