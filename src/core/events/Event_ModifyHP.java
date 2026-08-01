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
 * Événement : modification des HP (points de vie).
 * Valeur peut être positive (gain) ou négative (perte).
 */
public final class Event_ModifyHP implements Event {

	private final int hp;

	/** Constructeur **/
	public Event_ModifyHP(int hp) {
		if (hp == 0) throw new IllegalArgumentException("Modifier les HP de 0 ne sert à rien");
		this.hp = hp;
	}

	/** Autres méthodes **/
	@Override
	public void execute(GameContext gc) {
		Objects.requireNonNull(gc, "GameContext null");
		gc.getHero().addHp(hp);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Event_ModifyHP ev_mhp)) return false;
		return hp == ev_mhp.hp;
	}

	@Override
	public int hashCode() { return Objects.hash(getClass(), hp); }
}