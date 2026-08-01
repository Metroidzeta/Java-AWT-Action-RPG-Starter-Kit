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

import core.GameMap;
import core.Config;
import core.GameContext;
import core.Hero;

import java.util.Objects;

public final class Event_TP implements Event {

	private final int xDst, yDst;
	private final GameMap gameMapDst;

	/** Constructeur **/
	public Event_TP(int xCellDst, int yCellDst, GameMap gameMapDst) {
		Objects.requireNonNull(gameMapDst, "CarteDst event_tp null");
		if (xCellDst < 0 || xCellDst >= gameMapDst.getWidth() || yCellDst < 0 || yCellDst >= gameMapDst.getHeight()) {
			throw new IllegalArgumentException("Coordonnées d'event_tp hors limite carteDst " + gameMapDst.getName() + " : " + xCellDst + ", " + yCellDst);
		}
		xDst = xCellDst * Config.CELL_SIZE; // vraie valeur de x : il faut multiplier par CELL_SIZE
		yDst = yCellDst * Config.CELL_SIZE; // vraie valeur de y : il faut multiplier par CELL_SIZE
		this.gameMapDst = gameMapDst;
	}

	/** Autres méthodes **/
	@Override
	public void execute(GameContext gc) {
		Objects.requireNonNull(gc, "GameContext null");
		gc.getAudioManager().playMusic(gameMapDst.getMusic());
		Hero hero = gc.getHero();
		hero.setCurrentGameMap(gameMapDst);
		hero.updatePosition(xDst, yDst);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Event_TP ev_tp)) return false;
		return xDst == ev_tp.xDst
			&& yDst == ev_tp.yDst
			&& Objects.equals(gameMapDst.getName(), ev_tp.gameMapDst.getName());
	}

	@Override
	public int hashCode() { return Objects.hash(getClass(), xDst, yDst, gameMapDst.getName()); }
}