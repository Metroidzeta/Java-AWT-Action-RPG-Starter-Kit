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
import core.Util;

import java.util.Objects;

public final class Event_MSG implements Event {

	private final String msg;

	/** Constructeur **/
	public Event_MSG(String msg) {
		this.msg = Util.requireNonBlank(msg, "Le message de l'event_MSG");
	}

	/** Getters **/
	public String getMessage() { return msg; }

	/** Autres méthodes **/
	@Override
	public void execute(GameContext gc) {
		Objects.requireNonNull(gc, "GameContext null");
		gc.getHero().setIsWriting(false);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Event_MSG ev_msg)) return false;
		return Objects.equals(msg, ev_msg.msg);
	}

	@Override
	public int hashCode() { return Objects.hash(getClass(), msg); }
}