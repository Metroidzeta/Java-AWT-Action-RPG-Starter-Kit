/**
 * @author Alain Barbier alias "Metroidzeta"
 * Copyright © 2026 Alain Barbier (Metroidzeta) - All rights reserved.
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
 * Caméra 2D fluide capable de suivre dynamiquement une entité "CamLock".
 * Elle interpole la position de la cible pour obtenir un mouvement fluide,
 * même à des fréquences de rendu supérieures au taux de mise à jour logique.
 */
public final class Camera {

	public interface CamLock { int getXCam(); int getYCam(); }

	private CamLock target; // cible actuellement suivie
	private double xPrev, yPrev;
	private double xInter, yInter;
	private int xCurr, yCurr;

	/** Getters **/
	public double getX() { return xInter; }
	public double getY() { return yInter; }

	/** Définit la cible suivie par la caméra **/
	public void setTarget(CamLock target) {
		this.target = target;
		if (target != null) {
			xCurr = target.getXCam();
			yCurr = target.getYCam();
			sync();
		}
	}

	/** Met à jour les positions précédentes et actuelles (appelé à chaque update logique) */
	public void update() {
		if (target == null) return;
		xPrev = xCurr;
		yPrev = yCurr;
		xCurr = target.getXCam();
		yCurr = target.getYCam();
	}

	/** Interpolation entre la position précédente et la position actuelle **/
	public void interpolate(double interpolation) {
		xInter = xPrev + (xCurr - xPrev) * interpolation;
		yInter = yPrev + (yCurr - yPrev) * interpolation;
	}

	/** Force une synchronisation complète entre les positions (utile après un TP ou changement de carte) **/
	public void sync() {
		xPrev = xCurr;
		yPrev = yCurr;
		xInter = xCurr;
		yInter = yCurr;
	}

	@Override
	public String toString() {
		return "Camera[Prev: (x: %.2f, y: %.2f), Inter: (x: %.2f, y: %.2f), Curr: (x: %d, y: %d)]"
				.formatted(xPrev, yPrev, xInter, yInter, xCurr, yCurr);
	}
}