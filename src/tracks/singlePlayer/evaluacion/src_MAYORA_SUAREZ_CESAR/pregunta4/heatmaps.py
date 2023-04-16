#!/usr/bin/env python3
import os
import numpy as np
import matplotlib.pyplot as plt

RTA_HEUR_FILENAME = "/home/cam/Classes/TSI/prac/GVGAI-pathfinding/src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/pregunta4/heur_rta.txt"
LRTA_HEUR_FILENAME = "/home/cam/Classes/TSI/prac/GVGAI-pathfinding/src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/pregunta4/heur_lrta.txt"
ASTAR_HEUR_FILENAME = "/home/cam/Classes/TSI/prac/GVGAI-pathfinding/src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/pregunta4/heur_astar.txt"

HEATMAP_RTA_FILENAME = "/home/cam/Classes/TSI/prac/GVGAI-pathfinding/src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/rta_heatmap.png"
HEATMAP_LRTA_FILENAME = "/home/cam/Classes/TSI/prac/GVGAI-pathfinding/src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/lrta_heatmap.png"
HEATMAP_ASTAR_FILENAME = "/home/cam/Classes/TSI/prac/GVGAI-pathfinding/src/tracks/singlePlayer/evaluacion/src_MAYORA_SUAREZ_CESAR/astar_heatmap.png"

# INF = 900


def gen_heatmap_as_png(dst_path, src_path, title):
    # Leemos la matrix de valores heuristicos de src_path
    z = []
    with open(src_path, "r") as f:
        for line in f:
            line = line.split(",")
            h_values = []
            for val in line:
                h_values.append(int(val))
            z.append(h_values)
        z = np.array(z, dtype=np.int32)

    # Defino los parametros del mapa
    xmin, xmax, ymin, ymax = 0, 16, 0, 14

    # Generamos el grid
    x, y = np.meshgrid(np.arange(xmin, xmax), np.arange(ymin, ymax))

    # Normalizamos la matrix de heuristicas
    min_h, max_h = np.min(z), np.max(z)
    norm_z = (z - min_h) / (max_h - min_h)

    # Creamos el plot
    fig, ax = plt.subplots()
    grid_image = ax.pcolormesh(x, np.flip(y, axis=0), norm_z, shading="auto")
    fig.colorbar(grid_image)

    # Ponemos las etiquetas y el titulo
    ax.set_xlabel("X")
    ax.set_ylabel("Y")
    ax.set_title(title)

    # Exportar a una imagen png
    plt.savefig(dst_path, format="png")


def main():

    # Generamos el heatmap de RTA*, LRTA* y A*
    gen_heatmap_as_png(HEATMAP_RTA_FILENAME, RTA_HEUR_FILENAME, "RTA* Heatmap")
    gen_heatmap_as_png(HEATMAP_LRTA_FILENAME, LRTA_HEUR_FILENAME, "LRTA* Heatmap")
    gen_heatmap_as_png(HEATMAP_ASTAR_FILENAME, ASTAR_HEUR_FILENAME, "A* Heatmap")


if __name__ == "__main__":
    main()
