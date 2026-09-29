package main.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.world.Node;
import main.domain.world.NodeLocation;
import main.exceptions.DataLoadException;

/* loads nodes, their locations, and their enemy pools from text files. */
public class NodeFileRepository {
    private static final String LOCATIONS_PATH = "data/node_locations.txt";
    private static final String NODES_PATH = "data/nodes.txt";
    private static final String FIELD_SEPARATOR = "\\|";
    
    private final FileReaderUtil fileReaderUtil;

    public NodeFileRepository(FileReaderUtil fileReaderUtil) {
        this.fileReaderUtil = fileReaderUtil;
    }

    /* node_locations.txt format: id|name|description */
    public Map<String, NodeLocation> loadNodeLocations() throws DataLoadException {
        Map<String, NodeLocation> locations = new HashMap<>();
        for (String line : readDataLines(LOCATIONS_PATH)) {
            String[] fields = line.split(FIELD_SEPARATOR, 3);
            if (fields.length != 3) {
                throw new DataLoadException("Malformed line in " + LOCATIONS_PATH + ": " + line);
            }
            String id = fields[0].trim();
            String name = fields[1].trim();
            String description = fields[2].trim();
            locations.put(id, new NodeLocation(id, name, description));
        }
        return locations;
    }

    /* nodes.txt format: id|locationId|difficulty|hasBoss */
    public List<Node> loadNodes() throws DataLoadException {
        Map<String, NodeLocation> locations = loadNodeLocations();
        List<Node> nodes = new ArrayList<>();

        for (String line : readDataLines(NODES_PATH)) {
            String[] fields = line.split(FIELD_SEPARATOR, 4);
            if (fields.length != 4) {
                throw new DataLoadException("Malformed line in " + NODES_PATH + ": " + line);
            }
            String id = fields[0].trim();
            String locationId = fields[1].trim();

            NodeLocation location = locations.get(locationId);
            if (location == null) {
                throw new DataLoadException(
                        "Node '" + id + "' in " + NODES_PATH + " references unknown location: " + locationId);
            }

            int difficulty;
            try {
                difficulty = Integer.parseInt(fields[2].trim());
            } catch (NumberFormatException e) {
                throw new DataLoadException("Node '" + id + "' has a non-numeric difficulty: " + fields[2], e);
            }

            boolean hasBoss = parseBoolean(fields[3].trim(), id);

            nodes.add(new Node(id, location, difficulty, hasBoss));
        }
        return nodes;
    }

    private boolean parseBoolean(String value, String nodeId) throws DataLoadException {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new DataLoadException("Node '" + nodeId + "' has an invalid hasBoss value: " + value);
    }

    private List<String> readDataLines(String path) throws DataLoadException {
        List<String> result = new ArrayList<>();
        for (String line : fileReaderUtil.readLines(path)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            result.add(trimmed);
        }
        return result;
    }
}
