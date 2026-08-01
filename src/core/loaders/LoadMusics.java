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

	private static final List<Music> MUSICS_LIST = List.of( // Création des musiques : new Music(fileName)
		new Music("Castle_1.ogg"),                            // 0
		new Music("Sarosa.ogg"),                              // 1
		new Music("bahamut_lagoon.ogg"),                      // 2
		new Music("Castle_3.ogg"),                            // 3
		new Music("2000_ordeal.ogg"),                         // 4
		new Music("cc_viper_manor.ogg"),                      // 5
		new Music("suikoden-ii-two-rivers.ogg"),              // 6
		new Music("mystery3.ogg"),                            // 7
		new Music("hunter.ogg"),                              // 8
		new Music("illusionary_world.ogg"),                   // 9
		new Music("chapt1medfill.ogg")                        // 10
	);

	/** Getters **/
	public static Map<String, Music> get() {
		final Map<String, Music> musics = new HashMap<>(MUSICS_LIST.size());
		MUSICS_LIST.forEach(music -> musics.put(music.getName(), music));
		return Map.copyOf(musics);
	}
}