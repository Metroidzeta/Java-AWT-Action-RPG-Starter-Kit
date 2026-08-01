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
package core.renderer;

import static core.Config.WINDOW_WIDTH;
import static core.Config.WINDOW_HEIGHT;

public final class RenderMetrics {

	/** Constructeur **/
	private RenderMetrics() {}

	private static int wh(double ratio) { return (int) (WINDOW_HEIGHT * ratio); }
	private static int ww(double ratio) { return (int) (WINDOW_WIDTH * ratio); }

	/** Pourcentages de la hauteur de la fenêtre **/
	public static final int WH_1PERCENT  = wh(0.01);
	public static final int WH_2PERCENT  = wh(0.02);
	public static final int WH_4PERCENT  = wh(0.04);
	public static final int WH_5PERCENT  = wh(0.05);
	public static final int WH_8PERCENT  = wh(0.08);
	public static final int WH_20PERCENT = wh(0.20);
	public static final int WH_26PERCENT = wh(0.26);
	public static final int WH_37PERCENT = wh(0.37);
	public static final int WH_69PERCENT = wh(0.69);
	public static final int WH_94PERCENT = wh(0.94);
	public static final int WH_95PERCENT = wh(0.95);
	public static final int WH_98PERCENT = wh(0.98);

	/** Pourcentages de la largeur de la fenêtre **/
	public static final int WW_1PERCENT    = ww(0.01);
	public static final int WW_2PERCENT    = ww(0.02);
	public static final int WW_3PERCENT    = ww(0.03);
	public static final int WW_4PERCENT    = ww(0.04);
	public static final int WW_15PERCENT   = ww(0.15);
	public static final int WW_17PERCENT   = ww(0.17);
	public static final int WW_17_5PERCENT = ww(0.175);
	public static final int WW_65PERCENT   = ww(0.65);
	public static final int WW_80PERCENT   = ww(0.80);
	public static final int WW_84PERCENT   = ww(0.84);
	public static final int WW_94PERCENT   = ww(0.94);
	public static final int WW_96PERCENT   = ww(0.96);
}