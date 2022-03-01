package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

/**
 * Repräsentiert alle set_ii-Optionen, notwendig für REST-Schnittstelle
 *
 */
public class SetIiOptions {

    private String[] options = new String[]{};

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }
}
