package simblock.node.routing;

import simblock.node.Link;
import simblock.node.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SampleRoutingTable extends UndirectedRoutingTable {

    static HashMap<Integer, ArrayList<Integer>> links = new HashMap<>();

    static public int networkSize;

    /**
     * Sample topology load from file with each line formatted as "nodeXX <----> nodeXX"
     * @param sampleTopoStream
     */
    static public void loadTopology(InputStream sampleTopoStream) {
        int maxNodeIdx = -1;
        Pattern p = Pattern.compile("node(\\d+) <----> node(\\d+)");
        BufferedReader reader = new BufferedReader(new InputStreamReader(sampleTopoStream));
        try {
            while (reader.ready()) {
                String line = reader.readLine();
                Matcher m = p.matcher(line);
                if (!m.matches()) {
                    throw new RuntimeException("Malformatted topology file on line: " + line);
                }
                Integer from = Integer.valueOf(m.group(1));
                Integer to = Integer.valueOf(m.group(2));
                ArrayList<Integer> targets = links.getOrDefault(from, new ArrayList<>());
                targets.add(to);
                links.putIfAbsent(from, targets);
                if (from > maxNodeIdx) {
                    maxNodeIdx = from;
                }
                if (to > maxNodeIdx) {
                    maxNodeIdx = to;
                }
                networkSize = maxNodeIdx + 1;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sample topology: " + e.getMessage());
        }
    }

    static public int getNetworkSize() {
        return networkSize;
    }

    public SampleRoutingTable(Node n) {
        super(n);
    }

    @Override
    public void initTable() {
        int index = allNodes.indexOf(selfNode);
        for (Integer toIdx : links.getOrDefault(index, new ArrayList<>())) {
            addConnectionTo(allNodes.get(toIdx));
        }
    }

    @Override
    public Set<Node> updateConnections(Set<Node> neighbors) {
        return neighbors;
    }

    @Override
    public void reconnect2Network() {
        throw new RuntimeException("Sample topology does not support CHURN mode");
    }
}
