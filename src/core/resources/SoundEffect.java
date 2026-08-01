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
package core.resources;

import core.Util;
import static core.Config.SOUND_EFFECTS_DIRECTORY;

import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gère la lecture de bruitages courts (.wav), préchargés en mémoire.
 * Lecture non bloquante via un pool de threads.
 * Idéal pour les sons d'attaque, d’impact, de menu, etc.
 */
public final class SoundEffect implements AutoCloseable {

	private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2, r -> {
		Thread t = new Thread(r, "SoundEffect-Thread");
		t.setDaemon(true);
		return t;
	});

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(SoundEffect::shutdown, "SoundEffect-Shutdown")); // Fermeture automatique à l’arrêt du programme
	}

	private final String name;
	private final Clip clip;

	/** Constructeur **/
	public SoundEffect(String fileName) {
		name = Util.requireNonBlank(fileName, "Nom fichier du bruitage");
		File file = new File(SOUND_EFFECTS_DIRECTORY, fileName);
		if (!file.exists()) throw new IllegalStateException("Fichier introuvable : " + file.getAbsolutePath());

		try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) { // try-with-resources
			clip = AudioSystem.getClip();
			clip.open(ais); // charge tout en mémoire
		}
		catch (UnsupportedAudioFileException e) { throw new RuntimeException("Format audio non supporté pour " + name, e); }
		catch (LineUnavailableException e) { throw new RuntimeException("Ligne audio indisponible pour " + name, e); }
		catch (IOException e) { throw new RuntimeException("Erreur lecture du fichier : " + name, e); }
	}

	public String getName() { return name; }
	public void play() { play(false); }

	public void play(boolean loop) {
		EXECUTOR_SERVICE.execute(() -> {
			synchronized (clip) {
				if (!clip.isOpen()) return; // sécurité
				if (clip.isRunning()) clip.stop();
				clip.setFramePosition(0);
				clip.loop(loop ? Clip.LOOP_CONTINUOUSLY : 0);
			}
		});
	}

	public void stop() {
		synchronized (clip) {
			if (clip.isRunning()) clip.stop();
		}
	}

	@Override
	public void close() {
		synchronized (clip) {
			if (clip.isOpen()) clip.close();
		}
	}

	public static void shutdown() { EXECUTOR_SERVICE.shutdownNow(); }
}