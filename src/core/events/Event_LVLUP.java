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

public final class Event_LVLUP implements Event {

	/** Constructeur **/
	public Event_LVLUP() {}

	/** Autres méthodes **/
	@Override
	public void execute(GameContext gc) {
		Objects.requireNonNull(gc, "GameContext null");
		gc.getHero().levelUp();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
        return o instanceof Event_LVLUP;
    }

	@Override
	public int hashCode() { return Objects.hash(getClass()); }
}