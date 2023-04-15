package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;

public class AgenteDijkstra extends AbstractPlayer {
    // Factor de escala
    Vector2d scaleF;

    // Posicion del portal
    Vector2d portal;

    // Plan de acciones a tomar para llegar al portal
    Stack<ACTIONS> plan;

    // Matrix booleana de posiciones invalidas.
    // Posiciones invalidas son las posiciones en las que el jugador no puede poner un pie (paredes, trampas y casillas exploradas)
    // puede jugar el rol de una lista de visitados, ya que Dijkstra no permite visitar de nuevo nodos ya visitados (solo Dijkstra).
    boolean[][] invalid;

    // Matrix de distancias/costes de todos los nodos
    int[][] g;

    // Matrix de las posiciones de los padres de los nodos. parent[x][y] === padre del nodo con pos. (x,y)
    Vector2d[][] parent;

    // Numero de nodos expandidos
    int expandedNodes;

    // Numero de celdas por fila y columna
    int nx, ny;

    public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Inicializamos el factor de escala
        this.scaleF = new Vector2d((float)stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                (float)stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);


        // Obtenemos la cantidad de casillas de ancho y alto
        this.nx = stateObs.getObservationGrid().length;
        this.ny = stateObs.getObservationGrid()[0].length;

        this.invalid = new boolean[this.nx][this.ny];
        this.g = new int[this.nx][this.ny];
        this.parent = new Vector2d[this.nx][this.ny];

        // Inicializar la matrix de distancias/costes a infinito
        for (int i = 0; i < this.nx; ++i) {
            Arrays.fill(this.g[i], Integer.MAX_VALUE);
        }

        ////////////////// Guardar las posiciones de las paredes y trampas  /////////////////
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

        // Guardar la posicion del portal
        ArrayList<Observation>[] positions = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

        portal = positions[0].get(0).position;
        portal.x = Math.floor(portal.x / scaleF.x);
        portal.y = Math.floor(portal.y / scaleF.y);

        // Plan inicial vacio y 0 nodos expandidos
        this.plan = new Stack<>();
        this.expandedNodes = 0;
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Inicializo el contador del act
        long start = System.nanoTime();

        // Si hay un plan no vacio, obtener la siguiente accion a tomar
        if (!this.plan.empty()) {
            return this.plan.pop();
        }

        // Obtener la posicion inicial del avatar
        Vector2d avatar = new Vector2d(Math.floor(stateObs.getAvatarPosition().x / scaleF.x),
                                        Math.floor(stateObs.getAvatarPosition().y / scaleF.y));
        // inicializar su coste a 0
        this.g[(int)avatar.x][(int)avatar.y] = 0;

        // Inicializar lista de abiertos/fronteras
        ArrayList<Vector2d> frontier = new ArrayList<>(this.nx * this.ny);

        // Agregar nodo inicial a abiertos
        frontier.add(avatar);

        // Bucle de busqueda
        int curr_x, curr_y;
        while (true) {
            // Obtenemos el nodo actual como el que tiene minimo coste entre todos los nodos abiertos
            int minDist = Integer.MAX_VALUE;
            curr_x = -1;
            curr_y = -1;
            Vector2d curr = null;
            for (Vector2d f : frontier) {
                int f_x = (int)f.x;
                int f_y = (int)f.y;
                // el coste del nodo is minimo y su posicion es valida
                if (!this.invalid[f_x][f_y] && minDist > this.g[f_x][f_y]) {
                    minDist = this.g[f_x][f_y];
                    curr_x = f_x;
                    curr_y = f_y;
                    curr = f;
                }
            }

            // Comprobar si actual es el objetivo
            ++this.expandedNodes;   // un nodo al que compruebo si es objetivo, es un nodo expandido (Guion seccion 6)
            if (curr_x == this.portal.x && curr_y == this.portal.y) {
                break;
            }

            // el nodo actual lo marcamos como invalido, equivalente a introducirlo a cerrados
            this.invalid[curr_x][curr_y] = true;
            // Removerlo de abiertos
            frontier.remove(curr);

            /////////////// Expandir el nodo ///////////////

            // Para todos los nodos hijos, su padre es el actual
            Vector2d parent = new Vector2d(curr_x, curr_y);

            // Para cada nodo hijo, comprobamos que sea valido y actualizamos mejor hijo (menor coste)
            // ARRIBA
            if (!this.invalid[curr_x][curr_y - 1] && this.g[curr_x][curr_y - 1] > minDist + 1) {
                this.g[curr_x][curr_y - 1] = minDist + 1;
                this.parent[curr_x][curr_y - 1] = parent;
                frontier.add(new Vector2d(curr_x, curr_y - 1));
            }
            // ABAJO
            if (!this.invalid[curr_x][curr_y + 1] && this.g[curr_x][curr_y + 1] > minDist + 1) {
                this.g[curr_x][curr_y + 1] = minDist + 1;
                this.parent[curr_x][curr_y + 1] = parent;
                frontier.add(new Vector2d(curr_x, curr_y + 1));
            }
            // IZQUIERDA
            if (!this.invalid[curr_x - 1][curr_y] && this.g[curr_x - 1][curr_y] > minDist + 1) {
                this.g[curr_x - 1][curr_y] = minDist + 1;
                this.parent[curr_x - 1][curr_y] = parent;
                frontier.add(new Vector2d(curr_x - 1, curr_y));
            }
            // DERECHA
            if (!this.invalid[curr_x + 1][curr_y] && this.g[curr_x + 1][curr_y] > minDist + 1) {
                this.g[curr_x + 1][curr_y] = minDist + 1;
                this.parent[curr_x + 1][curr_y] = parent;
                frontier.add(new Vector2d(curr_x + 1, curr_y));
            }
        }

        // Reconstruir camino
        rebuildPath(curr_x, curr_y);

        // Termina la busqueda, paramos el contador
        long end = System.nanoTime();

        System.out.println("Runtime: " + (end - start) / 1e6 + " ms");
        System.out.println("Computed path length: " + this.plan.size());
        System.out.println("Nodes expanded: " + this.expandedNodes);

        // Devolvemos el tope del plan
        return this.plan.pop();
    }

    /**
     * @brief Reconstruir el camino al portal
     * @param goal_x coordenada x del portal
     * @param goal_y coordenada y del portal
     */
    private void rebuildPath(int goal_x, int goal_y) {
        int curr_x = goal_x;
        int curr_y = goal_y;

        // Recorremos todos los padres infiriendo la accion que hizo el nodo padre
        while (true) {
            Vector2d parent = this.parent[curr_x][curr_y];

            // Nodo inicial no tiene padre
            if (parent == null)
                break;

            // Inferir la accion del padre de la diferencia en la posicion padre-hijo
            if (parent.y > curr_y) {
                // ARRIBA
                this.plan.push(ACTIONS.ACTION_UP);
            } else if (parent.y < curr_y) {
                // ABAJO
                this.plan.push(ACTIONS.ACTION_DOWN);
            } else if (parent.x > curr_x) {
                // IZQUIERDA
                this.plan.push(ACTIONS.ACTION_LEFT);
            } else if (parent.x < curr_x) {
                // DERECHA
                this.plan.push(ACTIONS.ACTION_RIGHT);
            }

            // Seguimos hacia atras
            curr_x = (int)parent.x;
            curr_y = (int)parent.y;
        }
    }
}
