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

import static core.Config.DEBUG_MODE;

public final class AudioManager implements AutoCloseable {

	private final Map<String, SoundEffect> soundEffects;
	private final Map<String, Music> musics;
	private Music currentMusic = null;

	public AudioManager(Map<String, SoundEffect> soundEffects, Map<String, Music> musics) {
		this.soundEffects = Map.copyOf(Objects.requireNonNull(soundEffects, "soundEffects null"));
		this.musics = Map.copyOf(Objects.requireNonNull(musics, "musics null"));
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

		final int voiceIndex = sound.play();

		if (voiceIndex == -1 && DEBUG_MODE) {
			System.err.println("Aucune voix disponible : " + name);
		}
	}

	@Override
	public void close() {
		RuntimeException failure = null;
		// Arrêter la musique active.
		try { stopMusic(); } catch (RuntimeException e) { failure = e; }

		// Fermer tous les bruitages.
		for (SoundEffect sound : soundEffects.values()) {
			try { sound.close(); } catch (RuntimeException e) { failure = collectFailure(failure, e); }
		}

		// Fermer toutes les musiques.
		for (Music music : musics.values()) {
			try { music.close(); } catch (RuntimeException e) { failure = collectFailure(failure, e); }
		}

		// Signaler les erreurs après les tentatives de nettoyage.
		if (failure != null) { throw failure; }
	}

	private static RuntimeException collectFailure(RuntimeException previous, RuntimeException current) {
		if (previous == null) return current;
		if (previous != current) previous.addSuppressed(current);
		return previous;
	}
}