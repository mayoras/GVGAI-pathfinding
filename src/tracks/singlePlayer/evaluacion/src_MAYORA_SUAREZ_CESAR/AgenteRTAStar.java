package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;

public class AgenteRTAStar extends AbstractPlayer {
    // Scale factor
    Vector2d scaleF;

    // Portal/Goal's position
    Vector2d portal;

    // Invalid positions are those which player cannot put a foot on (walls, traps, explored cells, ...)
    // plays the role of explored nodes list
    boolean[][] invalid;

    // Num of invalid items
    int n_invalid;

    // Number of expanded nodes
    int expandedNodes;

    // Length of the path taken by the agent
    int pathLength;

    // Sum of all nanoseconds taken everytime that computes an action
    long totalRuntime;

    // Map's number of cells per row and column
    int nx, ny;

    // Matrix to keep track of heuristics of nodes
    int[][] h;

    public static ACTIONS[] EXPANDED_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteRTAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        long start = System.nanoTime();
        this.scaleF = new Vector2d((float)stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                (float)stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);


        this.nx = stateObs.getObservationGrid().length;
        this.ny = stateObs.getObservationGrid()[0].length;

        this.invalid = new boolean[this.nx][this.ny];
        this.h = new int[this.nx][this.ny];

        // Fill default heuristics
        for (int i = 0; i < this.nx; ++i) {
            Arrays.fill(this.h[i], -1);
        }

        ////////////////// RECORD WALLS & TRAPS POSITIONS /////////////////
        ArrayList<Observation>[] immPositions = stateObs.getImmovablePositions();
        ArrayList<Observation> wallsAux = immPositions[0];
        ArrayList<Observation> trapsAux = immPositions[1];

        this.n_invalid = wallsAux.size() + trapsAux.size();
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

        this.expandedNodes = 0;
        this.pathLength = 0;
        this.totalRuntime = 0;
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Start timer
        long start = System.nanoTime();

        // get avatar's current position as current node
        Vector2d curr = new Vector2d((int)Math.floor(stateObs.getAvatarPosition().x / scaleF.x),
                                    (int)Math.floor(stateObs.getAvatarPosition().y / scaleF.y));
        int currX = (int)curr.x, currY = (int)curr.y;

        // El nodo actual es un nodo que ha sido expandido (Guion seccion 6)
        ++this.expandedNodes;

        // Check if the world has changed
        if (worldHasChanged(stateObs)) {
            updateInnerWorld(stateObs);
        }

        // Starting position
        if (this.h[currX][currY] < 0) {
            this.h[currX][currY] = manhattanDistance(currX, currY);
        }

        // Neighbours of current
        Vector2d next = new Vector2d();
        int minF = Integer.MAX_VALUE;
        int secondMinF = Integer.MAX_VALUE;
        ACTIONS bestAction = ACTIONS.ACTION_NIL;
        for (ACTIONS action : AgenteRTAStar.EXPANDED_ACTIONS) {
            nextSuccessor(action, next, currX, currY);
            int nextX = (int)next.x, nextY = (int)next.y;

            // if it's invalid, don't bother
            if (this.invalid[nextX][nextY]) {
                continue;
            }

            // Check if neighbour is the goal, if so just move to it.
            if (next.x == this.portal.x && next.y == this.portal.y) {
                ++this.expandedNodes;       // Sumamos el ultimo nodo de expandidos antes de ir al objetivo

                // Agregar el tiempo restante
                long end = System.nanoTime();
                this.totalRuntime += (end - start);

                // agregar el ultimo nodo final a la longitud del camino
                ++this.pathLength;

                // Imprimir la informacion del recorrido antes de marcharse
                System.out.println("Runtime: " + this.totalRuntime / 1e6 + " ms");
                System.out.println("Computed Path length: " + this.pathLength);
                System.out.println("Nodes expanded: " + this.expandedNodes);

                return action;
            }

            // if it's a new undiscovered node, record it's heuristic value h(next)
            if (this.h[nextX][nextY] < 0)
                this.h[nextX][nextY] = manhattanDistance(nextX, nextY);


            // get the first and second minimum f-value from neighbours
            int f = this.h[nextX][nextY] + 1;
            if (minF > f) {
                secondMinF = minF;
                minF = f;
                bestAction = action;
            } else if (f < secondMinF) {
                secondMinF = f;
            }
        }

        // update heuristic of current with the maximum of { second minimum, h(curr) }
        int z = (secondMinF == Integer.MAX_VALUE ? minF : secondMinF); // if there's only one minimum use it as second
        if (z > this.h[currX][currY])
            this.h[currX][currY] = z;

        // Increment the path length is being taken
        ++this.pathLength;

        // Stop timer and add runtime
        long end = System.nanoTime();
        this.totalRuntime += (end - start);

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

    private boolean worldHasChanged(StateObservation stateObs) {
        ArrayList<Observation>[] immPositions = stateObs.getImmovablePositions();

        // If the number of walls and traps changed, then the world has changed!
        return this.n_invalid != immPositions[0].size() + immPositions[1].size();
    }

    private void updateInnerWorld(StateObservation stateObs) {
        ArrayList<Observation>[] immPositions = stateObs.getImmovablePositions();
        ArrayList<Observation> wallsAux = immPositions[0];
        ArrayList<Observation> trapsAux = immPositions[1];

        this.n_invalid = wallsAux.size() + trapsAux.size();
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
