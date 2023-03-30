package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;


import tools.Vector2d;

/**
 * TODO: Add estimatedCost attribute for A* algorithm
 */
public class Node implements Comparable<Node> {
    public Vector2d position;
    public int cost;
    public Node parent;

    ////////// Constructors
    public Node(Vector2d pos) {
        init(pos);
        this.parent = null;
    }
    public Node(Vector2d pos, Node parent) {
        init(pos);
        this.parent = parent;
    }
    private void init(Vector2d pos) {
        // TODO: Check if this is necessary, could consume precious time
        this.position = pos.copy();
        this.cost = 0;
    }
    ///////////

    @Override
    public int compareTo(Node other) {
        if (this.cost < other.cost)
            return -1;
        if (this.cost > other.cost)
            return 1;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return this.position.equals(((Node)o).position);
    }
    
    private float distanceTo(Node to) {
        return (int)(Math.abs(this.position.x - to.position.x) +
                Math.abs(this.position.y - to.position.y));
    }

    @Override
    public String toString() {
        return "Node{" +
                "position=" + position +
                ", cost=" + cost +
                ", parent=" + parent +
                '}';
    }
}
