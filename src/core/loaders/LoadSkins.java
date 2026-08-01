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

import core.Skin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit et retourne la map complète (nom -> skin) des skins du jeu.
 */
public final class LoadSkins {

	private LoadSkins() { throw new AssertionError("La classe LoadSkins ne doit pas être instanciée."); } // Empêche toute instanciation

	private static final List<Skin> SKINS_LIST = List.of( // Création des skins : new Skin(fileName)
		new Skin("Evil.png")                                      // 0
	);

	/** Getters **/
	public static Map<String, Skin> get() {
		final Map<String, Skin> skins = new HashMap<>(SKINS_LIST.size());
		SKINS_LIST.forEach(skin -> skins.put(skin.getName(), skin));
		return Map.copyOf(skins);
	}
}