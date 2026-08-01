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

import core.Hero;
import core.Util;
import static core.Config.CELL_SIZE;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.util.Objects;

public final class HeroRenderer {

	private static final int OVERHEAD_MESSAGE_PADDING = 3; // marge interne à gauche/droite/haut/bas
	private static final int OVERHEAD_MESSAGE_WIDTH_CELLS = 7;

	private final Hero hero;
	private final RendererContext ctx;

	/** Constructeur **/
	public HeroRenderer(Hero hero, RendererContext ctx) {
		this.hero = Objects.requireNonNull(hero, "Hero null");
		this.ctx = Objects.requireNonNull(ctx, "RendererContext null");
	}

	/** Affichage - Héros **/
	private void drawHeroName(Graphics g, int x, int y) {
		String name = hero.getName();
		Font defaultFont = ctx.getDefaultFont();
		FontMetrics fm = g.getFontMetrics(defaultFont);

		// Largeur et hauteur réelle du texte
		final int textWidth = fm.stringWidth(name);
		final int textHeight = fm.getHeight();

		// Position centrée horizontalement en-dessous du héros
		final int xText = x + (CELL_SIZE / 2) - (textWidth / 2);
		final int yText = y - fm.getDescent() + (CELL_SIZE / 3) + textHeight + 1; // + 1 ajuste légèrement la hauteur

		ctx.drawText(g, Color.WHITE, defaultFont, name, xText, yText);
	}

	private void drawHeroSkin(Graphics g, int x, int y) {
		final int regionIndex = hero.getDirection().ordinal() * 3 + (hero.getMoveFrame() / 4);
		hero.getSkin().draw(g, regionIndex, x, y);
	}

	public void drawHero(Graphics g) {
		final int x = hero.getXScreen(), y = hero.getYScreen();
		drawHeroName(g, x, y); // dessiner nom du héros
		drawHeroSkin(g, x, y); // dessiner skin du héros
	}

	public void drawHeroSwordHitbox(Graphics g) {
		ctx.drawRectangle(g, Color.WHITE, hero.getSwordHitboxScreen());
	}

	public void drawHeroOverheadMessage(Graphics g, String msg) {
		if (msg == null || msg.isEmpty()) return;

		final Font defaultFont = ctx.getDefaultFont();
		final FontMetrics fm = g.getFontMetrics(defaultFont);

		final int boxMaxWidth  = OVERHEAD_MESSAGE_WIDTH_CELLS * CELL_SIZE; // largeur externe cible
		final int innerMax = Math.max(1, boxMaxWidth  - (OVERHEAD_MESSAGE_PADDING * 2)); // largeur dispo pour le texte

		// Mesures cohérentes avec le même innerMax que pour le rendu
		final int textHeight = Util.calculateTextHeight(msg, fm, innerMax);
		final int textWidth  = Util.getWrappedTextWidth(msg, fm, innerMax);

		// Taille réelle du cadre (texte + padding de chaque côté)
		final int boxWidth  = textWidth  + (OVERHEAD_MESSAGE_PADDING * 2);
		final int boxHeight = textHeight + (OVERHEAD_MESSAGE_PADDING * 2);

		// Position du cadre (on centre sur le héros avec la largeur réelle du cadre)
		final int xBox = hero.getXScreen() + CELL_SIZE / 2 - boxWidth / 2;
		final int yBox = hero.getYScreen() - CELL_SIZE / 4 - boxHeight;

		Rectangle boxRect = new Rectangle(xBox, yBox, boxWidth, boxHeight);
		ctx.drawRectangle(g, ctx.getCurrentBoxColor(), boxRect);

		// IMPORTANT : on dessine à l'intérieur (x + padding, y + padding) et on wrap avec innerMax
		ctx.drawWrappedText(g, Color.WHITE, defaultFont, msg, innerMax, xBox + OVERHEAD_MESSAGE_PADDING, yBox + OVERHEAD_MESSAGE_PADDING);
	}
}