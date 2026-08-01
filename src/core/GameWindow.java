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

import javax.swing.JFrame;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferStrategy;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Fenêtre principale du jeu (contient le canvas de rendu et gère la configuration graphique)
 */
public final class GameWindow extends JFrame {

	@Serial
    private static final long serialVersionUID = 1L;

	private final Canvas canvas;
	private final int windowedWidth;
	private final int windowedHeight;
	private boolean fullscreen;
	private volatile boolean bufferStrategyDirty = true;

	public GameWindow(String title, int width, int height, Controls controls) {
		super(title);
		if (width <= 0 || height <= 0) throw new IllegalArgumentException("Dimensions invalides : " + width + "x" + height);
		windowedWidth = width;
		windowedHeight = height;
		canvas = createCanvas(width, height, Objects.requireNonNull(controls, "Controls null"));
		configureWindow();
	}

	private Canvas createCanvas(int width, int height, Controls controls) {
		Canvas c = new Canvas();
		c.setPreferredSize(new Dimension(width, height));
		c.setBackground(Color.BLACK);
		c.setIgnoreRepaint(true);
		c.addKeyListener(controls);
		return c;
	}

	private void configureWindow() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ferme le programme lorsqu'on clique sur la croix rouge
		setResizable(false); // ne peut pas être agrandi ou réduit
		setIgnoreRepaint(true); // pas de rafraîchissement automatique de la fenêtre

		add(canvas);
		pack(); // ajuste la fenêtre selon la taille du canvas
		setLocationRelativeTo(null); // centre la fenêtre
		setVisible(true); // visible (après le pack()!)

		EventQueue.invokeLater(() -> {
			canvas.createBufferStrategy(3);
			canvas.requestFocusInWindow();
		});
	}

	private static void runOnEDTAndWait(Runnable task) {
		if (EventQueue.isDispatchThread()) {
			task.run();
			return;
		}

		try {
			EventQueue.invokeAndWait(task);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interruption pendant le changement de mode plein écran", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException runtimeException) throw runtimeException;
			if (cause instanceof Error error) throw error;
			throw new IllegalStateException("Erreur pendant le changement de mode plein écran", cause);
		}
	}


	/** Getters **/
	public Canvas getCanvas() { return canvas; }
	public boolean isFullscreen() { return fullscreen; }

	/** Setters **/
	public void setFullscreen(boolean enabled) {
		if (fullscreen == enabled) return;

		runOnEDTAndWait(() -> {
			if (fullscreen == enabled) return;

			BufferStrategy bs = canvas.getBufferStrategy();
			if (bs != null) bs.dispose();

			dispose();

			if (enabled) {
				setUndecorated(true);
				setResizable(false);
				setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
			} else {
				setUndecorated(false);
				setResizable(false);
				canvas.setPreferredSize(new Dimension(windowedWidth, windowedHeight));
				pack();
				setLocationRelativeTo(null);
			}

			fullscreen = enabled;
			markBufferStrategyDirty();

			setVisible(true);
			canvas.requestFocusInWindow();
		});
	}

	/** Autres méthodes **/
	public void markBufferStrategyDirty() {
		bufferStrategyDirty = true;
	}

	public BufferStrategy getOrCreateBufferStrategy() {
		if (!canvas.isDisplayable() || canvas.getWidth() <= 0 || canvas.getHeight() <= 0) return null;

		BufferStrategy bs = canvas.getBufferStrategy();
		if (bufferStrategyDirty || bs == null) {
			if (bs != null) bs.dispose();

			try {
				canvas.createBufferStrategy(3);
				bs = canvas.getBufferStrategy();
				bufferStrategyDirty = false;
				canvas.requestFocusInWindow();
			} catch (IllegalStateException e) {
				bufferStrategyDirty = true;
				return null;
			}
		}

		return bs;
	}
	public void close() { dispose(); }
}