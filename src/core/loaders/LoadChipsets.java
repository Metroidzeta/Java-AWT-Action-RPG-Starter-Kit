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

import core.Chipset;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Construit et retourne la map complète (nom -> chipset) des chipsets du jeu.
 */
public final class LoadChipsets {

	private LoadChipsets() { throw new AssertionError("La classe LoadChipsets ne doit pas être instanciée."); } // Empêche toute instanciation

	private static final List<Chipset> CHIPSETS_LIST = List.of( // Création des chipsets : new Chipset(fileName, tileSize [par défaut : 16])
		new Chipset("BZ.png",                   16),            // 0
		new Chipset("VillageTangaFinal.png",    16),            // 1
		new Chipset("grey_cas42.png",           16),            // 2
		new Chipset("PalaisRoland2.png",        16),            // 3
		new Chipset("PalaisRolandInt.png",      16),            // 4
		new Chipset("PalaisRolandNouveau.png",  48),            // 5
		new Chipset("MaraisTanga.png",          16),            // 6
		new Chipset("marais2.png",              16),            // 7
		new Chipset("Coacville_exterieur.png",  16),            // 8
		new Chipset("chipset173.png",           16),            // 9
		new Chipset("chipset175.png",           16),            // 10
		new Chipset("HunterArene.png",          16),            // 11
		new Chipset("grass.png",                32),            // 12
		new Chipset("chipset5c.png",            16)             // 13
	);

	/** Getters **/
	public static Map<String, Chipset> get() {
		final Map<String, Chipset> chipsets = new HashMap<>(CHIPSETS_LIST.size());
		CHIPSETS_LIST.forEach(chipset -> chipsets.put(chipset.getName(), chipset));
		return Map.copyOf(chipsets);
	}
}