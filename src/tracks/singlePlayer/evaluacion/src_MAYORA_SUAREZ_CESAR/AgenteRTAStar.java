package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import javax.swing.plaf.nimbus.State;
import java.util.*;

public class AgenteRTAStar extends AbstractPlayer {
    // Scale factor
    Vector2d scaleF;

    // Portal/Goal's position
    Vector2d portal;

    // Plan of actions to take in order to reach the portal
    Stack<ACTIONS> plan;

    // Invalid positions are those which player cannot put a foot on (walls, traps, explored cells, ...)
    // plays the role of explored nodes list
    boolean[][] invalid;

    // Number of expanded nodes
    int expandedNodes;

    // Map's number of cells per row and column
    int nx, ny;

    HashMap<Integer, Integer> h;

    public static ACTIONS[] EXPANDED_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteRTAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        long start = System.nanoTime();
        this.scaleF = new Vector2d((float)stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                (float)stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);


        this.nx = stateObs.getObservationGrid().length;
        this.ny = stateObs.getObservationGrid()[0].length;

        this.invalid = new boolean[this.nx][this.ny];
        this.h = new HashMap<>();

        ////////////////// RECORD WALLS & TRAPS POSITIONS /////////////////
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
        //////////////////////////////////////////////////////////////////////

        // get portal/goal position
        ArrayList<Observation>[] positions = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

        portal = positions[0].get(0).position;
        portal.x = Math.floor(portal.x / scaleF.x);
        portal.y = Math.floor(portal.y / scaleF.y);

        this.plan = new Stack<>();
        this.expandedNodes = 0;
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // get avatar's current position as current node
        Vector2d curr = new Vector2d((int)Math.floor(stateObs.getAvatarPosition().x / scaleF.x),
                                    (int)Math.floor(stateObs.getAvatarPosition().y / scaleF.y));
        int currX = (int)curr.x, currY = (int)curr.y;
        int currID = positionID(curr);

        // Check if the world has changed
        if (worldHasChanged(stateObs)) {
            updateInnerWorld(stateObs);
        }

        // Starting position
        if (!this.h.containsKey(currID))
            this.h.put(currID, manhattanDistance(currX, currY));

        // Neighbours of current
        Vector2d next = new Vector2d();
        int minF = Integer.MAX_VALUE;
        int secondMinF = Integer.MAX_VALUE;
        ACTIONS bestAction = ACTIONS.ACTION_NIL;
        for (ACTIONS action : AgenteRTAStar.EXPANDED_ACTIONS) {
            nextSuccessor(action, next, currX, currY);
            int nextX = (int)next.x, nextY = (int)next.y;
            int nextID = positionID(next);

            // Check if neighbour is the goal, if so just move to it.
            if (next.x == this.portal.x && next.y == this.portal.y)
                return action;

            // if it's invalid, don't bother
            if (this.invalid[nextX][nextY]) {
                this.h.put(nextID, Integer.MAX_VALUE);
                continue;
            }

            // if it's a new undiscovered node, record it's heuristic value h(next)
            if (!this.h.containsKey(nextID))
                this.h.put(nextID, manhattanDistance(nextX, nextY));

            // get the first and second minimum f-value from neighbours
            if (minF > this.h.get(nextID) + 1) {
                secondMinF = minF;
                minF = this.h.get(nextID) + 1;
                bestAction = action;
            }
        }

        // update heuristic of current with the maximum of { second minimum, h(curr) }
        int z = (secondMinF == Integer.MAX_VALUE ? minF : secondMinF); // if there's only one minimum use it as second
        if (z > this.h.get(currID)) {
            this.h.put(currID, z);
        }
        this.h.put(currID, z);

        // return action to the best node from current
        return bestAction;
    }

    private void nextSuccessor(ACTIONS action, Vector2d next, int currX, int currY) {
        if (action == ACTIONS.ACTION_UP) {
            next.x = currX;
            next.y = currY - 1;
        } else if (action == ACTIONS.ACTION_DOWN) {
            next.x = currX;
            next.y = currY + 1;
        } else if (action == ACTIONS.ACTION_LEFT) {
            next.x = currX - 1;
            next.y = currY;
        } else if (action == ACTIONS.ACTION_RIGHT) {
            next.x = currX + 1;
            next.y = currY;
        }
    }

    private int manhattanDistance(int x, int y) {
        return (int)(Math.abs(x - this.portal.x) + Math.abs(y - this.portal.y));
    }

    // Uniquely identify a position in a grid-world
    private int positionID(Vector2d pos) {
        return ((int)(pos.x) * this.ny + (int)(pos.y));
    }

    private boolean worldHasChanged(StateObservation stateObs) {
        ArrayList<Observation>[] immPositions = stateObs.getImmovablePositions();
        ArrayList<Observation> wallsAux = immPositions[0];
        ArrayList<Observation> trapsAux = immPositions[1];

        for (Observation aux : wallsAux) {
            int i = (int)Math.floor(aux.position.x / scaleF.x);
            int j = (int)Math.floor(aux.position.y / scaleF.y);

            // it is valid a position that shouldn't be, the world has changed!
            if (!this.invalid[i][j])
                return true;
        }
        for (Observation aux : trapsAux) {
            int i = (int)Math.floor(aux.position.x / scaleF.x);
            int j = (int)Math.floor(aux.position.y / scaleF.y);

            // not only the walls are the only things that can appear
            if (!this.invalid[i][j])
                return true;
        }

        return false;
    }

    private void updateInnerWorld(StateObservation stateObs) {
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
    }
}
