package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import tools.Utils;
import tools.Vector2d;
import tracks.ArcadeMachine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Random;

public class Pregunta4 {
    public static final String MAP_FILEPATH = "./examples/gridphysics/labyrinth_lvl6.txt";
    public static final String TEMP_MAP_FILEPATH = "./examples/gridphysics/labyrinth_tmp.txt";
    public static final String SCRIPT_FILENAME = "./src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/pregunta4/heatmaps.py";

    public String[][] map;
    public BufferedWriter writer;
    public BufferedReader reader;
    public static void main(String[] args) {
        String rtaStarController = "tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR.pregunta4.AgenteRTAStarP4";
        String lrtaStarController = "tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR.pregunta4.AgenteLRTAStarP4";
        String aStarController = "tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR.pregunta4.AgenteAStarP4";

        // cargamos los juegos
        String spGamesCollection =  "examples/all_games_sp.csv";
        String[][] games = Utils.readGames(spGamesCollection);

        // Inicializamos semilla
        int seed = new Random().nextInt();

        // Jugamos al juego labyrinth, mapa 6
        int gameIdx = 58;
        String gameName = games[gameIdx][1];
        String game = games[gameIdx][0];
        String level_tmp = game.replace(gameName, gameName + "_tmp");

        ////////////////// Copiar el mapa del laberinto nivel 6 ////////////////////
        try {
            Files.copy(Path.of(MAP_FILEPATH), Path.of(TEMP_MAP_FILEPATH), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("---ERROR in copy---");
            e.printStackTrace();
        }

        // Generamos una lista de filas del mapa para mejor manipulacion
        ArrayList<String> rows = generateRowList();
        if (rows == null) {
            System.out.println("File read is null.");
            System.exit(1);
        }

        // Obtener todas las posiciones validas
        ArrayList<Vector2d> validPositions = getValidPosition(rows);

        //////// Por cada posicion aleatoria, ejecutamos el algoritmo RTA* y guardamos los valores heuristicos /////////
        for (Vector2d pos : validPositions) {
            // Elegimos una posicion al azar
            // Modificamos el mapa cambiando la posicion del avatar a la posicion nueva
            changeAvatarPosition(pos, rows);

            // Ejecutar algoritmo, con la nueva posicion
            ArcadeMachine.runOneGame(game, level_tmp, false, rtaStarController, null, seed, 0);
        }
        //////// Por cada posicion aleatoria, ejecutamos el algoritmo LRTA* y guardamos los valores heuristicos /////////
        for (Vector2d pos : validPositions) {
            // Elegimos una posicion al azar
            // Modificamos el mapa cambiando la posicion del avatar a la posicion nueva
            changeAvatarPosition(pos, rows);

            // Ejecutar algoritmo, con la nueva posicion
            ArcadeMachine.runOneGame(game, level_tmp, false, lrtaStarController, null, seed, 0);
        }
        /////////////// Obtenemos los valores heuristicos con A* //////////////////
        for (Vector2d pos : validPositions) {
            // Elegimos una posicion al azar
            // Modificamos el mapa cambiando la posicion del avatar a la posicion nueva
            changeAvatarPosition(pos, rows);

            // Ejecutar algoritmo, con la nueva posicion
            ArcadeMachine.runOneGame(game, level_tmp, false, aStarController, null, seed, 0);
        }

        /////////////// Generar heatmaps //////////////
        // Ejecutamos el script de Python para generar las imagenes
        String[] command = new String[]{"python3", SCRIPT_FILENAME};
        ProcessBuilder pb = new ProcessBuilder(command);

        // Lanzamos un proceso que ejecute el script
        try {
            System.out.println("Generating Heatmaps...");
            Process process = pb.start();

            // Esperamos a que acabe y recuperamos su codigo de estado
            try {
                int _status = process.waitFor();
                System.out.println("Heatmaps generated.");
            } catch (InterruptedException e) {
                System.out.println("---ERROR in process.waitFor---");
            }
        } catch (IOException e) {
            System.out.println("---ERROR in start child process---");
        }

        /////////////// Remover ficheros auxiliares utilizados //////////////
        File astar = new File("/home/cam/Classes/TSI/prac/GVGAI-pathfinding/src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/pregunta4/heur_astar.txt");
        File rta = new File("/home/cam/Classes/TSI/prac/GVGAI-pathfinding/src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/pregunta4/heur_rta.txt");
        File lrta = new File("/home/cam/Classes/TSI/prac/GVGAI-pathfinding/src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/pregunta4/heur_lrta.txt");
        File lab_temp = new File("/home/cam/Classes/TSI/prac/GVGAI-pathfinding/examples/gridphysics/labyrinth_tmp.txt");

        if (astar.exists())
            astar.delete();
        if (rta.exists())
            rta.delete();
        if (lrta.exists())
            lrta.delete();
        if (lab_temp.exists())
            lab_temp.delete();
    }

    public static ArrayList<String> generateRowList() {
        // Crear un descriptor para leer del fichero temporal
        try {
            ArrayList<String> rows = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(TEMP_MAP_FILEPATH));

            // Añadir las linea que corresponde a una fila del mapa
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(line);
            }
            reader.close();
            return rows;
        } catch (IOException e) {
            System.out.println("---ERROR in generateRowList---");
            e.printStackTrace();
        }

        return null;
    }

    public static ArrayList<Vector2d> getValidPosition(ArrayList<String> rows) {
        ArrayList<Vector2d> validPositions = new ArrayList<>();
        for (int i = 0; i < rows.size(); ++i) {
            String row = rows.get(i);
            for (int j = 0; j < row.length(); ++j) {
                char c = row.charAt(j);
                // es valida la posicion si es un . o A
                if (c == '.' || c == 'A') {
                    validPositions.add(new Vector2d(i, j));
                }
            }
        }
        return validPositions;
    }

    public static void changeAvatarPosition(Vector2d pos, ArrayList<String> rows) {
        // Remove the old position
        for (int i = 0; i < rows.size(); ++i) {
            String row = rows.get(i);
            for (int j = 0; j < row.length(); ++j) {
                if (row.charAt(j) == 'A') {
                    char[] rowBytes = row.toCharArray();
                    rowBytes[j] = '.';
                    rows.set(i, new String(rowBytes));
                    break;
                }
            }
        }
        // Assign new position to pos
        for (int i = 0; i < rows.size(); ++i) {
            String row = rows.get(i);
            for (int j = 0; j < row.length(); ++j) {
                if (i == (int)pos.x && j == (int)pos.y) {
                    char[] rowBytes = row.toCharArray();
                    rowBytes[j] = 'A';
                    rows.set(i, new String(rowBytes));
                    break;
                }
            }
        }

        // Crear un descriptor para escribir en el fichero temporal
        try {
            // Abrimos el fichero
            FileWriter writer = new FileWriter(TEMP_MAP_FILEPATH, false);

            // Unir las filas en uno solo para escribir en fichero
            String fileContent = String.join("\n", rows);

            // Sobreescribimos el mapa con la nueva posicion
            writer.write(fileContent);

            // Cerramos el fichero
            writer.close();
        } catch (IOException e) {
            System.out.println("---ERROR in BufferedWriter---");
            e.printStackTrace();
        }
    }
}