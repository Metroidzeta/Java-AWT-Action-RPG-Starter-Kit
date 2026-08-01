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

public class Main {

    private Main() { throw new AssertionError("La classe Main ne doit pas être instanciée."); } // Empêche toute instanciation

    public static void main(String[] args) {
        Game game = new Game();
        game.play();
    }
}
