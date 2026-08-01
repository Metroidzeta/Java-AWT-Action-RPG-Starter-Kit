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

/**
 * Représente une jauge bornée entre 0 et un maximum
 * (par exemple : HP, MP, etc.).
 */
public final class Gauge {
	private int value; // valeur actuelle
	private int max;   // valeur maximale

	public Gauge(int value, int max) {
		if (max < 1) throw new IllegalArgumentException("Le max doit être >= 1");
		if (value < 0 || value > max) throw new IllegalArgumentException("La valeur doit être comprise entre 0 et " + max);
		this.value = value;
		this.max = max;
	}

	/** Getters **/
	public int getValue() { return value; }
	public int getMax() { return max; }
	public double getRatio() { return (double) value / max; }

	/** Setters **/
	public void setValue(int newValue) { value = Math.clamp(newValue, 0, max); } // newValue est compris entre [0, max]
	public void setMax(int newMax) {
		if (newMax < 1) throw new IllegalArgumentException("Le newMax doit être >= 1");
		max = newMax;
		value = Math.min(value, max); // Ajuste automatiquement si valeur > max
	}

	/** Autres méthodes **/
	public boolean isFull() { return value == max; }
	public boolean isEmpty() { return value == 0; }
	public void add(int delta) { // Évite l'overflow potentiel lors de l'addition valeur + delta.
		long result = (long) value + delta;
		result = Math.clamp(result, 0L, max);
		value = (int) result;
	}

	@Override
	public String toString() { return "[" + value + "/" + max + "]"; }
}