package de.unileipzig.irpsim.gams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Set;
import de.unileipzig.irpsim.core.simulation.data.SetElement;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;

/**
 * Verwaltet die Parametrisierung eines simplen GAMS-Modells, d.h. die Umsetzung von GAMSParametersJSON zu der GAMSHandler-Repräsentation von Daten.
 *
 * @author reichelt
 */
public class GAMSModelParametrisationUtil {

   private static final double MIKRO = 10E6;
   private static final Logger LOG = LogManager.getLogger(GAMSModelParametrisationUtil.class);
   private final GAMSHandler handler;
   private final BackendParametersYearData yeardata;
   private final List<SetElement> timeElements;
   private final int yearIndex;
   private final Map<Integer, Timeseries> referencedTimeseries = new LinkedHashMap<>();
   private final Map<Integer, Number> referencedScalars = new LinkedHashMap<>();
   private static final String[] BACKEND_SETS = new String[] { "set_t", "set_optstore", "set_optinitial", "set_optsteps", "set_a", "set_ii_0", "set_ii", "set_jj" };
   private static final String[] BACKEND_SCALARS = new String[] { "sca_a" };

   /**
    * Initialisiert ein ParametrisationUtil mit dem zu parametrisierenden GAMSHandler und den zu verwendenden Parametern.
    *
    * @param handler Zu parametrisierender Handler.
    * @param yeardata Die Parameterjahresdaten.
    * @param yearIndex Der Index des Jahres
    */
   public GAMSModelParametrisationUtil(final GAMSHandler handler, final BackendParametersYearData yeardata, final int yearIndex) {
      this.handler = handler;
      this.yeardata = yeardata;
      this.yearIndex = yearIndex;
      timeElements = buildTimeElements();
   }

   /**
    * Lädt die Modellparameter aus der Datenbank.
    *
    * @throws TimeseriesTooShortException Wenn eine der Übergebenen Zeitreihen zu kurz ist
    */
   public final void loadParameters() throws TimeseriesTooShortException {
      if (Thread.currentThread().isInterrupted()) {
         return;
      }

      checkTimeseriesLength();
      TimeseriesLoader loader = new TimeseriesLoader(yeardata);
      loader.load(referencedTimeseries, referencedScalars);
   }

   private void checkTimeseriesLength() throws TimeseriesTooShortException {
      String errorMessage = "";
      for (final Map.Entry<String, List<Number>> entry : yeardata.collectTimeseries(true).entrySet()) {
         if (entry.getValue().size() != yeardata.getConfig().getSimulationlength()) {
            errorMessage += "Zeitreihe für " + entry.getKey() + " ist " + entry.getValue().size() + " lang, sollte aber " + yeardata.getConfig().getSimulationlength()
                  + " lang sein.";
         }
      }
      if (errorMessage.length() > 0) {
         throw new TimeseriesTooShortException(errorMessage);
      }
   }

   /**
    * Parametrisiert den Handler mit den übergebenen Parametern.
    *
    * @throws TimeseriesTooShortException Wird geworfen, wenn eine Zeitreihe zu kurz ist.
    */
   public final void parameterizeModel() throws TimeseriesTooShortException {
      final long parametrisationStart = System.nanoTime();
      if (Thread.currentThread().isInterrupted()) {
         LOG.info("Interrupted");
         return;
      }
      final Map<String, Map<String, Number>> gamsSingleDependentSetParameters = new HashMap<>();
      final Map<String, Map<Vector<String>, Number>> gamsTimeDependentSetParameters = new HashMap<>();

      deleteBackendParameters();

      parameterizeSets(gamsSingleDependentSetParameters, gamsTimeDependentSetParameters);
      if (Thread.currentThread().isInterrupted()) {
         LOG.info("Interrupted");
         return;
      }
      LOG.info("Beginne Handler-Parametrisierung");

      parameterizeHandler(gamsSingleDependentSetParameters, gamsTimeDependentSetParameters);

      final long parametrisationEnd = System.nanoTime();
      LOG.info("Parametrisierungszeit: {}", (parametrisationEnd - parametrisationStart) / MIKRO);

      System.gc();
   }

   private void parameterizeSets(final Map<String, Map<String, Number>> gamsSingleDependentSetParameters,
         final Map<String, Map<Vector<String>, Number>> gamsTimeDependentSetParameters) throws TimeseriesTooShortException {
      for (final Set set : yeardata.getSets().values()) {
         if (set.getName().equals("set_a_total")) {
            continue;
         }
         LOG.trace("Elemente: {}", set.fetchElementNames().size());
         handler.addSetParameter(set.getName(), set.fetchElementNames());
         for (final SetElement setElement : set.getElements()) {
            if (Thread.currentThread().isInterrupted()) {
               LOG.info("Interrupted");
               return;
            }

            buildSingleDependents(gamsSingleDependentSetParameters, setElement);
            buildSetTimeseries(gamsTimeDependentSetParameters, setElement);
            if (Thread.currentThread().isInterrupted()) {
               LOG.info("Interrupted");
               return;
            }
         }
      }
   }

   private void buildSetTimeseries(final Map<String, Map<Vector<String>, Number>> gamsTimeDependentSetParameters, final SetElement setElement)
         throws TimeseriesTooShortException {
      for (final Entry<String, Timeseries> timeseriesEntry : setElement.getTimeseries().entrySet()) {
         // LOG.debug("Parametrisiere: {}", timeseriesEntry.getKey());
         if (!gamsTimeDependentSetParameters.containsKey(timeseriesEntry.getKey())) {
            gamsTimeDependentSetParameters.put(timeseriesEntry.getKey(), new LinkedHashMap<>());
         }
         try {
            Timeseries timeseries = getTimeseries(timeseriesEntry);
            // LOG.debug("Länge: {}", timeseries.getData().size());
            for (int i = 0; i < timeElements.size(); i++) {
               final Vector<String> dependents = new Vector<>();
               dependents.add(timeElements.get(i).getName());
               dependents.add(setElement.getName());
               final Number n = timeseries.getData().get(i);
               final Double value = n.doubleValue();
               gamsTimeDependentSetParameters.get(timeseriesEntry.getKey()).put(dependents, value);
            }
         } catch (final IndexOutOfBoundsException e) {
            final String errorMessage = "Zeitreihe " + timeseriesEntry.getKey() + " ist zu kurz! Setelement: " + setElement.getName() + ", Länge der Zeitreihe: "
                  + timeseriesEntry.getValue().size();
            throw new TimeseriesTooShortException(errorMessage);
         }
      }
   }

   private void buildSingleDependents(final Map<String, Map<String, Number>> gamsSingleDependentSetParameters, final SetElement setElement) {
      for (final Entry<String, Object> attributes : setElement.getAttributes().entrySet()) {
         final double value = getScalar(attributes);
         if (!gamsSingleDependentSetParameters.containsKey(attributes.getKey())) {
            gamsSingleDependentSetParameters.put(attributes.getKey(), new HashMap<>());
         }
         LOG.trace("Single-Dependent: {}", attributes.getKey());
         gamsSingleDependentSetParameters.get(attributes.getKey()).put(setElement.getName(), value);
      }
   }

   /**
    * Löscht die Sets und Skalare aus den von der UI übergebenen Daten, die später vom Backend erstellt und hinzugefügt werden.
    */
   public final void deleteBackendParameters() {
      for (final String deleteUISet : BACKEND_SETS) {
         yeardata.getSets().remove(deleteUISet);
      }
      for (final String deleteUIScalars : BACKEND_SCALARS) {
         yeardata.getScalars().remove(deleteUIScalars);
      }
   }

   public List<SetElement> getTimeElements() {
      return timeElements;
   }

   /**
    * Gibt die Zeitreihen-Setelemente zurück, die aus der Simulationslänge generiert werden.
    *
    * @return Die Zeitreihen-Setelemente.
    */
   public final List<SetElement> buildTimeElements() {
      final Set set_ii = yeardata.getTimeseriesSets()[0];
      final List<String> timeElements = set_ii.getElements().stream().map(b -> b.getName()).collect(Collectors.toList());
      final List<SetElement> resultTimeElements = set_ii.getElements();

      final List<String> optimizeElements = yeardata.getTimeseriesSets()[1].getElements().stream().map(b -> b.getName()).collect(Collectors.toList());
      handler.addSetParameter(yeardata.getTimeseriesSets()[1].getName(), optimizeElements);

      handler.addSetParameter(set_ii.getName(), timeElements);

      LOG.info("Simulationlength: {}", resultTimeElements.size());

      addOptimizationSets(timeElements, resultTimeElements);

      setSet_a_total();

      LOG.debug("Starte mit Index: {}", yearIndex);

      handler.addSetParameter("set_a", Arrays.asList(new String[] { "a" + yearIndex }));

      handler.addScalarParameter("sca_a", yearIndex);

      switch (yeardata.getConfig().getResolution()) {
      case 35040:
         handler.addScalarParameter("sca_delta_ii", 0.25);
         break;
      case 8760:
         handler.addScalarParameter("sca_delta_ii", 1);
         break;
      case 730:
         handler.addScalarParameter("sca_delta_ii", 12);
         break;
      case 365:
         handler.addScalarParameter("sca_delta_ii", 24);
         break;
      case 182:
         handler.addScalarParameter("sca_delta_ii", 48);
         break;
      case 52:
         handler.addScalarParameter("sca_delta_ii", 168);
         break;
      case 12:
         handler.addScalarParameter("sca_delta_ii", 730);
         break;
      }

      // yeardata.getConfig().getResolution();

      createMonth();
      return resultTimeElements;
   }

   public void addOptimizationSets(final List<String> timeElements, final List<SetElement> resultTimeElements) {
      handler.addSetParameter("set_t", timeElements.subList(0, yeardata.getConfig().getOptimizationlength()));

      handler.addSetParameter("set_optstore", timeElements.subList(0, yeardata.getConfig().getSavelength()));

      createOptSteps(resultTimeElements);
   }

   private void createMonth() {
      final List<String> month = new LinkedList<>();
      month.add("Januar");
      month.add("Februar");
      month.add("Maerz");
      month.add("April");
      month.add("Mai");
      month.add("Juni");
      month.add("Juli");
      month.add("August");
      month.add("September");
      month.add("Oktober");
      month.add("November");
      month.add("Dezember");

      handler.addSetParameter("set_jj", month);
   }

   private void setSet_a_total() {
      if (yeardata.getSetWithName("set_a_total") == null) {
         LOG.debug("Sonderbehandlung set_a_total");
         final List<String> years = new ArrayList<>();
         for (int year = 0; year <= yearIndex; year++) {
            years.add("a" + year);
         }
         handler.addSetParameter("set_a_total", years);
      } else {
         final List<String> set_a_total = yeardata.getSetWithName("set_a_total").fetchElementNames();
         Collections.sort(set_a_total);
         handler.addSetParameter("set_a_total", set_a_total);
      }
   }

   private void createOptSteps(final List<SetElement> resultTimeElements) {
      LOG.info("Erzeuge Optsteps: " + resultTimeElements.size());
      final List<String> optsteps = new LinkedList<>();
      for (int i = 0; i < resultTimeElements.size(); i += yeardata.getConfig().getSavelength()) {
         optsteps.add(resultTimeElements.get(i).getName());
      }
      handler.addSetParameter("set_optsteps", optsteps);

      LOG.info("Optsteps erzeugt");
   }

   /**
    * Parametrisiert den Handler mit den übergebenen Werten.
    *
    * @param gamsSingleDependentParameters Map der einfach abhängigen Parametern
    * @param gamsTimeDependentParameters Map der Zeitreihen, die von einfachen Parametern abhängen
    * @throws TimeseriesTooShortException Wird geworfen, wenn eine Zeitreihe zu kurz ist
    */
   public final void parameterizeHandler(final Map<String, Map<String, Number>> gamsSingleDependentParameters,
         final Map<String, Map<Vector<String>, Number>> gamsTimeDependentParameters) throws TimeseriesTooShortException {
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      parameterizeScalars();
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      parameterizeSingleDependents(gamsSingleDependentParameters);
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      parameterizeSetTimeDependents(gamsTimeDependentParameters);
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      parameterizeTimeseries();
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      parameterizeTableScalar();
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      parameterizeTableTimeseries();

   }

   /**
    * Parametrisiert den Handler mit den skalaren Werten.
    */
   private final void parameterizeScalars() {
      /**
       * Da sca_a für die Jahre selbst generiert wird - kann wieder raus, falls sca_a von der UI erstellt wird
       */
      yeardata.getScalars().remove("sca_a");
      yeardata.getScalars().remove("sca_delta_ii");
      for (final Map.Entry<String, Object> scalar : yeardata.getScalars().entrySet()) {
         handler.addScalarParameter(scalar.getKey(), getScalar(scalar));
      }
   }

   private double getScalar(final Map.Entry<String, Object> scalar) {
      final double value;
      if (scalar.getValue() instanceof Number) {
         value = ((Number) scalar.getValue()).doubleValue();
      } else if (scalar.getValue() instanceof String) {
         LOG.debug("Lade: " + scalar.getValue());
         value = referencedScalars.get(Integer.parseInt((String) scalar.getValue())).doubleValue();
         LOG.debug("Wert: {}", value);
      } else {
         throw new RuntimeException("Unerwarteter Typ in " + scalar.getKey() + ": " + scalar.getValue() + " " + scalar.getValue().getClass());
      }
      if (scalar.getValue() == null) {
         LOG.error("Wert ist nicht vorhanden!");
      }
      return value;
   }

   /**
    * Parametrisiert den Handler mit den Zeitreihen.
    */
   private final void parameterizeTimeseries() {
      yeardata.getTimeseries().remove("par_Setii");

      for (final Entry<String, Timeseries> timeseriesEntry : yeardata.getTimeseries().entrySet()) {
         try {
            Timeseries timeseries = getTimeseries2(timeseriesEntry);
            final Map<String, Number> map = new LinkedHashMap<>();
            for (int i = 0; i < timeseries.size() && i < timeElements.size(); i++) {
               map.put(timeElements.get(i).getName(), timeseries.getData().get(i));
            }
            if (map.isEmpty()) {
               LOG.error("Wert ist nicht vorhanden!");
            }
            handler.addSingleDependentParameter(timeseriesEntry.getKey(), map);
         } catch (final RuntimeException t) {
            t.printStackTrace();
            throw new RuntimeException("Unerwartete Fehlermeldung bei der Parametrisierung von " + timeseriesEntry.getKey() + " " + timeseriesEntry.getValue() + ": "
                  + t.getClass() + " " + t.getLocalizedMessage());
         }
      }
      final Map<String, Number> map = new HashMap<>();
      for (int i = 0; i < timeElements.size(); i++) {
         map.put(timeElements.get(i).getName(), i);
      }
      handler.addSingleDependentParameter("par_Setii", map);
   }

   // TODO mergen
   private Timeseries getTimeseries(final Entry<String, Timeseries> timeseriesEntry) {
      Timeseries timeseries;
      if (timeseriesEntry.getValue() != null && timeseriesEntry.getValue().hasReference()) {
         timeseries = referencedTimeseries.get(timeseriesEntry.getValue().getSeriesname());
      } else if (timeseriesEntry.getValue() != null && timeseriesEntry.getValue().hasTimeseries()) {
         timeseries = timeseriesEntry.getValue();
      } else {
         throw new IndexOutOfBoundsException(timeseriesEntry.getKey() + "hat keine gespeicherte Zeitreihe!");
      }
      return timeseries;
   }

   private Timeseries getTimeseries2(final Entry<String, Timeseries> timeseriesEntry) {
      Timeseries timeseries;
      final Integer reference = timeseriesEntry.getValue().getSeriesname();
      if (reference != null) {
         timeseries = referencedTimeseries.get(reference);
      } else {
         timeseries = timeseriesEntry.getValue();
      }
      return timeseries;
   }

   /**
    * Parametrisiert den Handler mit den Zeitabhängigen Parametern.
    *
    * @param gamsTimeDependentParameters Zeitabhängige Parameter
    */
   private final void parameterizeSetTimeDependents(final Map<String, Map<Vector<String>, Number>> gamsTimeDependentParameters) {
      for (final Entry<String, Map<Vector<String>, Number>> element : gamsTimeDependentParameters.entrySet()) {
         LOG.trace("Set time dependent: {}{}", element.getKey(), element.getValue());
         if (element.getValue() == null || element.getValue().isEmpty()) {
            LOG.error("Wert ist nicht vorhanden!");
         }
         handler.addMultiDependentParameter(element.getKey(), element.getValue());
      }
   }

   /**
    * Parametrisiert den Handler mit den einfach abhängigen Parametern.
    *
    * @param gamsSingleDependentParameters Die einfach abhängigen Parameter jeweils mit Abhängigen Sets und zugehörigem Wert.
    */
   private final void parameterizeSingleDependents(final Map<String, Map<String, Number>> gamsSingleDependentParameters) {
      for (final Entry<String, Map<String, Number>> element : gamsSingleDependentParameters.entrySet()) {
         LOG.trace("Single-Dependent: {} {}", element.getKey(), element.getValue());
         if (element.getValue() == null || element.getValue().isEmpty()) {
            LOG.error("Wert ist nicht vorhanden!");
         }
         handler.addSingleDependentParameter(element.getKey(), element.getValue());
      }
   }

   /**
    * Parametrisiert den Handler mit den Tabellen.
    */
   private final void parameterizeTableScalar() {
      yeardata.getTableValues().forEach((parameter, firstDependents) -> {
         firstDependents.forEach((firstDependent, secondDependents) -> {
            for (final Map.Entry<String, Object> entry : secondDependents.entrySet()) {
               LOG.trace("Parametrisiere {} - Abhängigkeiten {} {}", parameter, firstDependent, entry.getKey());
               handler.addMultiDependentScalar(parameter, firstDependent, entry.getKey(), getScalar(entry));
            }
         });
      });
   }

   /**
    * Parametrisiert den Handler mit den Zeitreihentabellen.
    * 
    * @throws TimeseriesTooShortException
    */
   private final void parameterizeTableTimeseries() throws TimeseriesTooShortException {
      yeardata.executeOnTableTimeseries((parameter, firstDependent, secondDependent, timeseries) -> {
         if (Thread.currentThread().isInterrupted()) {
            return;
         }
         final List<String> dependents = Arrays.asList(new String[] { firstDependent, secondDependent });
         final Integer reference = timeseries.getSeriesname();
         if (timeseries.hasReference()) {
            timeseries = referencedTimeseries.get(reference);
         } else {
            if (timeseries.getData() == null || timeseries.getData().size() == 0) {
               LOG.error("Referenz für {} ist nicht vorhanden: {}", parameter, reference);
            }
         }
         if (timeseries != null) {
            addTimeseries(parameter, timeseries.getData(), dependents);
         } else {
            LOG.error("Zeitreihe {} konnte nicht geladen werden", reference);
            final Number value = referencedScalars.get(reference);
            if (value != null) {
               LOG.info("Skalar konnte geladen werden: {}", value);
            } else {
               LOG.info("Skalar konnte auch nicht geladen werden");
            }
         }

      });

   }

   /**
    * Parametrisiert den GAMS-Handler mit der übergebenen Zeitreihe.
    *
    * @param name Name des Parameters.
    * @param valueList Werte der Zeitreihe.
    * @param otherNames Sonstige Set-Elemente, von denen die Zeitreihe abhängt.
    * @throws TimeseriesTooShortException
    */
   private void addTimeseries(final String name, final List<Number> valueList, final List<String> otherNames) throws TimeseriesTooShortException {
      try {
         final Map<Vector<String>, Number> values = new LinkedHashMap<>();
         LOG.trace("Name: {} Zeit: {} Zeitreihe: {} Other: {}", name, timeElements.size(), valueList.size(), otherNames);
         for (int i = 0; i < timeElements.size(); i++) {
            final Vector<String> vector = new Vector<>();
            vector.add(timeElements.get(i).getName());
            vector.addAll(otherNames);
            values.put(vector, valueList.get(i));
            if (valueList.get(i) == null) {
               LOG.error("Wert ist nicht vorhanden!");
            }
         }
         LOG.trace("Werte: {}", values.keySet().iterator().next());
         if (values.isEmpty()) {
            LOG.error("Wert ist nicht vorhanden!");
         }
         handler.addMultiDependentParameter(name, values);
      } catch (final IndexOutOfBoundsException e) {
         throw new TimeseriesTooShortException(
               "Zeitreihe " + name + " ist zu kurz! Erwartete Länge: " + timeElements.size() + ", Tatsächliche Länge: " + valueList.size() + ", Abhängigkeiten: " + otherNames);
      }

   }

   public final Map<Integer, Timeseries> getReferencedTimeseries() {
      return referencedTimeseries;
   }
}
