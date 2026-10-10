/**
 * @author Alain Barbier
 * Copyright © 2026 Alain Barbier (alias Metroidzeta) - All rights reserved.
 *
 * This file is part of the project covered by the
 * "Educational and Personal Use License / Licence d’Utilisation Personnelle et Éducative".
 *
 * Permission is granted to fork and use this code for educational and personal purposes only.
 *
 * Commercial use, redistribution, or public republishing of modified versions
 * is strictly prohibited without the express written consent of the author.
 */
package core;

import core.events.Event;
import core.resources.Music;
import static core.Config.DEBUG_MODE;
import static core.Config.CELL_SIZE;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Représente une carte (niveau du jeu) composée de plusieurs couches de tuiles,
 * avec une largeur et une hauteur fixes, un chipset, une musique et des événements associés.
 */
public final class GameMap {
	private record CellCoord(int xCell, int yCell) {}
	public static final class CellBounds {
		private int x0, x1, y0, y1;

		public void set(int x0, int x1, int y0, int y1) {
			this.x0 = x0; this.x1 = x1; this.y0 = y0; this.y1 = y1;
		}

		public int x0() { return x0; }
		public int x1() { return x1; }
		public int y0() { return y0; }
		public int y1() { return y1; }
	}

	public static final int MATRIX_SIZE_MAX = 100; // n*n, par défaut : 100
	private static final int LAYERS_COUNT = 3; // par défaut : 3
	private static final Rectangle[][] GLOBAL_RECT_MATRIX = createGlobalRectMatrix(); // Matrice de rectangles globale représentant les cases (partagée entre toutes les cartes)

	private final String name;
	private final int width, height; // largeur * hauteur en cases
	private final Chipset chipset;
	private final Music music;
	private final int[][][] layers; // 3 couches (matrices) de numTileChipset (C0 < C1 < Héros < C2)
	private final boolean[][] walls; // Matrice booléenne représentant les murs sur chaque case (false = pas de mur, true = mur)
	private final Map<CellCoord, EventGroup> eventsGroups = new HashMap<>(); // Events par cases
	private final ArrayList<Enemy> enemies = new ArrayList<>();

	/** Méthodes static **/
	private static boolean hasDimensions(int[][] matrix, int width, int height) {
		if (matrix == null || matrix.length != height) return false;
		for (final int[] row : matrix) {
			if (row == null || row.length != width) return false;
		}
		return true;
	}

	private static boolean hasDimensions(boolean[][] matrix, int width, int height) {
		if (matrix == null || matrix.length != height) return false;
		for (final boolean[] row : matrix) {
			if (row == null || row.length != width) return false;
		}
		return true;
	}

	private static void validateLayerTileNumbers(int[][] layer, int layerIndex, String gameMapName, Chipset chipset) {
		for (int row = 0; row < layer.length; row++) {
			for (int column = 0; column < layer[row].length; column++) {
				final int tileNumber = layer[row][column];

				if (!chipset.isValidLayerTileNumber(tileNumber)) {
					throw new IllegalArgumentException(
						(
							"Carte[%s] : numéro de tuile invalide %d dans la couche %d "
							+ "à la position [colonne=%d, ligne=%d] | "
							+ "valeur attendue entre %d et %d"
						).formatted(
							gameMapName, tileNumber, layerIndex,
							column, row,
							Chipset.EMPTY_TILE, chipset.getTileCount()
						)
					);
				}
			}
		}
	}

	private static Rectangle[][] createGlobalRectMatrix() {
		final Rectangle[][] matrix = new Rectangle[MATRIX_SIZE_MAX][MATRIX_SIZE_MAX];
		for (int i = 0; i < MATRIX_SIZE_MAX; i++) {
			final int y = i * CELL_SIZE;
			for (int j = 0; j < MATRIX_SIZE_MAX; j++) {
				matrix[i][j] = new Rectangle(j * CELL_SIZE, y, CELL_SIZE, CELL_SIZE);
			}
		}
		return matrix;
	}

	/** Constructeur **/
	public GameMap(String name, int width, int height, Chipset chipset, Music music, int[][] l0, int[][] l1, int[][] l2, boolean[][] walls) {
		this.name = Util.requireNonBlank(name, "Nom carte");
		if (width < 1 || width > MATRIX_SIZE_MAX)
			throw new IllegalArgumentException("Carte[%s] : largeur invalide %d ; valeur attendue entre 1 et %d".formatted(name, width, MATRIX_SIZE_MAX));
		if (height < 1 || height > MATRIX_SIZE_MAX)
			throw new IllegalArgumentException("Carte[%s] : hauteur invalide %d ; valeur attendue entre 1 et %d".formatted(name, height, MATRIX_SIZE_MAX));
		this.width = width;
		this.height = height;
		this.chipset = Objects.requireNonNull(chipset, "Carte[" + name + "]: chipset null passé en paramètre");
		this.music = music;
		this.layers = new int[LAYERS_COUNT][][];
		final int[][][] srcLayers = {l0, l1, l2};
		for (int l = 0; l < LAYERS_COUNT; l++) {
			final int[][] source = srcLayers[l];
			if (source == null || source.length == 0) {
				this.layers[l] = Util.createIntMatrix(height, width, Chipset.EMPTY_TILE);
			} else {
				if (!hasDimensions(source, width, height)) {
					throw new IllegalArgumentException("Carte[%s] : dimensions invalides pour la couche %d".formatted(name, l));
				}
				validateLayerTileNumbers(source, l, this.name, this.chipset);
				this.layers[l] = Util.copyMatrix(source);
			}
		}
		if (walls == null || walls.length == 0) {
			this.walls = new boolean[height][width];
		} else {
			if (!hasDimensions(walls, width, height)) {
				throw new IllegalArgumentException("Carte[" + this.name + "] : dimensions invalides pour la matrice de murs");
			}
			this.walls = Util.copyMatrix(walls);
		}
	}

	public static GameMap newEmptyGameMap(String name, int width, int height, Chipset chipset, Music music) {
		return new GameMap(name, width, height, chipset, music, null, null, null, null);
	}

	private boolean isInvalidMatrixIndex(int i, int j) { return i < 0 || i >= height || j < 0 || j >= width; }

	private CellBounds getCellBounds(Rectangle rect) {
		Objects.requireNonNull(rect, "Rectangle null");

		CellBounds cellBounds = new CellBounds();
		cellBounds.set(
				Math.max(0, rect.x / CELL_SIZE),
				Math.min(width, (rect.x + rect.width - 1) / CELL_SIZE + 1),
				Math.max(0, rect.y / CELL_SIZE),
				Math.min(height, (rect.y + rect.height - 1) / CELL_SIZE + 1)
		);

		return cellBounds;
	}

	/** Getters **/
	public String getName()     { return name; }
	public int getWidth()       { return width; }
	public int getHeight()      { return height; }
	public Chipset getChipset() { return chipset; }
	public Music getMusic()     { return music; }
	public int getNumTile(int layerIndex, int i, int j) {
		if (layerIndex < 0 || layerIndex >= LAYERS_COUNT) throw new IndexOutOfBoundsException("LayerIndex < 0 ou >= " + LAYERS_COUNT);
		if (isInvalidMatrixIndex(i, j)) throw new IndexOutOfBoundsException("i < 0 ou i >= " + height + " ou j < 0 ou j >= " + width);
		return layers[layerIndex][i][j];
	}
	public boolean isWall(int i, int j) {
		if (isInvalidMatrixIndex(i, j)) throw new IndexOutOfBoundsException("i < 0 ou i >= " + height + " ou j < 0 ou j >= " + width);
		return walls[i][j];
	}
	public ArrayList<Enemy> getEnemies() { return enemies; }

	/** Autres méthodes **/
	// --- Collisions murs ---
	public boolean hasWallCollision(Rectangle rect) {
		final CellBounds b = getCellBounds(rect);
		if (DEBUG_MODE) System.out.printf("CellBounds xCells: %d-%d, yCells: %d-%d%n", b.x0(), b.x1(), b.y0(), b.y1());

		for (int y = b.y0(); y < b.y1(); y++) {
			for (int x = b.x0(); x < b.x1(); x++) {
				if (walls[y][x] && rect.intersects(GLOBAL_RECT_MATRIX[y][x])) return true;
			}
		}
		return false;
	}

	// --- Collisions events ---
	public EventGroup findCollidingEvents(Rectangle rect) {
		final CellBounds b = getCellBounds(rect);

		for (int y = b.y0(); y < b.y1(); y++) {
			for (int x = b.x0(); x < b.x1(); x++) {
				final EventGroup group = eventsGroups.get(new CellCoord(x, y));
				if (group != null && rect.intersects(GLOBAL_RECT_MATRIX[y][x])) return group;
			}
		}
		return null;
	}

	// --- Ajouter events ---
	public void addEvent(int numPage, int xCell, int yCell, Event ev) {
		if (numPage < 0) {
			throw new IllegalArgumentException("Numéro de page négatif : " + numPage);
		}
		if (isInvalidMatrixIndex(yCell, xCell)) {
			throw new IllegalArgumentException("Carte[%s]: coordonnées d'événement hors limites [xCell:%d, yCell:%d]".formatted(name, xCell, yCell));
		}
		Objects.requireNonNull(ev, "Event null ajouté à la carte " + name);
		eventsGroups.computeIfAbsent(new CellCoord(xCell, yCell), coord -> new EventGroup()).add(numPage, ev);
	}

	public void addEnemy(Enemy enemy) {
		Objects.requireNonNull(enemy, "Enemy null ajouté à la carte " + name);
		final int xCell = enemy.getXCell(), yCell = enemy.getYCell();
		if (isInvalidMatrixIndex(yCell, xCell)) {
			throw new IllegalArgumentException("Carte[%s]: coordonnées de l'ennemi hors limites [xCell:%d, yCell:%d]".formatted(name, xCell, yCell));
		}
		enemies.add(enemy);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof GameMap gameMap)) return false;

		return name.equals(gameMap.name)
			&& width == gameMap.width
			&& height == gameMap.height
			&& Objects.equals(chipset, gameMap.chipset)
			&& Objects.equals(music, gameMap.music)
			&& Arrays.deepEquals(layers, gameMap.layers)
			&& Arrays.deepEquals(walls, gameMap.walls);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, width, height, chipset, music, Arrays.deepHashCode(layers), Arrays.deepHashCode(walls));
	}
}