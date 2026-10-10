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

import core.EnemyData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit et retourne la map complète (nom -> enemyData) de la data des ennemis du jeu.
 */
public final class LoadEnemiesData {

	private LoadEnemiesData() { throw new AssertionError("La classe LoadEnemiesData ne doit pas être instanciée."); } // Empêche toute instanciation

	/** Structure de données pour un enemyData **/
	private record EnemyDataInfo(String fileName, String name, int hpMax, int xp, int goldCoins) {}

	private static final List<EnemyDataInfo> ENEMIES_DATA_LIST = List.of( // Création de la data des ennemis : new EnemyDataInfo(fileName, name, hpMax, xp, goldCoins)
		new EnemyDataInfo("blob_bleu.png", "Blob Bleu", 432, 33, 9),  // 0 (Blob bleu)
		new EnemyDataInfo("lapin.png", "Lapin", 609, 163, 14)         // 1 (Lapin)
	);

	/** Getters **/
	public static Map<String, EnemyData> load() {
		final Map<String, EnemyData> enemiesData = new HashMap<>(ENEMIES_DATA_LIST.size());
		ENEMIES_DATA_LIST.forEach(elem -> {
			EnemyData enemyData = new EnemyData(elem.fileName(), elem.name(), elem.hpMax(), elem.xp(), elem.goldCoins());
			enemiesData.put(elem.name(), enemyData);
		});
		return Map.copyOf(enemiesData);
	}
}