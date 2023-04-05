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
    int[][] g;
    Vector2d[][] parent;
    int expandedNodes;
    int nx, ny;

    public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.scaleF = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

//        System.out.println("Factor de escala: " + this.scaleF);

        this.nx = stateObs.getObservationGrid().length;
        this.ny = stateObs.getObservationGrid()[0].length;

        this.invalid = new boolean[this.nx][this.ny];
        this.g = new int[this.nx][this.ny];
        this.parent = new Vector2d[this.nx][this.ny];

        // Initialize distance matrix
        for (int i = 0; i < this.nx; ++i) {
            Arrays.fill(this.g[i], Integer.MAX_VALUE);
        }

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
        this.expandedNodes = 0;
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // If there's a path, follow it.
        if (!this.plan.empty()) {
            return this.plan.pop();
        }

        Vector2d avatar = new Vector2d(Math.floor(stateObs.getAvatarPosition().x / scaleF.x),
                                        Math.floor(stateObs.getAvatarPosition().y / scaleF.y));

        this.g[(int)avatar.x][(int)avatar.y] = 0;
        int curr_x, curr_y;
        ArrayList<Vector2d> frontier = new ArrayList<>(this.nx * this.ny);
        frontier.add(avatar);
        long start = System.nanoTime();
        while (true) {
            // get curr node
            int minDist = Integer.MAX_VALUE;
            curr_x = -1;
            curr_y = -1;
            Vector2d curr = null;
            for (Vector2d f : frontier) {
                if (minDist > this.g[(int)f.x][(int)f.y] && !this.invalid[(int)f.x][(int)f.y]) {
                    minDist = this.g[(int)f.x][(int)f.y];
                    curr_x = (int)f.x;
                    curr_y = (int)f.y;
                    curr = f;
                }
            }

            if (curr_x == this.portal.x && curr_y == this.portal.y) {
                break;
            }

            // visit node
            this.invalid[curr_x][curr_y] = true;
            frontier.remove(curr);

            // Expand node
            Vector2d parent = new Vector2d(curr_x, curr_y);
            // up
            if (!this.invalid[curr_x][curr_y - 1] && this.g[curr_x][curr_y - 1] > minDist + 1) {
                this.g[curr_x][curr_y - 1] = minDist + 1;
                this.parent[curr_x][curr_y - 1] = parent;
                frontier.add(new Vector2d(curr_x, curr_y - 1));
            }
            // down
            if (!this.invalid[curr_x][curr_y + 1] && this.g[curr_x][curr_y + 1] > minDist + 1) {
                this.g[curr_x][curr_y + 1] = minDist + 1;
                this.parent[curr_x][curr_y + 1] = parent;
                frontier.add(new Vector2d(curr_x, curr_y + 1));
            }
            // left
            if (!this.invalid[curr_x - 1][curr_y] && this.g[curr_x - 1][curr_y] > minDist + 1) {
                this.g[curr_x - 1][curr_y] = minDist + 1;
                this.parent[curr_x - 1][curr_y] = parent;
                frontier.add(new Vector2d(curr_x - 1, curr_y));
            }
            // right
            if (!this.invalid[curr_x + 1][curr_y] && this.g[curr_x + 1][curr_y] > minDist + 1) {
                this.g[curr_x + 1][curr_y] = minDist + 1;
                this.parent[curr_x + 1][curr_y] = parent;
                frontier.add(new Vector2d(curr_x + 1, curr_y));
            }
        }
        long end = System.nanoTime();

        System.out.println("Elapsed time - Dijkstra: " + (end - start) / 1e6 + " ms");

        System.out.println("Found!");
        rebuildPath(curr_x, curr_y);
        return this.plan.pop();
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

            if (parent == null)
                break;

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
}
