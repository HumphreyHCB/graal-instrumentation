package jdk.graal.compiler.hotspot.amd64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;
import jdk.graal.compiler.util.json.JsonParser;

public class GTBlockSlowDownLookUp {
    // Main map for all method and block costs
    private static final EconomicMap<String, EconomicMap<Integer, Integer>> METHOD_BLOCK_COST_MAP = EconomicMap.create();
    // Separate map for backend block costs
    private static final EconomicMap<String, EconomicMap<Integer, Integer>> BACKEND_BLOCK_COST_MAP = EconomicMap.create();

    static {
        try {
            loadMethodBlockCostsFromJSON("BlockSlowdown.json");
        } catch (IOException e) {
            System.out.println("Failed to load method block costs: " + e.getMessage());
        }
    }

    /**
     * Loads method block costs, including backend blocks, from a JSON file.
     *
     * @param filePath the path to the JSON file
     * @throws IOException if there is an error reading the file
     */
    public static void loadMethodBlockCostsFromJSON(String filePath) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            System.out.println("Could not locate " + filePath);
            System.out.println("Skipping Loading (this might cause a fatal crash if GTSlowdown is on)");
            return;
        }

        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
        JsonParser parser = new JsonParser(jsonContent);
        Object parsedJson = parser.parse();

        if (parsedJson instanceof EconomicMap) {
            @SuppressWarnings("unchecked")
            EconomicMap<String, Object> jsonMap = (EconomicMap<String, Object>) parsedJson;

            for (MapCursor<String, Object> cursor = jsonMap.getEntries(); cursor.advance();) {
                String methodName = cursor.getKey();

                if (cursor.getValue() instanceof EconomicMap) {
                    @SuppressWarnings("unchecked")
                    EconomicMap<String, Object> blocksMap = (EconomicMap<String, Object>) cursor.getValue();
                    EconomicMap<Integer, Integer> blockCostMap = EconomicMap.create();
                    EconomicMap<Integer, Integer> backendBlockCostMap = EconomicMap.create();

                    // Populate block cost maps for this method
                    for (MapCursor<String, Object> blockCursor = blocksMap.getEntries(); blockCursor.advance();) {
                        String blockKey = blockCursor.getKey();

                        if (blockKey.equals("Backend Blocks") && blockCursor.getValue() instanceof EconomicMap) {
                            @SuppressWarnings("unchecked")
                            EconomicMap<String, Integer> backendBlocks = (EconomicMap<String, Integer>) blockCursor.getValue();

                            // Use a MapCursor to iterate over backend blocks
                            MapCursor<String, Integer> backendCursor = backendBlocks.getEntries();
                            while (backendCursor.advance()) {
                                String backendKey = backendCursor.getKey();
                                Integer cost = backendCursor.getValue();
                                String[] keyParts = backendKey.split(" \\(Vtune Block");
                                if (keyParts.length > 1) {
                                    Integer blockNumber = Integer.parseInt(keyParts[0]);
                                    backendBlockCostMap.put(blockNumber, cost);
                                }
                            }
                        } else if (blockCursor.getValue() instanceof Integer) {
                            Integer cost = (Integer) blockCursor.getValue();
                            String[] keyParts = blockKey.split(" \\(Vtune Block");
                            if (keyParts.length > 1) {
                                Integer blockNumber = Integer.parseInt(keyParts[0]);
                                blockCostMap.put(blockNumber, cost);
                            }
                        }
                    }

                    METHOD_BLOCK_COST_MAP.put(methodName, blockCostMap);
                    BACKEND_BLOCK_COST_MAP.put(methodName, backendBlockCostMap);
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
        int index = methodName.indexOf('(');
        if (index != -1) {
            methodName = methodName.substring(0, index).trim();
        }

        if (METHOD_BLOCK_COST_MAP.containsKey(methodName)) {
            EconomicMap<Integer, Integer> blockCostMap = METHOD_BLOCK_COST_MAP.get(methodName);
            if (blockCostMap.containsKey(blockNumber)) {
                return blockCostMap.get(blockNumber);
            }
        }
        return 0; // Default value if the method or block is not found
    }

    /**
     * Look up the cost of a backend block in a specific method.
     *
     * @param methodName the name of the method
     * @param blockNumber the block number within the method
     * @return the cost associated with the backend block, or 0 if not found
     */
    public static int getBackendBlockCost(String methodName, int blockNumber) {
        int index = methodName.indexOf('(');
        if (index != -1) {
            methodName = methodName.substring(0, index).trim();
        }

        if (BACKEND_BLOCK_COST_MAP.containsKey(methodName)) {
            EconomicMap<Integer, Integer> backendCostMap = BACKEND_BLOCK_COST_MAP.get(methodName);
            if (backendCostMap.containsKey(blockNumber)) {
                return backendCostMap.get(blockNumber);
            }
        }
        return 0; // Default value if the backend block or method is not found
    }
}
