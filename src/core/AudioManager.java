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

import core.resources.Music;
import core.resources.SoundEffect;

import java.util.Map;
import java.util.Objects;

public final class AudioManager {

	private final Map<String, SoundEffect> soundEffects;
	private Music currentMusic = null;

	public AudioManager(Map<String, SoundEffect> soundEffects) {
		this.soundEffects = Map.copyOf(Objects.requireNonNull(soundEffects, "soundEffects null"));
	}

	/** Autres méthodes **/
	public void playMusic(Music music) {
		if (Objects.equals(currentMusic, music)) return;
		stopMusic();
		if (music != null) {
			music.play();
			currentMusic = music;
		}
	}

	public void stopMusic() {
		if (currentMusic == null) return;
		currentMusic.stop();
		currentMusic = null;
	}

	public void playSound(String name) {
		SoundEffect sound = soundEffects.get(Objects.requireNonNull(name, "soundEffect name null"));

		if (sound == null) {
			System.err.println("Missing sound effect: " + name);
			return;
		}

		sound.play();
	}

	public void freeAudioResources() {
		stopMusic();
		soundEffects.values().forEach(SoundEffect::close);
		SoundEffect.shutdown();
		Music.shutdown();
	}
}