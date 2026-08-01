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
package core;

import static core.Config.CELL_SIZE;

import java.awt.Rectangle;
import java.util.Objects;

public final class Position {
	private int x, y;

	/** Constructeur **/
	public Position(int x, int y) { this.x = x; this.y = y; }

	/** Getters **/
	public int getX() { return x; }
	public int getY() { return y; }
	public int getXCell() { return x / CELL_SIZE; }
	public int getYCell() { return y / CELL_SIZE; }

	/** Setters **/
	public void set(int x, int y) { this.x = x; this.y = y; }
	public void move(int dx, int dy) { x += dx; y += dy; }

	/** Autres méthodes **/
	public Rectangle getRectHitbox(int size) {
		if (size < 1) throw new IllegalArgumentException("La taille doit être >= 1");
		return new Rectangle(x, y, size, size);
	}

	@Override
	public String toString() { return "(" + x + "," + y + ")"; }
	public String toStringCells() { return "(" + (x / CELL_SIZE) + "," + (y / CELL_SIZE) + ")"; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Position position)) return false;
		return x == position.x && y == position.y;
	}

	@Override
	public int hashCode() { return Objects.hash(x, y); }
}