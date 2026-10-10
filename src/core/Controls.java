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
 * Gère les entrées clavier du jeu.
 *
 * Les événements AWT alimentent les états held/pressed/released.
 * Au début de chaque tick logique, beginTick() crée un snapshot
 * cohérent utilisé par le thread de jeu.
 */
public final class Controls implements KeyListener {

	public enum GameKey { // Touches reconnues par le jeu
		UP, DOWN, LEFT, RIGHT,
		A, B, Q, S,
		SPACE, ENTER, ESCAPE, BACK_SPACE,
		F1, F3, F5
	}

	private final Object statesLock = new Object();
	private final StringBuilder typedPending = new StringBuilder();
	private String tickTypedText = "";

	private boolean textInputEnabled;

	// État modifié par l'EDT AWT
	private final EnumSet<GameKey> held = EnumSet.noneOf(GameKey.class);
	private final EnumSet<GameKey> pressedPending = EnumSet.noneOf(GameKey.class);
	private final EnumSet<GameKey> releasedPending = EnumSet.noneOf(GameKey.class);

	// Snapshot utilisé par le thread du jeu
	private final EnumSet<GameKey> tickHeld = EnumSet.noneOf(GameKey.class);
	private final EnumSet<GameKey> tickPressed = EnumSet.noneOf(GameKey.class);
	private final EnumSet<GameKey> tickReleased = EnumSet.noneOf(GameKey.class);

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

	public Controls() {}

	@Override
	public void keyPressed(KeyEvent e) { // Quand une touche est pressée
		GameKey key = KEY_MAP.get(e.getKeyCode());
		if (key == null) return;

		synchronized (statesLock) {
			if (held.add(key) || key == GameKey.BACK_SPACE) pressedPending.add(key);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) { // Quand une touche est relachée
		GameKey key = KEY_MAP.get(e.getKeyCode());
		if (key == null) return;

		synchronized (statesLock) {
			if (held.remove(key)) releasedPending.add(key);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		char inputChar = e.getKeyChar();
		if (Character.isISOControl(inputChar)) return;
		synchronized (statesLock) {
			if (!textInputEnabled) return;
			if (typedPending.length() < Config.MSG_SIZE_MAX) {
				typedPending.append(inputChar);
			}
		}
	}

	public void beginTick() {
		synchronized (statesLock) {
			tickHeld.clear();
			tickHeld.addAll(held);

			tickPressed.clear();
			tickPressed.addAll(pressedPending);

			tickReleased.clear();
			tickReleased.addAll(releasedPending);

			tickTypedText = typedPending.toString();

			pressedPending.clear();
			releasedPending.clear();
			typedPending.setLength(0);
		}
	}

	public boolean isHeld(GameKey key) { return tickHeld.contains(Objects.requireNonNull(key, "Key null")); }
	public boolean isPressed(GameKey key) { return tickPressed.contains(Objects.requireNonNull(key, "Key null")); }
	public boolean isReleased(GameKey key) { return tickReleased.contains(Objects.requireNonNull(key, "Key null")); }

	public String getTypedText() { return tickTypedText; }

	void setTextInputEnabled(boolean b) {
		synchronized (statesLock) {
			textInputEnabled = b;
			if (!b) typedPending.setLength(0);
		}
	}

	public void clear() {
		synchronized (statesLock) {
			held.clear();
			pressedPending.clear();
			releasedPending.clear();
			typedPending.setLength(0);
		}
	}
}