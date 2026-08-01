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
import static core.Config.MUSIC_DIRECTORY;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gère la lecture, la pause et l'arrêt d'une musique de fond au format audio ogg.
 * L'implémentation est thread-safe et optimise l'utilisation de la mémoire et du CPU
 * pour les musiques jouées. Une seule musique peut être active à la fois.
 */
public final class Music implements AutoCloseable {

	private static final int BUFFER_SIZE = 16384;
	private static final int STOP_TIMEOUT_MS = 2000;
	private static final Object INIT_LOCK = new Object();
	private static Music activeMusic;
	private static boolean vorbisInitialized;

	private final String name;
	private final File file;
	private final AudioFormat decodedFormat;

	private Thread playbackThread;
	private final AtomicBoolean isRunning = new AtomicBoolean(false);
	private volatile boolean loop;

	private final Lock pauseLock = new ReentrantLock();
	private final Condition pauseCondition = pauseLock.newCondition();
	private final AtomicBoolean paused = new AtomicBoolean(false);

	private SourceDataLine audioLine;

	private static void initVorbisSPI() {
		if (!vorbisInitialized) {
			synchronized (INIT_LOCK) {
				vorbisInitialized = true;
				Runtime.getRuntime().addShutdownHook(new Thread(Music::shutdown, "Music-Shutdown")); // Fermeture automatique à l’arrêt du programme
			}
		}
	}

	/** Constructeur **/
	public Music(String fileName) {
		name = Util.requireNonBlank(fileName, "Nom fichier de la musique");
		file = new File(MUSIC_DIRECTORY, fileName);
		if (!file.exists()) throw new IllegalStateException("Fichier introuvable : " + file.getAbsolutePath());

		initVorbisSPI();
		decodedFormat = initDecodedFormat();
	}

	/** Getters **/
	public String getName() { return name; }

	private AudioFormat initDecodedFormat() {
		try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) { // try-with-resources
			AudioFormat baseFormat = ais.getFormat();
			return new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(),
					16,
					baseFormat.getChannels(),
					baseFormat.getChannels() * 2,
					baseFormat.getSampleRate(),
					false
			);
		}
		catch (UnsupportedAudioFileException e) { throw new RuntimeException("Format audio non supporté : " + name, e); }
		catch (IOException e) { throw new RuntimeException("Erreur lecture fichier : " + name, e); }
	}

	private synchronized void openAudioLine() {
		if (audioLine != null && audioLine.isOpen()) return;

		try {
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
			audioLine = (SourceDataLine) AudioSystem.getLine(info);
			audioLine.open(decodedFormat);
		} catch (LineUnavailableException e) {
			throw new RuntimeException("Ligne audio indisponible : " + name, e);
		}
	}

	private synchronized void closeAudioLine() {
		if (audioLine != null && audioLine.isOpen()) {
			audioLine.drain();
			audioLine.close();
			audioLine = null;
		}
	}

	public void play() { play(true); }

	public synchronized void play(boolean loop) {
		if (isRunning.get()) return;
		this.loop = loop;

		stopActiveMusic();
		activeMusic = this;

		openAudioLine();

		isRunning.set(true);
		paused.set(false);

		if (audioLine != null) audioLine.start();

		playbackThread = new Thread(this::playbackLoop, "Music-" + name);
		playbackThread.setDaemon(true);
		playbackThread.start();
	}

	private void stopActiveMusic() {
		if (activeMusic != null && activeMusic != this) activeMusic.stop();
	}

	private void playbackLoop() {
		try {
			do { if (!playOnce()) break; }
			while (loop && isRunning.get());
		} finally {
			stopAudioLine();
			isRunning.set(false);
		}
	}

	private boolean playOnce() {
		try (AudioInputStream ais = AudioSystem.getAudioInputStream(file);
			 AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, ais)) { // try-with-ressources

			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;

			while ((bytesRead = din.read(buffer)) != -1 && isRunning.get()) {
				if (!waitWhilePaused()) return false;
				if (!isRunning.get()) return false;
				if (audioLine != null) audioLine.write(buffer, 0, bytesRead);
			}
			if (audioLine != null) audioLine.drain();
			return true;

		} catch (UnsupportedAudioFileException | IOException e) {
			System.err.println("Erreur durant lecture de " + name + " : " + e.getMessage());
			return false;
		}
	}

	private boolean waitWhilePaused() {
		pauseLock.lock();
		try {
			while (paused.get() && isRunning.get()) pauseCondition.await();
			return isRunning.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} finally {
			pauseLock.unlock();
		}
	}

	private void stopAudioLine() {
		if (audioLine != null) {
			audioLine.stop();
			audioLine.flush();
		}
	}

	public synchronized void stop() {
		if (!isRunning.get()) return;

		isRunning.set(false);
		paused.set(false);

		pauseLock.lock();
		try { pauseCondition.signalAll(); }
		finally { pauseLock.unlock(); }

		stopAudioLine();
		waitThreadEnd();
		closeAudioLine();
	}

	private void waitThreadEnd() {
		if (playbackThread != null && playbackThread.isAlive()) {
			playbackThread.interrupt();
			try { playbackThread.join(STOP_TIMEOUT_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		}
	}

	public void pause() {
		pauseLock.lock();
		try { paused.set(true); }
		finally { pauseLock.unlock(); }
	}

	public void resume() {
		pauseLock.lock();
		try {
			if (paused.get()) {
				paused.set(false);
				pauseCondition.signalAll();
			}
		} finally {
			pauseLock.unlock();
		}
	}

	public boolean isRunning() { return isRunning.get() && !paused.get(); }
	public boolean isPaused() { return paused.get(); }
	public boolean isPlaying() { return isRunning.get(); }

	@Override
	public synchronized void close() {
		stop();
		if (activeMusic == this) activeMusic = null;
		closeAudioLine();
	}

	public static synchronized void shutdown() {
		if (activeMusic != null) {
			activeMusic.close();
			activeMusic = null;
		}
		synchronized (INIT_LOCK) {
			vorbisInitialized = false;
		}
	}
}