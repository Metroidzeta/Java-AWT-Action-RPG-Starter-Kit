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
package core.loaders;

import core.Util;
import static core.Config.UI_ASSETS_DIRECTORY;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit et retourne la map complète (nom -> affichage) des images d’affichage du jeu.
 */
public final class LoadUiAssets {

	private LoadUiAssets() { throw new AssertionError("La classe LoadUiAssets ne doit pas être instanciée."); } // Empêche toute instanciation
	
	/** Structure de données pour un affichage **/
	private record UiAssetData(String name, String fileName) {}

	private static final List<UiAssetData> UI_ASSETS_LIST = List.of( // Création des affichages : new UiAssetData(name, fileName)
		new UiAssetData("Fioles", "fioles.png"),                      // 0 : icônes de fioles
		new UiAssetData("BarreXp", "xp.png")                          // 1 : barre d’expérience
	);

	/** Getters **/
	public static Map<String, BufferedImage> get() {
		final Map<String, BufferedImage> uiAssets = new HashMap<>(UI_ASSETS_LIST.size());
		UI_ASSETS_LIST.forEach(data -> {
			BufferedImage image = Util.loadImage(UI_ASSETS_DIRECTORY + "/" + data.fileName());
			uiAssets.put(data.name(), image);
		});
		return Map.copyOf(uiAssets);
	}
}