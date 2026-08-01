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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.GameMap;
import core.Game;
import core.resources.Music;
import core.events.*;
import static core.Config.GAME_MAP_DIRECTORY;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Charge automatiquement tous les events depuis le dossier "cartes" et les injecte dans les cartes correspondantes
 */
public final class LoadEvents {

	private LoadEvents() { throw new AssertionError("La classe LoadEvents ne doit pas être instanciée."); } // Empêche toute instanciation

	/** Structure de données pour un event **/
	private record EventData(String gameMapName, int xCell, int yCell, Event event) {}

	/** Méthodes static **/
	private static List<EventData> getEvents(Game game) {
		final List<EventData> events = new ArrayList<>();
		try {
			final Path gameMapDirectory = Path.of(GAME_MAP_DIRECTORY);
			if (!Files.exists(gameMapDirectory) || !Files.isDirectory(gameMapDirectory)) return List.of();

			try (DirectoryStream<Path> files = Files.newDirectoryStream(gameMapDirectory, "*_ME.json")) {
				for (final Path file : files) {
					if (!Files.isRegularFile(file)) continue;
					final String fileName = file.getFileName().toString();
					if (fileName.length() <= "_ME.json".length()) continue;
					readJsonFile(file, game, events);
				}
			}
		} catch (IOException e) { throw new IllegalArgumentException("[ERREUR] Lecture des fichiers d'événements : " + e.getMessage(), e); }
		return events;
	}

	private static String extractGameMapName(Path path) {
		final String fileName = path.getFileName().toString();
		final String suffix = "_ME.json";

		return fileName.endsWith(suffix)
				? fileName.substring(0, fileName.length() - suffix.length())
				: fileName;
	}

	private static void readJsonFile(Path path, Game game, List<EventData> events) {
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { // try-with-resources
			final JsonElement rootElement = JsonParser.parseReader(reader);
			if (!rootElement.isJsonObject()) {
				System.err.println("[ERREUR] Racine JSON invalide dans " + path.getFileName());
				return;
			}

			final JsonObject root = rootElement.getAsJsonObject();
			if (!JsonUtil.isArrayKey(root, "ensemblesEvents")) return;

			final JsonArray eventGroups = root.getAsJsonArray("ensemblesEvents");
			if (eventGroups.isEmpty()) return;

			final String gameMapName = extractGameMapName(path);
			for (final JsonElement blocElem : eventGroups) {
				if (!blocElem.isJsonObject()) {
					System.err.println("[ERREUR] Bloc d'événement invalide ignoré dans " + path.getFileName());
					continue;
				}
				final JsonObject bloc = blocElem.getAsJsonObject();
				if (!JsonUtil.isIntKey(bloc, "x") || !JsonUtil.isIntKey(bloc, "y") || !JsonUtil.isArrayKey(bloc, "events")) {
					System.err.println("[ERREUR] Bloc d'événement incomplet ignoré dans " + path.getFileName());
					continue;
				}
				final int xCell = bloc.get("x").getAsInt();
				final int yCell = bloc.get("y").getAsInt();
				final JsonArray arr = bloc.getAsJsonArray("events");
				if (arr.isEmpty()) continue;

				for (final JsonElement evElem : arr) {
					if (!evElem.isJsonObject()) {
						System.err.println("[ERREUR] Événement invalide ignoré (" + path.getFileName() + " [" + xCell + "," + yCell + "])");
						continue;
					}
					final JsonObject ev = evElem.getAsJsonObject();
					if (!JsonUtil.isStringKey(ev, "type")) {
						System.err.println("[ERREUR] Événement sans type ignoré (" + path.getFileName() + " [" + xCell + "," + yCell + "])");
						continue;
					}

					final String type = ev.get("type").getAsString();
					switch (type) {
						case "MSG" -> {
							if (!JsonUtil.isStringKey(ev, "texte")) {
								System.err.println("[AVERTISSEMENT] MSG sans texte ignoré dans " + path.getFileName());
								continue;
							}
							final String msg = ev.get("texte").getAsString();
							events.add(new EventData(gameMapName, xCell, yCell, new Event_MSG(msg)));
						}
						case "TP" -> {
							if (!JsonUtil.isIntKey(ev, "xDst") || !JsonUtil.isIntKey(ev, "yDst") || !JsonUtil.isStringKey(ev, "carteDst")) {
								System.err.println("[AVERTISSEMENT] TP incomplet ignoré dans " + path.getFileName());
								continue;
							}
							final int xCellDst = ev.get("xDst").getAsInt();
							final int yCellDst = ev.get("yDst").getAsInt();
							final String gameMapDstName = ev.get("carteDst").getAsString();
							final GameMap gameMapDst = game.getGameMap(gameMapDstName);
							if (gameMapDst == null) {
								System.err.println("[AVERTISSEMENT] Carte de destination introuvable \"" + gameMapDstName + "\" dans " + path.getFileName());
								continue;
							}
							events.add(new EventData(gameMapName, xCell, yCell, new Event_TP(xCellDst, yCellDst, gameMapDst)));
						}
						case "JouerMusique" -> {
							if (!JsonUtil.isStringKey(ev, "nom")) {
								System.err.println("[AVERTISSEMENT] JouerMusique sans nom de musique ignoré dans " + path.getFileName());
								continue;
							}
							final String musicName = ev.get("nom").getAsString();
							final Music music = game.getMusic(musicName);
							if (music == null) {
								System.err.println("[AVERTISSEMENT] Musique introuvable \"" + musicName + "\" dans " + path.getFileName());
								continue;
							}
							events.add(new EventData(gameMapName, xCell, yCell, new Event_JM(music)));
						}
						case "ArretMusique" -> events.add(new EventData(gameMapName, xCell, yCell, new Event_AM()));
						case "PV" -> {
							if (!JsonUtil.isIntKey(ev, "valeur")) {
								System.err.println("[AVERTISSEMENT] modifyHP sans valeur ignoré dans " + path.getFileName());
								continue;
							}
							final int value = ev.get("valeur").getAsInt();
							events.add(new EventData(gameMapName, xCell, yCell, new Event_ModifyHP(value)));
						}
						case "PM" -> {
							if (!JsonUtil.isIntKey(ev, "valeur")) {
								System.err.println("[AVERTISSEMENT] modifyMP sans valeur ignoré dans " + path.getFileName());
								continue;
							}
							final int value = ev.get("valeur").getAsInt();
							events.add(new EventData(gameMapName, xCell, yCell, new Event_ModifyMP(value)));
						}
						case "LVLUP" -> events.add(new EventData(gameMapName, xCell, yCell, new Event_LVLUP()));
						default -> System.err.println("[AVERTISSEMENT] Type inconnu \"" + type + "\" ignoré dans " + path.getFileName());
					}
				}
			}
		} catch (Exception e) {
			System.err.println("[ERREUR] Fichier " + path + " : " + e.getMessage());
		}
	}

	private static void addEvent(EventData elem, Game game) {
		if (elem.event() == null) throw new IllegalArgumentException("[ERREUR] Event null");
		final GameMap gameMap = game.getGameMap(elem.gameMapName());
		if (gameMap == null) throw new IllegalArgumentException("[ERREUR] Carte introuvable : \"" +  elem.gameMapName() + "\" pour y insérer un event");
		gameMap.addEvent(0, elem.xCell(), elem.yCell(), elem.event());
	}

	public static void inject(Game game) {
		Objects.requireNonNull(game, "Game null");
		getEvents(game).forEach(elem -> addEvent(elem, game));
	}
}