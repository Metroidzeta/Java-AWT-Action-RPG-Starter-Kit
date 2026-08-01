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
import core.Hero;
import static core.Config.WINDOW_WIDTH;
import static core.Config.WINDOW_HEIGHT;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Graphics;

/**
 * Gestionnaire central de l'affichage du moteur de jeu.
 */
public final class Renderer {

	private final RendererContext ctx;
	private final MapRenderer mapRenderer;
	private final HeroRenderer heroRenderer;
	private final HudRenderer hudRenderer;

	private static final Rectangle BACKGROUND_RECT = new Rectangle(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT); // Rectangle background fond noir

	/** Constructeur **/
	public Renderer(Camera camera, Hero hero, Font fpsFont, Font baseFont, BufferedImage flasksTexture, BufferedImage xpBarTexture) {
		this.ctx = new RendererContext(baseFont, fpsFont);
		this.mapRenderer = new MapRenderer(camera, hero, ctx);
		this.heroRenderer = new HeroRenderer(hero, ctx);
		this.hudRenderer = new HudRenderer(hero, ctx, flasksTexture, xpBarTexture);
	}

	/** Getters **/
	public RendererContext getContext() { return ctx; }

	/** Autres méthodes de dessin **/
	public void drawBlackBackground(Graphics g) { ctx.drawRectangle(g, Color.BLACK, BACKGROUND_RECT); }

	/** Affichage - Interface **/
	public void drawComputePalette(Graphics g)               { hudRenderer.drawComputePalette(g); }
	public void drawFPS(Graphics g, double fpsResult)        { hudRenderer.drawFPS(g, fpsResult); }
	public void drawAlignment(Graphics g)                    { hudRenderer.drawAlignment(g); }
	public void drawHpFlask(Graphics g, int flaskFrame)      { hudRenderer.drawHpFlask(g, flaskFrame); }
	public void drawMpFlask(Graphics g, int flaskFrame)      { hudRenderer.drawMpFlask(g, flaskFrame); }
	public void drawXpBar(Graphics g)                        { hudRenderer.drawXpBar(g); }
	public void drawTextInputBox(Graphics g, String message) { hudRenderer.drawTextInputBox(g, message); }

	/** Affichages - Interface menu **/
	public void drawNavigationMenu(Graphics g) { hudRenderer.drawNavigationMenu(g);}
	public void drawStatsMenu(Graphics g)      { hudRenderer.drawStatsMenu(g); }

	/** Affichages - Messages **/
	public void drawEventMessage(Graphics g, String msg) { hudRenderer.drawEventMessage(g, msg); }

	/** Affichage - Héros **/
	public void drawHero(Graphics g)                            { heroRenderer.drawHero(g); }
	public void drawHeroSwordHitbox(Graphics g)                 { heroRenderer.drawHeroSwordHitbox(g); }
	public void drawHeroOverheadMessage(Graphics g, String msg) { heroRenderer.drawHeroOverheadMessage(g, msg); }

	/** Affichage - Carte **/
	public void updateMapDisplayBounds()              { mapRenderer.calculateDisplayBounds(); }
	public void drawLayer(Graphics g, int layerIndex) { mapRenderer.drawLayer(g, layerIndex); }
	public void drawWalls(Graphics g)                 { mapRenderer.drawWalls(g); }
}