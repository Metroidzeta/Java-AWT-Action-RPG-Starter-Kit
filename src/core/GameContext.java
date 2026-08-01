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

import core.resources.SoundEffect;

import java.util.Map;
import java.util.Objects;

public final class GameContext {

    private final Hero hero;
    private final AudioManager audioManager;
    private final Camera camera = new Camera();

    private final StringBuilder message = new StringBuilder();
    private final StringBuilder lastMessage = new StringBuilder();
    private final Object messageLock = new Object();
    private int nbEventPass = 0;

    /** Constructeur **/
    public GameContext(Hero hero, Map<String, SoundEffect> soundEffects) {
        this.hero = Objects.requireNonNull(hero, "hero null");
        this.audioManager = new AudioManager(soundEffects);
    }

    /** Getters **/
    public Hero getHero() { return hero; }
    public AudioManager getAudioManager() { return audioManager; }
    public Camera getCamera() { return camera; }
    public int getNbEventPass() { return nbEventPass; }
    public StringBuilder getMessage() { return message; }
    public StringBuilder getLastMessage() { return lastMessage; }
    public Object getMessageLock() { return messageLock; }

    /** Autres méthodes **/
    public void incrementNbEventPass() { nbEventPass++; }
    public void resetNbEventPass() { nbEventPass = 0; }

    public void resetCamera() {
        camera.setTarget(hero);
        camera.update(); // recalculer l'offset caméra
        camera.sync(); // synchroniser la caméra pour empêcher un glissement
    }

    public void clearMessage() {
        synchronized(messageLock) {
            message.setLength(0);
        }
    }

    public void saveMessage() {
        synchronized(messageLock) {
            lastMessage.setLength(0);
            lastMessage.append(message);
        }
    }

    public void restoreLastMessage() {
        synchronized(messageLock) {
            message.setLength(0);
            message.append(lastMessage);
        }
    }

    public void deleteLastCharacterMessage() {
        synchronized(messageLock) {
            if (!message.isEmpty()) message.deleteCharAt(message.length() - 1);
        }
    }
}