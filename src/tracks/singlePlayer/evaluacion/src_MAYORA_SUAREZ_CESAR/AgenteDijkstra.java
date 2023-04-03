package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import javax.swing.plaf.nimbus.State;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Vector;

/*
TODO:
- Get walls and traps positions to define the VALID actions to take at every step
 */

public class AgenteDijkstra extends AbstractPlayer {
    Vector2d scaleF;
    Vector2d portal;
    ArrayList<ACTIONS> plan;

    ArrayList<Vector2d> walls;

    public static final ACTIONS[] EXPAND_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.scaleF = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        System.out.println("Factor de escala: " + this.scaleF);

        ArrayList<Observation>[] positions = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
        ArrayList<Observation>[] immPositions = stateObs.getImmovablePositions();

        ArrayList<Observation> wallsAux = immPositions[0];

        this.walls = new ArrayList<>();
        for (Observation aux : wallsAux) {
            this.walls.add(
                    new Vector2d(Math.floor(aux.position.x / scaleF.x),
                            Math.floor(aux.position.y / scaleF.y))
            );
        }

        // Is this traps??
//        this.traps = immPositions[1];

        portal = positions[0].get(0).position;
        portal.x = Math.floor(portal.x / scaleF.x);
        portal.y = Math.floor(portal.y / scaleF.y);

        this.plan = new ArrayList<>();
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // If there's a route, follow it.
        if (!this.plan.isEmpty()) {
            ACTIONS action = this.plan.get(0);
            this.plan.remove(0);
            return action;
        }

        Vector2d avatar = new Vector2d(Math.floor(stateObs.getAvatarPosition().x / scaleF.x),
                                        Math.floor(stateObs.getAvatarPosition().y / scaleF.y));

        PriorityQueue<Node> frontier = new PriorityQueue<>();
        // TODO: Consider using other data structure for this (boolean matrix)
        HashSet<Node> explored = new HashSet<>();

        // Push the current avatar's position
        frontier.add(new Node(avatar));
        System.out.println(avatar);

        Node curr;
        while (!frontier.isEmpty()) {
//            System.out.println("Frontier list size: " + frontier.size());
            curr = frontier.poll();

            System.out.println("Nodo actual: " + curr);

            if (frontier.contains(curr)) {
                System.out.println("Contains something should be popped " + curr);
                System.exit(1);
            }

            // Check if it's the portal/goal
            if (curr.position == this.portal) {
                break;
            }

            // Current node is explored
            explored.add(curr);

            // Expand the current node
            Vector2d next_pos;
            Node childNode;
            for (ACTIONS expandAction : EXPAND_ACTIONS) {
                switch (expandAction) {
                    case ACTION_UP -> {
                        next_pos = new Vector2d(curr.position.x, curr.position.y - 1);
                        // Check if there's no a wall
                        if (posIsValid(next_pos)) {
                            childNode = new Node(next_pos, curr);
                            if (!explored.contains(childNode)) {
                                childNode.cost = curr.cost + 1;
                                frontier.add(childNode);
                            }
                        }
                    }

                    case ACTION_DOWN -> {
                        next_pos = new Vector2d(curr.position.x, curr.position.y + 1);
                        // Check if there's no a wall
                        if (posIsValid(next_pos)) {
                            childNode = new Node(next_pos, curr);
                            if (!explored.contains(childNode)) {
                                childNode.cost = curr.cost + 1;
                                frontier.add(childNode);
                            }
                        }
                    }

                    case ACTION_LEFT -> {
                        next_pos = new Vector2d(curr.position.x - 1, curr.position.y);
                        // Check if there's no a wall
                        if (posIsValid(next_pos)) {
                            childNode = new Node(next_pos, curr);
                            if (!explored.contains(childNode)) {
                                childNode.cost = curr.cost + 1;
                                frontier.add(childNode);
                            }
                        }
                    }

                    case ACTION_RIGHT -> {
                        next_pos = new Vector2d(curr.position.x + 1, curr.position.y);
                        if (posIsValid(next_pos)) {
                            childNode = new Node(next_pos, curr);
                            if (!explored.contains(childNode)) {
                                childNode.cost = curr.cost + 1;
                                frontier.add(childNode);
                            }
                        }
                    }

                    default -> {}
                }
            }
        }

        return ACTIONS.ACTION_NIL;
    }

    private boolean posIsValid(Vector2d pos) {
        // Check if it is a valid step position
        for (Vector2d w : this.walls) {
            if (w == pos) {
                return false;
            }
        }

        return true;
    }

    private int manhattanToGoal(Node from) {
        return (int)(Math.abs(from.position.x - this.portal.x) +
                Math.abs(from.position.y - this.portal.y));
    }
}
