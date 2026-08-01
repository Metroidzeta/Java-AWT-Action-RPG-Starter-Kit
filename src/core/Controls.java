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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

/**
 * Gère l’ensemble des contrôles clavier du jeu.
 * 
 * Cette classe centralise l’état des touches pressées via un modèle de type Singleton.
 * Elle traduit les codes clavier Java (KeyEvent) en touches logiques du jeu,
 * et permet d’associer un héros ainsi qu’un buffer de message pour la saisie de texte en jeu.
 * 
 * Fournit également des méthodes utilitaires simples (UP(), DOWN(), A(), etc.)
 * pour interroger directement l’état des commandes dans la boucle principale.
 */
public final class Controls implements KeyListener {

	private static final Controls INSTANCE = new Controls();

	public enum GameKey { // Touches reconnues par le jeu
		UP, DOWN, LEFT, RIGHT,
		A, B, Q, S,
		SPACE, ENTER, ESCAPE, BACK_SPACE,
		F1, F3, F5
	}

	private Hero hero;
	private StringBuilder message;
	private Object messageLock;
	private final Object statesLock = new Object();
	private final EnumSet<GameKey> states = EnumSet.noneOf(GameKey.class); // Toutes les touches actuellement pressées

	private Controls() {} // Empêche toute instanciation externe

	public static Controls getInstance() { return INSTANCE; }

	/** Permet de lier un héros et son buffer de message à ce contrôleur **/
	public void setTargets(Hero hero, StringBuilder message, Object messageLock) {
		if (hero == null || message == null || messageLock == null) {
			throw new IllegalArgumentException("Heros, message et messageLock doivent être tous non-null");
		}
		this.hero = hero;
		this.message = message;
		this.messageLock = messageLock;
	}

	// Mapping KeyCode -> Touche
	private static final Map<Integer, GameKey> KEY_MAP = Map.ofEntries(
		Map.entry(KeyEvent.VK_UP, GameKey.UP),
		Map.entry(KeyEvent.VK_DOWN, GameKey.DOWN),
		Map.entry(KeyEvent.VK_LEFT, GameKey.LEFT),
		Map.entry(KeyEvent.VK_RIGHT, GameKey.RIGHT),
		Map.entry(KeyEvent.VK_A, GameKey.A),
		Map.entry(KeyEvent.VK_B, GameKey.B),
		Map.entry(KeyEvent.VK_Q, GameKey.Q),
		Map.entry(KeyEvent.VK_S, GameKey.S),
		Map.entry(KeyEvent.VK_SPACE, GameKey.SPACE),
		Map.entry(KeyEvent.VK_ENTER, GameKey.ENTER),
		Map.entry(KeyEvent.VK_ESCAPE, GameKey.ESCAPE),
		Map.entry(KeyEvent.VK_BACK_SPACE, GameKey.BACK_SPACE),
		Map.entry(KeyEvent.VK_F1, GameKey.F1),
		Map.entry(KeyEvent.VK_F3, GameKey.F3),
		Map.entry(KeyEvent.VK_F5, GameKey.F5)
	);

	@Override
	public void keyPressed(KeyEvent e) { // Quand une touche est pressée
		GameKey key = KEY_MAP.get(e.getKeyCode());
		if (key != null) {
			synchronized (statesLock) {
				states.add(key);
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) { // Quand une touche est relachée
		GameKey key = KEY_MAP.get(e.getKeyCode());
		if (key != null) {
			synchronized (statesLock) {
				states.remove(key);
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (hero == null || message == null || messageLock == null || !hero.isWriting()) return; // sécurité
		char inputChar = e.getKeyChar();
		synchronized (messageLock) {
			if (!Character.isISOControl(inputChar) && message.length() < Config.MSG_SIZE_MAX) {
				message.append(inputChar);
			}
		}
	}

	public boolean isPressed(GameKey key) { // Vérifie si une touche est enfoncée
		synchronized (statesLock) {
			return states.contains(Objects.requireNonNull(key, "Key null"));
		}
	}
	/**
	 * Consomme une touche en retirant son état "appuyé" de l’ensemble des touches pressées
	 */
	public void consume(GameKey key) {
		synchronized (statesLock) {
			states.remove(Objects.requireNonNull(key, "Key null"));
		}
	}

	public boolean UP()             { return isPressed(GameKey.UP); }
	public boolean DOWN()           { return isPressed(GameKey.DOWN); }
	public boolean LEFT()           { return isPressed(GameKey.LEFT); }
	public boolean RIGHT()          { return isPressed(GameKey.RIGHT); }
	public boolean A()              { return isPressed(GameKey.A); }
	public boolean B()              { return isPressed(GameKey.B); }
	public boolean Q()              { return isPressed(GameKey.Q); }
	public boolean S()              { return isPressed(GameKey.S); }
	public boolean SPACE()          { return isPressed(GameKey.SPACE); }
	public boolean ENTER()          { return isPressed(GameKey.ENTER); }
	public boolean ESCAPE()         { return isPressed(GameKey.ESCAPE); }
	public boolean BACK_SPACE()     { return isPressed(GameKey.BACK_SPACE); }
	public boolean F1()             { return isPressed(GameKey.F1); }
	public boolean F3()             { return isPressed(GameKey.F3); }
	public boolean F5()             { return isPressed(GameKey.F5); }
}