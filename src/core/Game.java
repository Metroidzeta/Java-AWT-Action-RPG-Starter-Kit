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

import core.renderer.Renderer;
import core.loaders.*;
import core.resources.Music;
import core.resources.SoundEffect;
import static core.Config.WINDOW_TITLE;
import static core.Config.WINDOW_WIDTH;
import static core.Config.WINDOW_HEIGHT;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Classe principale du moteur de jeu temps réel.
 * Charge les données, gère la boucle de rendu, les entrées, les événements et le rafraîchissement graphique.
 */
public final class Game {

	private final Controls controls = Controls.getInstance();

	private final Map<String, BufferedImage> uiAssets = LoadUiAssets.get();
	private final Map<String, Skin> skins = LoadSkins.get();
	private final Map<String, Font> fonts = LoadFonts.get();
	private final Map<String, Music> musics = LoadMusics.get();
	private final Map<String, SoundEffect> soundEffects = LoadSoundEffects.get();
	private final Map<String, Chipset> chipsets = LoadChipsets.get();
	private final Map<String, GameMap> gameMaps = LoadGameMaps.get(this);

	private final GameWindow gameWindow;
	private final Hero hero;
	private final GameContext gc;
	private final Renderer renderer;
	private final GameLoop gameLoop;

	/** Méthodes static **/
	private static String loadHeroNameFromFile(String path) {
		try {
			String name = Files.readString(Path.of(path)).trim();
			if (name.isEmpty()) throw new IllegalStateException("Fichier " + path + " vide.");
			return name;
		} catch (Exception e) {
			System.err.println("Erreur lecture du fichier " + path + " : " + e.getMessage() + " -> Nom par défaut : Test");
			return "Test";
		}
	}

	/** Constructeur **/
	public Game() {
		gameWindow = new GameWindow(WINDOW_TITLE, WINDOW_WIDTH, WINDOW_HEIGHT, controls);
		String heroName = loadHeroNameFromFile("PSEUDO.txt");
		hero = new Hero(heroName, getSkin("Evil.png"), HeroClass.ROGUE, 1, 1000, 12, 12, getGameMap("Chateau_Roland_Cour_Interieure"), 10);
		gc = new GameContext(hero, soundEffects);
		gc.resetCamera();
		LoadEvents.inject(this);
		controls.setTargets(hero, gc.getMessage(), gc.getMessageLock());
		renderer = new Renderer(gc.getCamera(), hero, getFont("FPS"), getFont("Normal"), getUiAsset("Fioles"), getUiAsset("BarreXp"));
		gameLoop = new GameLoop(gc, gameWindow, renderer, controls);
	}

	/** Getters **/
	public BufferedImage getUiAsset(String name)   { return uiAssets.get(name); } // Recherche en O(1)
	public Skin getSkin(String name)               { return skins.get(name); }
	public Font getFont(String name)               { return fonts.get(name); }
	public Music getMusic(String name)             { return musics.get(name); }
	public SoundEffect getSoundEffect(String name) { return soundEffects.get(name); }
	public Chipset getChipset(String name)         { return chipsets.get(name); }
	public GameMap getGameMap(String name)         { return gameMaps.get(name); }

	/** Autres méthodes **/
	public void play() {
		try {
			gameLoop.start();
		} finally {
			gc.getAudioManager().freeAudioResources();
			gameWindow.close();
		}
	}
}