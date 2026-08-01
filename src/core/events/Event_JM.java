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
package core.events;

import core.GameContext;
import core.resources.Music;

import java.util.Objects;

/**
 * Événement : jouer une musique.
 * Stocke une référence vers une Musique.
 */
public final class Event_JM implements Event {

	private final Music music;

	/** Constructeur **/
	public Event_JM(Music music) {
		this.music = Objects.requireNonNull(music, "Music de l'Event_JM null");
	}

	/** Autres méthodes **/
	@Override
	public void execute(GameContext gc) {
		Objects.requireNonNull(gc, "GameContext null");
		gc.getAudioManager().playMusic(music);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Event_JM ev_jm)) return false;
		return Objects.equals(music, ev_jm.music);
	}

	@Override
	public int hashCode() { return Objects.hash(getClass(), music); }
}