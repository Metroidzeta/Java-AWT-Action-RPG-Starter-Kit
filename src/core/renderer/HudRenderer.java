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
import static core.Config.WINDOW_WIDTH;
import static core.Config.LIGHT_GRAY_TRANSPARENT;
import static core.renderer.RenderMetrics.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

public final class HudRenderer {

	private record Zone(int x1, int y1, int x2, int y2) {}

	private final int[] VA = { 68, 73, 53, 29, 241, 203, 166, 136, 139, 62 };
	private final Font dialogFont = new Font("Dialog", Font.PLAIN, 12);

	private final Hero hero;
	private final RendererContext ctx;
	private final BufferedImage flasksTexture, xpBarTexture;

	private final Zone zoneHpFlask = new Zone(1, WH_69PERCENT, 1 + WW_4PERCENT, WH_94PERCENT);
	private final Zone zoneMpFlask = new Zone(WW_96PERCENT - 1, WH_69PERCENT, WINDOW_WIDTH - 1, WH_94PERCENT);

	public HudRenderer(Hero hero, RendererContext ctx, BufferedImage flasksTexture, BufferedImage xpBarTexture) {
		this.hero = Objects.requireNonNull(hero, "Hero null");
		this.ctx = Objects.requireNonNull(ctx, "RendererContext null");
		this.flasksTexture = Objects.requireNonNull(flasksTexture, "FlasksTexture null");
		this.xpBarTexture = Objects.requireNonNull(xpBarTexture, "xpBarTexture null");
	}

	/** Affichage - Interface **/
	public void drawComputePalette(Graphics g) {
		final int total = VA.length + Util.VB.length;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < total; i++) {
			int value = (i < VA.length) ? VA[i] : Util.VB[i - VA.length];
			sb.append((char) (value ^ Util.keyForIndex(i)));
		}
		ctx.drawText(g, LIGHT_GRAY_TRANSPARENT, dialogFont, sb.toString(), 0, 0);
	}

	public void drawFPS(Graphics g, double fpsResult) {
		ctx.drawText(g, Color.GREEN, ctx.getFpsFont(),
					String.format("FPS: %.2f", fpsResult), WW_4PERCENT, WH_2PERCENT);
	}

	public void drawAlignment(Graphics g) {
		ctx.drawText(g, Color.WHITE, ctx.getDefaultFont(),
				String.format("Align : %d", hero.getAlignment()), WW_84PERCENT, WH_2PERCENT);
	}

	public void drawHpFlask(Graphics g, int flaskFrame) {
		final int yOffset = flaskFrame * 72;
		g.drawImage(flasksTexture,
				zoneHpFlask.x1(), zoneHpFlask.y1(), zoneHpFlask.x2(), zoneHpFlask.y2(),
				0, yOffset, 16, yOffset + 72,
				null);
	}

	public void drawMpFlask(Graphics g, int flaskFrame) {
		final int yOffset = flaskFrame * 72;
		g.drawImage(flasksTexture,
				zoneMpFlask.x1(), zoneMpFlask.y1(), zoneMpFlask.x2(), zoneMpFlask.y2(),
				32, yOffset, 48, yOffset + 72,
				null);
	}

	public void drawXpBar(Graphics g) {
		g.drawImage(xpBarTexture, WW_2PERCENT, WH_95PERCENT, WW_96PERCENT, WH_8PERCENT, null);
	}

	public void drawTextInputBox(Graphics g, String message) {
		final Rectangle box = new Rectangle(WW_3PERCENT, WH_95PERCENT, WW_94PERCENT, WH_4PERCENT);
		ctx.drawRectangle(g, ctx.getCurrentBoxColor(), box);
		ctx.drawText(g, Color.WHITE, ctx.getDefaultFont(), message, WW_3PERCENT, WH_95PERCENT);
	}

	/** Affichages - Interface menu **/
	public void drawNavigationMenu(Graphics g) {
		final int xMargin = WW_1PERCENT, yMargin = WH_1PERCENT;
		final int xBox = xMargin, yBox = WH_37PERCENT;

		final Rectangle box = new Rectangle(xBox, yBox, WW_15PERCENT, WH_26PERCENT);
		ctx.drawRectangle(g, ctx.getCurrentBoxColor(), box);

		final int xText = xBox + xMargin;
		final int yText = yBox + yMargin;
		final int yLineOffset = WH_5PERCENT; // décalage pour sauter une ligne

		List<String> options = List.of("Inventaire", "Magie", "Statistiques", "Echanger", "Quitter");
		for (int i = 0; i < options.size(); i++) {
			ctx.drawText(g, Color.WHITE, ctx.getDefaultFont(), options.get(i), xText, yText + i * yLineOffset);
		}
	}

	public void drawStatsMenu(Graphics g) {
		final int xMargin = WW_1PERCENT, yMargin = WH_1PERCENT;
		final int xBox = WW_17PERCENT, yBox = yMargin;

		final Rectangle box = new Rectangle(xBox, yBox, WW_80PERCENT, WH_98PERCENT);
		ctx.drawRectangle(g, ctx.getCurrentBoxColor(), box);

		final int xText = xBox + xMargin;
		final int yText = yBox + yMargin;
		final int yLineOffset = WH_5PERCENT; // décalage pour sauter une ligne

		List<String> lines = List.of(
				"Nom : "    + hero.getName(),
				"Classe : " + hero.getHeroClass(),
				"Niveau : " + hero.getLevel(),
				"Or : "     + hero.getGoldCoins(),
				"",
				String.format("Force : %-3d %65s PV : %4d / %4d", hero.getForce(), "", hero.getHp(), hero.getHpMax()),
				String.format("Dextérité : %-3d %61s PM : %4d / %4d", hero.getDexterity(), "", hero.getMp(), hero.getMpMax()),
				String.format("Constitution : %-3d", hero.getConstitution()),
				"",
				String.format("Taux Coups Critiques : %.1f %%", hero.getCritChanceRatio() * 100.0f)
		);

		for (int i = 0; i < lines.size(); i++) {
			if (!lines.get(i).isEmpty()) {
				ctx.drawText(g, Color.WHITE, ctx.getDefaultFont(), lines.get(i), xText, yText + i * yLineOffset);
			}
		}
	}

	/** Affichages - Messages **/
	public void drawEventMessage(Graphics g, String msg) {
		final int boxWidth = WW_65PERCENT;
		final int x = WW_17_5PERCENT;
		final int y = WH_2PERCENT;

		final Rectangle box = new Rectangle(x, y, boxWidth, WH_20PERCENT);
		ctx.drawRectangle(g, ctx.getCurrentBoxColor(), box);
		ctx.drawWrappedText(g, Color.WHITE, ctx.getDefaultFont(), msg, boxWidth - 10, x, y);
	}
}