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

import static core.Config.CHIPSETS_DIRECTORY;

import java.util.Objects;
import java.awt.image.BufferedImage;

/**
 * Représente un chipset, c’est-à-dire un ensemble de tuiles graphiques
 * découpées à partir d’une image source unique servant à composer les cartes du jeu.
 */
public final class Chipset {

	public static final int EMPTY_TILE = 0;

	private final String name;
	private final int tileSize; // Taille d'une tuile en pixels (n*n)
	private final int tileCountWidth, tileCountHeight; // nombre de tuiles en cases
	private final BufferedImage[] tiles;

	/** Méthodes static **/
	private static BufferedImage[] extractTiles(BufferedImage image, int tileSize, int tileCountWidth, int tileCountHeight) {
		final int nbTiles = tileCountWidth * tileCountHeight;
		final BufferedImage[] result = new BufferedImage[nbTiles];
		for (int i = 0; i < tileCountHeight; i++) {
			final int y = i * tileSize;
			final int ligneIndex = i * tileCountWidth;
			for (int j = 0; j < tileCountWidth; j++) {
				result[ligneIndex + j] = image.getSubimage(j * tileSize, y, tileSize, tileSize);
			}
		}
		return result;
	}

	/** Constructeur **/
	public Chipset(String fileName, int tileSize) {
		name = Util.requireNonBlank(fileName, "Nom du fichier du chipset");
		if (tileSize < 1) throw new IllegalArgumentException("TileSize du chipset " + name + " < 1");
		this.tileSize = tileSize;

		final BufferedImage texture = Util.loadImage(CHIPSETS_DIRECTORY + "/" + fileName); // Charger l'image source
		Objects.requireNonNull(texture, "Texture du chipset[" + name + "] null");
		final int textureWidth = texture.getWidth(), textureHeight = texture.getHeight();
		if (textureWidth % tileSize != 0 || textureHeight % tileSize != 0) {
			throw new IllegalArgumentException(
					(
						"Dimensions de l'image incompatibles avec la taille de tuile : %dx%d "
						+ "pour une taille de tuile de %d pixels"
					).formatted(textureWidth, textureHeight, tileSize)
			);
		}
		tileCountWidth = textureWidth / tileSize;
		tileCountHeight = textureHeight / tileSize;
		tiles = extractTiles(texture, tileSize, tileCountWidth, tileCountHeight);
	}

	/** Getters **/
	public String getName() { return name; }
	public int getTileSize() { return tileSize; }
	public int getTileCount() { return tiles.length; }
	public int getTileCountWidth() { return tileCountWidth; }
	public int getTileCountHeight() { return tileCountHeight; }
	public BufferedImage getTile(int index) {
		if (index < 0 || index >= tiles.length) throw new IndexOutOfBoundsException("Index tuile invalide : " + index);
		return tiles[index];
	}

	/** Autres méthodes **/
	boolean isValidLayerTileNumber(int tileNumber) { return tileNumber >= EMPTY_TILE && tileNumber <= getTileCount(); }

	@Override
	public String toString() {
		return "Chipset[name=%s, tileSize=%d, tileCountHeight=%d, tileCountWidth=%d]"
				.formatted(name, tileSize, tileCountHeight, tileCountWidth);
	}
}