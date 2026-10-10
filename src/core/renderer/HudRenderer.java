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
	private final static List<String> OPTIONS_MENU = List.of("Inventaire", "Magie", "Statistiques", "Echanger", "Quitter");
	private final Font dialogFont = new Font("Dialog", Font.PLAIN, 12);

	private final Hero hero;
	private final RendererContext ctx;
	private final BufferedImage flasksTexture, xpBarTexture;

	private long cachedFps = -1;
	private String fpsText;

	private int cachedAlignment = -1;
	private String alignmentText;

	private final Zone zoneHpFlask = new Zone(1, WH_69PERCENT, 1 + WW_4PERCENT, WH_94PERCENT);
	private final Zone zoneMpFlask = new Zone(WW_96PERCENT - 1, WH_69PERCENT, WINDOW_WIDTH - 1, WH_94PERCENT);

	private final Rectangle boxTextInput = new Rectangle(WW_3PERCENT, WH_95PERCENT, WW_94PERCENT, WH_4PERCENT);
	private final Rectangle boxEventMessage = new Rectangle(WW_17_5PERCENT, WH_2PERCENT, WW_65PERCENT, WH_20PERCENT);
	private final Rectangle boxNavigationMenu = new Rectangle(WW_1PERCENT, WH_37PERCENT, WW_15PERCENT, WH_26PERCENT);
	private final Rectangle boxStatsMenu = new Rectangle(WW_17PERCENT, WH_1PERCENT, WW_80PERCENT, WH_98PERCENT);

	public HudRenderer(Hero hero, RendererContext ctx, BufferedImage flasksTexture, BufferedImage xpBarTexture) {
		this.hero = Objects.requireNonNull(hero, "Hero null");
		this.ctx = Objects.requireNonNull(ctx, "RendererContext null");
		this.flasksTexture = Objects.requireNonNull(flasksTexture, "FlasksTexture null");
		this.xpBarTexture = Objects.requireNonNull(xpBarTexture, "xpBarTexture null");
		setFPSText(0.0);
		setAlignmentText(hero.getAlignment());
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

	public void setFPSText(double fps) {
		final long roundedFps = Math.round(fps * 100.0);
		if (cachedFps != roundedFps) {
			cachedFps = roundedFps;
			fpsText = String.format("FPS: %.2f", roundedFps / 100.0);
		}
	}

	public void drawFPS(Graphics g) {
		ctx.drawText(g, Color.GREEN, ctx.getFpsFont(), fpsText, WW_4PERCENT, WH_2PERCENT);
	}

	public void setAlignmentText(int alignment) {
		if (cachedAlignment != alignment) {
			cachedAlignment = alignment;
			alignmentText = String.format("Align : %d", alignment);
		}
	}

	public void drawAlignment(Graphics g) {
		ctx.drawText(g, Color.WHITE, ctx.getDefaultFont(), alignmentText, WW_84PERCENT, WH_2PERCENT);
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

	public void drawTextInput(Graphics g, String message) {
		ctx.drawRectangle(g, ctx.getCurrentBoxColor(), boxTextInput);
		ctx.drawText(g, Color.WHITE, ctx.getDefaultFont(), message, boxTextInput.x, boxTextInput.y);
	}

	/** Affichages - Interface menu **/
	public void drawNavigationMenu(Graphics g) {
		ctx.drawRectangle(g, ctx.getCurrentBoxColor(), boxNavigationMenu);

		final int xText = boxNavigationMenu.x + WW_1PERCENT;
		final int yText = boxNavigationMenu.y + WH_1PERCENT;

		for (int i = 0; i < OPTIONS_MENU.size(); i++) {
			ctx.drawText(g, Color.WHITE, ctx.getDefaultFont(), OPTIONS_MENU.get(i), xText, yText + i * WH_5PERCENT);
		}
	}

	public void drawStatsMenu(Graphics g) {
		ctx.drawRectangle(g, ctx.getCurrentBoxColor(), boxStatsMenu);

		final int xText = boxStatsMenu.x + WW_1PERCENT;
		final int yText = boxStatsMenu.y + WH_1PERCENT;

		List<String> rows = List.of(
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

		for (int i = 0; i < rows.size(); i++) {
			if (!rows.get(i).isEmpty()) {
				ctx.drawText(g, Color.WHITE, ctx.getDefaultFont(), rows.get(i), xText, yText + i * WH_5PERCENT);
			}
		}
	}

	/** Affichages - Messages **/
	public void drawEventMessage(Graphics g, String msg) {
		ctx.drawRectangle(g, ctx.getCurrentBoxColor(), boxEventMessage);
		ctx.drawWrappedText(g, Color.WHITE, ctx.getDefaultFont(), msg, boxEventMessage.width - 10, boxEventMessage.x, boxEventMessage.y);
	}
}