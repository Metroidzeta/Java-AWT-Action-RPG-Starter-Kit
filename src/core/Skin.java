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
import static core.Config.SKINS_DIRECTORY;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Objects;

public final class Skin {

	private static final int REGION_WIDTH = 48;
	private static final int REGION_HEIGHT = 48;
	private static final int ROWS = 4;
	private static final int COLS = 3;
	private static final int TOTAL_REGIONS = ROWS * COLS;

	private final String name;
	private final BufferedImage[] textureRegions; // régions de l'image source découpée

	/** Méthodes static **/
	private static BufferedImage[] extractRegions(BufferedImage image) {
		Objects.requireNonNull(image, "Image source de la skin null");
		final int minWidth = COLS * REGION_WIDTH;
		final int minHeight = ROWS * REGION_HEIGHT;

		if (image.getWidth() < minWidth || image.getHeight() < minHeight) {
			throw new IllegalArgumentException(
				"Dimensions de skin invalides : " + image.getWidth() + "x" + image.getHeight()
				+ " (minimum " + minWidth + "x" + minHeight + " pixels)"
			);
		}

		final BufferedImage[] result = new BufferedImage[TOTAL_REGIONS];
		for (int i = 0; i < ROWS; i++) {
			final int y = i * REGION_HEIGHT;
			final int lineIndex = i * COLS;
			for (int j = 0; j < COLS; j++) {
				result[lineIndex + j] = image.getSubimage(j * REGION_WIDTH, y, REGION_WIDTH, REGION_HEIGHT);
			}
		}
		return result;
	}

	/** Constructeur **/
	public Skin(String fileName) {
		name = Util.requireNonBlank(fileName, "Le nom du fichier de la skin");
		final BufferedImage texture = Util.loadImage(SKINS_DIRECTORY + "/" + fileName); // image source
		textureRegions = extractRegions(texture);
	}

	/** Getters **/
	public String getName() { return name; }

	/** Autres méthodes **/
	public void draw(Graphics g, int numRegion, int x, int y) {
		Objects.requireNonNull(g, "Graphics passé en paramètre null");
		if (numRegion < 0 || numRegion >= TOTAL_REGIONS) throw new IllegalArgumentException("Indice de région invalide : " + numRegion);
		g.drawImage(textureRegions[numRegion], x, y, CELL_SIZE, CELL_SIZE, null);
	}

	@Override
	public String toString() {
		return "Skin[name=%s]".formatted(name);
	}
}