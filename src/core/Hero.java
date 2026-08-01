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
import static core.Config.LEVEL_MAX;
import static core.Config.HERO_MOVE_STEP;
import static core.Config.WINDOW_WIDTH;
import static core.Config.WINDOW_HEIGHT;

import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.Objects;

/**
 * Représente le héros contrôlé par le joueur : nom, skin, classe, position, statistiques et état de jeu.
 */
public final class Hero implements Camera.CamLock {

	private static final class Statistics { // Initialisation des Stats de base
		static final int FORCE        = 12;
		static final int DEXTERITY    = 9;
		static final int CONSTITUTION = 10;
		static final int ALIGNMENT    = 50;
		static final int HP           = 600;
		static final int MP           = 250;
	}

	private static final int OFFSET_SWORD_PX = CELL_SIZE / 4;

	private final String name;
	private Skin skin;
	private final HeroClass classe;
	private int level;
	private int goldCoins;
	private final Position position; // position réelle
	private final Position screenPosition; // position SUR L'ECRAN
	private int xOffset, yOffset; // décalage entre position réelle (x,y) et la position (x,y) SUR L'ECRAN
	private int force = Statistics.FORCE;
	private int dexterity = Statistics.DEXTERITY;
	private int constitution = Statistics.CONSTITUTION;
	private final Gauge hpGauge = new Gauge(Statistics.HP, Statistics.HP); // HP / HPMax
	private final Gauge mpGauge = new Gauge(Statistics.MP, Statistics.MP); // MP / MPMax
	private Directions direction = Directions.DOWN; // regarde vers le bas par défaut
	private final Gauge alignGauge = new Gauge(Statistics.ALIGNMENT, 100);
	private boolean canSwordAttack, swordAttack, isBlocked, hasOverheadMessage, isWriting, isInEvent;
	private GameMap currentGameMap;
	private float critChanceRatio;

	private final Rectangle tempHitbox;
	private final EnumMap<Directions, Rectangle> swordHitbox = new EnumMap<>(Directions.class);
	private final EnumMap<Directions, Rectangle> swordHitboxScreen = new EnumMap<>(Directions.class);
	private int moveFrame = 7;

	/** Méthodes static **/
	private static int alignToTile(int value) { return value - (value % CELL_SIZE); }

	private static void createSwordHitBox(EnumMap<Directions, Rectangle> shb, int x, int y) { // HIT BOX EPEE (REELLE / A L'ECRAN)
		shb.put(Directions.DOWN,  new Rectangle(x, y + (CELL_SIZE / 2) + OFFSET_SWORD_PX, CELL_SIZE, CELL_SIZE / 2));
		shb.put(Directions.LEFT,  new Rectangle(x - OFFSET_SWORD_PX, y, CELL_SIZE / 2, CELL_SIZE));
		shb.put(Directions.RIGHT, new Rectangle(x + (CELL_SIZE / 2) + OFFSET_SWORD_PX, y, CELL_SIZE / 2, CELL_SIZE));
		shb.put(Directions.UP,    new Rectangle(x, y - (CELL_SIZE / 2) + OFFSET_SWORD_PX, CELL_SIZE, CELL_SIZE / 2));
	}

	/** Constructeur **/
	public Hero(String name, Skin skin, HeroClass hc, int level, int goldCoins, int xCell, int yCell, GameMap gameMap, float critChancePercent) {
		this.name = Util.requireNonBlank(name, "Le nom du héros");
		this.skin = Objects.requireNonNull(skin, "Skin du héros " + name + " null");
		classe = Objects.requireNonNull(hc, "Classe du héros " + name + " null");
		currentGameMap = Objects.requireNonNull(gameMap, "La carte actuelle du héros " + name + " null");

		if (level < 1 || level > LEVEL_MAX) throw new IllegalArgumentException("Niveau du héros " + name + " < 1 ou > " + LEVEL_MAX);
		if (goldCoins < 0) throw new IllegalArgumentException("Pieces d'or du heros " + name + " < 0");
		if (xCell < 0 || xCell >= gameMap.getWidth()) throw new IllegalArgumentException("Case x du héros " + name + " < 0 ou >= " + gameMap.getWidth());
		if (yCell < 0 || yCell >= gameMap.getHeight()) throw new IllegalArgumentException("Case y du héros " + name + " < 0 ou >= " + gameMap.getHeight());
		if (critChancePercent < 0 || critChancePercent > 100) throw new IllegalArgumentException("Pourcentage de chance de coups critiques du héros " + name + " < 0 ou > 100");

		this.level = level;
		this.goldCoins = goldCoins;
		position = new Position(xCell * CELL_SIZE, yCell * CELL_SIZE);
		screenPosition = new Position(alignToTile(WINDOW_WIDTH / 2), alignToTile(WINDOW_HEIGHT / 2));
		this.critChanceRatio = critChancePercent / 100f; // divise par 100 (pour obtenir un ratio)

		tempHitbox = new Rectangle(getX(), getY(), CELL_SIZE, CELL_SIZE);
		createSwordHitBox(swordHitbox, getX(), getY());
		createSwordHitBox(swordHitboxScreen, getXScreen(), getYScreen());
		updateOffset();
	}

	/** Getters **/
	public String getName() { return name; }
	public Skin getSkin() { return skin; }
	public HeroClass getHeroClass() { return classe; }
	public int getLevel() { return level; }
	public int getGoldCoins() { return goldCoins; }
	public int getXOffset() { return xOffset; }
	public int getYOffset() { return yOffset; }
	public int getX() { return position.getX(); }
	public int getY() { return position.getY(); }
	public int getXCell() { return position.getXCell(); }
	public int getYCell() { return position.getYCell(); }
	public String getPositionString() { return position.toString(); }
	public String getPositionCellsString() { return position.toStringCells(); }
	public Position getScreenPosition() { return screenPosition; }
	public int getXScreen() { return screenPosition.getX(); }
	public int getYScreen() { return screenPosition.getY(); }
	@Override public int getXCam() { return getXOffset(); }
	@Override public int getYCam() { return getYOffset(); }
	public int getForce() { return force; }
	public int getDexterity() { return dexterity; }
	public int getConstitution() { return constitution; }
	public int getHp() { return hpGauge.getValue(); }
	public int getHpMax() { return hpGauge.getMax(); }
	public double getHpRatio() { return hpGauge.getRatio(); }
	public int getMp() { return mpGauge.getValue(); }
	public int getMpMax() { return mpGauge.getMax(); }
	public double getMpRatio() { return mpGauge.getRatio(); }
	public Directions getDirection() { return direction; }
	public int getAlignment() { return alignGauge.getValue(); }
	public boolean canSwordAttack() { return canSwordAttack; }
	public boolean swordAttack() { return swordAttack; }
	public boolean isBlocked() { return isBlocked; }
	public boolean hasOverheadMessage() { return hasOverheadMessage; }
	public boolean isWriting() { return isWriting; }
	public boolean isInEvent() { return isInEvent; }
	public GameMap getCurrentGameMap() { return currentGameMap; }
	public float getCritChanceRatio() { return critChanceRatio; }
	public Rectangle getHitbox() { return position.getRectHitbox(CELL_SIZE); }
	public Rectangle getSwordHitbox() { return swordHitbox.get(direction); }
	public Rectangle getSwordHitboxScreen() { return swordHitboxScreen.get(direction); }
	public int getMoveFrame() { return moveFrame; }

	/** Setters **/
	public void setCanSwordAttack(boolean b) { canSwordAttack = b; }
	public void setSwordAttack(boolean b) { swordAttack = b; }
	public void setIsBlocked(boolean b) { isBlocked = b; }
	public void setHasOverheadMessage(boolean b) { hasOverheadMessage = b; }
	public void setIsWriting(boolean b) { isWriting = b; }
	public void setIsInEvent(boolean b) { isInEvent = b; }
	public void setCurrentGameMap(GameMap gameMap) { currentGameMap = Objects.requireNonNull(gameMap, "Carte actuelle null"); }
	public void setMoveFrame(int mf) {
		if (mf < 0 || mf >= 12) throw new IllegalArgumentException("Frame de déplacement hors bornes : " + mf);
		moveFrame = mf;
	}

	/** Autres méthodes **/
	private void incrementMoveFrame() { moveFrame = (moveFrame + 1) % 12; }

	public void updateOffset() {
		xOffset = screenPosition.getX() - position.getX();
		yOffset = screenPosition.getY() - position.getY();
	}

	public void updatePosition(int x, int y) { position.set(x, y); updateOffset(); }
	public void addAlignment(int value)      { alignGauge.add(value); }
	public void addHp(int value)             { hpGauge.add(value); }
	public void addMp(int value)             { mpGauge.add(value); }
	public void levelUp()                    { if (level < LEVEL_MAX) level++; }

	public void updateSwordHitBox() {
		final Rectangle rect = swordHitbox.get(direction);
		final int x = position.getX(), y = position.getY();
		switch (direction) {
			case DOWN   -> rect.setLocation(x, y + (CELL_SIZE / 2) + OFFSET_SWORD_PX);
			case LEFT   -> rect.setLocation(x - OFFSET_SWORD_PX, y);
			case RIGHT  -> rect.setLocation(x + (CELL_SIZE / 2) + OFFSET_SWORD_PX, y);
			case UP     -> rect.setLocation(x, y - (CELL_SIZE / 2) + OFFSET_SWORD_PX);
		}
	}

	public boolean move(Directions d) {
		Objects.requireNonNull(d, "Direction null");
		tempHitbox.setLocation(position.getX(), position.getY());
		switch (d) {
			case DOWN  -> tempHitbox.y += HERO_MOVE_STEP;
			case LEFT  -> tempHitbox.x -= HERO_MOVE_STEP;
			case RIGHT -> tempHitbox.x += HERO_MOVE_STEP;
			case UP    -> tempHitbox.y -= HERO_MOVE_STEP;
		}

		if (d != direction) direction = d;

		if (tempHitbox.x >= 0 && tempHitbox.y >= 0
			&& tempHitbox.x <= (currentGameMap.getWidth() - 1) * CELL_SIZE
			&& tempHitbox.y <= (currentGameMap.getHeight() - 1) * CELL_SIZE
			&& !currentGameMap.hasWallCollision(tempHitbox)) {

			updatePosition(tempHitbox.x, tempHitbox.y);
			incrementMoveFrame();
			System.out.printf("%s s'est deplacé vers %-6s : %s\n", name, direction, position.toStringCells());
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Hero[name: %s, skin: %s, class: %s, level: %d, gold: %d, HP: %s, MP: %s, carte: %s, position: %s]"
				.formatted(name, skin, classe, level, goldCoins, hpGauge, mpGauge, currentGameMap.getName(), position);
	}
}