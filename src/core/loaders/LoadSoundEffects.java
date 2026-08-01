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

	private static final List<SoundEffect> SOUND_EFFECTS_LIST = List.of( // Création des bruitages : new SoundEffect(fileName)
		new SoundEffect("Blow1.wav"),                                // 0
		new SoundEffect("Kill1.wav")                                 // 1
	);

	/** Getters **/
	public static Map<String, SoundEffect> get() {
		final Map<String, SoundEffect> soundEffects = new HashMap<>(SOUND_EFFECTS_LIST.size());
		SOUND_EFFECTS_LIST.forEach(soundEffect -> soundEffects.put(soundEffect.getName(), soundEffect));
		return Map.copyOf(soundEffects);
	}
}