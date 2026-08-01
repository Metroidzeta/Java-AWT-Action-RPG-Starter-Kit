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

import static core.Config.DARK_BLUE_TRANSPARENT;
import static core.Config.DARK_GREEN_TRANSPARENT;
import static core.Config.BURGUNDY_TRANSPARENT;
import static core.Config.DARK_GOLD_TRANSPARENT;
import static core.Config.DARK_GRAY_TRANSPARENT;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.FontMetrics;
import java.util.Objects;

public final class RendererContext {

	private final Font defaultFont;
	private final Font fpsFont;

	private static final Color[] BOX_COLORS = {
		DARK_BLUE_TRANSPARENT,
		DARK_GREEN_TRANSPARENT,
		BURGUNDY_TRANSPARENT,
		DARK_GOLD_TRANSPARENT,
		DARK_GRAY_TRANSPARENT
	};
	private int boxColorIndex = 0;

	/** Constructeur **/
	public RendererContext(Font defaultFont, Font fpsFont) {
		this.defaultFont = Objects.requireNonNull(defaultFont, "DefaultFont null");
		this.fpsFont = Objects.requireNonNull(fpsFont, "FpsFont null");
	}

	/** Getters **/
	public Font getDefaultFont() { return defaultFont; }
	public Font getFpsFont() { return fpsFont; }
	public Color getCurrentBoxColor() { return BOX_COLORS[boxColorIndex]; }

	/** Autres méthodes **/
	public void drawRectangle(Graphics g, Color color, Rectangle rect) {
		if (color == null) color = Color.WHITE;
		g.setColor(color);
		g.fillRect(rect.x, rect.y, rect.width, rect.height);
	}

	public void drawText(Graphics g, Color color, Font font, String text, int x, int y) {
		if (color == null) color = Color.WHITE;
		if (font == null) font = defaultFont;
		g.setColor(color);
		g.setFont(font);

		FontMetrics fm = g.getFontMetrics(font);
		int yDraw = y + fm.getAscent(); // décalage exact jusqu’au haut du texte
		g.drawString(text, x, yDraw);
	}

	public void drawWrappedText(Graphics g, Color color, Font font, String text, int maxWidth, int x, int y) {
		if (text == null || text.isEmpty()) return;
		if (color == null) color = Color.WHITE;
		if (font == null) font = defaultFont;
		g.setColor(color);
		g.setFont(font);

		FontMetrics fm = g.getFontMetrics(font);
		final int lineHeight = fm.getHeight();
		int yDraw = y + fm.getAscent();

		// Découpe par paragraphes pour gérer les retours à la ligne (\n)
		String[] paragraphs = text.split("\n");
		for (int p = 0; p < paragraphs.length; p++) {
			String paragraph = paragraphs[p];
			List<String> lines = new ArrayList<>();
			StringBuilder currentLine = new StringBuilder(64);

			for (String word : paragraph.split("\\s+")) { // gère espaces, tabulations, etc.
				if (word.isEmpty()) continue;

				String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;

				// --- Si la ligne dépasse la largeur max ---
				if (fm.stringWidth(testLine) > maxWidth) {
					if (!currentLine.isEmpty()) {
						lines.add(currentLine.toString());
						currentLine.setLength(0);
					}

					// --- Si un mot seul dépasse maxWidth, on le découpe proprement ---
					if (fm.stringWidth(word) > maxWidth) {
						StringBuilder chunk = new StringBuilder();
						for (char c : word.toCharArray()) {
							chunk.append(c);
							if (fm.stringWidth(chunk.toString()) > maxWidth) {
								lines.add(chunk.substring(0, chunk.length() - 1));
								chunk.setLength(1);
								chunk.setCharAt(0, c);
							}
						}
						if (chunk.length() > 0) lines.add(chunk.toString());
					} else {
						currentLine.append(word);
					}
				} else {
					currentLine.setLength(0);
					currentLine.append(testLine);
				}
			}

			if (currentLine.length() > 0) lines.add(currentLine.toString());

			// --- Dessin du texte pour ce paragraphe ---
			for (String line : lines) {
				g.drawString(line, x, yDraw);
				yDraw += lineHeight;
			}

			// --- Espace entre paragraphes (retour à la ligne manuel) ---
			if (p < paragraphs.length - 1) yDraw += lineHeight / 2;
		}
	}

	public void nextBoxColor() { boxColorIndex = (boxColorIndex + 1) % BOX_COLORS.length; }
}
