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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.Config.SOUND_EFFECTS_DIRECTORY;

/**
 * Bruitage en mémoire, avec mixage de type SDL_mixer.
 *
 * Toutes les instances SoundEffect partagent un seul mixeur :
 * - 32 canaux logiques globaux ;
 * - un seul SourceDataLine et un seul thread audio ;
 * - PCM 48 kHz, 16 bits signé, stéréo ;
 * - maximum 8 lectures simultanées par bruitage par défaut, dans la limite des 32 canaux globaux.
 *
 * La classe Music peut continuer à fonctionner séparément.
 * Fermer chaque SoundEffect avec close() à la fermeture du jeu.
 */
public final class SoundEffect implements AutoCloseable {

	private static final int GLOBAL_CHANNEL_COUNT = 32;
	private static final int DEFAULT_VOICE_COUNT = 8;
	private static final int SAMPLE_RATE = 48_000;
	private static final int FRAMES_PER_CHUNK = 256;
	private static final int BUFFER_CHUNKS = 4;
	private static final int OUTPUT_CHANNELS = 2;
	private static final int FRAME_SIZE = OUTPUT_CHANNELS * Short.BYTES;

	private static final AudioFormat OUTPUT_FORMAT = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, 16,
			OUTPUT_CHANNELS, FRAME_SIZE, SAMPLE_RATE, false
	);

	// Un seul moteur audio partagé entre toutes les instances SoundEffect.
	private static final Object SHARED_LOCK = new Object();
	private static Mixer sharedMixer;
	private static int liveEffects;

	private final String name;
	private final short[] pcm;              // échantillons PCM stéréo entrelacés
	private final int[] channelByVoice;     // index de canal global ; -1 = libre
	private final Mixer mixer;
	private boolean closed;                 // protégé par mixer.lock

	public SoundEffect(String fileName) { this(fileName, DEFAULT_VOICE_COUNT); }

	public SoundEffect(String fileName, int voiceCount) {
		name = Util.requireNonBlank(fileName, "Nom fichier du bruitage");
		if (voiceCount <= 0 || voiceCount > GLOBAL_CHANNEL_COUNT) {
			throw new IllegalArgumentException("Nombre de voix invalide : " + voiceCount);
		}

		final Path file = Path.of(SOUND_EFFECTS_DIRECTORY, fileName);
		if (!Files.isRegularFile(file)) throw new IllegalStateException("Fichier de bruitage introuvable : " + file.toAbsolutePath());

		this.pcm = loadAudio(file);
		this.channelByVoice = new int[voiceCount];
		Arrays.fill(channelByVoice, -1);

		synchronized (SHARED_LOCK) {
			if (sharedMixer == null) {
				sharedMixer = new Mixer();
			}
			mixer = sharedMixer;
			liveEffects++;
		}
	}

	/** Getters **/
	public String getName() { return name; }
	public int getVoiceCount() { return channelByVoice.length; }

	/**
	 * Joue sur la première voix libre de ce bruitage ET sur un canal global libre.
	 * Renvoie l'indice de voix local (comme l'ancienne API) ou -1 si indisponible.
	 */
	public int play() {
		synchronized (mixer.lock) {
			ensureOpen();
			if (!mixer.running) throw new IllegalStateException("Mixeur audio arrêté");

			int localVoice = -1;
			for (int i = 0; i < channelByVoice.length; i++) {
				if (channelByVoice[i] == -1) {
					localVoice = i;
					break;
				}
			}
			if (localVoice == -1) return -1;

			for (int i = 0; i < mixer.channels.length; i++) {
				Channel channel = mixer.channels[i];
				if (channel.effect == null) {
					channel.effect = this;
					channel.localVoice = localVoice;
					channel.samplePosition = 0;
					channelByVoice[localVoice] = i;
					return localVoice;
				}
			}
			return -1;
		}
	}

	public void stop(int voiceIndex) {
		synchronized (mixer.lock) {
			ensureOpen();
			if (voiceIndex < 0 || voiceIndex >= channelByVoice.length) {
				throw new IndexOutOfBoundsException("Indice de voix invalide : " + voiceIndex);
			}
			int globalIndex = channelByVoice[voiceIndex];
			if (globalIndex != -1) mixer.releaseChannel(globalIndex);
		}
	}

	public void stopAllVoices() {
		synchronized (mixer.lock) {
			ensureOpen();
			stopAllVoicesLocked();
		}
	}

	private void stopAllVoicesLocked() {
        for (int globalIndex : channelByVoice) {
            if (globalIndex != -1) mixer.releaseChannel(globalIndex);
        }
	}

	/** Ferme ce bruitage ; le mixeur commun s'arrête après le dernier close(). */
	@Override
	public void close() {
		synchronized (SHARED_LOCK) {
			synchronized (mixer.lock) {
				if (closed) return;
				closed = true;
				stopAllVoicesLocked();
			}

			if (--liveEffects == 0) {
				sharedMixer = null;
				mixer.shutdown();
			}
		}
	}

	private void ensureOpen() {
		if (closed) throw new IllegalStateException("Bruitage déjà fermé : " + name);
	}

	/** Chargement et conversion effectués une seule fois, jamais dans play(). */
	private static short[] loadAudio(Path file) {
		try (AudioInputStream source = AudioSystem.getAudioInputStream(file.toFile())) {
			AudioFormat sourceFormat = source.getFormat();
			int channels = sourceFormat.getChannels();
			float sourceRate = sourceFormat.getSampleRate();

			if ((channels != 1 && channels != 2) || !Float.isFinite(sourceRate) || sourceRate <= 0.0f) {
				throw new IllegalStateException("Format audio non pris en charge : " + sourceFormat);
			}

			// Conversion native vers PCM16 à la fréquence d'origine.
			AudioFormat pcmFormat = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					sourceRate,
					16,
					channels,
					channels * Short.BYTES,
					sourceRate,
					false
			);
			if (!AudioSystem.isConversionSupported(pcmFormat, sourceFormat)) {
				throw new IllegalStateException("Conversion PCM impossible : " + sourceFormat);
			}

			try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcmFormat, source)) {
				byte[] raw = decoded.readAllBytes();
				int frameSize = channels * Short.BYTES;
				if (raw.length == 0 || raw.length % frameSize != 0) {
					throw new IllegalStateException("Données audio vides ou incomplètes : " + file);
				}

				int inputFrames = raw.length / frameSize;
				double destinationSize = Math.ceil(inputFrames * (double) SAMPLE_RATE / sourceRate);
				if (!Double.isFinite(destinationSize) || destinationSize < 1 || destinationSize > Integer.MAX_VALUE / OUTPUT_CHANNELS) {
					throw new IllegalStateException("Bruitage trop long : " + file);
				}

				int outputFrames = (int) destinationSize;
				short[] output = new short[outputFrames * OUTPUT_CHANNELS];
				double ratio = sourceRate / SAMPLE_RATE;

				// Rééchantillonnage linéaire et conversion mono -> stéréo.
				for (int frame = 0; frame < outputFrames; frame++) {
					double sourcePosition = Math.min(frame * ratio, inputFrames - 1.0);
					int aFrame = (int)sourcePosition;
					int bFrame = Math.min(aFrame + 1, inputFrames - 1);
					double fraction = sourcePosition - aFrame;

					for (int ch = 0; ch < OUTPUT_CHANNELS; ch++) {
						int sourceCh = channels == 1 ? 0 : ch;
						int a = readSample(raw, aFrame, sourceCh, channels);
						int b = readSample(raw, bFrame, sourceCh, channels);
						output[frame * OUTPUT_CHANNELS + ch] = (short)Math.round(a + (b - a) * fraction);
					}
				}
				return output;
			}
		} catch (UnsupportedAudioFileException | IOException e) {
			throw new IllegalStateException("Échec de chargement du bruitage : " + file, e);
		}
	}

	private static short readSample(byte[] raw, int frame, int channel, int channels) {
		int index = (frame * channels + channel) * Short.BYTES;
		return (short)((raw[index] & 0xff) | ((raw[index + 1] & 0xff) << 8));
	}

	/** Un canal logique du mixeur commun (équivalent d'un canal SDL_mixer). */
	private static final class Channel {
		SoundEffect effect;
		int localVoice;
		int samplePosition;
	}

	/** Mixeur unique, entièrement privé à SoundEffect. */
	private static final class Mixer {
		final Object lock = new Object();
		final Channel[] channels = new Channel[GLOBAL_CHANNEL_COUNT];
		final SourceDataLine line;
		final Thread audioThread;
		volatile boolean running = true;
		private final AtomicBoolean outputClosed = new AtomicBoolean(false);

		Mixer() {
			for (int i = 0; i < channels.length; i++) channels[i] = new Channel();
			SourceDataLine output = null;
			try {
				output = AudioSystem.getSourceDataLine(OUTPUT_FORMAT);
				output.open(OUTPUT_FORMAT, BUFFER_CHUNKS * FRAMES_PER_CHUNK * FRAME_SIZE);
				output.start();
			} catch (LineUnavailableException | RuntimeException e) {
				if (output != null) {
					try { output.close(); } catch (RuntimeException closeError) { e.addSuppressed(closeError); }
				}
				throw new IllegalStateException("Impossible d'ouvrir la sortie audio", e);
			}
			line = output;
			try {
				audioThread = new Thread(this::runAudio, "SoundEffect-Mixer");
				audioThread.setDaemon(true);
				audioThread.start();
			} catch (RuntimeException | Error e) {
				closeOutput();
				throw e;
			}
		}

		/** Appel uniquement en possédant lock. */
		void releaseChannel(int index) {
			Channel channel = channels[index];
			if (channel.effect == null) return;
			channel.effect.channelByVoice[channel.localVoice] = -1;
			channel.effect = null;
			channel.localVoice = 0;
			channel.samplePosition = 0;
		}

		void runAudio() {
			final int[] sum = new int[FRAMES_PER_CHUNK * OUTPUT_CHANNELS];
			final byte[] buffer = new byte[FRAMES_PER_CHUNK * FRAME_SIZE];

			try {
				while (running) {
					Arrays.fill(sum, 0);
					synchronized (lock) {
						for (int index = 0; index < channels.length; index++) {
							Channel channel = channels[index];
							SoundEffect effect = channel.effect;
							if (effect == null) continue;

							int available = effect.pcm.length - channel.samplePosition;
							int count = Math.min(sum.length, available);
							for (int s = 0; s < count; s++) {
								sum[s] += effect.pcm[channel.samplePosition + s];
							}
							channel.samplePosition += count;
							if (channel.samplePosition >= effect.pcm.length) {
								releaseChannel(index);
							}
						}
					}

					// Saturation 16 bits seulement APRÈS avoir additionné les voix.
					for (int i = 0; i < sum.length; i++) {
						int sample = Math.clamp(sum[i], Short.MIN_VALUE, Short.MAX_VALUE);
						buffer[i * 2] = (byte) sample;
						buffer[i * 2 + 1] = (byte) (sample >>> 8);
					}

					// Ne jamais bloquer le lock du jeu sur l'écriture audio.
					int offset = 0;
					while (running && offset < buffer.length) {
						int written = line.write(buffer, offset, buffer.length - offset);
						if (written <= 0) {
							if (running) throw new IllegalStateException("La sortie audio n'accepte plus de données");
							break;
						}
						offset += written;
					}
				}
			} catch (RuntimeException e) {
				if (running) System.err.println("Arrêt inattendu du mixeur SoundEffect : " + e);
			} finally {
				running = false;
				closeOutput();
			}
		}

		private void closeOutput() {
			if (!outputClosed.compareAndSet(false, true)) return;
			try { line.stop(); } catch (RuntimeException e) { System.err.println("Échec arrêt audio : " + e); }
			try { line.flush(); } catch (RuntimeException e) { System.err.println("Échec vidage audio : " + e); }
			try { line.close(); } catch (RuntimeException e) { System.err.println("Échec fermeture audio : " + e); }
		}

		void shutdown() {
			running = false;
			// Ne pas attendre la fin des bruitages avec drain().
			audioThread.interrupt();
			closeOutput();

			if (Thread.currentThread() != audioThread) {
				try {
					audioThread.join(2_000L);
					if (audioThread.isAlive()) System.err.println("Le thread audio ne s'est pas terminé normalement");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
