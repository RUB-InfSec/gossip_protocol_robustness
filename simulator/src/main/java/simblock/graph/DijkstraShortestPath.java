package simblock.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.ejml.data.DMatrixSparse;


public class DijkstraShortestPath {

    public static class Path<T> {
        double length;
        List<List<T>> paths;    // multiple paths with the same length

        public static final String UNREACHABLE = "Unreachable!!";

        Path() {
            length = 0;
            paths = new ArrayList<>();
        }

        public Path (double l, List<List<T>> p) {
            length = l;
            paths = p;
        }

        public double getLength() {
            return length;
        }

        public boolean containNode(T n) {
            if (paths == null) {
                return false;
            }
            for (List<T> p: paths) {
                if (p.contains(n)) {
                    return true;
                }
            }
            return false;
        }

        public String printPath() {
            if (paths == null) {
                return UNREACHABLE;
            }
            String pathStr = "";
            boolean first = true;
            for (List<T> path: paths) {
                if (!first) {
                    pathStr += "|";
                }
                for (T n: path) {
                    pathStr += "->" + n;
                }
                first = false;
            }
            return pathStr;
        }
    }

    /**
     * Compute the shortest paths from @param src to other nodes
     * @param src
     * @param distMatrix distance matrix
     * @param unreachable unreachable nodes in the network that should be avoided when computing the paths
     * @return 
     */
    public static <T> Map<T, Path<T>> computeShortestPaths(T src, DMatrixSparse distMatrix, List<T> allNodes, Set<T> unreachable)
    {
        Map<T, Path<T>> shortestPaths = new HashMap<>();
        PriorityQueue<NodeDistance<T>> pq = new PriorityQueue<>();
        Set<T> settled;
        if (unreachable != null) {  // see unreachable nodes as settled
            settled = new HashSet<T>(unreachable);
        } else{
            settled = new HashSet<>();
        }

        shortestPaths.put(src, new Path<T>());
        pq.add(new NodeDistance<T>(src, 0));
 
        while (settled.size() != allNodes.size()) {
            if (pq.isEmpty()) {
                break;
            }

            T current = pq.remove().node;
            if (settled.contains(current)) {
                continue;
            }
            settled.add(current);
            // go through all its neighbors
            int currIdx = allNodes.indexOf(current);
            for (int i = 0; i < allNodes.size(); i++) {
                T next = allNodes.get(i);
                if (settled.contains(next)) { // next != current
                    continue;
                }
                double curr2nextLen = distMatrix.get(currIdx, i);
                if (!distMatrix.isAssigned(currIdx, i)) {
                    curr2nextLen = Double.MAX_VALUE;
                }
                double newPathLen2Next = shortestPaths.get(current).length + curr2nextLen;
                if (newPathLen2Next < 0) {   // interger overflow, set to max value
                    newPathLen2Next = Double.MAX_VALUE;
                }
                if (!shortestPaths.containsKey(next)) {
                    List<List<T>> paths = Arrays.asList(Arrays.asList(next));
                    if (newPathLen2Next == Double.MAX_VALUE) {
                        paths = null;
                    }
                    shortestPaths.put(next, new Path<T>(newPathLen2Next, paths));
                } else if (newPathLen2Next <= shortestPaths.get(next).length) {
                    if (newPathLen2Next == Double.MAX_VALUE) {
                        continue;
                    }
                    List<List<T>> paths2Curr = shortestPaths.get(current).paths;
                    List<List<T>> paths2Next = new ArrayList<>();
                    // if current.path==null, then current.length=MAX, then newPathLen=MAX, and then code will not reach here
                    assert paths2Curr != null;
                    for (List<T> path2Curr: paths2Curr) {
                        List<T> path2Next = new ArrayList<>(path2Curr);
                        path2Next.add(next);
                        paths2Next.add(path2Next);
                    }
                    if (newPathLen2Next == shortestPaths.get(next).length) {
                        paths2Next.addAll(shortestPaths.get(next).paths);
                    }
                    shortestPaths.put(next, new Path<T>(newPathLen2Next, paths2Next));
                }
                pq.add(new NodeDistance<T>(next, shortestPaths.get(next).length));
            }
        }
        return shortestPaths;
    }
 
    /**
     * Compute the shortest paths from @param src to other nodes
     * @param src
     * @param edgeDist direct edge distance between any two nodes including the src
     * @param unreachable unreachable nodes in the network that should be avoided when computing the paths
     * @return 
     */
    @Deprecated
    public static <T> Map<T, Path<T>> computeShortestPaths(T src, Map<T, List<NodeDistance<T>>> edgeDist, Set<T> unreachable)
    {
        Map<T, Path<T>> shortestPaths = new HashMap<>();
        PriorityQueue<NodeDistance<T>> pq = new PriorityQueue<>();
        Set<T> settled;
        if (unreachable != null) {  // see unreachable nodes as settled
            settled = new HashSet<T>(unreachable);
        } else{
            settled = new HashSet<>();
        }

        shortestPaths.put(src, new Path<T>());
        pq.add(new NodeDistance<T>(src, 0));
 
        int numAllNodes = edgeDist.size();
        while (settled.size() != numAllNodes) {
            if (pq.isEmpty()) {
                break;
            }

            T current = pq.remove().node;
            if (settled.contains(current)) {
                continue;
            }
            settled.add(current);
            // go through all its neighbors
            for (NodeDistance<T> next : edgeDist.get(current)) {
                if (!settled.contains(next.node)) {
                    // from n -> next, compare with from n-> current + current->next
                    double newPathLen = shortestPaths.get(current).length + next.dist;
                    if (newPathLen < 0) {   // interger overflow, set to max value
                        newPathLen = Double.MAX_VALUE;
                    }
    
                    if (!shortestPaths.containsKey(next.node)) {
                        List<List<T>> paths = Arrays.asList(Arrays.asList(next.node));
                        if (newPathLen == Double.MAX_VALUE) {
                            paths = null;
                        }
                        shortestPaths.put(next.node, new Path<T>(newPathLen, paths));
                    } else if (newPathLen <= shortestPaths.get(next.node).length) {
                        if (newPathLen == Double.MAX_VALUE) {
                            continue;
                        }
                        List<List<T>> paths2Curr = shortestPaths.get(current).paths;
                        List<List<T>> paths2Next = new ArrayList<>();
                        // if current.path==null, then current.length=MAX, then newPathLen=MAX, and then code will not reach here
                        assert paths2Curr != null;
                        for (List<T> path2Curr: paths2Curr) {
                            List<T> path2Next = new ArrayList<>(path2Curr);
                            path2Next.add(next.node);
                            paths2Next.add(path2Next);
                        }
                        if (newPathLen == shortestPaths.get(next.node).length) {
                            paths2Next.addAll(shortestPaths.get(next.node).paths);
                        }
                        shortestPaths.put(next.node, new Path<T>(newPathLen, paths2Next));
                    }
                    pq.add(new NodeDistance<T>(next.node, shortestPaths.get(next.node).length));
                }
            }
        }
        return shortestPaths;
    }
}
