package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.Stack;

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

    // Matrix of distances/costs of all nodes
    int[][] g;

    // Matrix of parent's positions of a node. parent[x][y] === parent of node with position (x,y)
    Vector2d[][] parent;

    // Number of expanded nodes
    int expandedNodes;

    // Map's number of cells per row and column
    int nx, ny;

    boolean[][] visited;

    int[][] h;

    public static ACTIONS[] EXPANDED_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteRTAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        long start = System.nanoTime();
        this.scaleF = new Vector2d((float)stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                (float)stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);


        this.nx = stateObs.getObservationGrid().length;
        this.ny = stateObs.getObservationGrid()[0].length;

        this.invalid = new boolean[this.nx][this.ny];
//        this.g = new int[this.nx][this.ny];
        this.h = new int[this.nx][this.ny];
//        this.parent = new Vector2d[this.nx][this.ny];

        // Default declaration value is false
//        this.visited = new boolean[this.nx][this.ny];

        // Initialize distance/cost matrix
//        for (int i = 0; i < this.nx; ++i) {
//            Arrays.fill(this.g[i], Integer.MAX_VALUE);
//        }

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
            this.h[i][j] = Integer.MAX_VALUE;
        }
        //////////////////////////////////////////////////////////////////////

        // get portal/goal position
        ArrayList<Observation>[] positions = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

        portal = positions[0].get(0).position;
        portal.x = Math.floor(portal.x / scaleF.x);
        portal.y = Math.floor(portal.y / scaleF.y);

        // Initialize heuristics
        for (int i = 0; i < this.nx; ++i) {
            for (int j = 0; j < this.ny; ++j) {
                if (!this.invalid[i][j])
                    this.h[i][j] = manhattanDistance(i, j);
            }
        }

        this.plan = new Stack<>();
        this.expandedNodes = 0;

        long end = System.nanoTime();
        System.out.println("Estimated time for constructor: " + (end - start)/1e6 + " ms");
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // get avatar's current position as current node
        Vector2d curr = new Vector2d(Math.floor(stateObs.getAvatarPosition().x / scaleF.x),
                Math.floor(stateObs.getAvatarPosition().y / scaleF.y));
        int currX = (int)curr.x, currY = (int)curr.y;
//        this.g[currX][currY] = 0;

        // Neighbours of current
        Vector2d next = new Vector2d();
        int minF = Integer.MAX_VALUE;
        int secondMinF = Integer.MAX_VALUE;
        ACTIONS bestAction = ACTIONS.ACTION_NIL;
        for (ACTIONS action : AgenteRTAStar.EXPANDED_ACTIONS) {
            nextSuccessor(action, next, currX, currY);
            int nextX = (int)next.x, nextY = (int)next.y;

            // get the first and second minimum f-value from neighbours
            if (
                    // if it's invalid, don't bother
                    !this.invalid[nextX][nextY]
                    // update best
                    && minF > this.h[nextX][nextY] + 1
            ) {
                secondMinF = minF;
                minF = this.h[nextX][nextY] + 1;
                bestAction = action;
            }
        }

        // update heuristic of current with the second minimum
        this.h[currX][currY] = (secondMinF == Integer.MAX_VALUE ? minF : secondMinF);

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
}
