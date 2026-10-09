package simblock.node;

public class Link {

    private Node from;
    private Node to;

    public Link(Node from, Node to) {
        this.from = from;
        this.to = to;
    }

    public Node getFrom() {
        return from;
    }

    public Node getTo() {
        return to;
    }

    @Override
    public String toString() {
        return String.format("Link(%s, %s)", from, to);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Link)) {
            return false;
        }
        return this.from == ((Link) other).from && this.to == ((Link) other).to;
    }
}
