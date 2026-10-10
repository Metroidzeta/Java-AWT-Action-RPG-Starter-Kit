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

import core.events.Event;
import core.events.Event_MSG;
import core.events.Event_TP;
import core.renderer.Renderer;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.awt.Color;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static core.Controls.GameKey.*;
import static core.Config.DEBUG_MODE;
import static core.Config.WINDOW_TITLE;
import static core.Config.WINDOW_WIDTH;
import static core.Config.WINDOW_HEIGHT;

public final class GameLoop {

    private static final long ATTACK_NANOS_COOLDOWN = 666_666_666L; // environ 667 ms (2/3 seconde)
    private static final long ATTACK_ACTIVE_NANOS = 50_000_000L; // 50 ms
    private static final int FLASK_ANIMATION_FRAME_COUNT = 3;
    private static final int MAX_UPDATES_PER_FRAME = 5;

    private final GameContext gc;
    private final GameWindow gameWindow;
    private final Renderer renderer;
    private final Controls controls;

    private boolean isRunning;
    private long frames = 0L;
    private double fpsResult = 0;
    private long attackStartedAt = 0L;
    private long damageStartTimeNanos = 0L;
    private boolean wallsVisible, menuVisible;
    private int flaskFrame = 0, messageCooldown = 0;
    private boolean showDamage;
    private boolean lastHitWasCritical;
    private boolean refreshNextFrame;
    private EventGroup currentEventGroup = null;

    private final Hero hero;
    private final Camera camera;
    private final ArrayList<Rectangle> hitEnemyHitboxes = new ArrayList<>();

    private volatile boolean stopRequested;

    public GameLoop(GameContext gc, GameWindow gameWindow, Renderer renderer, Controls controls) {
        this.gc = gc;
        this.gameWindow = gameWindow;
        this.renderer = renderer;
        this.controls = controls;
        this.hero = gc.getHero();
        this.camera = gc.getCamera();
    }

    public void requestStop() { stopRequested = true; }

    private void printFPS_Window() {
        String title = String.format("%s | FPS : %.2f", WINDOW_TITLE, fpsResult);
        EventQueue.invokeLater(() -> gameWindow.setTitle(title));
    }

    private void renderFrame(Graphics g) {
        final int canvasWidth = gameWindow.getCanvas().getWidth();
        final int canvasHeight = gameWindow.getCanvas().getHeight();

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            final double scale = Math.min(canvasWidth / (double) WINDOW_WIDTH, canvasHeight / (double) WINDOW_HEIGHT);

            final int renderWidth = (int) Math.round(WINDOW_WIDTH * scale);
            final int renderHeight = (int) Math.round(WINDOW_HEIGHT * scale);
            final int offsetX = (canvasWidth - renderWidth) / 2;
            final int offsetY = (canvasHeight - renderHeight) / 2;

            g2.translate(offsetX, offsetY);
            g2.scale(scale, scale);
            g2.clipRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            render(g2);
        } finally {
            g2.dispose();
        }
    }

    private void renderImmediately() {
        gc.resetCamera();
        BufferStrategy bs = gameWindow.getOrCreateBufferStrategy();
        if (bs == null) return;

        camera.interpolate(1.0); // Interpolation complète
        do {
            do {
                Graphics g = bs.getDrawGraphics();
                try {
                    renderFrame(g); // afficher directement la nouvelle carte
                } finally {
                    g.dispose();
                }
            } while (bs.contentsRestored());

            bs.show();
            Toolkit.getDefaultToolkit().sync();

        } while (bs.contentsLost());

        refreshNextFrame = true;
    }

    private void executeEvent(Event ev) {
        hero.setIsBlocked(true);
        hero.setIsInEvent(true);
        ev.execute(gc);
        boolean isEvent_MSG = false;
        if (ev instanceof Event_MSG) { gc.clearMessage(); isEvent_MSG = true; }
        if (ev instanceof Event_TP) {
            showDamage = false;
            System.out.println("Teleportation de " + hero.getName() + " vers " + hero.getCurrentGameMap().getName() + hero.getPositionCellsString());
            renderImmediately();
        }

        if (!isEvent_MSG) {
            gc.incrementNbEventPass();
            hero.setIsInEvent(false);
        }
    }

    private void updateCooldowns(long nowNanos) {
        final long elapsed = nowNanos >= attackStartedAt ? nowNanos - attackStartedAt : 0L;
        if (hero.swordAttack() && elapsed >= ATTACK_ACTIVE_NANOS) hero.setSwordAttack(false);
        if (!hero.canSwordAttack() && elapsed >= ATTACK_NANOS_COOLDOWN) hero.setCanSwordAttack(true);
    }

    private boolean updateHeroMovement() {
        // --- Déplacements avec flèches ---
        final boolean arrowsPressed = controls.isHeld(UP) || controls.isHeld(DOWN) || controls.isHeld(LEFT) || controls.isHeld(RIGHT);
        boolean moving = false;
        if (arrowsPressed && !hero.isBlocked()) {
            if (controls.isHeld(UP)    && !controls.isHeld(DOWN))  moving = hero.move(Direction.UP);
            if (controls.isHeld(DOWN)  && !controls.isHeld(UP))    moving = hero.move(Direction.DOWN)  || moving; // Ou moving car les autres directions peuvent fausser le résultat
            if (controls.isHeld(LEFT)  && !controls.isHeld(RIGHT)) moving = hero.move(Direction.LEFT)  || moving;
            if (controls.isHeld(RIGHT) && !controls.isHeld(LEFT))  moving = hero.move(Direction.RIGHT) || moving;
        }
        if (!moving) {
            hero.setMoveFrame(7);
        }
        return moving;
    }

    private static boolean randomChance(float probability) {
        if (probability <= 0.0f) return false;
        if (probability >= 1.0f) return true;

        final double randomValue = ThreadLocalRandom.current().nextDouble();
        if (DEBUG_MODE) System.out.printf("randomValue = %f < probability = %f%n", randomValue, probability);

        return randomValue < probability;
    }

    private void updateUPS(long now) {
        controls.beginTick();
        final String typedText = controls.getTypedText();
        if (hero.isWriting() && !typedText.isEmpty()) gc.appendMessage(typedText);

        final boolean moving = updateHeroMovement();
        camera.update();
        if (moving && !hero.isBlocked() && currentEventGroup == null) {
            currentEventGroup = hero.getCurrentGameMap().findCollidingEvents(hero.getHitbox());
        }
        if (currentEventGroup != null && !hero.isInEvent()) {
            Event ev = currentEventGroup.getEventIfExists(0, gc.getNbEventPass());
            if (ev != null) executeEvent(ev);
        }

        if (!hero.isWriting()) {
            if (controls.isPressed(A)) {
                wallsVisible = !wallsVisible;
                if (DEBUG_MODE) System.out.println((wallsVisible ? "Activation" : "Désactivation") + " de l'affichage des murs!");
            }

            if (controls.isPressed(B)) {
                gameWindow.setFullscreen(!gameWindow.isFullscreen());
            }

            if (controls.isPressed(Q)) {
                isRunning = false;
                return;
            }

            if (!hero.isBlocked()) {
                if (controls.isHeld(S) && hero.canSwordAttack()) {
                    gc.getAudioManager().playSound("Blow1.wav");
                    hero.updateSwordHitBox();
                    Rectangle swordHitbox = hero.getSwordHitbox();
                    boolean first = true;
                    boolean criticalHit = false;
                    for (Enemy enemy : hero.getCurrentGameMap().getEnemies()) {
                        final Rectangle enemyHitBox = enemy.getHitbox();
                        if (swordHitbox.intersects(enemyHitBox)) {
                            if (first) {
                                criticalHit = randomChance(hero.getCritChanceRatio());
                                hitEnemyHitboxes.clear();
                                first = false;
                            }
                            if (criticalHit) {
                                if (DEBUG_MODE) System.out.println("Coup critique! sur l'ennemi");
                                gc.getAudioManager().playSound("Damage3.ogg");
                                lastHitWasCritical = true;
                            } else {
                                if (DEBUG_MODE) System.out.println("Coup normal sur l'ennemi");
                                gc.getAudioManager().playSound("Kill1.wav");
                                lastHitWasCritical = false;
                            }
                            hitEnemyHitboxes.add(enemyHitBox);
                            showDamage = true;
                            damageStartTimeNanos = now;
                        }
                    }
                    hero.setSwordAttack(true);
                    hero.setCanSwordAttack(false);
                    attackStartedAt = now;
                }
            }
        }

        if (controls.isPressed(BACK_SPACE)) {
            if (hero.isWriting()) gc.deleteLastCharacterMessage();
        }

        if (controls.isPressed(ENTER)) {
            if (!hero.isBlocked()) {
                boolean isWriting = hero.isWriting();
                gc.setWritingMode(!isWriting);
                if (isWriting && !gc.messageIsEmpty()) {
                    messageCooldown = 0;
                    hero.setHasOverheadMessage(true);
                    System.out.println(hero.getName() + " : " + gc.getMessageText());
                    //ajouterMessageHistorique();
                    gc.saveMessage();
                    gc.clearMessage();
                }
            }
        }

        if (controls.isPressed(SPACE)) {
            if (hero.isInEvent() && currentEventGroup != null && currentEventGroup.getEventIfExists(0, gc.getNbEventPass()) instanceof Event_MSG) {
                gc.incrementNbEventPass();
                hero.setIsInEvent(false);
            }
        }

        if (controls.isPressed(ESCAPE)) {
            if (currentEventGroup == null) {
                //afficherRecap = 0;
                menuVisible = !menuVisible;
                if (menuVisible) {
                    gc.setWritingMode(false);
                    gc.clearMessage();
                    hero.setMoveFrame(7);
                }
                hero.setIsBlocked(!(hero.isBlocked()));
            }
        }

        if (!hero.isBlocked()) {
            //if (controls.isPressed(F1)) {}
            if (controls.isPressed(F3)) {
                gc.setWritingMode(true);
                gc.clearMessage();
                gc.restoreLastMessage();
            }
        }

        if (controls.isPressed(F5)) {
            renderer.getContext().nextBoxColor();
        }

        if (currentEventGroup != null && currentEventGroup.isFinished(0, gc.getNbEventPass())) {
            gc.resetNbEventPass();
            currentEventGroup = null;
            hero.setIsBlocked(false);
        }
    }

    private void updateFPS() {
        BufferStrategy bs = gameWindow.getOrCreateBufferStrategy();
        if (bs == null) return;

        do {
            do {
                Graphics g = bs.getDrawGraphics();
                try {
                    renderFrame(g); // dessiner nouvelle frame
                } finally {
                    g.dispose(); // vider les ressources
                }
            } while (bs.contentsRestored());

            bs.show(); // afficher le rendu de la frame
            Toolkit.getDefaultToolkit().sync();
        } while (bs.contentsLost());

        frames++;
    }

    private void render(Graphics g) {
        renderer.drawBlackBackground(g);
        renderer.updateMapDisplayBounds();
        renderer.drawLayer(g, 0); // couche 0 chipset
        renderer.drawLayer(g, 1); // couche 1 chipset
        if (wallsVisible) renderer.drawWalls(g);
        renderer.drawHero(g);
        renderer.drawEnemies(g);
        renderer.drawLayer(g, 2); // couche 2 chipset

        if (showDamage) {
            final long elapsedNanos = System.nanoTime() - damageStartTimeNanos;
            final double t = Math.clamp(elapsedNanos / 1_000_000_000.0, 0.0, 1.0);

            if (t >= 1.0) {
                showDamage = false;
            } else {
                renderer.drawDamage(g, lastHitWasCritical ? 64 : 32, lastHitWasCritical ? Color.RED : Color.WHITE, t, hitEnemyHitboxes);
            }
        }

        if (hero.swordAttack()) {
            renderer.drawHeroSwordHitbox(g);
        }

        renderer.drawHpFlask(g, flaskFrame);
        renderer.drawMpFlask(g, flaskFrame);
        renderer.drawXpBar(g);

        if (hero.isWriting()) renderer.drawTextInput(g, gc.getMessageText());
        if (hero.hasOverheadMessage()) renderer.drawHeroOverheadMessage(g, gc.getLastMessageText());
        if (hero.isInEvent() && currentEventGroup != null) {
            final Event ev = currentEventGroup.getEventIfExists(0, gc.getNbEventPass());
            if (ev instanceof Event_MSG ev_msg) renderer.drawEventMessage(g, ev_msg.getMessage());
        }

        if (menuVisible) {
            renderer.drawNavigationMenu(g); // affiche menu de navigation
            renderer.drawStatsMenu(g); // affiche sous-menu: statistiques
        }
        renderer.drawAlignment(g);
        renderer.drawFPS(g);
        if (refreshNextFrame) {
            renderer.drawComputePalette(g);
            refreshNextFrame = false;
        }
    }

    /** Boucles de jeu **/
    public void start() {
        // --- Constantes temporelles ---
        final long NANOS_PER_RENDER     = 1_000_000_000L / Config.FPS;
        final long NANOS_PER_TICK       = 1_000_000_000L / Config.UPS;
        final long NANOS_588MS          = 588_000_000L;    // 588 ms
        final long NANOS_1SEC           = 1_000_000_000L;  // 1 seconde
        final long NANOS_1MIN           = 60_000_000_000L; // 1 minute
        final long MAX_FRAME_SKIP_NANOS = 5_000_000_000L;  // 5 secondes
        final int NO_DELAYS_PER_YIELD = 16;

        // --- Variables de synchronisation ---
        long nowNanos   = System.nanoTime();
        long nextRender = nowNanos;
        long nextTick   = nowNanos;

        // --- Timers secondaires ---
        long lastFlaskTime = nowNanos, lastMinute = nowNanos, lastSecond = nowNanos;
        long overSleepNanos = 0L;
        int noDelays = 0;

        // --- Initialisation ---
        long lastFrameCount = 0L;
        gc.getAudioManager().playMusic(hero.getCurrentGameMap().getMusic());
        isRunning = true;

        // --- Boucle principale ---
        while (isRunning && !stopRequested) {
            nowNanos = System.nanoTime();

            // --- Détection d'une longue pause (veille, freeze, etc.) ---
            if (nowNanos - nextTick > MAX_FRAME_SKIP_NANOS) {
                nextTick = nowNanos + NANOS_PER_TICK;
                nextRender = nowNanos + NANOS_PER_RENDER;

                lastFlaskTime = nowNanos;
                lastMinute = nowNanos;
                lastSecond = nowNanos;
                lastFrameCount = frames;

                overSleepNanos = 0L;
                noDelays = 0;
            }

            // --- Logique globale ---
            updateCooldowns(nowNanos);

            // --- Animation fioles ---
            if (nowNanos - lastFlaskTime >= NANOS_588MS) {
                flaskFrame = (flaskFrame + 1) % FLASK_ANIMATION_FRAME_COUNT;
                lastFlaskTime += NANOS_588MS;
            }

            // --- Chaque seconde ---
            if (nowNanos - lastSecond >= NANOS_1SEC) {
                if (hero.hasOverheadMessage() && ++messageCooldown == 6) { // Si il y a déjà un message sur la tête du héros et que ça se termine
                    messageCooldown = 0;
                    hero.setHasOverheadMessage(false);
                }

                final long elapsedFrames = frames - lastFrameCount;
                final double elapsedSec = (nowNanos - lastSecond) / 1e9;
                fpsResult = elapsedFrames / elapsedSec;
                renderer.setFPSText(fpsResult);
                printFPS_Window();
                if (DEBUG_MODE) System.out.println("FPS = " + fpsResult);
                lastFrameCount = frames;
                lastSecond += NANOS_1SEC;
            }

            // --- Chaque minute ---
            if (nowNanos - lastMinute >= NANOS_1MIN) {
                hero.addAlignment(1);
                renderer.setAlignmentText(hero.getAlignment());
                lastMinute += NANOS_1MIN;
            }

            // --- Updates logiques (UPS) ---
            int updates = 0;
            while (nowNanos >= nextTick && updates < MAX_UPDATES_PER_FRAME) {
                updateUPS(nowNanos);
                if (!isRunning || stopRequested) break;
                nextTick += NANOS_PER_TICK;
                updates++;
                nowNanos = System.nanoTime(); // anti dérive
            }
            if (updates == MAX_UPDATES_PER_FRAME && nowNanos >= nextTick) {
                nextTick = nowNanos + NANOS_PER_TICK;
            }

            if (!isRunning || stopRequested) break; // quitte la boucle principale avant le rendu

            // --- Rendu graphique (FPS) ---
            if (nowNanos >= nextRender) {
                // calcul du facteur d'interpolation entre deux updates logiques
                double interpolation = (double)(nowNanos - (nextTick - NANOS_PER_TICK)) / NANOS_PER_TICK;
                interpolation = Math.clamp(interpolation, 0.0, 1.0); // intervalle [0..1]

                camera.interpolate(interpolation);
                updateFPS();
                nextRender += NANOS_PER_RENDER;

                nowNanos = System.nanoTime(); // important avant le calcul du sleep

                if (nowNanos >= nextRender) {
                    final long missedFrames = (nowNanos - nextRender) / NANOS_PER_RENDER + 1L;
                    nextRender += missedFrames * NANOS_PER_RENDER;
                }
            }

            // --- Gestion du CPU ---
            final long nextAction = Math.min(nextTick, nextRender);
            long sleepNanos = Math.max(0L, nextAction - System.nanoTime() - overSleepNanos);

            if (sleepNanos > 0L) {
                long before = System.nanoTime();
                try { Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); isRunning = false; break; }
                long after = System.nanoTime();
                overSleepNanos = Math.max(0L, (after - before) - sleepNanos);
                noDelays = 0;
            } else {
                overSleepNanos = 0L;
                if (++noDelays >= NO_DELAYS_PER_YIELD) {
                    Thread.yield();
                    noDelays = 0;
                }
            }
        }
    }
}