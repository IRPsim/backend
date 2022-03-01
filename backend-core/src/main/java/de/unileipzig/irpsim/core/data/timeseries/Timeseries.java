package de.unileipzig.irpsim.core.data.timeseries;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;

/**
 * Hält die Zeitreihe bzw. die Referenz zur Zeitreihe in der der Datenbank.
 */
public final class Timeseries {

   private static final Logger LOG = LogManager.getLogger(Timeseries.class);

   private List<Number> data;
   private Integer reference;

   /**
    * POJO Konstruktor für Deserialisierung. Sollte nicht direkt genutzt werden.
    */
   public Timeseries() {
      reference = null;
   }

   public List<Number> getData() {
      return data;
   }

   public void setData(final List<Number> data) {
      this.data = data;
   }

   /**
    * Die {@link Timeseries} Instanz, die nur die Nullzeitreihenreferenz enthält. Enthält nie die Daten.
    */
   public static final Timeseries ZEROTIMESERIES_REFERENCE = new Timeseries(Constants.ZERO_TIMESERIES_NAME);

   /**
    * Die {@link Timeseries} Instanz, die nur die Nullzeitreihendaten enthält. Enthält nie die Referenz.
    */
   public static final Timeseries ZEROTIMESERIES_DATA = new Timeseries(Constants.ZERO_TIMESERIES_VALUES);

   static {
      ZEROTIMESERIES_DATA.setData(ZEROTIMESERIES_DATA.getData());
   }

   /**
    * Nutze diese Funktion statt dem Konstruktor. Initialisiert das Objekt mit einer Zeitreihe. Die Referenz kann nicht hinzugefügt werden (Dafür muss ein neues Objekt
    * initialisiert werden).
    *
    * @param timeseries Die übergebene Zeitreihe
    * @return Die {@link Timeseries} Instanz, wenn alle (Anzahl >= 1) Elemente 0 sind wird die Nullzeitreiheninstanz zurückgegeben
    */
   public static Timeseries build(final List<Number> timeseries) {
      if (timeseries.size() > 0) {
         for (final Number number : timeseries) {
            if (!number.equals(0)) {
               return new Timeseries(timeseries);
            }
         }
         return ZEROTIMESERIES_DATA;
      } else {
         return new Timeseries(timeseries);
      }
   }

   /**
    * Nutze diese Funktion statt dem Konstruktor. Initialisiert das Objekt mit einer Zeitreihe. Die Referenz kann nicht hinzugefügt werden (Dafür muss ein neues Objekt
    * initialisiert werden).
    *
    * @param timeseries Die übergebene Zeitreihe
    * @param tested gibt an, ob bereits auf Nullzeitreihe getestet wurde, dann wird der Test nicht widerholt. Nur true setzen, wenn sicher ist, dass es scih nicht um eine
    *           Nullzeitreihe handelt!
    * @return Die {@link Timeseries} Instanz, wenn alle Elemente 0 sind wird die Nullzeitreiheninstanz zurückgegeben
    */
   public static Timeseries build(final List<Number> timeseries, final boolean tested) {
      if (tested) {
         return new Timeseries(timeseries);
      } else {
         return build(timeseries);
      }
   }

   /**
    * Nutze diese Funktion statt dem Konstruktor. Initialisiert das Objekt mit einer Referenz.
    *
    * @param reference Die übergebene Referenz
    * @return Die {@link Timeseries} Instanz, {@link Timeseries#ZEROTIMESERIES_REFERENCE} wenn die übergebene Referenz gleich {@link Constants#ZERO_TIMESERIES_NAME} ist
    */
   public static Timeseries build(final int reference) {
      if (reference == Constants.ZERO_TIMESERIES_NAME) {
         return ZEROTIMESERIES_REFERENCE;
      } else {
         return new Timeseries(reference);
      }
   }

   public boolean isZeroTimeseries() {
      if (reference == ZEROTIMESERIES_REFERENCE.getSeriesname()) {
         return true;
      }
      boolean hasNonZeroElement = false;
      for (final Number d : data) {
         if (d.doubleValue() != 0d) {
            hasNonZeroElement = true;
            break;
         }
      }
      return hasNonZeroElement;
   }

   /**
    * Initialisiert das Objekt mit einer Zeitreihe. Die Referenz kann nicht hinzugefügt werden (Dafür muss ein neues Objekt initialisiert werden).
    *
    * @param timeseries Die Zeitreihe
    */
   @JsonIgnore
   private Timeseries(final List<Number> timeseries) {
      setData(timeseries);
      this.reference = null;
   }

   /**
    * Initialisiert das Objekt mit einer Referenz.
    *
    * @param reference Die Referenz
    */
   @JsonIgnore
   private Timeseries(final int reference) {
      setData(new ArrayList<>());
      this.reference = reference;
   }

   @JsonIgnore
   public List<Double> getValues() {
      final List<Double> values = new ArrayList<>();
      getData().forEach(value -> values.add(value.doubleValue()));
      return values;
   }

   /**
    * @return Liefert wahr wenn eine Referenz vorhanden ist, sonst false.
    */
   @JsonIgnore
   public boolean hasReference() {
      return reference != null ? true : false;
   }

   /**
    * @return Liefert wahr wenn die Zeitreihenliste nicht leer ist.
    */
   @JsonIgnore
   public boolean hasTimeseries() {
      return !getData().isEmpty();
   }

   @Override
   public String toString() {
      final StringBuilder string = new StringBuilder("Referenz: ");
      string.append(hasReference() ? reference : hasReference());
      string.append("; Zeitreihe: ");
      string.append(hasTimeseries() ? getData() : hasTimeseries());
      return string.toString();
   }

   /**
    * @return Liefert die Größe der Zeitreihe oder 0 wenn diese leer ist.
    */
   public int size() {
      return hasTimeseries() ? getData().size() : 0;
   }

   /**
    * @param value Der hinzuzufügende Wert
    * @return True, wenn der Wert zur Zeitreihe hinzugefügt wurde. Kann nicht zur ZEROTIMESERIES hinzufügen.
    */
   public boolean add(final Number value) {
      if (this.equals(ZEROTIMESERIES_REFERENCE) || this.equals(ZEROTIMESERIES_DATA)) {
         LOG.error("Kann keine Werte zur Nullzeitreihenreferenz hinzufügen!");
         return false;
      }
      return getData().add(value);
   }

   /**
    * Lädt die Zeitreihe aus der Datenbank. Tut nichts, wenn keine Referenz vorhanden ist.
    *
    * @param override Bestimmt, ob die vorhandene Zeitreihe überschrieben werden soll.
    * @return Die Zeitreihe mit Daten, gibt für die Nullzeitreihe die Referenz zur Instanz mit Zeitreihe zurück.
    */
   public Timeseries loadTimeseries(final boolean override) {
      if (hasReference() && this != ZEROTIMESERIES_REFERENCE && (!hasTimeseries() || override)) {
         final String valueQuery = "select value " + "from series_data_in where seriesid=" + reference + " order by unixtimestamp";
         final String valueQuery2 = "select value " + "from series_data_out where seriesid=" + reference + " order by unixtimestamp";
         readDataFromQuery(valueQuery);
         if (size() == 0) {
            readDataFromQuery(valueQuery2);
         }
      } else if (this == ZEROTIMESERIES_REFERENCE) {
         this.data = ZEROTIMESERIES_DATA.getData();
      }
      return this;
   }

   @SuppressWarnings("unchecked")
   private void readDataFromQuery(final String valueQuery) {
      final List<Object> resultList;
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final Query query = em.createNativeQuery(valueQuery);
         resultList = query.getResultList();
      }
      LOG.trace("valueQuery liefert {} Ergebnisse.", resultList.size());
      for (final Object element : resultList) {
         final Double value = (Double) element;
         data.add(value);
      }
      LOG.debug("Länge: " + size());
   }

   /**
    * @return Die Zeitreihe, wobei jeder Wert auf float gecastet wurde, für Darstellungszwecke
    */
   public List<Number> fetchTimeseriesReadable() {
      final List<Number> timeseriesFloat = new ArrayList<>();

      getData().forEach(value -> {
         if (value instanceof Number) {
            final Number value2;
            if (value instanceof Integer) {
               value2 = (float) (int) value;
            } else if (value instanceof Double) {
               value2 = (float) (double) value;
            } else {
               value2 = (float) (double) value;
            }
            timeseriesFloat.add(value2);
         }
      });
      return timeseriesFloat;
   }

   @JsonIgnore
   public Integer getSeriesname() {
      return reference;
   }

   public void setSeriesname(final String seriesname) {
      reference = Integer.parseInt(seriesname);
   }

   public void setSeriesname(final Integer seriesname) {
      reference = seriesname;
   }

   public void setValues(final List<Double> changedTimeseries) {
      data = new LinkedList<>();
      changedTimeseries.forEach(value -> data.add(value.doubleValue()));
   }
}