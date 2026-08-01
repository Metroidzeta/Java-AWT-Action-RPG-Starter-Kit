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
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

/**
 * Représente un groupe d'événements organisés en plusieurs pages.
 * 
 * Chaque page contient une liste ordonnée d'events, permettant de structurer
 * des séquences d'actions successives dans le jeu (dialogues, téléportations, etc.).
 */
public final class EventGroup {

	private final List<List<Event>> eventsPages = new ArrayList<>();

	/** Constructeur **/
	public EventGroup() {}

	/** Autres méthodes **/
	public void add(int page, Event ev) {
		Objects.requireNonNull(ev, "Event ajoute null");
		if (page > eventsPages.size()) {
			throw new IllegalArgumentException("Impossible d’ajouter un event sur une page inexistante : seule la page suivante est acceptée pour agrandir la liste");
		}
		if (page == eventsPages.size()) eventsPages.add(new ArrayList<>());
		eventsPages.get(page).add(ev);
	}

	public Event getEventIfExists(int page, int index) {
		if (page < 0 || page >= eventsPages.size()) return null;
		List<Event> events = eventsPages.get(page);
		if (index < 0 || index >= events.size()) return null;
		return events.get(index);
	}

	public boolean isFinished(int page, int index) {
		if (page < 0 || page >= eventsPages.size()) return true;
		return index >= eventsPages.get(page).size();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof EventGroup eg)) return false;
		return Objects.equals(eventsPages, eg.eventsPages);
	}

	@Override
	public int hashCode() { return Objects.hash(eventsPages); }
}