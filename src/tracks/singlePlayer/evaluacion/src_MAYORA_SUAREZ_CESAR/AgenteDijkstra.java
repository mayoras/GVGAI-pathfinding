package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Stack;

public class AgenteDijkstra extends AbstractPlayer {
    Vector2d scaleF;
    Vector2d portal;
    Stack<ACTIONS> plan;

    // Invalid positions are those which player cannot put a foot on (walls, traps, explored cells, ...)
    boolean[][] invalid;
    Node goal;
    int expandedNodes;

    public static final ACTIONS[] EXPAND_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        System.out.println(stateObs.getObservationGrid().length);
        this.scaleF = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

//        System.out.println("Factor de escala: " + this.scaleF);

        this.invalid = new boolean[stateObs.getObservationGrid().length][stateObs.getObservationGrid()[0].length];

        ArrayList<Observation>[] positions = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
        ArrayList<Observation>[] immPositions = stateObs.getImmovablePositions();

        ArrayList<Observation> wallsAux = immPositions[0];
        ArrayList<Observation> trapsAux = immPositions[1];

        long start = System.nanoTime();
        for (Observation aux : wallsAux) {
            int i = (int)Math.floor(aux.position.x / scaleF.x);
            int j = (int)Math.floor(aux.position.y / scaleF.y);
            this.invalid[i][j] = true;
        }
        for (Observation aux : trapsAux) {
            int i = (int)Math.floor(aux.position.x / scaleF.x);
            int j = (int)Math.floor(aux.position.y / scaleF.y);
            this.invalid[i][j] = true;
        }
        long end = System.nanoTime();

        System.out.println("Elapsed time to construct obstacles: " + ((end-start) / 1e6) + " ms");

        portal = positions[0].get(0).position;
        portal.x = Math.floor(portal.x / scaleF.x);
        portal.y = Math.floor(portal.y / scaleF.y);

        this.plan = new Stack<>();
        this.goal = null;
        this.expandedNodes = 0;
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // If there's a path, follow it.
        if (!this.plan.isEmpty() || this.goal != null) {
            if (this.plan.isEmpty()) {
                rebuildPath();
                System.out.println("Computed path lenght: " + this.plan.size());
            }
            return this.plan.pop();
        }

        Vector2d avatar = new Vector2d(Math.floor(stateObs.getAvatarPosition().x / scaleF.x),
                                        Math.floor(stateObs.getAvatarPosition().y / scaleF.y));

        PriorityQueue<Node> frontier = new PriorityQueue<>();

        // Push the current avatar's position
        frontier.add(new Node(avatar));
        System.out.println(avatar);

        Node curr = null;
        boolean found = false;
        long start = System.nanoTime();
        while (!frontier.isEmpty()) {
            // Get current node
            curr = frontier.remove();

            // Check if it's the portal/goal
            if (this.portal.equals(curr.position)) {
                found = true;
                break;
            }

            // Current node is explored, therefore invalid
            this.invalid[(int)curr.position.x][(int)curr.position.y] = true;

            ////// Expand the current node
            Vector2d next_pos;
            Node childNode;
            ACTIONS expandAction;

            //ACTION_UP
            int x = (int)curr.position.x;
            int y = (int)curr.position.y - 1;
            // Check if position is valid (no walls, no traps, no explored)
            if (posIsValid(x, y)) {
                next_pos = new Vector2d(x, y);
                childNode = new Node(next_pos, curr, ACTIONS.ACTION_UP);
                frontier.add(childNode);
                ++this.expandedNodes;
            }

            // ACTION_DOWN
            x = (int)curr.position.x;
            y = (int)curr.position.y + 1;
            if (posIsValid(x, y)) {
                next_pos = new Vector2d(x, y);
                childNode = new Node(next_pos, curr, ACTIONS.ACTION_DOWN);
                frontier.add(childNode);
                ++this.expandedNodes;
            }

            // ACTION_LEFT
            x = (int)curr.position.x - 1;
            y = (int)curr.position.y;
            if (posIsValid(x, y)) {
                next_pos = new Vector2d(x, y);
                childNode = new Node(next_pos, curr, ACTIONS.ACTION_LEFT);
                frontier.add(childNode);
                ++this.expandedNodes;
            }

            // ACTION_RIGHT
            x = (int)curr.position.x + 1;
            y = (int)curr.position.y;
            if (posIsValid(x, y)) {
                next_pos = new Vector2d(x, y);
                childNode = new Node(next_pos, curr, ACTIONS.ACTION_RIGHT);
                frontier.add(childNode);
                ++this.expandedNodes;
            }
        }
        long end = System.nanoTime();
        System.out.println("Dijkstra: " + (end-start)/1e6 + " ms");
        System.out.println("Total expanded nodes: " + this.expandedNodes);

        // Rebuild path/plan
        if (found) {
//            System.out.println("FOUND!\nTotal cost: " + curr.cost + " steps");
//            rebuildPath(curr);
            this.goal = curr;
        }

        return ACTIONS.ACTION_NIL;
    }

    /**
     * @brief Rebuild the path to get to the portal
     */
    private void rebuildPath() {
        Node curr = this.goal;
        while (curr.parent != null) {
            this.plan.push(curr.parent_act);
            curr = curr.parent;
        }
    }

    private boolean posIsValid(int x, int y) {
        // if there's not obstacle, or it has already been visited
        return !this.invalid[x][y];
    }

    private int manhattanToGoal(Node from) {
        return (int)(Math.abs(from.position.x - this.portal.x) +
                Math.abs(from.position.y - this.portal.y));
    }
}
