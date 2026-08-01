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

import static core.Config.CELL_SIZE;

import java.awt.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit et retourne la map complète (nom -> police) des polices du jeu.
 */
public final class LoadFonts {

	private LoadFonts() { throw new AssertionError("La classe LoadFonts ne doit pas être instanciée."); } // Empêche toute instanciation

	/** Structure de données pour une police **/
	private record FontData(String name, String fontName, int style, int size) {}

	private static final List<FontData> FONTS_LIST = List.of( // Création des polices : new FontData(name, fontName, style, size)
		new FontData("FPS", "Courier New", Font.PLAIN, (int)(CELL_SIZE * 0.4)),    // 0 : police des FPS en jeu
		new FontData("Normal", "Arial", Font.PLAIN, (int)(CELL_SIZE * 0.68))       // 1 : police du texte normal
	);

	/** Getters **/
	public static Map<String, Font> get() {
		final Map<String, Font> fonts = new HashMap<>(FONTS_LIST.size());
		FONTS_LIST.forEach(data -> {
			Font font = new Font(data.fontName(), data.style(), data.size());
			fonts.put(data.name(), font);
		});
		return Map.copyOf(fonts);
	}
}