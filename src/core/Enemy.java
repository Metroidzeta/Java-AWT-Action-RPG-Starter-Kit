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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static core.Config.CELL_SIZE;

public final class Enemy {

    private final EnemyData data;
    private final Position position;
    private final Rectangle hitbox;
    private final Gauge hpGauge;

    /** Constructeur **/
    public Enemy(EnemyData data, int xCell, int yCell) {
        if (xCell < 0) throw new IllegalArgumentException("xCell < 0");
        if (yCell < 0) throw new IllegalArgumentException("yCell < 0");
        if (xCell > Integer.MAX_VALUE / CELL_SIZE || yCell > Integer.MAX_VALUE / CELL_SIZE) {
            throw new IllegalArgumentException("Overflow position xCell ou yCell");
        }

        this.data = Objects.requireNonNull(data, "EnemyData null");

        final int x = xCell * CELL_SIZE;
        final int y = yCell * CELL_SIZE;

        this.position = new Position(x, y);
        this.hitbox = new Rectangle(x, y, CELL_SIZE, CELL_SIZE);
        this.hpGauge = new Gauge(data.getHpMax(), data.getHpMax());
    }

    /** Getters **/
    public int getX()            { return position.getX(); }
    public int getY()            { return position.getY(); }
    public int getXCell()        { return position.getXCell(); }
    public int getYCell()        { return position.getYCell(); }
    public Rectangle getHitbox() { return hitbox; }
    public Gauge getHpGauge()    { return hpGauge; }
    public EnemyData getData()   { return data; }

    /** Affichage de l'ennemi **/
    public void draw(Graphics g, int numRegion, Rectangle dstRect) {
        if (g == null || dstRect == null) return;

        Rectangle[] regions = data.getTextureRegions();
        if (numRegion < 0 || numRegion >= regions.length) return;

        BufferedImage texture = data.getTexture();
        Rectangle srcRect = regions[numRegion];

        g.drawImage(texture, dstRect.x, dstRect.y,
                dstRect.x + dstRect.width,
                dstRect.y + dstRect.height,
                srcRect.x,
                srcRect.y,
                srcRect.x + srcRect.width,
                srcRect.y + srcRect.height,
                null
        );
    }
}
