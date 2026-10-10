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
import java.util.Map;
import java.util.Objects;

/**
 * Charge automatiquement tous les events depuis le dossier "cartes" et les injecte dans les cartes correspondantes
 */
public final class LoadEvents {

	private LoadEvents() { throw new AssertionError("La classe LoadEvents ne doit pas être instanciée."); } // Empêche toute instanciation

	/** Structure de données pour un event **/
	private record EventData(String gameMapName, int xCell, int yCell, Event event) {}

	/** Méthodes static **/
	private static List<EventData> getEvents(Map<String, GameMap> gameMaps, Map<String, Music> musics) {
		final List<EventData> events = new ArrayList<>();
		try {
			final Path gameMapDirectory = Path.of(GAME_MAP_DIRECTORY);
			if (!Files.exists(gameMapDirectory) || !Files.isDirectory(gameMapDirectory)) return List.of();

			try (DirectoryStream<Path> files = Files.newDirectoryStream(gameMapDirectory, "*_ME.json")) {
				for (final Path file : files) {
					if (!Files.isRegularFile(file)) continue;
					final String fileName = file.getFileName().toString();
					if (fileName.length() <= "_ME.json".length()) continue;
					readJsonFile(file, gameMaps, musics, events);
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

	private static void readJsonFile(Path path, Map<String, GameMap> gameMaps, Map<String, Music> musics, List<EventData> events) {
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { // try-with-resources
			final JsonElement rootElement = JsonParser.parseReader(reader);
			if (!rootElement.isJsonObject()) {
				throw new IllegalArgumentException("Racine JSON invalide dans " + path.getFileName());
			}

			final JsonObject root = rootElement.getAsJsonObject();
			if (!JsonUtil.isArrayKey(root, "ensemblesEvents")) {
				throw new IllegalArgumentException("Champ ensemblesEvents absent ou invalide dans " + path.getFileName());
			}

			final JsonArray eventGroups = root.getAsJsonArray("ensemblesEvents");
			if (eventGroups.isEmpty()) return;

			final String gameMapName = extractGameMapName(path);
			for (final JsonElement blocElem : eventGroups) {
				if (!blocElem.isJsonObject()) {
					throw new IllegalArgumentException("Bloc d'événements invalide dans " + path.getFileName());
				}
				final JsonObject bloc = blocElem.getAsJsonObject();
				if (!JsonUtil.isIntKey(bloc, "x") || !JsonUtil.isIntKey(bloc, "y") || !JsonUtil.isArrayKey(bloc, "events")) {
					throw new IllegalArgumentException("Bloc d'événements incomplet dans " + path.getFileName());
				}
				final int xCell = bloc.get("x").getAsInt();
				final int yCell = bloc.get("y").getAsInt();
				final JsonArray arr = bloc.getAsJsonArray("events");
				if (arr.isEmpty()) continue;

				for (final JsonElement evElem : arr) {
					if (!evElem.isJsonObject()) {
						throw new IllegalArgumentException("Event invalide dans " + path.getFileName() + " [" + xCell + ", " + yCell + "]");
					}
					final JsonObject ev = evElem.getAsJsonObject();
					if (!JsonUtil.isStringKey(ev, "type")) {
						throw new IllegalArgumentException("Event sans type dans " + path.getFileName() + " [" + xCell + ", " + yCell + "]");
					}

					final String type = ev.get("type").getAsString();
					switch (type) {
						case "MSG" -> {
							if (!JsonUtil.isStringKey(ev, "texte")) {
								throw new IllegalArgumentException("Event MSG sans texte dans " + path.getFileName());
							}
							final String msg = ev.get("texte").getAsString();
							events.add(new EventData(gameMapName, xCell, yCell, new Event_MSG(msg)));
						}
						case "TP" -> {
							if (!JsonUtil.isIntKey(ev, "xDst") || !JsonUtil.isIntKey(ev, "yDst") || !JsonUtil.isStringKey(ev, "carteDst")) {
								throw new IllegalArgumentException("Event TP incomplet dans " + path.getFileName());
							}
							final int xCellDst = ev.get("xDst").getAsInt();
							final int yCellDst = ev.get("yDst").getAsInt();
							final String gameMapDstName = ev.get("carteDst").getAsString();
							final GameMap gameMapDst = gameMaps.get(gameMapDstName);
							if (gameMapDst == null) {
								throw new IllegalArgumentException("Event TP: carte de destination introuvable \"" + gameMapDstName + "\" dans " + path.getFileName());
							}
							events.add(new EventData(gameMapName, xCell, yCell, new Event_TP(xCellDst, yCellDst, gameMapDst)));
						}
						case "JouerMusique" -> {
							if (!JsonUtil.isStringKey(ev, "nom")) {
								throw new IllegalArgumentException("Event JM sans nom de musique dans " + path.getFileName());
							}
							final String musicName = ev.get("nom").getAsString();
							final Music music = musics.get(musicName);
							if (music == null) {
								throw new IllegalArgumentException("Event JM: musique introuvable \"" + musicName + "\" dans " + path.getFileName());
							}
							events.add(new EventData(gameMapName, xCell, yCell, new Event_JM(music)));
						}
						case "ArretMusique" -> events.add(new EventData(gameMapName, xCell, yCell, new Event_AM()));
						case "PV" -> {
							if (!JsonUtil.isIntKey(ev, "valeur")) {
								throw new IllegalArgumentException("Event ModifyHP sans valeur dans " + path.getFileName());
							}
							final int value = ev.get("valeur").getAsInt();
							events.add(new EventData(gameMapName, xCell, yCell, new Event_ModifyHP(value)));
						}
						case "PM" -> {
							if (!JsonUtil.isIntKey(ev, "valeur")) {
								throw new IllegalArgumentException("Event ModifyMP sans valeur dans " + path.getFileName());
							}
							final int value = ev.get("valeur").getAsInt();
							events.add(new EventData(gameMapName, xCell, yCell, new Event_ModifyMP(value)));
						}
						case "LVLUP" -> events.add(new EventData(gameMapName, xCell, yCell, new Event_LVLUP()));
						default -> throw new IllegalArgumentException("Type d'événement inconnu : " + type + " dans " + path.getFileName());
					}
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Impossible de charger les événements du fichier : " + path.toAbsolutePath(), e);
		}
	}

	private static void addEvent(EventData elem, Map<String, GameMap> gameMaps) {
		if (elem.event() == null) throw new IllegalArgumentException("[ERREUR] Event null");
		final GameMap gameMap = gameMaps.get(elem.gameMapName());
		if (gameMap == null) throw new IllegalArgumentException("[ERREUR] Carte introuvable : \"" +  elem.gameMapName() + "\" pour y insérer un event");
		gameMap.addEvent(0, elem.xCell(), elem.yCell(), elem.event());
	}

	public static void load(Map<String, GameMap> gameMaps, Map<String, Music> musics) {
		Objects.requireNonNull(gameMaps, "Map: gameMaps null");
		Objects.requireNonNull(musics, "Map: musics null");
		getEvents(gameMaps, musics).forEach(elem -> addEvent(elem, gameMaps));
	}
}