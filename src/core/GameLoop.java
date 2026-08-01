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

import java.awt.Toolkit;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.awt.Color;

import static core.Config.DEBUG_MODE;
import static core.Config.WINDOW_TITLE;
import static core.Config.WINDOW_WIDTH;
import static core.Config.WINDOW_HEIGHT;

public final class GameLoop {

    private static final long ATTACK_NANOS_COOLDOWN = 666_666_666L; // environ 667 ms (2/3 seconde)
    private static final long ATTACK_ACTIVE_NANOS = 50_000_000L; // 50 ms
    private static final int FLASK_ANIMATION_FRAME_COUNT = 3;

    private final GameContext gc;
    private final GameWindow gameWindow;
    private final Renderer renderer;
    private final Controls controls;

    private boolean isRunning;
    private long frames = 0L;
    private double fpsResult = 0;
    private long attackStartedAt = 0L;
    private boolean wallsVisible, menuVisible;
    private int flaskFrame = 0, messageCooldown = 0;
    //private int damageVisible = 0;
    private boolean refreshNextFrame;
    private EventGroup currentsEvents = null;

    private final Hero hero;
    private final Camera camera;

    public GameLoop(GameContext gc, GameWindow gameWindow, Renderer renderer, Controls controls) {
        this.gc = gc;
        this.gameWindow = gameWindow;
        this.renderer = renderer;
        this.controls = controls;
        this.hero = gc.getHero();
        this.camera = gc.getCamera();
    }

    private void printFPS_Window() {
        gameWindow.setTitle(String.format("%s | FPS : %.2f", WINDOW_TITLE, fpsResult));
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
        BufferStrategy bs = gameWindow.getCanvas().getBufferStrategy();
        if (bs != null) {
            Graphics g = bs.getDrawGraphics();
            try {
                renderer.drawBlackBackground(g);
                camera.interpolate(1.0); // interpolation complète
                renderFrame(g); // afficher directement la nouvelle carte
                refreshNextFrame = true;
            } finally {
                g.dispose();
                bs.show();
                Toolkit.getDefaultToolkit().sync();
            }
        }
    }

    private void executeEvent(Event ev) {
        hero.setIsBlocked(true);
        hero.setIsInEvent(true);
        ev.execute(gc);
        boolean isEvent_MSG = false;
        if (ev instanceof Event_MSG) { gc.clearMessage(); isEvent_MSG = true; }
        if (ev instanceof Event_TP) {
            //damageVisible = 0;
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
        final boolean arrowsPressed = controls.UP() || controls.DOWN() || controls.LEFT() || controls.RIGHT();
        boolean moving = false;
        if (arrowsPressed && !hero.isBlocked()) {
            if (controls.UP()    && !controls.DOWN())  moving = hero.move(Directions.UP);
            if (controls.DOWN()  && !controls.UP())    moving = hero.move(Directions.DOWN)  || moving; // Ou moving car les autres directions peuvent fausser le résultat
            if (controls.LEFT()  && !controls.RIGHT()) moving = hero.move(Directions.LEFT)  || moving;
            if (controls.RIGHT() && !controls.LEFT())  moving = hero.move(Directions.RIGHT) || moving;
        }
        if (!moving) {
            hero.setMoveFrame(7);
        }
        return moving;
    }

    private void updateUPS(long frameStartTime) {
        final boolean moving = updateHeroMovement();
        camera.update();
        if (moving && !hero.isBlocked() && currentsEvents == null) {
            currentsEvents = hero.getCurrentGameMap().findCollidingEvents(hero.getHitbox());
        }
        if (currentsEvents != null && !hero.isInEvent()) {
            Event ev = currentsEvents.getEventIfExists(0, gc.getNbEventPass());
            if (ev != null) executeEvent(ev);
        }

        if (!hero.isWriting()) {
            if (controls.A()) {
                wallsVisible = !wallsVisible;
                if (DEBUG_MODE) System.out.println((wallsVisible ? "Activation" : "Désactivation") + " de l'affichage des murs!");
                controls.consume(Controls.GameKey.A);
            }

            if (controls.B()) {
                gameWindow.setFullscreen(!gameWindow.isFullscreen());
                controls.consume(Controls.GameKey.B);
            }

            if (controls.Q()) {
                isRunning = false;
                controls.consume(Controls.GameKey.Q);
                return;
            }

            if (!hero.isBlocked()) {
                if (controls.S()) {
                    if (hero.canSwordAttack()) {
                        gc.getAudioManager().playSound("Blow1.wav");
                        hero.updateSwordHitBox();
						/*if (hero.getSwordHitbox().intersects(blob_hitbox)) {
							resultatAleatoire = (double) rand() / RAND_MAX;
							if (Config.DEBUG_MODE) System.out.println("resultatAleatoire = " + resultatAleatoire + " > tauxCrit = " + hero.getCritChanceRatio());
							if (resultatAleatoire > hero.getCritChanceRatio()) {
								System.out.println("Coup normal sur le monstre");
								soundEffects.get(1).play();
							} else {
								System.out.println("Coup critique! sur le monstre");
								soundEffects.get(2).play();
							}
							damageVisible = 1;
						} */
                        hero.setSwordAttack(true);
                        hero.setCanSwordAttack(false);
                        attackStartedAt = frameStartTime;
                    }
                    controls.consume(Controls.GameKey.S);
                }
            }
        }

        if (controls.BACK_SPACE()) {
            if (hero.isWriting()) gc.deleteLastCharacterMessage();
            controls.consume(Controls.GameKey.BACK_SPACE);
        }

        if (controls.ENTER()) {
            if (!hero.isBlocked()) {
                boolean isWriting = hero.isWriting();
                hero.setIsWriting(!isWriting);
                if (isWriting && !gc.getMessage().isEmpty()) {
                    messageCooldown = 0;
                    hero.setHasOverheadMessage(true);
                    System.out.println(hero.getName() + " : " + gc.getMessage());
                    //ajouterMessageHistorique();
                    gc.saveMessage();
                    gc.clearMessage();
                }
            }
            controls.consume(Controls.GameKey.ENTER);
        }

        if (controls.SPACE()) {
            if (hero.isInEvent() && currentsEvents != null && currentsEvents.getEventIfExists(0, gc.getNbEventPass()) instanceof Event_MSG) {
                gc.incrementNbEventPass();
                hero.setIsInEvent(false);
            }
            controls.consume(Controls.GameKey.SPACE);
        }

        if (controls.ESCAPE()) {
            if (currentsEvents == null) {
                //afficherRecap = 0;
                menuVisible = !menuVisible;
                if (menuVisible) {
                    hero.setIsWriting(false);
                    gc.clearMessage();
                    hero.setMoveFrame(7);
                }
                hero.setIsBlocked(!(hero.isBlocked()));
                controls.consume(Controls.GameKey.ESCAPE);
            }
        }

        if (!hero.isBlocked()) {
            if (controls.F1()) {
                controls.consume(Controls.GameKey.F1);
            }
            if (controls.F3()) {
                hero.setIsWriting(true);
                gc.clearMessage();
                gc.restoreLastMessage();
                controls.consume(Controls.GameKey.F3);
            }
        }

        if (controls.F5()) {
            renderer.getContext().nextBoxColor();
            controls.consume(Controls.GameKey.F5);
        }

        if (currentsEvents != null && currentsEvents.isFinished(0, gc.getNbEventPass())) {
            gc.resetNbEventPass();
            currentsEvents = null;
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
        renderer.drawLayer(g, 2); // couche 2 chipset
        if (hero.swordAttack()) {
            renderer.drawHeroSwordHitbox(g);
        }

        renderer.drawHpFlask(g, flaskFrame);
        renderer.drawMpFlask(g, flaskFrame);
        renderer.drawXpBar(g);

        if (hero.isWriting()) renderer.drawTextInputBox(g, gc.getMessage().toString());
        if (hero.hasOverheadMessage()) renderer.drawHeroOverheadMessage(g, gc.getLastMessage().toString());
        if (hero.isInEvent() && currentsEvents != null) {
            final Event ev = currentsEvents.getEventIfExists(0, gc.getNbEventPass());
            if (ev instanceof Event_MSG ev_msg) renderer.drawEventMessage(g, ev_msg.getMessage());
        }

        if (menuVisible) {
            renderer.drawNavigationMenu(g); // affiche menu de navigation
            renderer.drawStatsMenu(g); // affiche sous-menu: statistiques
        }
        renderer.drawAlignment(g);
        renderer.drawFPS(g, fpsResult);
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
        while (isRunning) {
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
                printFPS_Window();
                if (DEBUG_MODE) System.out.println("FPS = " + fpsResult);
                lastFrameCount = frames;
                lastSecond += NANOS_1SEC;
            }

            // --- Chaque minute ---
            if (nowNanos - lastMinute >= NANOS_1MIN) {
                hero.addAlignment(1);
                lastMinute += NANOS_1MIN;
            }

            // --- Updates logiques (UPS) ---
            while (nowNanos >= nextTick) {
                updateUPS(nowNanos);
                if (!isRunning) break;
                nextTick += NANOS_PER_TICK;
                nowNanos = System.nanoTime(); // anti dérive
            }

            if (!isRunning) break; // quitte la boucle principale avant le rendu

            // --- Rendu graphique (FPS) ---
            if (nowNanos >= nextRender) {
                // calcul du facteur d'interpolation entre deux updates logiques
                double interpolation = (double)(nowNanos - (nextTick - NANOS_PER_TICK)) / NANOS_PER_TICK;
                interpolation = Math.clamp(interpolation, 0.0, 1.0); // intervalle [0..1]

                camera.interpolate(interpolation);
                updateFPS();
                nextRender += NANOS_PER_RENDER;

                nowNanos = System.nanoTime(); // important avant le calcul du sleep
            }

            // --- Gestion du CPU ---
            final long nextAction = Math.min(nextTick, nextRender);
            long sleepNanos = Math.max(0L, nextAction - System.nanoTime() - overSleepNanos);

            if (sleepNanos > 0L) {
                long before = System.nanoTime();
                try { Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); isRunning = false; break; }
                long after = System.nanoTime();
                overSleepNanos = (after - before) - sleepNanos;
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

    public void start2() {
        // --- Constantes temporelles ---
        final double MILLIS_PER_RENDER   = 1e3 / Config.FPS;
        final double MILLIS_PER_TICK     = 1e3 / Config.UPS;
        final long MS_588MS              = 588L; // 588 ms
        final long MS_1SEC               = 1_000L; // 1 seconde
        final long MS_1MIN               = 60_000L; // 1 minute
        final long MAX_FRAME_SKIP_MILLIS = 5_000L; // 5 secondes
        final int NO_DELAYS_PER_YIELD = 16;

        // --- Variables de synchronisation ---
        long now = System.currentTimeMillis();
        double nextRender = (double) now;
        double nextTick   = (double) now;

        // --- Timers secondaires ---
        long lastFlaskTime = now, lastMinute = now, lastSecond = now;
        long overSleepMillis = 0L;
        int noDelays = 0;

        // --- Initialisation ---
        long lastFrameCount = 0L;
        gc.getAudioManager().playMusic(hero.getCurrentGameMap().getMusic());
        isRunning = true;

        // --- Boucle principale ---
        while (isRunning) {
            now = System.currentTimeMillis();

            // --- Détection d'une longue pause (veille, freeze, etc.) ---
            if (now - nextTick > MAX_FRAME_SKIP_MILLIS) {
                nextTick = (double) now + MILLIS_PER_TICK;
                nextRender = (double) now + MILLIS_PER_RENDER;

                lastFlaskTime = now;
                lastMinute = now;
                lastSecond = now;
                lastFrameCount = frames;

                overSleepMillis = 0L;
                noDelays = 0;
            }

            // --- Logique globale ---
            updateCooldowns(now * 1_000_000L);

            // --- Animations fioles ---
            if (now - lastFlaskTime >= MS_588MS) {
                flaskFrame = (flaskFrame + 1) % FLASK_ANIMATION_FRAME_COUNT;
                lastFlaskTime += MS_588MS;
            }

            // --- Chaque Seconde ---
            if (now - lastSecond >= MS_1SEC) {
                if (hero.hasOverheadMessage() && ++messageCooldown == 6) { // Si il y a déjà un message sur la tête du héros et que ça se termine
                    messageCooldown = 0;
                    hero.setHasOverheadMessage(false);
                }
                final long elapsedFrames = frames - lastFrameCount;
                final double elapsedSec = (now - lastSecond) / 1e3;
                fpsResult = elapsedFrames / elapsedSec;
                if (DEBUG_MODE) System.out.println("FPS = " + fpsResult);
                printFPS_Window();
                lastFrameCount = frames;
                lastSecond += MS_1SEC;
            }

            // --- Chaque Minute ---
            if (now - lastMinute >= MS_1MIN) {
                hero.addAlignment(1);
                lastMinute += MS_1MIN;
            }

            // --- Updates logiques (UPS) ---
            while (now >= nextTick) {
                updateUPS(now * 1_000_000L);
                if (!isRunning) break;
                nextTick += MILLIS_PER_TICK;
                now = System.currentTimeMillis(); // anti dérive
            }

            if (!isRunning) break; // quitte la boucle principale avant le rendu

            // ---- Rendu Graphique (FPS) ----
            while (now >= nextRender) {
                // calcul du facteur d'interpolation entre deux updates logiques
                double interpolation = (now - (nextTick - MILLIS_PER_TICK)) / MILLIS_PER_TICK;
                interpolation = Math.clamp(interpolation, 0.0, 1.0); // intervalle [0..1]

                camera.interpolate(interpolation);
                updateFPS();
                nextRender += MILLIS_PER_RENDER;

                now = System.currentTimeMillis(); // important avant le calcul du sleep
            }

            // --- Gestion du CPU ---
            final long nextAction = (long) Math.min(nextTick, nextRender);
            long sleepTime = Math.max(0L, nextAction - System.currentTimeMillis() - overSleepMillis);

            if (sleepTime > 0L) {
                long before = System.currentTimeMillis();
                try { Thread.sleep(sleepTime); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); isRunning = false; break; }
                long after = System.currentTimeMillis();
                overSleepMillis = (after - before) - sleepTime;
                noDelays = 0;
            } else {
                overSleepMillis = 0L;
                if (++noDelays >= NO_DELAYS_PER_YIELD) {
                    Thread.yield();
                    noDelays = 0;
                }
            }
        }
    }
}