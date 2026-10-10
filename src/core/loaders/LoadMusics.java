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

import core.resources.Music;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit et retourne la map complète (nom -> musique) des musiques du jeu.
 */
public final class LoadMusics {

	private LoadMusics() { throw new AssertionError("La classe LoadMusics ne doit pas être instanciée."); } // Empêche toute instanciation

	private static final List<String> MUSICS_FILENAMES_LIST = List.of( // Création des musiques : "fileName"
		"Castle_1.ogg",                                      // 0
		"Sarosa.ogg",                                                  // 1
		"bahamut_lagoon.ogg",                                          // 2
		"Castle_3.ogg",                                                // 3
		"2000_ordeal.ogg",                                             // 4
		"cc_viper_manor.ogg",                                          // 5
		"suikoden-ii-two-rivers.ogg",                                  // 6
		"mystery3.ogg",                                                // 7
		"hunter.ogg",                                                  // 8
		"illusionary_world.ogg",                                       // 9
		"chapt1medfill.ogg"                                            // 10
	);

	/** Getters **/
	public static Map<String, Music> load() {
		final Map<String, Music> musics = new HashMap<>(MUSICS_FILENAMES_LIST.size());
		try {
			for (String fileName : MUSICS_FILENAMES_LIST) {
				final Music music = new Music(fileName);
				final Music previous = musics.putIfAbsent(music.getName(), music);
				if (previous != null) {
					music.close();
					throw new IllegalStateException("Musique dupliquée : " + music.getName());
				}
			}
			return Map.copyOf(musics);
		} catch (RuntimeException e) {
			musics.values().forEach(Music::close);
			throw e;
		}
	}
}