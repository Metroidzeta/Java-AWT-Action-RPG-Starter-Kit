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

import core.Enemy;
import core.EnemyData;
import core.GameMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Construit et retourne la map complète (nom -> enemy) des ennemis du jeu.
 */
public final class LoadEnemies {

	private LoadEnemies() { throw new AssertionError("La classe LoadEnemies ne doit pas être instanciée."); } // Empêche toute instanciation

	/** Structure de données pour un enemy **/
	private record EnemyInfo(String gameMapName, String enemyDataName, int xCell, int yCell) {}

	private static final List<EnemyInfo> ENEMIES_LIST = List.of( // Création des ennemis : new EnemyInfo(gameMapName, enemyDataName, xCell, yCell)
		new EnemyInfo("Sarosa_Foret_Est", "Blob Bleu", 9, 23),
		new EnemyInfo("Sarosa_Foret_Est", "Blob Bleu", 34, 13),
		new EnemyInfo("Sarosa_Foret_Est", "Lapin", 33, 24),
		new EnemyInfo("Arene_Hunter", "Blob Bleu", 9, 9),
		new EnemyInfo("Arene_Hunter", "Lapin", 10, 9),
		new EnemyInfo("Arene_Hunter", "Blob Bleu", 9, 10),
		new EnemyInfo("Arene_Hunter", "Lapin", 10, 10)
	);

	/** Getters **/
	private static void addEnemy(EnemyInfo elem, Map<String, GameMap> gameMaps, Map<String, EnemyData> enemiesData) {
		final GameMap gameMap = gameMaps.get(elem.gameMapName());
		if (gameMap == null) throw new IllegalArgumentException("[ERREUR] Carte introuvable : \"" +  elem.gameMapName() + "\" pour y insérer un ennemi");
		final EnemyData enemyData = enemiesData.get(elem.enemyDataName());
		Enemy enemy = new Enemy(enemyData, elem.xCell(), elem.yCell());
		gameMap.addEnemy(enemy);
	}

	public static void load(Map<String, GameMap> gameMaps, Map<String, EnemyData> enemiesData) {
		Objects.requireNonNull(gameMaps, "Map: GameMaps null");
		Objects.requireNonNull(enemiesData, "Map: enemiesData null");
		ENEMIES_LIST.forEach(elem -> addEnemy(elem, gameMaps, enemiesData));
	}
}