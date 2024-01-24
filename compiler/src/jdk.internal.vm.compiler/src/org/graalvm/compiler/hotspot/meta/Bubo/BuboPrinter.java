package org.graalvm.compiler.hotspot.meta.Bubo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * BuboPrinter
 */
public class BuboPrinter {

    public static void main(String[] args) {

        //HashMap<Integer, Long> data = BuboDataReader.readData("compiler/output.csv");
        //printPercentageBar(orderDataByTime(data));

    }

    public static void convertBrick(long brick) {
        long Id = brick/100_000_0000;
        long time = brick % 100_000_000;
        System.out.println("For the value : "+ brick);
        System.out.println("ID : " + Id );
        System.out.println("time : " + time);
    }

    public static HashMap<Integer, Long> orderDataByTime(HashMap<Integer, Long> data)
    {

        return data.entrySet()
                        .stream()
                        .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));

    }

    public static void printPercentageBar(HashMap<Integer, Long> data, HashMap<Integer, String> methods){
        long sum = 0;
        for (Long vars : data.values()) {
            sum+=vars;
        }

        int counter = 0;

        for (int key : data.keySet()) {
            if (counter== 11) {
                break; // very wastfull, find a better way
            }
            long fraction = (long) (((float) data.get(key)/sum) * 100);
            String bars = "";
            String spaces = "";
            for (int i = 0; i < fraction; i++) {
                bars+= "|";
            }
            for (int i = 0; i < 100 - fraction; i++) {
                spaces+= " ";
            }
           if (methods.containsKey(key)) {
            System.err.println("Method : " + methods.get(key) +" Percentage {" +bars + spaces + "} " + round(((float) data.get(key)/sum) * 100,2) + "% ");
           }
           else{
            System.err.println("Method : " + key +" Percentage {" +bars + spaces + "} " + round(((float) data.get(key)/sum) * 100,2) + "% ");
           }

            counter++;
        }

    }

    public static BigDecimal round(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d));
    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);      
    return bd;
}
}
