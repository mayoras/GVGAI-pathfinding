package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR.pregunta4;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class AgenteLRTAStarP4 extends AbstractPlayer {
    // Acciones que puede tomar el agente
    public static ACTIONS[] EXPANDED_ACTIONS = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

    // Fichero que contiene la informacion heuristica del agente
    public static String HEUR_FILEPATH = "./src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/pregunta4/heur_lrta.txt";

    // Factor de escala
    Vector2d scaleF;

    // Posicion del portal
    Vector2d portal;

    // Matrix booleana de posiciones invalidas.
    // Posiciones invalidas son las posiciones en las que el jugador no puede poner un pie (paredes y trampas)
    boolean[][] invalid;

    // Numero de casillas invalidas
    int n_invalid;

    // Numero de nodos expandidos
    int expandedNodes;

    // Longitud del camino que esta tomando el agente
    int pathLength;

    // Runtime acumulado en nanosegundos que toma el agente hacia el portal
    long totalRuntime;

    // Numero de celdas por fila y columna
    int nx, ny;

    // Matrix de valores heuristicos
    int[][] h;

    public AgenteLRTAStarP4(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Inicializamos el factor de escala
        this.scaleF = new Vector2d((float)stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                (float)stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        // Obtenemos la cantidad de casillas de ancho y alto
        this.nx = stateObs.getObservationGrid().length;
        this.ny = stateObs.getObservationGrid()[0].length;

        this.invalid = new boolean[this.nx][this.ny];

        File file = new File(HEUR_FILEPATH);
        if (file.exists()) {
            this.h = readHeuristicsFile();
        } else {
            this.h = createNewHeuristicsFile(file);

            // Valor heuristico por defecto -> -1
            for (int i = 0; i < this.ny; ++i) {
                Arrays.fill(this.h[i], -1);
            }
        }

        ////////////////// Guardar las posiciones de las paredes y trampas /////////////////
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

        // Guardar la posicion del portal
        ArrayList<Observation>[] positions = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

        portal = positions[0].get(0).position;
        portal.x = Math.floor(portal.x / scaleF.x);
        portal.y = Math.floor(portal.y / scaleF.y);

        // Plan inicial vacio, 0 nodos expandidos y longitud inicial 0
        this.expandedNodes = 0;
        this.pathLength = 0;
        this.totalRuntime = 0;
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Inicializo el contador del act
        long start = System.nanoTime();

        // Obtener la posicion inicial del avatar
        Vector2d curr = new Vector2d((int)Math.floor(stateObs.getAvatarPosition().x / scaleF.x),
                                    (int)Math.floor(stateObs.getAvatarPosition().y / scaleF.y));
        int currX = (int)curr.x, currY = (int)curr.y;

        // El nodo actual es un nodo que ha sido expandido (Guion seccion 6)
        ++this.expandedNodes;

        // Comprobar si el mundo ha cambiado (Labyrinth Extended)
        if (worldHasChanged(stateObs)) {
            updateInnerWorld(stateObs);
        }

        // Inicializar el valor heuristico del nodo inicial
        if (this.h[currY][currX] < 0) {
            // Trasponemos las coordenadas x,y a y,x para que la matrix h vaya acorde a los puntos del mapa
            this.h[currY][currX] = manhattanDistance(currX, currY);
        }

        // Expandir el nodo actual
        Vector2d next = new Vector2d();
        int minF = Integer.MAX_VALUE;
        int secondMinF = Integer.MAX_VALUE;
        ACTIONS bestAction = ACTIONS.ACTION_NIL;
        for (ACTIONS action : AgenteLRTAStarP4.EXPANDED_ACTIONS) {
            // Obtenemos el nuevo sucesor. Modificamos next
            nextSuccessor(action, next, currX, currY);
            int nextX = (int)next.x, nextY = (int)next.y;

            // si es una pared o trampa ni te molestes, ignoralo
            if (this.invalid[nextX][nextY]) continue;

            // Comprobar si el vecino es el objetivo, si es asi, entonces ir hacia el
            if (next.x == this.portal.x && next.y == this.portal.y) {
                ++this.expandedNodes;       // Sumamos el ultimo nodo de expandidos antes de ir al objetivo

                // Agregar el tiempo restante
                long end = System.nanoTime();
                this.totalRuntime += (end - start);

                // agregar el ultimo nodo final a la longitud del camino
                ++this.pathLength;

                // Para tener informacion heuristica completa del grafo antes de guardar en disco,
                // asignamos heuristica 0 al nodo objetivo
                this.h[nextY][nextX] = 0;

                // Sobreescribimos los valores heuristicos en disco antes de terminar
                writeToHeuristicsFile();

                return action;
            }

            // si es un nuevo nodo no descubierto, calcular y almacenar su heuristica h(next)
            if (this.h[nextY][nextX] < 0)
                this.h[nextY][nextX] = manhattanDistance(nextX, nextY);


            // Obtener el primer y segundo valor f minimo de los vecinos
            int f = this.h[nextY][nextX] + 1;
            if (minF > f) {
                secondMinF = minF;
                minF = f;
                bestAction = action;
            } else if (f < secondMinF) {
                secondMinF = f;
            }
        }

        // actualizar la heuristica del nodo actual como el maximo de { 2do minimo valor f, h(actual) }
        if (minF > this.h[currY][currX])
            this.h[currY][currX] = minF;

        // Incrementar la longitud the camino tomado
        ++this.pathLength;

        // Parar el temporizador y acumular runtime
        long end = System.nanoTime();
        this.totalRuntime += (end - start);

        // tomar la mejor accion
        return bestAction;
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
     * @brief       Calcular distancia Manhattan de un nodo n al portal
     * @param x     coordenada x del nodo n
     * @param y     coordenada y del nodo n
     * @return      devuelve la distancia Manhattan del nodo n al portal
     */
    private int manhattanDistance(int x, int y) {
        return (int)(Math.abs(x - this.portal.x) + Math.abs(y - this.portal.y));
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
     * @implNote se modifica el atributo miembro this.invalid
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

    public int[][] readHeuristicsFile() {
        int[][] heur = new int[this.ny][this.nx];

        // Abrimos y leemos de fichero
        try {
            BufferedReader reader = new BufferedReader(new FileReader(HEUR_FILEPATH));

            // Leemos cada linea almacenando cada fila en la matrix de heuristicas
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                String[] rowValues = line.split(",");

                for (int i = 0; i < rowValues.length; ++i) {
                    String val = rowValues[i];
                    int h = Integer.parseInt(val);
                    heur[row][i] = h;
                }

                ++row;
            }

            reader.close();
        } catch (IOException e) {
            System.out.println("---ERROR in BufferedReader---");
            e.printStackTrace();
        }

        return heur;
    }

    public void writeToHeuristicsFile() {
        try {
            // Creamos un descriptor para sobreescribir en el fichero
            BufferedWriter writer = new BufferedWriter(new FileWriter(HEUR_FILEPATH, false));

            for (int i = 0; i < this.ny; ++i) {
                // Serializamos una fila de valores
                ArrayList<String> row = new ArrayList<>();
                for (int j = 0; j < this.nx; ++j) {
                    String hVal = String.valueOf(this.h[i][j]);
                    row.add(hVal);
                }

                // Escribimos la fila como una linea de valores separadas por comas
                String line = String.join(",", row);
                writer.write(line);
                writer.newLine();
            }

            writer.close();
        } catch (IOException e) {
            System.out.println("---ERROR in BufferedWriter---");
            e.printStackTrace();
        }
    }

    public int[][] createNewHeuristicsFile(File file) {
        try {
            if (file.createNewFile()) {
                System.out.println("File created successfully.");
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("---ERROR in createNewFile---");
            e.printStackTrace();
        }
        // Retornamos una matrix correspondiente a los valores heuristicos de cada punto del mapa
        return new int[this.ny][this.nx];
    }
}
