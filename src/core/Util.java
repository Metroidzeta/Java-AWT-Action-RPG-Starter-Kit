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

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Files;
import java.awt.FontMetrics;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public final class Util {

	private Util() { throw new AssertionError("La classe Util ne doit pas être instanciée."); } // Empêche toute instanciation

	public static final int[] VB = { 112, 57, 15, 232, 214, 177, 147, 108, 80, 32, 18 };
	public static int keyForIndex(int i) { return (i * 31 + 7) & 0xFF; }

	public static String requireNonBlank(String s, String name) {
		if (s == null) throw new NullPointerException(name + " null");
		if (s.isBlank()) throw new IllegalArgumentException(name + " vide");
		return s;
	}

	public static int[][] createIntMatrix(int height, int width, int defaultValue) {
		int[][] matrix = new int[height][width];
		for (int i = 0; i < height; i++) Arrays.fill(matrix[i], defaultValue);
		return matrix;
	}

	public static int[][] copyMatrix(int[][] source) {
		final int[][] copy = new int[source.length][];
		for (int i = 0; i < source.length; i++) {
			copy[i] = Arrays.copyOf(source[i], source[i].length);
		}
		return copy;
	}

	public static boolean[][] copyMatrix(boolean[][] source) {
		final boolean[][] copy = new boolean[source.length][];
		for (int i = 0; i < source.length; i++) {
			copy[i] = Arrays.copyOf(source[i], source[i].length);
		}
		return copy;
	}

	public static BufferedImage loadImage(String chemin) {
		Path path = Path.of(chemin);
		if (!Files.exists(path)) { System.err.println("Fichier introuvable : " + chemin); return null; }
		try {
			BufferedImage img = ImageIO.read(path.toFile());
			if (img == null) throw new IOException("Format non supporté ou image corrompue : " + chemin);
			return img;
		} catch (IOException e) {
			throw new RuntimeException("Erreur lors du chargement de l'image : " + chemin, e);
		}
	}

	public static int calculateTextHeight(String text, FontMetrics fm, int maxWidth) {
		if (text == null || text.isEmpty()) return 0;

		int totalHeight = 0;
		final int lineHeight = fm.getHeight();
		final String[] paragraphs = text.split("\n");

		for (int p = 0; p < paragraphs.length; p++) {
			String paragraph = paragraphs[p];
			List<String> lines = new ArrayList<>();
			StringBuilder currentLine = new StringBuilder(64);

			for (String word : paragraph.split("\\s+")) {
				if (word.isEmpty()) continue;

				String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;

				if (fm.stringWidth(testLine) > maxWidth) {
					if (!currentLine.isEmpty()) {
						lines.add(currentLine.toString());
						currentLine.setLength(0);
					}

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
			totalHeight += lines.size() * lineHeight;

			// --- Espace entre paragraphes ---
			if (p < paragraphs.length - 1) totalHeight += lineHeight / 2;
		}

		return totalHeight;
	}

	public static int getWrappedTextWidth(String text, FontMetrics fm, int maxWidth) {
		if (text == null || text.isEmpty()) return 0;

		int maxLineWidth = 0;

		for (String paragraph : text.split("\n")) {
			StringBuilder currentLine = new StringBuilder(64);

			for (String word : paragraph.split("\\s+")) {
				if (word.isEmpty()) continue;

				String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
				int testWidth = fm.stringWidth(testLine);

				if (testWidth > maxWidth) {
					if (currentLine.length() > 0) {
						maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(currentLine.toString()));
						currentLine.setLength(0);
					}

					if (fm.stringWidth(word) > maxWidth) {
						StringBuilder chunk = new StringBuilder();
						for (char c : word.toCharArray()) {
							chunk.append(c);
							if (fm.stringWidth(chunk.toString()) > maxWidth) {
								maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(chunk.substring(0, chunk.length() - 1)));
								chunk.setLength(1);
								chunk.setCharAt(0, c);
							}
						}
						if (chunk.length() > 0)
							maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(chunk.toString()));
					} else {
						currentLine.append(word);
					}
				} else {
					currentLine.setLength(0);
					currentLine.append(testLine);
					maxLineWidth = Math.max(maxLineWidth, testWidth);
				}
			}

			if (currentLine.length() > 0) maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(currentLine.toString()));
		}

		return maxLineWidth;
	}
}