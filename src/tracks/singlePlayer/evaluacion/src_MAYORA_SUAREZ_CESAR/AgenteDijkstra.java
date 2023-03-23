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

/*
TODO:
- Get walls and traps positions to define the VALID actions to take at every step
 */

public class AgenteDijkstra extends AbstractPlayer {
    Vector2d scaleF;
    Vector2d portal;
    ArrayList<ACTIONS> plan;

    public static final ACTIONS[] EXPAND_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.scaleF = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid().length);

        System.out.println("Factor de escala: " + this.scaleF);

        ArrayList<Observation>[] positions = stateObs.getPortalsPositions(stateObs.getAvatarPosition());


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

        Vector2d avatar = new Vector2d(stateObs.getAvatarPosition().x / scaleF.x,
                                    stateObs.getAvatarPosition().y / scaleF.y);

        PriorityQueue<Node> frontier = new PriorityQueue<>();
        // TODO: Consider using other data structure for this
        HashSet<Node> explored = new HashSet<>();

        // Push the current avatar's position
        frontier.add(new Node(avatar));

        Node curr;
        while (true) {
            curr = frontier.poll();

            // Check if it's the portal/goal
            if (curr.position == this.portal) {
                break;
            }

            // Current node is explored
            explored.add(curr);

            // Expand the current node
            for (ACTIONS expandAction : EXPAND_ACTIONS) {
                switch (expandAction) {
                    case ACTION_UP ->
                        frontier.add(new Node(new Vector2d(curr.position.x, curr.position.y - 1)));

                    case ACTION_DOWN ->
                        frontier.add(new Node(new Vector2d(curr.position.x, curr.position.y + 1)));

                    case ACTION_LEFT ->
                        frontier.add(new Node(new Vector2d(curr.position.x - 1, curr.position.y)));

                    case ACTION_RIGHT ->
                        frontier.add(new Node(new Vector2d(curr.position.x + 1, curr.position.y)));

                    default -> {}
                }
            }
        }

        return ACTIONS.ACTION_NIL;
    }

    private boolean posIsValid(Vector2d pos) {
        return false;
    }

    private int manhattanToGoal(Node from) {
        return (int)(Math.abs(from.position.x - this.portal.x) +
                Math.abs(from.position.y - this.portal.y));
    }
}
