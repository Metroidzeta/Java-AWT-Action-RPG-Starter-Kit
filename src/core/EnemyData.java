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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static core.Config.ENEMIES_DIRECTORY;

public final class EnemyData {

    private static final int ENEMY_DATA_ROWS = 4;
    private static final int ENEMY_DATA_COLS = 9;
    private static final int ENEMY_DATA_TOTAL_REGIONS = (ENEMY_DATA_ROWS * ENEMY_DATA_COLS);

    private final String name;
    private final BufferedImage texture;
    private final Rectangle[] textureRegions;

    private final int hpMax;
    private final int xp;
    private final int goldCoins;

    /** Constructeur **/
    public EnemyData(String fileName, String name, int hpMax, int xp, int goldCoins) {
        Util.requireNonBlank(fileName, "Nom fichier");
        this.name = Util.requireNonBlank(name, "Nom de l'ennemi");

        if (hpMax < 1) throw new IllegalArgumentException("hpMax < 1");
        if (xp < 0) throw new IllegalArgumentException("xp < 0");
        if (goldCoins < 0) throw new IllegalArgumentException("goldCoins < 0");

        this.hpMax = hpMax;
        this.xp = xp;
        this.goldCoins = goldCoins;

        this.texture = Util.loadImage(ENEMIES_DIRECTORY + "/" + fileName); // image source
        this.textureRegions = new Rectangle[ENEMY_DATA_TOTAL_REGIONS];

        for (int i = 0; i < ENEMY_DATA_ROWS; i++) {
            final int y = 15 + i * 32;
            final int rowIndex = i * ENEMY_DATA_COLS;
            for (int j = 0; j < ENEMY_DATA_COLS; j++) {
                textureRegions[rowIndex + j] = new Rectangle(3 + j * 24, y, 18, 18);
            }
        }
    }

    /** Getters **/
    public String getName()                { return name; }
    public BufferedImage getTexture()      { return texture; }
    public Rectangle[] getTextureRegions() { return textureRegions; }
    public int getHpMax()                  { return hpMax; }
    public int getXp()                     { return xp; }
    public int getGoldCoins()              { return goldCoins; }
}