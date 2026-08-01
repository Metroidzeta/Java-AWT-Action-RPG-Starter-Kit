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
package core.loaders;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.GameMap;
import core.Chipset;
import core.Game;
import core.resources.Music;
import static core.Config.GAME_MAP_DIRECTORY;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * Charge automatiquement toutes les cartes (données de base + couches + murs) depuis le dossier "cartes" et renvoie une map complète (nom -> carte)
 */
public final class LoadGameMaps {

	private LoadGameMaps() { throw new AssertionError("La classe LoadGameMaps ne doit pas être instanciée."); } // Empêche toute instanciation

	/** Structure de données pour une carte **/
	private record GameMapData(String gameMapName, int width, int height, String chipsetName, String musicName, int[][] l0, int[][] l1, int[][] l2, boolean[][] walls) {}

	private static final Gson GSON = new Gson();

	/** Méthodes static **/
	private static List<GameMapData> getGameMaps() {
		final List<GameMapData> gameMaps = new ArrayList<>();
		try {
			final Path gameMapDirectory = Path.of(GAME_MAP_DIRECTORY);
			if (!Files.exists(gameMapDirectory) || !Files.isDirectory(gameMapDirectory)) return List.of();

			try (DirectoryStream<Path> files = Files.newDirectoryStream(gameMapDirectory, "*_BC.json")) {
				for (final Path file : files) {
					if (!Files.isRegularFile(file)) continue;
					readJsonFile(file, gameMaps);
				}
			}
		} catch (IOException e) { throw new IllegalArgumentException("[ERREUR] Lecture des fichiers de cartes : " + e.getMessage(), e); }

		return gameMaps;
	}

	private static boolean hasValidMusic(JsonObject obj) {
		if (!obj.has("musique")) return false;
		final JsonElement music = obj.get("musique");
		return music.isJsonNull() || (music.isJsonPrimitive() && music.getAsJsonPrimitive().isString());
	}

	private static int[][] matrixHasValidDimensions(int[][] matrix, int height, int width, String gameMapName, String layerName) {
		if (matrix == null || matrix.length == 0) {
			throw new IllegalArgumentException("[ERREUR] " + gameMapName + " : " + layerName + " manquante ou invalide");
		}

		if (matrix.length != height) {
			throw new IllegalArgumentException(
					"[ERREUR] %s : hauteur de %s incorrecte (%d au lieu de %d)"
							.formatted(gameMapName, layerName, matrix.length, height)
			);
		}

		for (int i = 0; i < height; i++) {
			if (matrix[i] == null) {
				throw new IllegalArgumentException(
						"[ERREUR] %s : ligne %d de %s manquante ou invalide"
								.formatted(gameMapName, i, layerName)
				);
			}

			if (matrix[i].length != width) {
				throw new IllegalArgumentException(
						"[ERREUR] %s : largeur de %s incorrecte à la ligne %d (%d au lieu de %d)"
								.formatted(gameMapName, layerName, i, matrix[i].length, width)
				);
			}
		}

		return matrix;
	}

	private static boolean[][] convertWallsToBooleans(int[][] src, int height, int width) {
		final boolean[][] dst = new boolean[height][width];
		if (src == null) return dst;

		final int h = Math.min(height, src.length);
		for (int i = 0; i < h; i++) {
			final int w = Math.min(width, src[i].length);
			for (int j = 0; j < w; j++) {
				final int value = src[i][j];

				if (value != 0 && value != 1) {
					System.err.println("[ERREUR] Valeur de mur invalide " + value + " à la position [ligne: " + i + ", colonne: " + j + "]");
					continue;
				}

				dst[i][j] = (value == 1);
			}
		}
		return dst;
	}

	private static boolean[][] readWalls(String gameMapName, int height, int width) {
		final Path fileME = Path.of(GAME_MAP_DIRECTORY, gameMapName + "_ME.json");
		if (!Files.exists(fileME)) return new boolean[height][width];

		try (BufferedReader reader = Files.newBufferedReader(fileME, StandardCharsets.UTF_8)) { // try-with-resources
			final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			final int[][] wallsInt = matrixHasValidDimensions(GSON.fromJson(root.get("murs"), int[][].class), height, width, gameMapName, "murs");
			return convertWallsToBooleans(wallsInt, height, width);
		} catch (Exception e) {
			System.err.println("[ERREUR] Lecture murs de " + gameMapName + " : " + e.getMessage());
			return new boolean[height][width];
		}
	}

	private static String extractGameMapName(Path path, String suffix) {
		final String fileName = path.getFileName().toString();
		return fileName.endsWith(suffix) ? fileName.substring(0, fileName.length() - suffix.length()) : fileName;
	}

	private static void readJsonFile(Path path, List<GameMapData> gameMaps) {
		final String gameMapName = extractGameMapName(path, "_BC.json");

		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { // try-with-ressources
			final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			if (root == null || !JsonUtil.isIntKey(root, "largeur") || !JsonUtil.isIntKey(root, "hauteur")
				|| !JsonUtil.isStringKey(root, "chipset") || !hasValidMusic(root)) {
				throw new IllegalArgumentException("[ERREUR] Fichier " + gameMapName + " incomplet ou invalide (champs manquants).");
			}

			final int width = root.get("largeur").getAsInt();
			final int height = root.get("hauteur").getAsInt();
			if (width < 1 || width > GameMap.MATRIX_SIZE_MAX || height < 1 || height > GameMap.MATRIX_SIZE_MAX) {
				throw new IllegalArgumentException(
						"[ERREUR] %s : dimensions invalides (%d x %d), valeurs attendues entre 1 et %d"
								.formatted(gameMapName, width, height, GameMap.MATRIX_SIZE_MAX)
				);
			}

			final String chipsetName = root.get("chipset").getAsString();
			final String musicName = root.get("musique").isJsonNull() ? null : root.get("musique").getAsString();

			final int[][] l0 = matrixHasValidDimensions(GSON.fromJson(root.get("couche0"), int[][].class), height, width, gameMapName, "couche0");
			final int[][] l1 = matrixHasValidDimensions(GSON.fromJson(root.get("couche1"), int[][].class), height, width, gameMapName, "couche1");
			final int[][] l2 = matrixHasValidDimensions(GSON.fromJson(root.get("couche2"), int[][].class), height, width, gameMapName, "couche2");
			final boolean[][] walls = readWalls(gameMapName, height, width);

			gameMaps.add(new GameMapData(gameMapName, width, height, chipsetName, musicName, l0, l1, l2, walls));

		} catch (Exception e) {
			System.err.println("[ERREUR] Fichier " + path.getFileName() + " : " + e.getMessage());
		}
	}

	private static void addGameMap(GameMapData elem, Map<String, GameMap> gameMaps, Game game) {
		final Chipset chipset = game.getChipset(elem.chipsetName());
		if (chipset == null) throw new IllegalArgumentException("[ERREUR] Chipset \"" + elem.chipsetName() + "\" introuvable pour la carte \"" + elem.gameMapName() + "\"");

		Music music = null;
		if (elem.musicName() != null) {
			music = game.getMusic(elem.musicName());
			if (music == null) throw new IllegalArgumentException("[ERREUR] Musique \"" + elem.musicName() + "\" introuvable pour la carte \"" + elem.gameMapName() + "\"");
		}


		final GameMap gameMap = new GameMap(elem.gameMapName(), elem.width(), elem.height(), chipset, music, elem.l0(), elem.l1(), elem.l2(), elem.walls());
		gameMaps.put(elem.gameMapName(), gameMap);
	}

	/** Getters **/
	public static Map<String, GameMap> get(Game game) {
		Objects.requireNonNull(game, "Game ne peut pas être null");
		final List<GameMapData> gameMapsData = getGameMaps();
		final Map<String, GameMap> gameMaps = new HashMap<>(gameMapsData.size());
		gameMapsData.forEach(elem -> addGameMap(elem, gameMaps, game));
		return Map.copyOf(gameMaps);
	}
}