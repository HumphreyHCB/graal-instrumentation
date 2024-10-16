package jdk.graal.compiler.hotspot.amd64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import jdk.graal.compiler.util.json.JsonParser;

public class GTBlockSlowDownLookUp {
    // Using EconomicMap for GraalVM performance
    private static final EconomicMap<String, EconomicMap<Integer, Integer>> METHOD_BLOCK_COST_MAP = EconomicMap.create();

    static {
        try {
            loadMethodBlockCostsFromJSON("BlockSlowdown.json");
        } catch (IOException e) {
            System.out.println("Failed to load method block costs: " + e.getMessage());
        }
    }

    /**
     * Loads method block costs from a JSON file into the METHOD_BLOCK_COST_MAP.
     *
     * @param filePath the path to the JSON file
     * @throws IOException if there is an error reading the file
     */
    public static void loadMethodBlockCostsFromJSON(String filePath) throws IOException {
        // Read the JSON file content
        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
    
        // Parse the JSON content using the GraalVM JsonParser
        JsonParser parser = new JsonParser(jsonContent);
        Object parsedJson = parser.parse(); // Do not cast immediately
    
        // Verify the parsed object is a Map (or close to it)
        if (parsedJson instanceof EconomicMap) {
            // Safely cast and iterate over the parsed Map
            @SuppressWarnings("unchecked")
            EconomicMap<String, Object> jsonMap = (EconomicMap<String, Object>) parsedJson;
    
            // Iterate over the method entries
            for (MapCursor<String, Object> cursor = jsonMap.getEntries(); cursor.advance();) {
                String methodName = cursor.getKey();
    
                // Ensure the method entry's value is also a Map (blocks map)
                if (cursor.getValue() instanceof EconomicMap) {
                    @SuppressWarnings("unchecked")
                    EconomicMap<String, Integer> blocksMap = (EconomicMap<String, Integer>) cursor.getValue();
                    EconomicMap<Integer, Integer> blockCostMap = EconomicMap.create();
    
                    // Populate block cost map for this method
                    for (MapCursor<String, Integer> blockCursor = blocksMap.getEntries(); blockCursor.advance();) {
                        String blockKey = blockCursor.getKey();
                        Integer cost = blockCursor.getValue();
    
                        // Extract block number from key, which contains block info
                        String[] keyParts = blockKey.split(" \\(Vtune Block");
                        if (keyParts.length > 1) {
                            Integer blockNumber = Integer.parseInt(keyParts[0]);
                            blockCostMap.put(blockNumber, cost);
                        }
                    }
    
                    METHOD_BLOCK_COST_MAP.put(methodName, blockCostMap);
                }
            }
        } else {
            throw new IOException("Parsed JSON is not of the expected type.");
        }
    }

    /**
     * Method to look up the cost of a given block in a specific method.
     *
     * @param methodName the name of the method
     * @param blockNumber the block number within the method
     * @return the cost associated with the block, or 0 if the block or method is not found
     */
    public static int getBlockCost(String methodName, int blockNumber) {
        // Remove everything after the first '(' in methodName
        int index = methodName.indexOf('(');
        if (index != -1) {
            methodName = methodName.substring(0, index).trim(); // Remove from '(' onwards and trim spaces
        }
    
        // Proceed with checking the METHOD_BLOCK_COST_MAP
        if (METHOD_BLOCK_COST_MAP.containsKey(methodName)) {
            //System.out.println("Block : " +blockNumber+ " Found " + methodName );
            EconomicMap<Integer, Integer> blockCostMap = METHOD_BLOCK_COST_MAP.get(methodName);
            if (blockCostMap.containsKey(blockNumber)) {
                return blockCostMap.get(blockNumber);
            }
        }
        return 0; // Default value if the method or block is not found
    }
}
