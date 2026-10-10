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

import core.Camera;
import core.GameMap;
import core.Chipset;
import core.Hero;
import core.Enemy;
import core.GameMap.CellBounds;

import static core.Config.*;
import static core.renderer.RenderMetrics.WH_20PERCENT;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Gestion du rendu de la carte.
 */
public final class MapRenderer {

	private final Camera camera;
	private final Hero hero;
	private final RendererContext ctx;
	private final CellBounds bounds = new CellBounds();

	/** Constructeur **/
	public MapRenderer(Camera camera, Hero hero, RendererContext ctx) {
		this.camera = Objects.requireNonNull(camera, "Camera null");
		this.hero = Objects.requireNonNull(hero, "Hero null");
		this.ctx = Objects.requireNonNull(ctx, "RendererContext null");
	}

	public void calculateDisplayBounds() { // limiter l'affichage des murs à la vue de la caméra (optimisation)
		final int xCellCam = (int)Math.floor(-camera.getX() / CELL_SIZE);
		final int yCellCam = (int)Math.floor(-camera.getY() / CELL_SIZE);
		final GameMap gameMap = hero.getCurrentGameMap();

		bounds.set(
			Math.max(xCellCam - 1, 0), // x0
			Math.min(xCellCam + WINDOW_WIDTH_CELLS + 2, gameMap.getWidth()), // x1
			Math.max(yCellCam - 1, 0), // y0
			Math.min(yCellCam + WINDOW_HEIGHT_CELLS + 2, gameMap.getHeight())// y1
		);

		if (DEBUG_MODE) System.out.printf("displayBounds xCells: %d-%d, yCells: %d-%d%n", bounds.x0(), bounds.x1(), bounds.y0(), bounds.y1());
	}

	/** Affichage - Carte **/
	public void drawLayer(Graphics g, int layerIndex) {
		if (g instanceof Graphics2D g2d) g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		final int xCam = (int)camera.getX(), yCam = (int)camera.getY();
		final GameMap gameMap = hero.getCurrentGameMap();
		final Chipset chipset = gameMap.getChipset();
		//final int tileSize = chipset.getTileSize();
		//final int tileCountWidth = chipset.getTileCountWidth();

		for (int i = bounds.y0(); i < bounds.y1(); i++) {
			for (int j = bounds.x0(); j < bounds.x1(); j++) {
				int numTile = gameMap.getNumTile(layerIndex, i, j) - 1; // - 1 car le tableau de tuiles du chipset commence à 0
				if (numTile >= 0) {
					/* g.drawImage(
						chipset.getTexture(),
						j * CELL_SIZE + xCam, i * CELL_SIZE + yCam, (j + 1) * CELL_SIZE + xCam, (i + 1) * CELL_SIZE + yCam,
						(numTile % tileCountWidth) * tileSize, (numTile / tileCountWidth) * tileSize,
						((numTile % tileCountWidth) + 1) * tileSize, ((numTile / tileCountWidth) + 1) * tileSize,
						null
					); */
					g.drawImage(chipset.getTile(numTile), j * CELL_SIZE + xCam, i * CELL_SIZE + yCam, CELL_SIZE, CELL_SIZE, null);
				}
			}
		}
	}

	public void drawWalls(Graphics g) {
		final int xCam = (int)camera.getX(), yCam = (int)camera.getY();
		final GameMap gameMap = hero.getCurrentGameMap();
		final Rectangle wallRect = new Rectangle(CELL_SIZE, CELL_SIZE);

		for (int i = bounds.y0(); i < bounds.y1(); i++) {
			for (int j = bounds.x0(); j < bounds.x1(); j++) {
				if (gameMap.isWall(i, j)) {
					wallRect.setLocation(j * CELL_SIZE + xCam, i * CELL_SIZE + yCam);
					ctx.drawRectangle(g, PURPLE_TRANSPARENT, wallRect);
				}
			}
		}
	}

	public void drawEnemies(Graphics g) {
		final int xCam = (int)camera.getX(), yCam = (int)camera.getY();

		final Rectangle dstRect = new Rectangle(CELL_SIZE, CELL_SIZE);
		ArrayList<Enemy> enemies = hero.getCurrentGameMap().getEnemies();

		for (Enemy enemy : enemies) {
			final int yCell = enemy.getYCell(), xCell = enemy.getXCell();
			if (yCell >= bounds.y0() && yCell < bounds.y1() && xCell >= bounds.x0() && xCell < bounds.x1()) {
				dstRect.setLocation(enemy.getX() + xCam, enemy.getY() + yCam);
				enemy.draw(g,28, dstRect);
			}
		}
	}

	public void drawDamage(Graphics g, int number, Color color, double t, ArrayList<Rectangle> hitEnemyHitboxes) {
		final int xCam = (int)camera.getX(), yCam = (int)camera.getY();
		final int yOffset = (int)(WH_20PERCENT * t);

		final Font font = ctx.getDefaultFont();
		final String text = Integer.toString(number);
		final int textWidth = g.getFontMetrics(font).stringWidth(text);

		for (Rectangle enemyHitbox : hitEnemyHitboxes) {
			final int x = enemyHitbox.x + xCam + (CELL_SIZE - textWidth) / 2;
			final int y = enemyHitbox.y + yCam - CELL_SIZE / 2 - yOffset;

			ctx.drawText(g, color, font, text, x, y);
		}
	}
}