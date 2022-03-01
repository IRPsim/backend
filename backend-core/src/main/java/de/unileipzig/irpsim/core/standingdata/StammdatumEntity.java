package de.unileipzig.irpsim.core.standingdata;


import de.unileipzig.irpsim.core.simulation.data.TimeInterval;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;

interface StammdatumValueSetter {
	void setValue(Stammdatum s, String value);
}

/**
 * Enthält alle Stammdaten-Eigenschaften sowie dazugehörige Funktionen, um an einem vorhanden Stammdatum einen Wert zu setzen, zu holen oder die jeweilige Eigenschaft zu vergleichen.
 *
 */
public enum StammdatumEntity {
	BEZEICHNUNG(1, "Bezeichnung",
			(s -> s.getName()),
			((s, value) -> s.setName(value)),
			((s1, s2) -> s1.getName().equals(s2.getName()))),
	TYP(2, "Typ",
			(s -> s.getTyp()),
			((s, value) -> s.setTyp(value)),
			((s1, s2) -> s1.getTyp().equals(s2.getTyp()))),
	BEZUGSJAHR(3, "Bezugsjahr",
			(s -> s.getBezugsjahr() != null ? "" + s.getBezugsjahr() : null),
			((s, value) -> s.setBezugsjahr(Integer.valueOf(value))),
			((s1, s2) -> s1.getTyp().equals(s2.getTyp()))),
	PROGNOSEHORIZONT(4, "Prognosehorizont",
			(s -> s.getPrognoseHorizont() != null ? "" + s.getPrognoseHorizont() : null),
			((s, value) -> s.setPrognoseHorizont(Integer.valueOf(value))),
			((s1, s2) -> s1.getPrognoseHorizont() == s2.getPrognoseHorizont())),
	TAKTUNG(5, "Taktung",
			(s -> s.getZeitintervall() != null ? "" + s.getZeitintervall().getLabel() : null),
			((s, value) -> s.setZeitintervall(TimeInterval.getInterval(value))),
			((s1, s2) -> s1.getZeitintervall() == s2.getZeitintervall())),
	SZENARIEN(6, "Szenarien",
			(s -> ""+s.isStandardszenario()),
			((s, value) -> s.setStandardszenario(Boolean.valueOf(value))),
			((s1, s2) -> {
				return s1.isStandardszenario() == s2.isStandardszenario();
			})),
	VERANTWORTLICHER_BEZUGSJAHR(7, "Email Verantwortlicher Bezugsjahr",
			(s -> s.getVerantwortlicherBezugsjahr().getEmail()),
			((s, value) -> s.getVerantwortlicherBezugsjahr().setEmail(value)),
			((s1, s2) -> s1.getVerantwortlicherBezugsjahr().getEmail().equals(s2.getVerantwortlicherBezugsjahr().getEmail()))),
	VERANTWORTLICHER_PRONOGEJAHR(8, "Email Verantwortlicher Prognosejahr",
			(s -> s.getVerantwortlicherPrognosejahr().getEmail()),
			((s, value) -> s.getVerantwortlicherPrognosejahr().setEmail(value)),
			((s1, s2) -> s1.getVerantwortlicherPrognosejahr().getEmail().equals(s2.getVerantwortlicherPrognosejahr().getEmail())));

	public int getIndex() {
		return index;
	}

	public String getName() {
		return name;
	}

	public String getValue(final Stammdatum stammdatum) {
		return converter.getValue(stammdatum);
	}

	public void setValue(final Stammdatum stammdatum, final String value) {
		setter.setValue(stammdatum, value);
	}

	public boolean compare(final Stammdatum stammdatum, final Stammdatum stammdatum2) {
		return comparator.compareValues(stammdatum, stammdatum2);
	}

	private int index; 
	private String name;
	private StammdatumValueGetter converter;
	private StammdatumValueSetter setter;
	private StammdatumComparator comparator;

	private StammdatumEntity(final int index, final String name, final StammdatumValueGetter converter, final StammdatumValueSetter setter, final StammdatumComparator comparator) {
		this.index = index;
		this.name = name;
		this.converter = converter;
		this.setter = setter;
		this.comparator = comparator;
	}

}