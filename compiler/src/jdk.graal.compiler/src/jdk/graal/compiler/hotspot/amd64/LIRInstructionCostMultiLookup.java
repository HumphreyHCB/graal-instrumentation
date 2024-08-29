package jdk.graal.compiler.hotspot.amd64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.graalvm.collections.EconomicMap;

import jdk.graal.compiler.util.json.JsonParser;

public class LIRInstructionCostMultiLookup {

    private static final EconomicMap<String, LIRCost> CLASS_COST_MAP = EconomicMap.create();

    static {
        try {
            loadClassCostsFromJSON("LIRCostVaware2.json");
        } catch (IOException e) {
            System.err.println("Failed to load class costs: " + e.getMessage());
        }
    }

    /**
     * Class to hold both normal and V costs.
     */
    public static class LIRCost {
        public final int normalCost;
        public final int vCost;

        public LIRCost(int normalCost, int vCost) {
            this.normalCost = normalCost;
            this.vCost = vCost;
        }
    }

    /**
     * Loads class costs from a JSON file into the CLASS_COST_MAP.
     *
     * @param filePath the path to the JSON file
     * @throws IOException if there is an error reading the file
     */
    private static void loadClassCostsFromJSON(String filePath) throws IOException {
        // Read the JSON file content
        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));

        // Parse the JSON content into a List of EconomicMap
        JsonParser parser = new JsonParser(jsonContent);
        List<EconomicMap<String, Object>> jsonList = (List<EconomicMap<String, Object>>) parser.parse();

        // Iterate over the list and populate CLASS_COST_MAP
        for (EconomicMap<String, Object> map : jsonList) {
            String key = ((String) map.get("Class")).replaceFirst("class ", "");
            Integer normalCost = (Integer) map.get("normalCost");
            Integer vCost = (Integer) map.get("vCost");

            CLASS_COST_MAP.put(key, new LIRCost(normalCost, vCost));
        }
    }

    /**
     * Method to look up the normal cost of a given class.
     *
     * @param className the fully qualified name of the class
     * @return the normal cost associated with the class, or 1 if the class is not found
     */
    public static int getNormalCost(String className) {
        String sanitizedClassName = className.replaceFirst("class ", "").trim();
        if (CLASS_COST_MAP.containsKey(sanitizedClassName)) {
            return CLASS_COST_MAP.get(sanitizedClassName).normalCost;
        } else {
            //System.out.println("Have no cost for " + sanitizedClassName);
            return 1; // Default value if the class is not found
        }
    }

    /**
     * Method to look up the V cost of a given class.
     *
     * @param className the fully qualified name of the class
     * @return the V cost associated with the class, or 0 if the class is not found
     */
    public static int getVCost(String className) {
        String sanitizedClassName = className.replaceFirst("class ", "").trim();
        if (CLASS_COST_MAP.containsKey(sanitizedClassName)) {
            return CLASS_COST_MAP.get(sanitizedClassName).vCost;
        } else {
            //System.out.println("Have no cost for " + sanitizedClassName);
            return 1; // Default value if the class is not found
        }
    }
}