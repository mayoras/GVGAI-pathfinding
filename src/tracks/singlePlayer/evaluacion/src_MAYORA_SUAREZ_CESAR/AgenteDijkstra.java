package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Stack;

/*
TODO:
- Get walls and traps positions to define the VALID actions to take at every step
 */

public class AgenteDijkstra extends AbstractPlayer {
    Vector2d scaleF;
    Vector2d portal;
    Stack<ACTIONS> plan;

    // Invalid positions are those which player cannot put a foot on (walls, traps, explored cells, ...)
    boolean[][] invalid;

    public static final ACTIONS[] EXPAND_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        System.out.println(stateObs.getObservationGrid().length);
        this.scaleF = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        System.out.println("Factor de escala: " + this.scaleF);

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
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // If there's a path, follow it.
        if (!this.plan.isEmpty()) {
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
        while (!frontier.isEmpty()) {
            // Get current node
            curr = frontier.poll();

            // Check if it's the portal/goal
            if (this.portal.equals(curr.position)) {
                found = true;
                break;
            }

            // Current node is explored, therefore invalid
            this.invalid[(int)curr.position.x][(int)curr.position.y] = true;

            // Expand the current node
            Vector2d next_pos;
            Node childNode;
            for (ACTIONS expandAction : EXPAND_ACTIONS) {
                switch (expandAction) {
                    case ACTION_UP -> {
                        next_pos = new Vector2d(curr.position.x, curr.position.y - 1);
                        // Check if position is valid (no walls, no traps, no explored)
                        if (posIsValid(next_pos)) {
                            childNode = new Node(next_pos, curr, expandAction);
                            childNode.cost = curr.cost + 1;
                            frontier.add(childNode);
                        }
                    }

                    case ACTION_DOWN -> {
                        next_pos = new Vector2d(curr.position.x, curr.position.y + 1);
                        if (posIsValid(next_pos)) {
                            childNode = new Node(next_pos, curr, expandAction);
                            childNode.cost = curr.cost + 1;
                            frontier.add(childNode);
                        }
                    }

                    case ACTION_LEFT -> {
                        next_pos = new Vector2d(curr.position.x - 1, curr.position.y);
                        if (posIsValid(next_pos)) {
                            childNode = new Node(next_pos, curr, expandAction);
                            childNode.cost = curr.cost + 1;
                            frontier.add(childNode);
                        }
                    }

                    case ACTION_RIGHT -> {
                        next_pos = new Vector2d(curr.position.x + 1, curr.position.y);
                        if (posIsValid(next_pos)) {
                            childNode = new Node(next_pos, curr, expandAction);
                            childNode.cost = curr.cost + 1;
                            frontier.add(childNode);
                        }
                    }

                    default -> {}
                }
            }
        }

        // Rebuild path/plan
        if (found) {
            System.out.println("FOUND!\nTotal cost: " + curr.cost + " steps");
            rebuildPath(curr);
        }

        return ACTIONS.ACTION_NIL;
    }

    /**
     * @brief Rebuild the path to get to the portal
     * @param goal goal node
     */
    private void rebuildPath(Node goal) {
        Node curr = goal;
        while (curr.parent != null) {
            this.plan.push(curr.parent_act);
            curr = curr.parent;
        }
    }

    private boolean posIsValid(Vector2d pos) {
//        // Check if it is a valid step position
//        for (Vector2d w : this.walls) {
//            if (w.equals(pos))
//                return false;
//        }
//
//        // Check it is not a trap
//        for (Vector2d t : this.traps) {
//            if (t.equals(pos))
//                return false;
//        }

        // if there's an obstacle
        if (this.invalid[(int)pos.x][(int)pos.y])
            return false;

        return true;
    }

    private int manhattanToGoal(Node from) {
        return (int)(Math.abs(from.position.x - this.portal.x) +
                Math.abs(from.position.y - this.portal.y));
    }
}
