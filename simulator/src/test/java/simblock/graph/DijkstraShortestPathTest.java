package simblock.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ejml.data.DMatrixSparseCSC;
import org.junit.jupiter.api.Test;

import simblock.graph.DijkstraShortestPath.Path;

public class DijkstraShortestPathTest {

    @Test
    public void testComputeShortestPathDirected() {
        //      1        2        3
        // A ------> B -----> D -----> E
        //  \         \       ^       ^
        //   \4        \6     |3      |1
        //    \         v     |       |
        //     ---> C --------+-------+
        Map<String, List<NodeDistance<String>>> edgeDist = new HashMap<>();
        List<NodeDistance<String>> edgeDistFromA = new ArrayList<>();
        edgeDistFromA.add(new NodeDistance<String>("B", 1));
        edgeDistFromA.add(new NodeDistance<String>("C", 4));
        edgeDistFromA.add(new NodeDistance<String>("D", Double.MAX_VALUE));
        edgeDistFromA.add(new NodeDistance<String>("E", Double.MAX_VALUE));
        List<NodeDistance<String>> edgeDistFromB = new ArrayList<>();
        edgeDistFromB.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromB.add(new NodeDistance<String>("C", 6));
        edgeDistFromB.add(new NodeDistance<String>("D", 2));
        edgeDistFromB.add(new NodeDistance<String>("E", Double.MAX_VALUE));
        List<NodeDistance<String>> edgeDistFromC = new ArrayList<>();
        edgeDistFromC.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromC.add(new NodeDistance<String>("B", Double.MAX_VALUE));
        edgeDistFromC.add(new NodeDistance<String>("D", 3));
        edgeDistFromC.add(new NodeDistance<String>("E", 1));
        List<NodeDistance<String>> edgeDistFromD = new ArrayList<>();
        edgeDistFromD.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromD.add(new NodeDistance<String>("B", Double.MAX_VALUE));
        edgeDistFromD.add(new NodeDistance<String>("C", Double.MAX_VALUE));
        edgeDistFromD.add(new NodeDistance<String>("E", 3));
        List<NodeDistance<String>> edgeDistFromE = new ArrayList<>();
        edgeDistFromE.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromE.add(new NodeDistance<String>("B", Double.MAX_VALUE));
        edgeDistFromE.add(new NodeDistance<String>("C", Double.MAX_VALUE));
        edgeDistFromE.add(new NodeDistance<String>("D", Double.MAX_VALUE));
        edgeDist.put("A", edgeDistFromA);
        edgeDist.put("B", edgeDistFromB);
        edgeDist.put("C", edgeDistFromC);
        edgeDist.put("D", edgeDistFromD);
        edgeDist.put("E", edgeDistFromE);

        Map<String, Path<String>> distFromA = DijkstraShortestPath.computeShortestPaths("A", edgeDist, null);
        assertEquals(0, distFromA.get("A").length);
        assertEquals("", distFromA.get("A").printPath());
        assertEquals(1, distFromA.get("B").length);
        assertEquals("->B", distFromA.get("B").printPath());
        assertEquals(4, distFromA.get("C").length);
        assertEquals("->C", distFromA.get("C").printPath());
        assertEquals(3, distFromA.get("D").length);
        assertEquals("->B->D", distFromA.get("D").printPath());
        assertEquals(5, distFromA.get("E").length);
        assertEquals("->C->E", distFromA.get("E").printPath());

        Map<String, Path<String>> distFromB = DijkstraShortestPath.computeShortestPaths("B", edgeDist, null);
        assertEquals(Double.MAX_VALUE, distFromB.get("A").length);
        assertEquals(Path.UNREACHABLE, distFromB.get("A").printPath());
        assertEquals(0, distFromB.get("B").length);
        assertEquals("", distFromB.get("B").printPath());
        assertEquals(6, distFromB.get("C").length);
        assertEquals("->C", distFromB.get("C").printPath());
        assertEquals(2, distFromB.get("D").length);
        assertEquals("->D", distFromB.get("D").printPath());
        assertEquals(5, distFromB.get("E").length);
        assertEquals("->D->E", distFromB.get("E").printPath());

        Map<String, Path<String>> distFromC = DijkstraShortestPath.computeShortestPaths("C", edgeDist, null);
        assertEquals(Double.MAX_VALUE, distFromC.get("A").length);
        assertEquals(Path.UNREACHABLE, distFromC.get("A").printPath());
        assertEquals(Double.MAX_VALUE, distFromC.get("B").length);
        assertEquals(Path.UNREACHABLE, distFromC.get("B").printPath());
        assertEquals(0, distFromC.get("C").length);
        assertEquals("", distFromC.get("C").printPath());
        assertEquals(3, distFromC.get("D").length);
        assertEquals("->D", distFromC.get("D").printPath());
        assertEquals(1, distFromC.get("E").length);
        assertEquals("->E", distFromC.get("E").printPath());

        Map<String, Path<String>> distFromD = DijkstraShortestPath.computeShortestPaths("D", edgeDist, null);
        assertEquals(Double.MAX_VALUE, distFromD.get("A").length);
        assertEquals(Path.UNREACHABLE, distFromD.get("A").printPath());
        assertEquals(Double.MAX_VALUE, distFromD.get("B").length);
        assertEquals(Path.UNREACHABLE, distFromD.get("B").printPath());
        assertEquals(Double.MAX_VALUE, distFromD.get("C").length);
        assertEquals(Path.UNREACHABLE, distFromD.get("C").printPath());
        assertEquals(0, distFromD.get("D").length);
        assertEquals("", distFromD.get("D").printPath());
        assertEquals(3, distFromD.get("E").length);
        assertEquals("->E", distFromD.get("E").printPath());

        Map<String, Path<String>> distFromE = DijkstraShortestPath.computeShortestPaths("E", edgeDist, null);
        assertEquals(Double.MAX_VALUE, distFromE.get("A").length);
        assertEquals(Path.UNREACHABLE, distFromE.get("A").printPath());
        assertEquals(Double.MAX_VALUE, distFromE.get("B").length);
        assertEquals(Path.UNREACHABLE, distFromE.get("B").printPath());
        assertEquals(Double.MAX_VALUE, distFromE.get("C").length);
        assertEquals(Path.UNREACHABLE, distFromE.get("C").printPath());
        assertEquals(Double.MAX_VALUE, distFromE.get("D").length);
        assertEquals(Path.UNREACHABLE, distFromE.get("D").printPath());
        assertEquals(0, distFromE.get("E").length);
        assertEquals("", distFromE.get("E").printPath());
    }

    @Test
    public void testComputeShortestPathDirectedWMatrix() {
        //      1        2        3
        // A ------> B -----> D -----> E
        //  \         \       ^       ^
        //   \4        \6     |3      |1
        //    \         v     |       |
        //     ---> C --------+-------+
        DMatrixSparseCSC distMatrix = new DMatrixSparseCSC(5, 5);
        distMatrix.set(0, 1, 1);
        distMatrix.set(0, 2, 4);
        distMatrix.set(1, 2, 6);
        distMatrix.set(1, 3, 2);
        distMatrix.set(2, 3, 3);
        distMatrix.set(2, 4, 1);
        distMatrix.set(3, 4, 3);

        List<String> allNodes = Arrays.asList("A", "B", "C", "D", "E");
        Map<String, Path<String>> distFromA = DijkstraShortestPath.computeShortestPaths("A", distMatrix, allNodes, null);
        assertEquals(0, distFromA.get("A").length);
        assertEquals("", distFromA.get("A").printPath());
        assertEquals(1, distFromA.get("B").length);
        assertEquals("->B", distFromA.get("B").printPath());
        assertEquals(4, distFromA.get("C").length);
        assertEquals("->C", distFromA.get("C").printPath());
        assertEquals(3, distFromA.get("D").length);
        assertEquals("->B->D", distFromA.get("D").printPath());
        assertEquals(5, distFromA.get("E").length);
        assertEquals("->C->E", distFromA.get("E").printPath());

        Map<String, Path<String>> distFromB = DijkstraShortestPath.computeShortestPaths("B", distMatrix, allNodes, null);
        assertEquals(Double.MAX_VALUE, distFromB.get("A").length);
        assertEquals(Path.UNREACHABLE, distFromB.get("A").printPath());
        assertEquals(0, distFromB.get("B").length);
        assertEquals("", distFromB.get("B").printPath());
        assertEquals(6, distFromB.get("C").length);
        assertEquals("->C", distFromB.get("C").printPath());
        assertEquals(2, distFromB.get("D").length);
        assertEquals("->D", distFromB.get("D").printPath());
        assertEquals(5, distFromB.get("E").length);
        assertEquals("->D->E", distFromB.get("E").printPath());

        Map<String, Path<String>> distFromC = DijkstraShortestPath.computeShortestPaths("C", distMatrix, allNodes, null);
        assertEquals(Double.MAX_VALUE, distFromC.get("A").length);
        assertEquals(Path.UNREACHABLE, distFromC.get("A").printPath());
        assertEquals(Double.MAX_VALUE, distFromC.get("B").length);
        assertEquals(Path.UNREACHABLE, distFromC.get("B").printPath());
        assertEquals(0, distFromC.get("C").length);
        assertEquals("", distFromC.get("C").printPath());
        assertEquals(3, distFromC.get("D").length);
        assertEquals("->D", distFromC.get("D").printPath());
        assertEquals(1, distFromC.get("E").length);
        assertEquals("->E", distFromC.get("E").printPath());

        Map<String, Path<String>> distFromD = DijkstraShortestPath.computeShortestPaths("D", distMatrix, allNodes, null);
        assertEquals(Double.MAX_VALUE, distFromD.get("A").length);
        assertEquals(Path.UNREACHABLE, distFromD.get("A").printPath());
        assertEquals(Double.MAX_VALUE, distFromD.get("B").length);
        assertEquals(Path.UNREACHABLE, distFromD.get("B").printPath());
        assertEquals(Double.MAX_VALUE, distFromD.get("C").length);
        assertEquals(Path.UNREACHABLE, distFromD.get("C").printPath());
        assertEquals(0, distFromD.get("D").length);
        assertEquals("", distFromD.get("D").printPath());
        assertEquals(3, distFromD.get("E").length);
        assertEquals("->E", distFromD.get("E").printPath());

        Map<String, Path<String>> distFromE = DijkstraShortestPath.computeShortestPaths("E", distMatrix, allNodes, null);
        assertEquals(Double.MAX_VALUE, distFromE.get("A").length);
        assertEquals(Path.UNREACHABLE, distFromE.get("A").printPath());
        assertEquals(Double.MAX_VALUE, distFromE.get("B").length);
        assertEquals(Path.UNREACHABLE, distFromE.get("B").printPath());
        assertEquals(Double.MAX_VALUE, distFromE.get("C").length);
        assertEquals(Path.UNREACHABLE, distFromE.get("C").printPath());
        assertEquals(Double.MAX_VALUE, distFromE.get("D").length);
        assertEquals(Path.UNREACHABLE, distFromE.get("D").printPath());
        assertEquals(0, distFromE.get("E").length);
        assertEquals("", distFromE.get("E").printPath());
    }

    @Test
    public void testComputeShortestPathUndirected() {
        //          F ------+--------+
        //          | 2     | 1      | 4
        //      1   |    2  |    3   |
        // A ------ B ----- D ------ E
        //  \        \      |        |
        //   \4       \6    |3       |1
        //    \        \    |        |
        //     --- C -------+--------+
        Map<String, List<NodeDistance<String>>> edgeDist = new HashMap<>();
        List<NodeDistance<String>> edgeDistFromA = new ArrayList<>();
        edgeDistFromA.add(new NodeDistance<String>("A", 0));
        edgeDistFromA.add(new NodeDistance<String>("B", 1));
        edgeDistFromA.add(new NodeDistance<String>("C", 4));
        edgeDistFromA.add(new NodeDistance<String>("D", Double.MAX_VALUE));
        edgeDistFromA.add(new NodeDistance<String>("E", Double.MAX_VALUE));
        edgeDistFromA.add(new NodeDistance<String>("F", Double.MAX_VALUE));
        List<NodeDistance<String>> edgeDistFromB = new ArrayList<>();
        edgeDistFromB.add(new NodeDistance<String>("A", 1));
        edgeDistFromB.add(new NodeDistance<String>("B", 0));
        edgeDistFromB.add(new NodeDistance<String>("C", 6));
        edgeDistFromB.add(new NodeDistance<String>("D", 2));
        edgeDistFromB.add(new NodeDistance<String>("E", Double.MAX_VALUE));
        edgeDistFromB.add(new NodeDistance<String>("F", 2));
        List<NodeDistance<String>> edgeDistFromC = new ArrayList<>();
        edgeDistFromC.add(new NodeDistance<String>("A", 4));
        edgeDistFromC.add(new NodeDistance<String>("B", 6));
        edgeDistFromC.add(new NodeDistance<String>("C", 0));
        edgeDistFromC.add(new NodeDistance<String>("D", 3));
        edgeDistFromC.add(new NodeDistance<String>("E", 1));
        edgeDistFromB.add(new NodeDistance<String>("F", Double.MAX_VALUE));
        List<NodeDistance<String>> edgeDistFromD = new ArrayList<>();
        edgeDistFromD.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromD.add(new NodeDistance<String>("B", 2));
        edgeDistFromD.add(new NodeDistance<String>("C", 3));
        edgeDistFromD.add(new NodeDistance<String>("D", 0));
        edgeDistFromD.add(new NodeDistance<String>("E", 3));
        edgeDistFromD.add(new NodeDistance<String>("F", 1));
        List<NodeDistance<String>> edgeDistFromE = new ArrayList<>();
        edgeDistFromE.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromE.add(new NodeDistance<String>("B", Double.MAX_VALUE));
        edgeDistFromE.add(new NodeDistance<String>("C", 1));
        edgeDistFromE.add(new NodeDistance<String>("D", 3));
        edgeDistFromE.add(new NodeDistance<String>("E", 0));
        edgeDistFromE.add(new NodeDistance<String>("F", 4));
        List<NodeDistance<String>> edgeDistFromF = new ArrayList<>();
        edgeDistFromF.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromF.add(new NodeDistance<String>("B", 2));
        edgeDistFromF.add(new NodeDistance<String>("C", Double.MAX_VALUE));
        edgeDistFromF.add(new NodeDistance<String>("D", 1));
        edgeDistFromF.add(new NodeDistance<String>("E", 4));
        edgeDistFromF.add(new NodeDistance<String>("F", 0));
        edgeDist.put("A", edgeDistFromA);
        edgeDist.put("B", edgeDistFromB);
        edgeDist.put("C", edgeDistFromC);
        edgeDist.put("D", edgeDistFromD);
        edgeDist.put("E", edgeDistFromE);
        edgeDist.put("F", edgeDistFromF);

        Map<String, Path<String>> distFromA = DijkstraShortestPath.computeShortestPaths("A", edgeDist, null);
        assertEquals(0, distFromA.get("A").length);
        assertEquals("", distFromA.get("A").printPath());
        assertEquals(1, distFromA.get("B").length);
        assertEquals("->B", distFromA.get("B").printPath());
        assertEquals(4, distFromA.get("C").length);
        assertEquals("->C", distFromA.get("C").printPath());
        assertEquals(3, distFromA.get("D").length);
        assertEquals("->B->D", distFromA.get("D").printPath());
        assertEquals(5, distFromA.get("E").length);
        assertEquals("->C->E", distFromA.get("E").printPath());
        assertEquals(3, distFromA.get("F").length);
        assertEquals("->B->F", distFromA.get("F").printPath());

        Map<String, Path<String>> distFromB = DijkstraShortestPath.computeShortestPaths("B", edgeDist, null);
        assertEquals(1, distFromB.get("A").length);
        assertEquals("->A", distFromB.get("A").printPath());
        assertEquals(0, distFromB.get("B").length);
        assertEquals("", distFromB.get("B").printPath());
        assertEquals(5, distFromB.get("C").length);
        assertEquals("->D->C|->A->C", distFromB.get("C").printPath());
        assertEquals(2, distFromB.get("D").length);
        assertEquals("->D", distFromB.get("D").printPath());
        assertEquals(5, distFromB.get("E").length);
        assertEquals("->D->E", distFromB.get("E").printPath());
        assertEquals(2, distFromB.get("F").length);
        assertEquals("->F", distFromB.get("F").printPath());

        Map<String, Path<String>> distFromC = DijkstraShortestPath.computeShortestPaths("C", edgeDist, null);
        assertEquals(4, distFromC.get("A").length);
        assertEquals("->A", distFromC.get("A").printPath());
        assertEquals(5, distFromC.get("B").length);
        assertEquals("->A->B|->D->B", distFromC.get("B").printPath());
        assertEquals(0, distFromC.get("C").length);
        assertEquals("", distFromC.get("C").printPath());
        assertEquals(3, distFromC.get("D").length);
        assertEquals("->D", distFromC.get("D").printPath());
        assertEquals(1, distFromC.get("E").length);
        assertEquals("->E", distFromC.get("E").printPath());
        assertEquals(4, distFromC.get("F").length);
        assertEquals("->D->F", distFromC.get("F").printPath());

        Map<String, Path<String>> distFromD = DijkstraShortestPath.computeShortestPaths("D", edgeDist, null);
        assertEquals(3, distFromD.get("A").length);
        assertEquals("->B->A", distFromD.get("A").printPath());
        assertEquals(2, distFromD.get("B").length);
        assertEquals("->B", distFromD.get("B").printPath());
        assertEquals(3, distFromD.get("C").length);
        assertEquals("->C", distFromD.get("C").printPath());
        assertEquals(0, distFromD.get("D").length);
        assertEquals("", distFromD.get("D").printPath());
        assertEquals(3, distFromD.get("E").length);
        assertEquals("->E", distFromD.get("E").printPath());
        assertEquals(1, distFromD.get("F").length);
        assertEquals("->F", distFromD.get("F").printPath());

        Map<String, Path<String>> distFromE = DijkstraShortestPath.computeShortestPaths("E", edgeDist, null);
        assertEquals(5, distFromE.get("A").length);
        assertEquals("->C->A", distFromE.get("A").printPath());
        assertEquals(5, distFromE.get("B").length);
        assertEquals("->D->B", distFromE.get("B").printPath());
        assertEquals(1, distFromE.get("C").length);
        assertEquals("->C", distFromE.get("C").printPath());
        assertEquals(3, distFromE.get("D").length);
        assertEquals("->D", distFromE.get("D").printPath());
        assertEquals(0, distFromE.get("E").length);
        assertEquals("", distFromE.get("E").printPath());
        assertEquals(4, distFromE.get("F").length);
        assertEquals("->D->F|->F", distFromE.get("F").printPath());

        Map<String, Path<String>> distFromF = DijkstraShortestPath.computeShortestPaths("F", edgeDist, null);
        assertEquals(3, distFromF.get("A").length);
        assertEquals("->B->A", distFromF.get("A").printPath());
        assertEquals(2, distFromF.get("B").length);
        assertEquals("->B", distFromF.get("B").printPath());
        assertEquals(4, distFromF.get("C").length);
        assertEquals("->D->C", distFromF.get("C").printPath());
        assertEquals(1, distFromF.get("D").length);
        assertEquals("->D", distFromF.get("D").printPath());
        assertEquals(4, distFromF.get("E").length);
        assertEquals("->D->E|->E", distFromF.get("E").printPath());
        assertEquals(0, distFromF.get("F").length);
        assertEquals("", distFromF.get("F").printPath());
    }

    @Test
    public void testComputeShortestPathUndirectedWMatrix() {
        //          F ------+--------+
        //          | 2     | 1      | 4
        //      1   |    2  |    3   |
        // A ------ B ----- D ------ E
        //  \        \      |        |
        //   \4       \6    |3       |1
        //    \        \    |        |
        //     --- C -------+--------+
        DMatrixSparseCSC distMatrix = new DMatrixSparseCSC(6, 6);
        distMatrix.set(0, 1, 1);
        distMatrix.set(0, 2, 4);
        distMatrix.set(1, 0, 1);
        distMatrix.set(1, 2, 6);
        distMatrix.set(1, 3, 2);
        distMatrix.set(1, 5, 2);
        distMatrix.set(2, 0, 4);
        distMatrix.set(2, 1, 6);
        distMatrix.set(2, 3, 3);
        distMatrix.set(2, 4, 1);
        distMatrix.set(3, 1, 2);
        distMatrix.set(3, 2, 3);
        distMatrix.set(3, 4, 3);
        distMatrix.set(3, 5, 1);
        distMatrix.set(4, 2, 1);
        distMatrix.set(4, 3, 3);
        distMatrix.set(4, 5, 4);
        distMatrix.set(5, 1, 2);
        distMatrix.set(5, 3, 1);
        distMatrix.set(5, 4, 4);
        List<String> allNodes = Arrays.asList("A", "B", "C", "D", "E", "F");

        Map<String, Path<String>> distFromA = DijkstraShortestPath.computeShortestPaths("A", distMatrix, allNodes, null);
        assertEquals(0, distFromA.get("A").length);
        assertEquals("", distFromA.get("A").printPath());
        assertEquals(1, distFromA.get("B").length);
        assertEquals("->B", distFromA.get("B").printPath());
        assertEquals(4, distFromA.get("C").length);
        assertEquals("->C", distFromA.get("C").printPath());
        assertEquals(3, distFromA.get("D").length);
        assertEquals("->B->D", distFromA.get("D").printPath());
        assertEquals(5, distFromA.get("E").length);
        assertEquals("->C->E", distFromA.get("E").printPath());
        assertEquals(3, distFromA.get("F").length);
        assertEquals("->B->F", distFromA.get("F").printPath());

        Map<String, Path<String>> distFromB = DijkstraShortestPath.computeShortestPaths("B", distMatrix, allNodes, null);
        assertEquals(1, distFromB.get("A").length);
        assertEquals("->A", distFromB.get("A").printPath());
        assertEquals(0, distFromB.get("B").length);
        assertEquals("", distFromB.get("B").printPath());
        assertEquals(5, distFromB.get("C").length);
        assertEquals("->D->C|->A->C", distFromB.get("C").printPath());
        assertEquals(2, distFromB.get("D").length);
        assertEquals("->D", distFromB.get("D").printPath());
        assertEquals(5, distFromB.get("E").length);
        assertEquals("->D->E", distFromB.get("E").printPath());
        assertEquals(2, distFromB.get("F").length);
        assertEquals("->F", distFromB.get("F").printPath());

        Map<String, Path<String>> distFromC = DijkstraShortestPath.computeShortestPaths("C", distMatrix, allNodes, null);
        assertEquals(4, distFromC.get("A").length);
        assertEquals("->A", distFromC.get("A").printPath());
        assertEquals(5, distFromC.get("B").length);
        assertEquals("->A->B|->D->B", distFromC.get("B").printPath());
        assertEquals(0, distFromC.get("C").length);
        assertEquals("", distFromC.get("C").printPath());
        assertEquals(3, distFromC.get("D").length);
        assertEquals("->D", distFromC.get("D").printPath());
        assertEquals(1, distFromC.get("E").length);
        assertEquals("->E", distFromC.get("E").printPath());
        assertEquals(4, distFromC.get("F").length);
        assertEquals("->D->F", distFromC.get("F").printPath());

        Map<String, Path<String>> distFromD = DijkstraShortestPath.computeShortestPaths("D", distMatrix, allNodes, null);
        assertEquals(3, distFromD.get("A").length);
        assertEquals("->B->A", distFromD.get("A").printPath());
        assertEquals(2, distFromD.get("B").length);
        assertEquals("->B", distFromD.get("B").printPath());
        assertEquals(3, distFromD.get("C").length);
        assertEquals("->C", distFromD.get("C").printPath());
        assertEquals(0, distFromD.get("D").length);
        assertEquals("", distFromD.get("D").printPath());
        assertEquals(3, distFromD.get("E").length);
        assertEquals("->E", distFromD.get("E").printPath());
        assertEquals(1, distFromD.get("F").length);
        assertEquals("->F", distFromD.get("F").printPath());

        Map<String, Path<String>> distFromE = DijkstraShortestPath.computeShortestPaths("E", distMatrix, allNodes, null);
        assertEquals(5, distFromE.get("A").length);
        assertEquals("->C->A", distFromE.get("A").printPath());
        assertEquals(5, distFromE.get("B").length);
        assertEquals("->D->B", distFromE.get("B").printPath());
        assertEquals(1, distFromE.get("C").length);
        assertEquals("->C", distFromE.get("C").printPath());
        assertEquals(3, distFromE.get("D").length);
        assertEquals("->D", distFromE.get("D").printPath());
        assertEquals(0, distFromE.get("E").length);
        assertEquals("", distFromE.get("E").printPath());
        assertEquals(4, distFromE.get("F").length);
        assertEquals("->D->F|->F", distFromE.get("F").printPath());

        Map<String, Path<String>> distFromF = DijkstraShortestPath.computeShortestPaths("F", distMatrix, allNodes, null);
        assertEquals(3, distFromF.get("A").length);
        assertEquals("->B->A", distFromF.get("A").printPath());
        assertEquals(2, distFromF.get("B").length);
        assertEquals("->B", distFromF.get("B").printPath());
        assertEquals(4, distFromF.get("C").length);
        assertEquals("->D->C", distFromF.get("C").printPath());
        assertEquals(1, distFromF.get("D").length);
        assertEquals("->D", distFromF.get("D").printPath());
        assertEquals(4, distFromF.get("E").length);
        assertEquals("->D->E|->E", distFromF.get("E").printPath());
        assertEquals(0, distFromF.get("F").length);
        assertEquals("", distFromF.get("F").printPath());
    }
    
    @Test
    public void testComputeShortestPathUndirectedWithUnreached() {
        //          F ------+--------+
        //          | 2     | 1      | 4
        //      1   |    2  |    3   |
        // A ------ B ----- D ------ E
        //  \        \      |        |
        //   \4       \6    |3       |1
        //    \        \    |        |
        //     --- C -------+--------+
        Map<String, List<NodeDistance<String>>> edgeDist = new HashMap<>();
        List<NodeDistance<String>> edgeDistFromA = new ArrayList<>();
        edgeDistFromA.add(new NodeDistance<String>("A", 0));
        edgeDistFromA.add(new NodeDistance<String>("B", 1));
        edgeDistFromA.add(new NodeDistance<String>("C", 4));
        edgeDistFromA.add(new NodeDistance<String>("D", Double.MAX_VALUE));
        edgeDistFromA.add(new NodeDistance<String>("E", Double.MAX_VALUE));
        edgeDistFromA.add(new NodeDistance<String>("F", Double.MAX_VALUE));
        List<NodeDistance<String>> edgeDistFromB = new ArrayList<>();
        edgeDistFromB.add(new NodeDistance<String>("A", 1));
        edgeDistFromB.add(new NodeDistance<String>("B", 0));
        edgeDistFromB.add(new NodeDistance<String>("C", 6));
        edgeDistFromB.add(new NodeDistance<String>("D", 2));
        edgeDistFromB.add(new NodeDistance<String>("E", Double.MAX_VALUE));
        edgeDistFromB.add(new NodeDistance<String>("F", 2));
        List<NodeDistance<String>> edgeDistFromC = new ArrayList<>();
        edgeDistFromC.add(new NodeDistance<String>("A", 4));
        edgeDistFromC.add(new NodeDistance<String>("B", 6));
        edgeDistFromC.add(new NodeDistance<String>("C", 0));
        edgeDistFromC.add(new NodeDistance<String>("D", 3));
        edgeDistFromC.add(new NodeDistance<String>("E", 1));
        edgeDistFromB.add(new NodeDistance<String>("F", Double.MAX_VALUE));
        List<NodeDistance<String>> edgeDistFromD = new ArrayList<>();
        edgeDistFromD.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromD.add(new NodeDistance<String>("B", 2));
        edgeDistFromD.add(new NodeDistance<String>("C", 3));
        edgeDistFromD.add(new NodeDistance<String>("D", 0));
        edgeDistFromD.add(new NodeDistance<String>("E", 3));
        edgeDistFromD.add(new NodeDistance<String>("F", 1));
        List<NodeDistance<String>> edgeDistFromE = new ArrayList<>();
        edgeDistFromE.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromE.add(new NodeDistance<String>("B", Double.MAX_VALUE));
        edgeDistFromE.add(new NodeDistance<String>("C", 1));
        edgeDistFromE.add(new NodeDistance<String>("D", 3));
        edgeDistFromE.add(new NodeDistance<String>("E", 0));
        edgeDistFromE.add(new NodeDistance<String>("F", 4));
        List<NodeDistance<String>> edgeDistFromF = new ArrayList<>();
        edgeDistFromF.add(new NodeDistance<String>("A", Double.MAX_VALUE));
        edgeDistFromF.add(new NodeDistance<String>("B", 2));
        edgeDistFromF.add(new NodeDistance<String>("C", Double.MAX_VALUE));
        edgeDistFromF.add(new NodeDistance<String>("D", 1));
        edgeDistFromF.add(new NodeDistance<String>("E", 4));
        edgeDistFromF.add(new NodeDistance<String>("F", 0));
        edgeDist.put("A", edgeDistFromA);
        edgeDist.put("B", edgeDistFromB);
        edgeDist.put("C", edgeDistFromC);
        edgeDist.put("D", edgeDistFromD);
        edgeDist.put("E", edgeDistFromE);
        edgeDist.put("F", edgeDistFromF);

        Map<String, Path<String>> distFromA = DijkstraShortestPath.computeShortestPaths("A", edgeDist,
                new HashSet<String>(Arrays.asList("D")));
        assertEquals(0, distFromA.get("A").length);
        assertEquals(1, distFromA.get("B").length);
        assertEquals(4, distFromA.get("C").length);
        assertEquals(5, distFromA.get("E").length);
        assertEquals(3, distFromA.get("F").length);

        Map<String, Path<String>> distFromB = DijkstraShortestPath.computeShortestPaths("B", edgeDist, new HashSet<String>(Arrays.asList("C")));
        assertEquals(1, distFromB.get("A").length);
        assertEquals(0, distFromB.get("B").length);
        assertEquals(2, distFromB.get("D").length);
        assertEquals(5, distFromB.get("E").length);
        assertEquals(2, distFromB.get("F").length);

        Map<String, Path<String>> distFromC = DijkstraShortestPath.computeShortestPaths("C", edgeDist, new HashSet<>(Arrays.asList("D")));
        assertEquals(4, distFromC.get("A").length);
        assertEquals(5, distFromC.get("B").length);
        assertEquals(0, distFromC.get("C").length);
        assertEquals(1, distFromC.get("E").length);
        assertEquals(5, distFromC.get("F").length);

        Map<String, Path<String>> distFromD = DijkstraShortestPath.computeShortestPaths("D", edgeDist, new HashSet<>(Arrays.asList("E")));
        assertEquals(3, distFromD.get("A").length);
        assertEquals(2, distFromD.get("B").length);
        assertEquals(3, distFromD.get("C").length);
        assertEquals(0, distFromD.get("D").length);
        assertEquals(1, distFromD.get("F").length);

        Map<String, Path<String>> distFromE = DijkstraShortestPath.computeShortestPaths("E", edgeDist, new HashSet<>(Arrays.asList("C", "F")));
        assertEquals(6, distFromE.get("A").length);
        assertEquals(5, distFromE.get("B").length);
        assertEquals(3, distFromE.get("D").length);
        assertEquals(0, distFromE.get("E").length);

        Map<String, Path<String>> distFromF = DijkstraShortestPath.computeShortestPaths("F", edgeDist, new HashSet<>(Arrays.asList("A", "D")));
        assertEquals(2, distFromF.get("B").length);
        assertEquals(5, distFromF.get("C").length);
        assertEquals(4, distFromF.get("E").length);
        assertEquals(0, distFromF.get("F").length);
    }

    @Test
    public void testComputeShortestPathUndirectedWithUnreachedWMatrix() {
        //          F ------+--------+
        //          | 2     | 1      | 4
        //      1   |    2  |    3   |
        // A ------ B ----- D ------ E
        //  \        \      |        |
        //   \4       \6    |3       |1
        //    \        \    |        |
        //     --- C -------+--------+
        DMatrixSparseCSC distMatrix = new DMatrixSparseCSC(6, 6);
        distMatrix.set(0, 1, 1);
        distMatrix.set(0, 2, 4);
        distMatrix.set(1, 0, 1);
        distMatrix.set(1, 2, 6);
        distMatrix.set(1, 3, 2);
        distMatrix.set(1, 5, 2);
        distMatrix.set(2, 0, 4);
        distMatrix.set(2, 1, 6);
        distMatrix.set(2, 3, 3);
        distMatrix.set(2, 4, 1);
        distMatrix.set(3, 1, 2);
        distMatrix.set(3, 2, 3);
        distMatrix.set(3, 4, 3);
        distMatrix.set(3, 5, 1);
        distMatrix.set(4, 2, 1);
        distMatrix.set(4, 3, 3);
        distMatrix.set(4, 5, 4);
        distMatrix.set(5, 1, 2);
        distMatrix.set(5, 3, 1);
        distMatrix.set(5, 4, 4);
        List<String> allNodes = Arrays.asList("A", "B", "C", "D", "E", "F");

        Map<String, Path<String>> distFromA = DijkstraShortestPath.computeShortestPaths("A", distMatrix, allNodes,
                new HashSet<String>(Arrays.asList("D")));
        assertEquals(0, distFromA.get("A").length);
        assertEquals(1, distFromA.get("B").length);
        assertEquals(4, distFromA.get("C").length);
        assertEquals(5, distFromA.get("E").length);
        assertEquals(3, distFromA.get("F").length);

        Map<String, Path<String>> distFromB = DijkstraShortestPath.computeShortestPaths("B", distMatrix, allNodes, new HashSet<String>(Arrays.asList("C")));
        assertEquals(1, distFromB.get("A").length);
        assertEquals(0, distFromB.get("B").length);
        assertEquals(2, distFromB.get("D").length);
        assertEquals(5, distFromB.get("E").length);
        assertEquals(2, distFromB.get("F").length);

        Map<String, Path<String>> distFromC = DijkstraShortestPath.computeShortestPaths("C", distMatrix, allNodes, new HashSet<>(Arrays.asList("D")));
        assertEquals(4, distFromC.get("A").length);
        assertEquals(5, distFromC.get("B").length);
        assertEquals(0, distFromC.get("C").length);
        assertEquals(1, distFromC.get("E").length);
        assertEquals(5, distFromC.get("F").length);

        Map<String, Path<String>> distFromD = DijkstraShortestPath.computeShortestPaths("D", distMatrix, allNodes, new HashSet<>(Arrays.asList("E")));
        assertEquals(3, distFromD.get("A").length);
        assertEquals(2, distFromD.get("B").length);
        assertEquals(3, distFromD.get("C").length);
        assertEquals(0, distFromD.get("D").length);
        assertEquals(1, distFromD.get("F").length);

        Map<String, Path<String>> distFromE = DijkstraShortestPath.computeShortestPaths("E", distMatrix, allNodes, new HashSet<>(Arrays.asList("C", "F")));
        assertEquals(6, distFromE.get("A").length);
        assertEquals(5, distFromE.get("B").length);
        assertEquals(3, distFromE.get("D").length);
        assertEquals(0, distFromE.get("E").length);

        Map<String, Path<String>> distFromF = DijkstraShortestPath.computeShortestPaths("F", distMatrix, allNodes, new HashSet<>(Arrays.asList("A", "D")));
        assertEquals(2, distFromF.get("B").length);
        assertEquals(5, distFromF.get("C").length);
        assertEquals(4, distFromF.get("E").length);
        assertEquals(0, distFromF.get("F").length);
    }
}
