package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

public class KostenUmsatz {

    private double umsatz;
    private double kosten;

    public KostenUmsatz(double umsatz, double kosten) {
        this.umsatz = umsatz;
        this.kosten = kosten;
    }

    public double getUmsatz() {
        return umsatz;
    }

    public void setUmsatz(double umsatz) {
        this.umsatz = umsatz;
    }

    public double getKosten() {
        return kosten;
    }

    public void setKosten(double kosten) {
        this.kosten = kosten;
    }
}
