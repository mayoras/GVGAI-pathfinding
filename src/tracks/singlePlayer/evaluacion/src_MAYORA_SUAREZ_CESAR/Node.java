package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;


import tools.Vector2d;

/**
 * TODO: Add estimatedCost attribute for A* algorithm
 */
public class Node implements Comparable<Node> {
    public Vector2d position;
    public int cost;
    public Node parent;
    public int id;

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

        // `id` depends on the Node's position, this is important for comparison between nodes
        this.id = ((int)(this.position.x) * 100 + (int)(this.position.y));
    }
    ///////////

    @Override
    public int compareTo(Node other) {
        return Integer.compare(this.cost, other.cost);
    }

    /**
     * @brief Custom hashCode builder using the `id` as hashCode
     * @return Node's `id` as the Node's hashCode
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.position == null) ? 0 : this.id);
        return result;
    }

    /**
     * @brief Custom `equals` method for Node's position
     * @param obj Comparison node
     * @return `true` if obj is equal to this, `false` otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Node))
            return false;
        if (obj == this)
            return true;
        Node other = (Node)obj;
        if (this.position == null) {
            if (other.position != null)
                return false;
        } else if (!(this.id == other.id))
            // If two Nodes have the same `id`, they have the same `position`
            return false;

        return true;
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
