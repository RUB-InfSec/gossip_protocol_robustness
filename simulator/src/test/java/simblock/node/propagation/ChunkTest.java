package simblock.node.propagation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import simblock.node.Node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;



public class ChunkTest {

    static String routingTableClass = "simblock.node.routing.UndirectedRoutingTable";
    static String consensusAlgoClass = "simblock.node.consensus.RumorSpreading";
    static String propagationClass = "simblock.node.propagation.Push";

    public List<Integer> createNodes(int numNodes) {
        List<Integer> nodes = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            nodes.add(i);
        }
        return nodes;
    }

    /** Real Node instances (weightedShuffle only operates on Node), one per given stakeProportion. */
    public List<Node> createStakedNodes(float[] stakes) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < stakes.length; i++) {
            Node n = new Node(i + 1, 0, 0, routingTableClass, consensusAlgoClass, propagationClass, false);
            n.setStakeProportion(stakes[i]);
            nodes.add(n);
        }
        return nodes;
    }

    @Test
    public void testGetChildrenInPropagationTree() {
        List<Integer> nodes = createNodes(10);
        int k = 2;
        //  0, 1, 2, 3, 4, 5, 6, 7, 8, 9
        //       0
        //     /   \
        //    1     2
        //   / \   / \
        //  3   4  5  6
        // / \ /
        // 7 8 9
        List<Integer> children;
        children = Chunk.getChildrenFromFullTree(nodes, 0, k);
        assertArrayEquals(new int[]{1, 2}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 1, k);
        assertArrayEquals(new int[]{3, 4}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 2, k);
        assertArrayEquals(new int[]{5, 6}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 3, k);
        assertArrayEquals(new int[]{7, 8}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 4, k);
        assertArrayEquals(new int[]{9}, children.stream().mapToInt(i->i).toArray());
        for (int i = 5; i < nodes.size(); i++) {
            assertEquals(null, Chunk.getChildrenFromFullTree(nodes, i, k));
        }

        nodes = createNodes(24);
        k = 3;
        //                                      0
        //                /                     |             \
        //               1                      2              3
        //      /        |        \          /  |  \        /  |  \
        //     4         5         6        7   8   9     10   11  12
        //   / | \     / | \     / | \    / |
        // 13 14 15  16 17 18  19 20 21  22 23
        children = Chunk.getChildrenFromFullTree(nodes, 0, k);
        assertArrayEquals(new int[]{1, 2, 3}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 1, k);
        assertArrayEquals(new int[]{4, 5, 6}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 2, k);
        assertArrayEquals(new int[]{7, 8, 9}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 3, k);
        assertArrayEquals(new int[]{10, 11, 12}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 4, k);
        assertArrayEquals(new int[]{13, 14, 15}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 5, k);
        assertArrayEquals(new int[]{16, 17, 18}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 6, k);
        assertArrayEquals(new int[]{19, 20, 21}, children.stream().mapToInt(i->i).toArray());
        children = Chunk.getChildrenFromFullTree(nodes, 7, k);
        assertArrayEquals(new int[]{22, 23}, children.stream().mapToInt(i->i).toArray());
        for (int i = 8; i < nodes.size(); i++) {
            assertEquals(null, Chunk.getChildrenFromFullTree(nodes, i, k));
        }
    }

    // --- getChildrenFromStrideTree: dormant, not yet wired into getChildrenInPropagationTree ---

    @Test
    public void testGetChildrenFromStrideTree_k2n10WorkedExample() {
        // Same 10-node, k=2 network as testGetChildrenInPropagationTree, but stride-fanout
        // children differ from level 2 onward (each parent claims a stride-k "column" through
        // the next level instead of a contiguous block):
        //       0
        //     /   \
        //    1     2
        //   / \   / \
        //  3   5 4   6
        // / \     \
        // 7 9      8
        List<Integer> nodes = createNodes(10);
        int k = 2;
        assertArrayEquals(new int[]{1, 2}, Chunk.getChildrenFromStrideTree(nodes, 0, k).stream().mapToInt(i -> i).toArray());
        assertArrayEquals(new int[]{3, 5}, Chunk.getChildrenFromStrideTree(nodes, 1, k).stream().mapToInt(i -> i).toArray());
        assertArrayEquals(new int[]{4, 6}, Chunk.getChildrenFromStrideTree(nodes, 2, k).stream().mapToInt(i -> i).toArray());
        assertArrayEquals(new int[]{7, 9}, Chunk.getChildrenFromStrideTree(nodes, 3, k).stream().mapToInt(i -> i).toArray());
        assertArrayEquals(new int[]{8}, Chunk.getChildrenFromStrideTree(nodes, 4, k).stream().mapToInt(i -> i).toArray());
        assertNull(Chunk.getChildrenFromStrideTree(nodes, 5, k));
        assertNull(Chunk.getChildrenFromStrideTree(nodes, 6, k));
    }

    @Test
    public void testGetChildrenFromStrideTree_fullCoverageNoDuplicates() {
        int[][] cases = {{10, 2}, {24, 3}, {50, 4}, {17, 2}, {1, 2}};
        for (int[] c : cases) {
            int n = c[0];
            int k = c[1];
            List<Integer> nodes = createNodes(n);
            Set<Integer> visited = new HashSet<>();
            visited.add(0);
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(0);
            while (!queue.isEmpty()) {
                int cur = queue.poll();
                List<Integer> children = Chunk.getChildrenFromStrideTree(nodes, cur, k);
                if (children == null) {
                    continue;
                }
                for (int ch : children) {
                    assertTrue(visited.add(ch), "n=" + n + " k=" + k + ": node " + ch + " reached more than once");
                    queue.add(ch);
                }
            }
            assertEquals(n, visited.size(), "n=" + n + " k=" + k + ": not every node was reached");
        }
    }

    // --- weightedShuffle ---

    @Test
    public void testWeightedShuffleDeterminism() {
        List<Node> nodes = createStakedNodes(new float[]{0.5f, 0.3f, 0f, 0.15f, 0f, 0.05f});
        List<Node> a = Chunk.weightedShuffle(new ArrayList<>(nodes), new Random(42));
        List<Node> b = Chunk.weightedShuffle(new ArrayList<>(nodes), new Random(42));
        assertEquals(a, b);
    }

    @Test
    public void testWeightedShuffleZeroStakeAlwaysLast() {
        List<Node> nodes = createStakedNodes(new float[]{0.4f, 0f, 0.2f, 0f, 0.1f, 0f});
        for (int seed = 0; seed < 200; seed++) {
            List<Node> shuffled = Chunk.weightedShuffle(new ArrayList<>(nodes), new Random(seed));
            int lastStakedIdx = -1;
            int firstUnstakedIdx = Integer.MAX_VALUE;
            for (int i = 0; i < shuffled.size(); i++) {
                Node n = shuffled.get(i);
                if (n.getStakeProportion() > 0) {
                    lastStakedIdx = Math.max(lastStakedIdx, i);
                } else {
                    firstUnstakedIdx = Math.min(firstUnstakedIdx, i);
                }
            }
            assertTrue(lastStakedIdx < firstUnstakedIdx, "seed=" + seed + ": an unstaked node sorted before a staked one");
        }
    }

    @Test
    public void testWeightedShuffleAllZeroDegradesToUniformShuffle() {
        List<Node> nodes = createStakedNodes(new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f});
        for (int seed = 0; seed < 50; seed++) {
            List<Node> expected = new ArrayList<>(nodes);
            Collections.shuffle(expected, new Random(seed));
            List<Node> actual = Chunk.weightedShuffle(new ArrayList<>(nodes), new Random(seed));
            assertEquals(expected, actual, "seed=" + seed);
        }
    }

    @Test
    public void testWeightedShuffleStakeBiasStatistical() {
        List<Node> nodes = createStakedNodes(new float[]{0.7f, 0.1f, 0.1f, 0.1f});
        Node highStake = nodes.get(0);
        int trials = 5000;
        int firstCount = 0;
        Random trialSeedSource = new Random(1);
        for (int t = 0; t < trials; t++) {
            List<Node> shuffled = Chunk.weightedShuffle(new ArrayList<>(nodes), new Random(trialSeedSource.nextLong()));
            if (shuffled.get(0) == highStake) {
                firstCount++;
            }
        }
        double freq = firstCount / (double) trials;
        // Uniform baseline would be 1/4 = 0.25; a 0.7-weighted node should land first close to
        // 70% of the time. Assert well above baseline with a generous margin to avoid flakiness.
        assertTrue(freq > 0.5, "high-stake node landed first only " + freq + " of trials, expected well above the 0.25 uniform baseline");
    }

}
