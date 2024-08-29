package jdk.graal.compiler.hotspot.amd64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import jdk.graal.compiler.options.Option;
import jdk.graal.compiler.options.OptionKey;
import jdk.graal.compiler.options.OptionType;
import jdk.graal.compiler.util.json.JsonParser;

public class LIRInstructionCostLookup {
    private static final EconomicMap<String, Integer> CLASS_COST_MAP = EconomicMap.create();



    static {
        try {
            loadClassCostsFromJSON("MandelbrotInstructionsAdjusted.json");
        } catch (IOException e) {
            System.err.println("Failed to load class costs: " + e.getMessage());
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

        // Parse the JSON content into an EconomicMap
        JsonParser parser = new JsonParser(jsonContent);
        EconomicMap<String, Object> jsonMap = (EconomicMap<String, Object>) parser.parse();

        // Iterate over the entries using a MapCursor and populate CLASS_COST_MAP
        MapCursor<String, Object> cursor = jsonMap.getEntries();
        while (cursor.advance()) {
            String key = cursor.getKey().replaceFirst("class ", "");
            Integer value = (Integer) cursor.getValue();
            CLASS_COST_MAP.put(key, value);
        }
    }

    /**
     * Method to look up the cost of a given class.
     *
     * @param className the fully qualified name of the class
     * @return the cost associated with the class, or 1 if the class is not found
     */
    public static int getCost(String className) {
        // Sanitize the class name and look it up in the map
        String sanitizedClassName = className.replaceFirst("class ", "").trim();
        if (CLASS_COST_MAP.containsKey(sanitizedClassName)) {
            return CLASS_COST_MAP.get(sanitizedClassName);
        } else {
            //System.out.println("Missing " + className);
            return 1; // Default value if the class is not found
        }
    }
}
