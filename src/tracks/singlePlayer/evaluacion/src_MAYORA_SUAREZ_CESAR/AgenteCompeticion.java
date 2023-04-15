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

public class AgenteCompeticion extends AbstractPlayer {
    // Factor de escala
    Vector2d scaleF;

    // Posicion del portal
    Vector2d portal;

    // Plan de acciones a tomar para llegar al portal
    Stack<ACTIONS> plan;

    // Matrix booleana de posiciones invalidas.
    // Posiciones invalidas son las posiciones en las que el jugador no puede poner un pie (paredes y trampas)
    boolean[][] invalid;

    // Numero de casillas invalidas
    int n_invalid;

    // Matrix de distancias/costes de todos los nodos
    int[][] g;

    // Matrix de las posiciones de los padres de los nodos. parent[x][y] === padre del nodo con pos. (x,y)
    Vector2d[][] parent;

    // Numero de nodos expandidos
    int expandedNodes;

    // Numero de celdas por fila y columna
    int nx, ny;

    // Matrix booleana de nodos visitados/explorados
    boolean[][] visited;

    // Longitud del camino recorrido por el agente
    int pathLength;

    // Runtime acumulado en nanosegundos que toma el agente hacia el portal
    long totalRuntime;

    // Acciones que puede tomar el agente
    public static ACTIONS[] EXPANDED_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    public AgenteCompeticion(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Inicializamos el factor de escala
        this.scaleF = new Vector2d((float)stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                (float)stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        // Obtenemos la cantidad de casillas de ancho y alto
        this.nx = stateObs.getObservationGrid().length;
        this.ny = stateObs.getObservationGrid()[0].length;

        this.invalid = new boolean[this.nx][this.ny];
        this.g = new int[this.nx][this.ny];
        this.parent = new Vector2d[this.nx][this.ny];
        this.visited = new boolean[this.nx][this.ny];   // por defecto todos los valores son false

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
            ++this.n_invalid;
        }
        for (Observation aux : trapsAux) {
            int i = (int)Math.floor(aux.position.x / scaleF.x);
            int j = (int)Math.floor(aux.position.y / scaleF.y);
            this.invalid[i][j] = true;
            ++this.n_invalid;
        }
        //////////////////////////////////////////////////////////////////////

        // Guardar la posicion del portal
        ArrayList<Observation>[] positions = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

        portal = positions[0].get(0).position;
        portal.x = Math.floor(portal.x / scaleF.x);
        portal.y = Math.floor(portal.y / scaleF.y);

        // Inicializamos la informacion de la ruta y el plan de acciones del agente
        this.expandedNodes = 0;
        this.pathLength = 0;
        this.totalRuntime = 0;
        this.plan = new Stack<>();
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Inicializo el contador del act
        long start = System.nanoTime();

        // Comprobar si el mundo ha cambiado (Labyrinth Extended)
        if (worldHasChanged(stateObs)) {
            updateInnerWorld(stateObs);

            // Necesitamos replanificar
            this.plan.clear();

            // Si el mundo cambia, entonces la informacion hasta el momento
            // se invalida, tenemos que volver a declarar nuestras estructuras
            // de datos para replanificar correctamente.
            this.g = new int[this.nx][this.ny];
            for (int i = 0; i < this.nx; ++i) {
                Arrays.fill(this.g[i], Integer.MAX_VALUE);
            }
            this.parent = new Vector2d[this.nx][this.ny];
            this.visited = new boolean[this.nx][this.ny];
        }
        // Si hay un plan no vacio, obtener la siguiente accion a tomar e incrementar
        // el numero de casillas recorridas
        else if (!this.plan.empty()) {
            ++this.pathLength;

            // Antes de finalizar, imprimir informacion relevante de la ruta
            if (this.plan.size() == 1) {

                // Convertimos a milisegundos el runtime acumulado
                this.totalRuntime /= 1e6;

                System.out.println("Runtime: " + this.totalRuntime + " ms");
                System.out.println("Computed path length: " + this.pathLength);
                System.out.println("Expanded nodes: " + this.expandedNodes);
            }

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

        Vector2d next = new Vector2d();
        Vector2d curr = null;
        int currX, currY;
        while (true) {
            // Obtenemos el nodo actual como el que tiene minimo valor f entre todos los nodos abiertos
            int minF = Integer.MAX_VALUE;
            currX = -1;
            currY = -1;
            for (Vector2d f_node : frontier) {
                int f_x = (int)f_node.x;
                int f_y = (int)f_node.y;
                // if the f=g+h heuristic value of node is minimum and its position is valid
                // el valor f=g+h del nodo y su posicion es valida
                if (!this.invalid[f_x][f_y] && minF > f(f_x, f_y)) {
                    minF = f(f_x, f_y);
                    currX = f_x;
                    currY = f_y;
                    curr = f_node;
                }
            }
            // Obtenemos ademas el coste minimo del actual
            int minDist = this.g[currX][currY];

            // Comprobar si actual es el objetivo
            ++this.expandedNodes;       // un nodo al que compruebo si es objetivo, es un nodo expandido (Guion seccion 6)
            if (currX == this.portal.x && currY == this.portal.y) {
                break;
            }

            // Nodo es visitado y removido de abiertos
            this.visited[currX][currY] = true;
            frontier.remove(curr);

            //////////// Expandir nodo //////////

            // Para todos los nodos hijo, su padre es el actual
            Vector2d parent = new Vector2d(currX, currY);

            // Para cada sucesor
            for (ACTIONS action : AgenteAStar.EXPANDED_ACTIONS) {
                // Obtenemos el nuevo sucesor. Modificamos next
                nextSuccessor(action, next, currX, currY);

                int nextX = (int)next.x;
                int nextY = (int)next.y;

                // si es una pared o trampa ni te molestes, ignoralo
                if (this.invalid[nextX][nextY]) continue;

                if (
                    // si ha sido visitda y tiene menor costo, hemos encontrado un mejor camino para llegar a este nodo
                    // (aproximacion de Judea Pearl)
                        this.visited[nextX][nextY]
                                && this.g[nextX][nextY] > minDist + 1
                ) {
                    // sacamos el nodo de visitados y lo metemos de nuevo en abiertos
                    this.visited[nextX][nextY] = false;
                    frontier.add(new Vector2d(nextX, nextY));

                    // actualizamos mejor padre y su coste
                    this.parent[nextX][nextY] = parent;
                    this.g[nextX][nextY] = minDist + 1;
                } else if (
                    // si no ha sido visitado y no esta en abiertos, situacion normal, expandir nodo
                        !this.visited[nextX][nextY]
                                && !frontier.contains(next)
                ) {
                    // Agregamos nuevo nodo a abiertos
                    frontier.add(new Vector2d(nextX, nextY));

                    // actualizamos mejor padre y su coste
                    this.parent[nextX][nextY] = parent;
                    this.g[nextX][nextY] = minDist + 1;
                } else if (
                    // esta en abiertos y el coste es menor, se ha encontrado mejor padre en la expansion, actualiza el coste
                        frontier.contains(next)
                                && this.g[nextX][nextY] > minDist + 1
                ) {
                    this.g[nextX][nextY] = minDist + 1;
                }
            }
        }
        // Reconstruir camino
        rebuildPath(currX, currY);

        // Termina la busqueda, paramos el contador
        long end = System.nanoTime();

        // Como nuestro agente puede enfrentarse a problemas donde el mundo cambia
        // puede haber replanificacion, por tanto, el runtime sera el runtime acumulado
        // de todas las ejecuciones de A*
        this.totalRuntime += (end - start);

        // Devolvemos el tope del plan (incrementamos casilla recorrida)
        ++this.pathLength;
        return this.plan.pop();
    }

    /**
     * @brief Obtener proximo sucesor a expandir
     * @param action    accion que toma el padre
     * @param next      posicion del siguiente nodo
     * @param currX     coordenada x del nodo actual
     * @param currY     coordenada y del nodo actual
     * @implNote el metodo modifica el parametro next
     */
    private void nextSuccessor(ACTIONS action, Vector2d next, int currX, int currY) {
        // Para cada accion posible actualizamos la posicion correspondiente del nodo actual
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

    /**
     * @brief   Calcular la funcion f=g+h para un nodo n
     * @param x coordenada x del nodo n
     * @param y coordenada y del nodo n
     * @return suma del coste del nodo inicial al nodo n y la distancia Manhattan del nodo n al objetivo
     */
    private int f(int x, int y) {
        return this.g[x][y] + (int)(Math.abs(x - this.portal.x) + Math.abs(y - this.portal.y));
    }

    /**
     * @brief           Comprobar si la representacion del mundo ha cambiado
     * @param stateObs  Estado de observacion del juego
     * @return          True si ha cambiado el mundo, False de lo contrario
     */
    private boolean worldHasChanged(StateObservation stateObs) {
        // Obtener paredes y trampas
        ArrayList<Observation>[] immPositions = stateObs.getImmovablePositions();

        // si el numero de paredes y trampas cambiaron, entonces el mundo ha cambiado!
        return this.n_invalid != immPositions[0].size() + immPositions[1].size();
    }

    /**
     * @brief           Actualizar la representacion del mundo del agente
     * @param stateObs  Estado de observacion del juego
     * @implNote        se modifica el atributo miembro this.invalid
     */
    private void updateInnerWorld(StateObservation stateObs) {
        ArrayList<Observation>[] immPositions = stateObs.getImmovablePositions();
        ArrayList<Observation> wallsAux = immPositions[0];
        ArrayList<Observation> trapsAux = immPositions[1];

        // Aumentamos el numero de casillas invalidas
        this.n_invalid = wallsAux.size() + trapsAux.size();

        // Marcamos como casillas invalidas las paredes y trampas //
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
