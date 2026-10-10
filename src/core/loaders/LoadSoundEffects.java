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

import core.resources.SoundEffect;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit et retourne la map complète (nom -> bruitage) des bruitages du jeu.
 */
public final class LoadSoundEffects {

	private LoadSoundEffects() { throw new AssertionError("La classe LoadSoundEffects ne doit pas être instanciée."); } // Empêche toute instanciation

	private static final List<String> SOUND_EFFECT_FILENAMES_LIST = List.of( // Création des bruitages : "fileName"
		"Blow1.wav",                                       // 0
		"Kill1.wav",                                       // 1
		"Damage3.ogg"                                      // 2
	);

	/** Getters **/
	public static Map<String, SoundEffect> load() {
		final Map<String, SoundEffect> soundEffects = new HashMap<>(SOUND_EFFECT_FILENAMES_LIST.size());
		try {
			for (String fileName : SOUND_EFFECT_FILENAMES_LIST) {
				final SoundEffect soundEffect = new SoundEffect(fileName);
				final SoundEffect previous = soundEffects.putIfAbsent(soundEffect.getName(), soundEffect);
				if (previous != null) {
					soundEffect.close();
					throw new IllegalStateException("Bruitage dupliqué : " + soundEffect.getName());
				}
			}
			return Map.copyOf(soundEffects);
		} catch (RuntimeException e) {
			soundEffects.values().forEach(SoundEffect::close);
			throw e;
		}
	}
}