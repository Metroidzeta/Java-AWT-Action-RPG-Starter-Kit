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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static core.Config.MUSIC_DIRECTORY;

/**
 * Lit une musique en streaming (notamment OGG si un décodeur Java Sound est installé).
 * Une lecture possède sa propre ligne audio, son thread et son état : un ancien
 * thread ne peut jamais modifier les ressources d'une lecture plus récente.
 *
 * La musique active est sélectionnée par {@link core.AudioManager}.
 */
public final class Music implements AutoCloseable {

	private static final int BUFFER_SIZE = 16_384;
	private static final long STOP_TIMEOUT_MS = 2_000L;

	private final String name;
	private final File file;
	private final AudioFormat decodedFormat;

	/** Protège activeSession et closed. Les workers ne prennent jamais ce verrou. */
	private final Object lifecycleLock = new Object();
	private PlaybackSession activeSession;
	private boolean closed;

	public Music(String fileName) {
		name = Util.requireNonBlank(fileName, "Nom fichier de la musique");
		file = new File(MUSIC_DIRECTORY, fileName);
		if (!file.isFile()) {
			throw new IllegalStateException("Fichier introuvable : " + file.getAbsolutePath());
		}
		decodedFormat = initDecodedFormat();
	}

	public String getName() { return name; }

	private AudioFormat initDecodedFormat() {
		try (AudioInputStream source = AudioSystem.getAudioInputStream(file)) {
			final AudioFormat input = source.getFormat();
			final int channels = input.getChannels();
			final float sampleRate = input.getSampleRate();

			if (channels <= 0 || !Float.isFinite(sampleRate) || sampleRate <= 0) {
				throw new IllegalStateException("Format audio invalide : " + input);
			}

			final AudioFormat pcm = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					sampleRate,
					16,
					channels,
					channels * Short.BYTES,
					sampleRate,
					false
			);

			if (!AudioSystem.isConversionSupported(pcm, input)) {
				throw new IllegalStateException("Conversion audio non supportée : " + input + " -> " + pcm);
			}
			return pcm;
		} catch (UnsupportedAudioFileException | IOException e) {
			throw new IllegalStateException("Impossible de décoder la musique : " + name, e);
		}
	}

	public void play() { play(true); }

	public void play(boolean loop) {
		synchronized (lifecycleLock) {
			ensureOpen();
			if (isActive(activeSession)) return;

			// Une précédente session terminée n'est jamais réutilisée.
			final SourceDataLine line = openAudioLine();
			final PlaybackSession session = new PlaybackSession(line, loop);
			final Thread thread = new Thread(() -> playbackLoop(session), "Music-" + name);
			thread.setDaemon(true);
			session.thread = thread;
			activeSession = session;

			try {
				line.start();
				thread.start();
			} catch (RuntimeException | Error e) {
				activeSession = null;
				session.requestStop();
				throw e;
			}
		}
	}

	private SourceDataLine openAudioLine() {
		SourceDataLine line = null;
		try {
			line = AudioSystem.getSourceDataLine(decodedFormat);
			line.open(decodedFormat);
			return line;
		} catch (LineUnavailableException | RuntimeException e) {
			if (line != null) {
				try { line.close(); }
				catch (RuntimeException closeFailure) { e.addSuppressed(closeFailure); }
			}
			throw new IllegalStateException("Impossible d'ouvrir la sortie audio : " + name, e);
		}
	}

	private void playbackLoop(PlaybackSession session) {
		final int frameSize = decodedFormat.getFrameSize();
		final int bufferBytes = BUFFER_SIZE - (BUFFER_SIZE % frameSize);
		final byte[] buffer = new byte[Math.max(frameSize, bufferBytes)];

		try {
			do {
				if (!playOnce(session, buffer)) return;
			} while (session.loop && !session.stopRequested.get());

			// Uniquement à la fin naturelle d'une lecture non bouclée.
			if (!session.loop && session.awaitUnpaused()) {
				session.line.drain();
			}
		} catch (UnsupportedAudioFileException | IOException | RuntimeException e) {
			if (!session.stopRequested.get()) {
				System.err.println("Erreur de lecture de la musique " + name + " : " + e);
			}
		} finally {
			try {
				session.closeOutput();
			} finally {
				session.finished = true;
			}
		}
	}

	/** Retourne true si le fichier est arrivé à sa fin naturelle. */
	private boolean playOnce(PlaybackSession session, byte[] buffer)
			throws UnsupportedAudioFileException, IOException {
		try (AudioInputStream source = AudioSystem.getAudioInputStream(file);
		     AudioInputStream decoded = AudioSystem.getAudioInputStream(decodedFormat, source)) { // try-with-resources

			boolean hasAudioData = false;
			while (!session.stopRequested.get()) {
				if (!session.awaitUnpaused()) return false;

				final int bytesRead = decoded.read(buffer, 0, buffer.length);
				if (bytesRead == -1) {
					if (!hasAudioData) {
						throw new IllegalStateException("Musique sans échantillons : " + name);
					}
					return true;
				}
				if (bytesRead == 0) continue;
				hasAudioData = true;

				int offset = 0;
				while (offset < bytesRead) {
					if (!session.awaitUnpaused()) return false;

					final int written = session.line.write(buffer, offset, bytesRead - offset);
					if (written <= 0) {
						if (session.stopRequested.get()) return false;
						if (session.isPaused()) continue; // la ligne a pu s'arrêter entre deux contrôles
						throw new IllegalStateException("Écriture audio interrompue : " + name);
					}
					offset += written;
				}
			}
			return false;
		}
	}

	private static boolean isActive(PlaybackSession session) {
		return session != null && !session.stopRequested.get() && !session.finished;
	}

	public void pause() {
		synchronized (lifecycleLock) {
			ensureOpen();
			if (isActive(activeSession)) activeSession.pause();
		}
	}

	public void resume() {
		synchronized (lifecycleLock) {
			ensureOpen();
			if (isActive(activeSession)) activeSession.resume();
		}
	}

	public void stop() {
		synchronized (lifecycleLock) {
			ensureOpen();
			stopActiveSession();
		}
	}

	private void stopActiveSession() {
		final PlaybackSession session = activeSession;
		activeSession = null;
		if (session == null) return;

		session.requestStop();
		final Thread thread = session.thread;
		if (thread == null || thread == Thread.currentThread()) return;

		try {
			thread.join(STOP_TIMEOUT_MS);
			if (thread.isAlive()) {
				System.err.println("Le thread audio ne s'est pas terminé dans le délai : " + name);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public boolean isRunning() {
		synchronized (lifecycleLock) {
			return isActive(activeSession);
		}
	}

	public boolean isPlaying() {
		synchronized (lifecycleLock) {
			final PlaybackSession session = activeSession;
			return isActive(session) && !session.isPaused();
		}
	}

	public boolean isPaused() {
		synchronized (lifecycleLock) {
			final PlaybackSession session = activeSession;
			return isActive(session) && session.isPaused();
		}
	}

	private void ensureOpen() {
		if (closed) throw new IllegalStateException("Musique déjà fermée : " + name);
	}

	@Override
	public void close() {
		synchronized (lifecycleLock) {
			if (closed) return;
			closed = true;
			stopActiveSession();
		}
	}

	/** Etat privé d'UNE lecture. Ne référence jamais la session suivante. */
	private static final class PlaybackSession {
		final SourceDataLine line;
		final boolean loop;
		final AtomicBoolean stopRequested = new AtomicBoolean(false);
		final ReentrantLock pauseLock = new ReentrantLock();
		final Condition unpaused = pauseLock.newCondition();
		final AtomicBoolean outputClosed = new AtomicBoolean(false);

		volatile boolean finished;
		Thread thread; // défini avant Thread.start, sous lifecycleLock
		private boolean paused; // protégé par pauseLock

		PlaybackSession(SourceDataLine line, boolean loop) {
			this.line = line;
			this.loop = loop;
		}

		void pause() {
			pauseLock.lock();
			try {
				if (stopRequested.get() || finished || paused) return;
				paused = true;
				line.stop();
			} finally {
				pauseLock.unlock();
			}
		}

		void resume() {
			pauseLock.lock();
			try {
				if (stopRequested.get() || finished || !paused) return;
				line.start();
				paused = false;
				unpaused.signalAll();
			} finally {
				pauseLock.unlock();
			}
		}

		boolean isPaused() {
			pauseLock.lock();
			try { return paused; }
			finally { pauseLock.unlock(); }
		}

		boolean awaitUnpaused() {
			pauseLock.lock();
			try {
				while (paused && !stopRequested.get()) {
					try { unpaused.await(); }
					catch (InterruptedException e) {
						if (!stopRequested.get()) Thread.currentThread().interrupt();
						return false;
					}
				}
				return !stopRequested.get();
			} finally {
				pauseLock.unlock();
			}
		}

		void requestStop() {
			stopRequested.set(true);
			pauseLock.lock();
			try {
				paused = false;
				unpaused.signalAll();
			} finally {
				pauseLock.unlock();
			}
			if (thread != null) thread.interrupt();
			closeOutput(); // débloque normalement SourceDataLine.write() et drain()
		}

		void closeOutput() {
			if (!outputClosed.compareAndSet(false, true)) return;
			try { line.stop(); }
			catch (RuntimeException e) { System.err.println("Erreur arrêt audio : " + e); }
			try { line.flush(); }
			catch (RuntimeException e) { System.err.println("Erreur vidage audio : " + e); }
			try { line.close(); }
			catch (RuntimeException e) { System.err.println("Erreur fermeture audio : " + e); }
		}
	}
}
