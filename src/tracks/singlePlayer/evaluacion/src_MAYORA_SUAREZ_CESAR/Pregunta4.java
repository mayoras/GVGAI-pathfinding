package tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR;

import tools.Utils;
import tools.Vector2d;
import tracks.ArcadeMachine;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Pregunta4 {
    public static final String MAP_FILEPATH = "./examples/gridphysics/labyrinth_lvl6.txt";
    public static final String TEMP_MAP_FILEPATH = "./examples/gridphysics/labyrinth_tmp.txt";
    public static final int NUM_EXECUTIONS = 100;

    public String[][] map;
    public BufferedWriter writer;
    public BufferedReader reader;
    public static void main(String[] args) {
        String rtaStarController = "tracks.singlePlayer.evaluacion.src_MAYORA_SUAREZ_CESAR.AgenteRTAStar";

        // cargamos los juegos
        String spGamesCollection =  "examples/all_games_sp.csv";
        String[][] games = Utils.readGames(spGamesCollection);

        // Inicializamos semilla
        int seed = new Random().nextInt();

        // Jugamos al juego labyrinth, mapa 6
        int gameIdx = 58;
        int levelIdx = 6; // level names from 0 to 4 (game_lvlN.txt).
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

        // Obtener un vector de posiciones aleatorias para posicionar al jugador
        ArrayList<Vector2d> randomPositions = getRandomValidPositions(validPositions, seed);

        // Por cada posicion aleatoria, ejecutamos el algoritmo RTA*
        for (int i = 0; i < randomPositions.size(); ++i) {
            // Elegimos una posicion al azar
            Vector2d randPos = randomPositions.get(i);

            // Modificamos el mapa cambiando la posicion del avatar a la posicion nueva
            changeAvatarPosition(randPos, rows);

            // Ejecutar algoritmo, con la nueva posicion
            ArcadeMachine.runOneGame(game, level_tmp, false, rtaStarController, null, seed, 0);
        }
    }

    public static ArrayList<String> generateRowList() {
        // Crear un descriptor para leer del fichero temporal
        try {
            ArrayList<String> rows = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(TEMP_MAP_FILEPATH));

            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(line);
            }
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
                if (c == '.' || c == 'A') {
                    validPositions.add(new Vector2d(i, j));
                }
            }
        }
        return validPositions;
    }

    public static ArrayList<Vector2d> getRandomValidPositions(ArrayList<Vector2d> validPositions, long seed) {
        ArrayList<Vector2d> randomPositions = new ArrayList<>();

        // Defino el rango de valores
        int min = 0, max = validPositions.size() - 1;

        // Genero una posicion aleatoria del avatar tantas veces como ejecuciones se realizaran
        Random random = new Random(seed);
        for (int i = 0; i < NUM_EXECUTIONS; ++i) {
            // https://stackoverflow.com/questions/363681/how-do-i-generate-random-integers-within-a-specific-range-in-java#363692
            int randomIdx = random.nextInt(max - min + 1) + min;
            randomPositions.add(validPositions.get(randomIdx));
        }
        return randomPositions;
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