package simblock.graph;

/**
 * Distance to a node
 */
public class NodeDistance<T> implements Comparable<NodeDistance<T>> {
    public T node;
    public double dist;

    public NodeDistance(T node, double dist) {
        this.node = node;
        this.dist = dist;
    }

    @Override 
    public int compareTo(NodeDistance<T> other) {
        if (this.dist < other.dist) {
            return -1;
        } else if (this.dist > other.dist) {
            return 1;
        } else {
            return 0;
        }
    }
}