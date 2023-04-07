package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class AgenteAStar extends AbstractPlayer {
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

    public static ACTIONS[] EXPANDED_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.scaleF = new Vector2d((float)stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                (float)stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);


        this.nx = stateObs.getObservationGrid().length;
        this.ny = stateObs.getObservationGrid()[0].length;

        this.invalid = new boolean[this.nx][this.ny];
        this.g = new int[this.nx][this.ny];
        this.parent = new Vector2d[this.nx][this.ny];

        // Default declaration value is false
        this.visited = new boolean[this.nx][this.ny];

        // Initialize distance/cost matrix
        for (int i = 0; i < this.nx; ++i) {
            Arrays.fill(this.g[i], Integer.MAX_VALUE);
        }

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
        // If there's a path, follow it.
        if (!this.plan.empty()) {
            return this.plan.pop();
        }

        // get avatar's starting position and initialize its cost to 0
        Vector2d avatar = new Vector2d(Math.floor(stateObs.getAvatarPosition().x / scaleF.x),
                                        Math.floor(stateObs.getAvatarPosition().y / scaleF.y));
        this.g[(int)avatar.x][(int)avatar.y] = 0;

        int currX, currY;
        ArrayList<Vector2d> frontier = new ArrayList<>(this.nx * this.ny);
        // add starting node to frontier list
        frontier.add(avatar);

        Vector2d next = new Vector2d();
        Vector2d curr = null;

        long start = System.nanoTime();
        while (true) {
            // get curr node, as the one with minimum cost of frontier nodes
            int minF = Integer.MAX_VALUE;
            currX = -1;
            currY = -1;
            for (Vector2d f_node : frontier) {
                int f_x = (int)f_node.x;
                int f_y = (int)f_node.y;
                // if the f=g+h heuristic value of node is minimum and its position is valid
                if (!this.invalid[f_x][f_y] && minF > f(f_x, f_y)) {
                    minF = f(f_x, f_y);
                    currX = f_x;
                    currY = f_y;
                    curr = f_node;
                }
            }
            int minDist = this.g[currX][currY];

            // check if the current position is goal's
            if (currX == this.portal.x && currY == this.portal.y) {
                break;
            }

            // node is visited, therefore an invalid position for not going backwards
            this.visited[currX][currY] = true;
            frontier.remove(curr);

            //////////// Expand node //////////

            // save the position of parent
            Vector2d parent = new Vector2d(currX, currY);

            for (ACTIONS action : AgenteAStar.EXPANDED_ACTIONS) {
                nextSuccessor(action, next, currX, currY);

                int nextX = (int)next.x;
                int nextY = (int)next.y;

                // if it's a wall or trap, don't expand
                if (this.invalid[nextX][nextY]) continue;

                if (
                        // it's been visited and less cost, we found a better path to get this node (Judea Pearl approach)
                        this.visited[nextX][nextY]
                        && this.g[nextX][nextY] > minDist + 1
                ) {
                    this.visited[nextX][nextY] = false;
                    frontier.add(new Vector2d(nextX, nextY));
                    this.parent[nextX][nextY] = parent;
                    this.g[nextX][nextY] = minDist + 1;
                    ++this.expandedNodes;
                } else if (
                        // it's been not visited and it's not frontier, expand node
                        !this.visited[nextX][nextY]
                        && !frontier.contains(next)
                ) {
                    frontier.add(new Vector2d(nextX, nextY));
                    this.parent[nextX][nextY] = parent;
                    this.g[nextX][nextY] = minDist + 1;
                    ++this.expandedNodes;
                } else if (
                        // it's frontier and the cost to get to it is less, update its cost
                        frontier.contains(next)
                        && this.g[nextX][nextY] > minDist + 1
                ) {
                    this.g[nextX][nextY] = minDist + 1;
                }
            }
        }
        long end = System.nanoTime();

        System.out.println("Runtime - AStar : " + (end - start) / 1e6 + " ms");
        System.out.println("Nodes expanded: " + this.expandedNodes);

        // Reconstruct path
        rebuildPath(currX, currY);

        System.out.println("Computed path length: " + this.plan.size());

        return this.plan.pop();
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

    /**
     * @brief Rebuild the path to get to the portal
     * @param goal_x portal's x coordinate
     * @param goal_y portal's y coordinate
     */
    private void rebuildPath(int goal_x, int goal_y) {
        int curr_x = goal_x;
        int curr_y = goal_y;
        while (true) {
            Vector2d parent = this.parent[curr_x][curr_y];

            // Starting node
            if (parent == null)
                break;

            // infer parent's action from parent-child position's difference
            if (parent.y > curr_y) {
                // up
                this.plan.push(ACTIONS.ACTION_UP);
            } else if (parent.y < curr_y) {
                // down
                this.plan.push(ACTIONS.ACTION_DOWN);
            } else if (parent.x > curr_x) {
                // left
                this.plan.push(ACTIONS.ACTION_LEFT);
            } else if (parent.x < curr_x) {
                // right
                this.plan.push(ACTIONS.ACTION_RIGHT);
            }

            // go backwards
            curr_x = (int)parent.x;
            curr_y = (int)parent.y;
        }
    }

    private int f(int x, int y) {
        return this.g[x][y] + (int)(Math.abs(x - this.portal.x) + Math.abs(y - this.portal.y));
    }
}
