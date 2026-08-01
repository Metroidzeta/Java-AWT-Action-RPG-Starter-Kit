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
import java.util.Objects;

/**
 * Événement : modification des MP (points de magie / mana).
 * Valeur peut être positive (gain) ou négative (perte).
 */
public final class Event_ModifyMP implements Event {

	private final int mp;

	/** Constructeur **/
	public Event_ModifyMP(int mp) {
		if (mp == 0) throw new IllegalArgumentException("Modifier les MP de 0 ne sert à rien");
		this.mp = mp;
	}

	/** Autres méthodes **/
	@Override
	public void execute(GameContext gc) {
		Objects.requireNonNull(gc, "GameContext null");
		gc.getHero().addMp(mp);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Event_ModifyMP ev_mmp)) return false;
		return mp == ev_mmp.mp;
	}

	@Override
	public int hashCode() { return Objects.hash(getClass(), mp); }
}