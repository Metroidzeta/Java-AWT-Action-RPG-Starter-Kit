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

import core.resources.Music;
import core.resources.SoundEffect;

import java.util.Map;
import java.util.Objects;

public final class GameContext {

    private final Hero hero;
    private final AudioManager audioManager;
    private final Camera camera = new Camera();
    private final Controls controls;

    private final StringBuilder message = new StringBuilder();
    private final StringBuilder lastMessage = new StringBuilder();
    private int nbEventPass = 0;

    /** Constructeur **/
    public GameContext(Hero hero, Map<String, SoundEffect> soundEffects, Map<String, Music> musics, Controls controls) {
        this.hero = Objects.requireNonNull(hero, "hero null");
        this.audioManager = new AudioManager(soundEffects, musics);
        this.controls = Objects.requireNonNull(controls, "Controls null");
    }

    /** Getters **/
    public Hero getHero()                 { return hero; }
    public AudioManager getAudioManager() { return audioManager; }
    public Camera getCamera()             { return camera; }
    public int getNbEventPass()           { return nbEventPass; }
    public boolean messageIsEmpty()       { return message.isEmpty(); }
    public String getMessageText()        { return message.toString(); }
    public String getLastMessageText()    { return lastMessage.toString(); }

    /** Setters **/
    public void setWritingMode(boolean enabled) {
        hero.setIsWriting(enabled);
        controls.setTextInputEnabled(enabled);
    }

    /** Autres méthodes **/
    public void incrementNbEventPass() { nbEventPass++; }
    public void resetNbEventPass() { nbEventPass = 0; }

    public void resetCamera() {
        camera.setTarget(hero);
        camera.update(); // recalculer l'offset caméra
        camera.sync(); // synchroniser la caméra pour empêcher un glissement
    }

    public void appendMessage(String text) {
        Objects.requireNonNull(text, "Text null");

        final int remaining = Config.MSG_SIZE_MAX - message.length();
        if (remaining <= 0 || text.isEmpty()) return;

        final int length = Math.min(text.length(), remaining);
        message.append(text, 0, length);
    }

    public void clearMessage() {
         message.setLength(0);
    }

    public void saveMessage() {
        lastMessage.setLength(0);
        lastMessage.append(message);
    }

    public void restoreLastMessage() {
        message.setLength(0);
        message.append(lastMessage);
    }

    public void deleteLastCharacterMessage() {
        if (message.isEmpty()) return;

        final int codePoint = message.codePointBefore(message.length());
        final int charCount = Character.charCount(codePoint);

        message.delete(message.length() - charCount, message.length());
    }
}