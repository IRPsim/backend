package de.unileipzig.irpsim.core.simulation.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Bildet ein GAMS-Set ab.
 *
 * @author reichelt
 */
public class Set {
	private String name;
	private List<SetElement> elements = new ArrayList<>();

	/**
	 * Leerer Konstruktor.
	 */
	public Set() {
	}

	/**
	 * Konstruktor mit übergebenem Setnamen.
	 *
	 * @param name Name des Sets
	 */
	public Set(final String name) {
		this.name = name;
	}

	public final List<SetElement> getElements() {
		return elements;
	}

	public final void setElements(final List<SetElement> elements) {
		this.elements = elements;
	}

	public final String getName() {
		return name;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	/**
	 * Liefert das SetElement des Sets mit dem mitgegebenen Namen.
	 *
	 * @param name Der Name des zu liefernden SetElements
	 * @return The first SetElement dessen Name mit dem mitgegebenen Namen übereinstimmt gibt es kein solches SetElement
	 *         wird null zurück gegeben
	 */
	public final SetElement getElement(final String name) {
		final Optional<SetElement> first = elements.stream().filter(b -> b.getName().equals(name)).findFirst();
		return first.isPresent() ? first.get() : null;
	}

	/**
	 * Liefert eine Liste der Namen aller SetElemente des Sets.
	 *
	 * @return Liste mit SetElementnamen als List<String>
	 */
	public final List<String> fetchElementNames() {
		return elements.stream().map(b -> b.getName()).collect(Collectors.toList());
	}

	@Override
	public final String toString() {
		final StringBuilder stringer = new StringBuilder(name).append("{");
		elements.forEach(e -> stringer.append(e.getName()).append(", "));
		stringer.delete(stringer.length() - 2, stringer.length() - 1).append("}");
		return stringer.toString();
	}
}