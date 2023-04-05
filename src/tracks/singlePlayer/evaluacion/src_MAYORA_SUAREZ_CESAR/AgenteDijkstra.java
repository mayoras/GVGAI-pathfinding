package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;

public class AgenteDijkstra extends AbstractPlayer {
    Vector2d scaleF;
    Vector2d portal;
    Stack<ACTIONS> plan;

    // Invalid positions are those which player cannot put a foot on (walls, traps, explored cells, ...)
    boolean[][] invalid;
    Node goal;
    int expandedNodes;

//    public static final ACTIONS[] EXPAND_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.scaleF = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        this.invalid = new boolean[stateObs.getObservationGrid().length][stateObs.getObservationGrid()[0].length];

        ArrayList<Observation>[] positions = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
        ArrayList<Observation>[] immPositions = stateObs.getImmovablePositions();

        ArrayList<Observation> wallsAux = immPositions[0];
        ArrayList<Observation> trapsAux = immPositions[1];

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

        Queue<Node> frontier = new ArrayDeque<>();

        // Push the current avatar's position
        frontier.offer(new Node(avatar));

        Node curr = null;
        boolean found = false;
        while (!frontier.isEmpty()) {
            // Get current node
            curr = frontier.poll();
            int curr_x = (int)curr.position.x;
            int curr_y = (int)curr.position.y;

            // Check if it's the portal/goal
            if (this.portal.equals(curr.position)) {
                found = true;
                break;
            }

            // Current node is explored, therefore invalid
            this.invalid[curr_x][curr_y] = true;

            ////// Expand the current node
            Vector2d next_pos;
//            Node childNode;

            boolean[] actions = getValidActions(curr_x, curr_y);
            if (actions[0]) {
                next_pos = new Vector2d(curr_x, curr_y - 1);
                frontier.offer(new Node(next_pos, curr, ACTIONS.ACTION_UP));
                ++this.expandedNodes;
            }
            if (actions[1]) {
                next_pos = new Vector2d(curr_x, curr_y + 1);
                frontier.offer(new Node(next_pos, curr, ACTIONS.ACTION_DOWN));
                ++this.expandedNodes;
            }
            if (actions[2]) {
                next_pos = new Vector2d(curr_x - 1, curr_y);
                frontier.offer(new Node(next_pos, curr, ACTIONS.ACTION_LEFT));
                ++this.expandedNodes;
            }
            if (actions[3]) {
                next_pos = new Vector2d(curr_x + 1, curr_y);
                frontier.offer(new Node(next_pos, curr, ACTIONS.ACTION_RIGHT));
                ++this.expandedNodes;
            }

//            //ACTION_UP
//            int x = curr_x;
//            int y = curr_y - 1;
//            // Check if position is valid (no walls, no traps, no explored)
//            if (posIsValid(x, y)) {
//                next_pos = new Vector2d(x, y);
//                childNode = new Node(next_pos, curr, ACTIONS.ACTION_UP);
//                frontier.offer(childNode);
//                ++this.expandedNodes;
//            }
//
//            // ACTION_DOWN
//            y = curr_y + 1;
//            if (posIsValid(x, y)) {
//                next_pos = new Vector2d(x, y);
//                childNode = new Node(next_pos, curr, ACTIONS.ACTION_DOWN);
//                frontier.offer(childNode);
//                ++this.expandedNodes;
//            }
//
//            // ACTION_LEFT
//            x = curr_x - 1;
//            y = curr_y;
//            if (posIsValid(x, y)) {
//                next_pos = new Vector2d(x, y);
//                childNode = new Node(next_pos, curr, ACTIONS.ACTION_LEFT);
//                frontier.offer(childNode);
//                ++this.expandedNodes;
//            }
//
//            // ACTION_RIGHT
//            x = curr_x + 1;
//            if (posIsValid(x, y)) {
//                next_pos = new Vector2d(x, y);
//                childNode = new Node(next_pos, curr, ACTIONS.ACTION_RIGHT);
//                frontier.offer(childNode);
//                ++this.expandedNodes;
//            }
        }
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

    private boolean[] getValidActions(int x, int y) {
//        ArrayList<ACTIONS> actions = new ArrayList<>();
        boolean[] actions = new boolean[4];
        // Up
        if (!this.invalid[x][y-1]) {
            actions[0] = true;
        }

        // Down
        if (!this.invalid[x][y+1]) {
            actions[1] = true;
        }

        // Left
        if (!this.invalid[x-1][y]) {
            actions[2] = true;
        }

        // Right
        if (!this.invalid[x+1][y]) {
            actions[3] = true;
        }

//        this.expandedNodes += actions.size();
        return actions;
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
