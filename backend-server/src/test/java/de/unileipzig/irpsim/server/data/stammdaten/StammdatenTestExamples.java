package de.unileipzig.irpsim.server.data.stammdaten;

import de.unileipzig.irpsim.core.simulation.data.TimeInterval;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;

public class StammdatenTestExamples {

	public static Stammdatum getThermalLoadExample() {
		final Stammdatum sd_therm = new Stammdatum();
		sd_therm.setBezugsjahr(2016);
		sd_therm.setPrognoseHorizont(2);
		sd_therm.setZeitintervall(TimeInterval.DAY);
		sd_therm.setName("Thermisches Lastprofil");
		sd_therm.setTyp("par_L_DS_G");
		sd_therm.getVerantwortlicherBezugsjahr().setName("schulze");
		sd_therm.getVerantwortlicherBezugsjahr().setEmail("schulze@test.de");
		sd_therm.getVerantwortlicherPrognosejahr().setName("meier");
		sd_therm.getVerantwortlicherPrognosejahr().setEmail("meier@test.de");
		sd_therm.setStandardszenario(false);

		return sd_therm;
	}

	public static Stammdatum getElectricLoadExample() {
		final Stammdatum sd_el = new Stammdatum();
		sd_el.setBezugsjahr(2016);
		sd_el.setPrognoseHorizont(5);
		sd_el.setZeitintervall(TimeInterval.DAY);
		sd_el.setName("Elektrisches Lastprofil");
		sd_el.setTyp("par_L_DS_E");
		sd_el.getVerantwortlicherBezugsjahr().setName("schulze");
		sd_el.getVerantwortlicherBezugsjahr().setEmail("schulze@test.de");
		sd_el.getVerantwortlicherPrognosejahr().setName("meier");
		sd_el.getVerantwortlicherPrognosejahr().setEmail("meier@test.de");
		return sd_el;
	}

	public static Stammdatum getMissingDataExample() {
		final Stammdatum sd_el = new Stammdatum();
		sd_el.setBezugsjahr(2016);
		sd_el.setPrognoseHorizont(10);
		sd_el.setName("Elektrisches Lastprofil Fehlend");
		sd_el.setTyp("par_L_DS_E");
		sd_el.getVerantwortlicherBezugsjahr().setName("mueller");
		sd_el.getVerantwortlicherBezugsjahr().setEmail("mueller@test.de");
		sd_el.getVerantwortlicherPrognosejahr().setName("meier");
		sd_el.getVerantwortlicherPrognosejahr().setEmail("meier@test.de");
		return sd_el;
	}

	public static Stammdatum getLoadExample() {
		final Stammdatum sd_last = new Stammdatum();
		sd_last.setBezugsjahr(2016);
		sd_last.setPrognoseHorizont(10);
		sd_last.setName("Lastprofil");
		sd_last.setTyp("par_L_DS");
		sd_last.getVerantwortlicherBezugsjahr().setName("mueller");
		sd_last.getVerantwortlicherBezugsjahr().setEmail("mueller@test.de");
		sd_last.getVerantwortlicherPrognosejahr().setName("meier");
		sd_last.getVerantwortlicherPrognosejahr().setEmail("meier@test.de");
		return sd_last;

	}
}
