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

import java.awt.Color;

public final class Config {

	private Config() { throw new AssertionError("La classe Config ne doit pas être instanciée."); } // Empêche toute instanciation

	/** Titre de la fenêtre **/
	public static final String WINDOW_TITLE = "Java/AWT Action RPG Starter Kit";

	/** Paramètres de base **/
	public static final int WINDOW_WIDTH = 1280, WINDOW_HEIGHT = 960; // par défaut : 1280 * 960
	public static final int UPS = 30; // par défaut : 30
	public static final int FPS = 60; // par défaut : 60
	public static final int LEVEL_MAX = 80; // par défaut : 80

	/** Constantes dérivées — NE PAS LES MODIFIER /!\ **/
	public static final int CELL_SIZE = (WINDOW_HEIGHT / 20) - ((WINDOW_HEIGHT / 20) % 4); // par défaut : 48
	public static final int HERO_MOVE_STEP = CELL_SIZE / 4;
	public static final int WINDOW_WIDTH_CELLS = (WINDOW_WIDTH + CELL_SIZE - 1) / CELL_SIZE;
	public static final int WINDOW_HEIGHT_CELLS = (WINDOW_HEIGHT + CELL_SIZE - 1) / CELL_SIZE;

	/** Couleurs semi-transparentes RVB **/
	public static final Color DARK_BLUE_TRANSPARENT  = new Color(0, 0, 189, 180);
	public static final Color DARK_GREEN_TRANSPARENT = new Color(0, 100, 0, 180);
	public static final Color BURGUNDY_TRANSPARENT   = new Color(109, 7, 26, 180);
	public static final Color DARK_GOLD_TRANSPARENT  = new Color(181, 148, 16, 180);
	public static final Color LIGHT_GRAY_TRANSPARENT = new Color(180, 190, 200, 48);
	public static final Color DARK_GRAY_TRANSPARENT  = new Color(58, 58, 58, 180);
	public static final Color PURPLE_TRANSPARENT     = new Color(143, 0, 255, 128);

	/** Dossiers **/
	public static final String UI_ASSETS_DIRECTORY = "img";
	public static final String CHIPSETS_DIRECTORY = "img";
	public static final String SKINS_DIRECTORY = "img";
	public static final String SOUND_EFFECTS_DIRECTORY = "sfx";
	public static final String MUSIC_DIRECTORY = "music";
	public static final String GAME_MAP_DIRECTORY = "maps";

	/** Limites **/
	public static final int MSG_SIZE_MAX = 45; // par défaut : 45
	public static final boolean DEBUG_MODE = false; // par défaut : false
}