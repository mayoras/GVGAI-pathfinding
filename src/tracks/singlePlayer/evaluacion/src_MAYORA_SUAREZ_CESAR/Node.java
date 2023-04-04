package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;
import ontology.Types.ACTIONS;
import tools.Vector2d;

/**
 * TODO: Add estimatedCost attribute for A* algorithm
 */
public class Node implements Comparable<Node> {
    public Vector2d position;
    public int cost;
    public Node parent;
    public ACTIONS parent_act;
    public int id;

    ////////// Constructors
    public Node(Vector2d pos) {
        this.position = pos;
        this.parent = null;
        this.cost = 0;
    }
    public Node(Vector2d pos, Node parent, ACTIONS parent_act) {
        this.position = pos;
        this.parent = parent;
        this.parent_act = parent_act;
        this.cost = parent.cost + 1;

        // `id` depends on the Node's position, this is important for comparison between nodes
        this.id = ((int)(this.position.x) * 100 + (int)(this.position.y));
    }
    ///////////

    @Override
    public int compareTo(Node other) {
        if (this.cost < other.cost)
            return -1;
        else if (this.cost > other.cost)
            return 1;
        return 0;
    }

    /**
     * @brief Custom `equals` method for Node's position
     * @param obj Comparison node
     * @return `true` if obj is equal to this, `false` otherwise.
     */
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null)
//            return false;
//        if (!(obj instanceof Node))
//            return false;
//        if (obj == this)
//            return true;
//        Node other = (Node)obj;
//        if (this.position == null) {
//            if (other.position != null)
//                return false;
//        } else if (!(this.id == other.id))
//            // If two Nodes have the same `id`, they have the same `position`
//            return false;
//
//        return true;
//    }
    public boolean equals(Object o)
    {
        return this.position.equals(((tools.pathfinder.Node)o).position);
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
