package de.unileipzig.irpsim.utils.transformer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Set;
import de.unileipzig.irpsim.core.simulation.data.SetElement;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersSingleModel;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;

/**
 * Werkzeuge, um eine JSON-Datei im {@link JSONParametersSingleModel} Format zu bearbeiten.
 *
 * @author krauss
 */
public final class ExtendedScenarioBuilder {

   private static final Logger LOG = LogManager.getLogger(ExtendedScenarioBuilder.class);
   public static final ObjectMapper MAPPER = new ObjectMapper();

   static {
      MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
   }

   // private static File source;
   private JSONParametersMultimodel data;

   // public static void setSource(final File source) {
   // ExtendedScenarioBuilder.source = source;
   // }

   // public static void setData(GAMSParametersJSON data){
   // ExtendedScenarioBuilder.data = data;
   // }

   /**
    * @param args Argumente
    * @throws TimeseriesTooShortException
    */
   public static void main(final String[] args) throws JsonParseException,
         JsonMappingException, IOException, TimeseriesTooShortException {
      final File parameterFile = new File(args[0]);
      if (!parameterFile.exists()) {
         System.err.println("Fehler: Eingabedatei " + args[0]
               + " existiert nicht");
      }

      final ExtendedScenarioBuilder builder = new ExtendedScenarioBuilder(parameterFile);

      builder.changeNumberOfDays(1).extendSet("set_p_DS", 1);
      builder.save(new File(args[0].substring(0, args[0].lastIndexOf("."))
            + "_extended.json"));
   }

   /**
    * Erstellt das Werkzeug für eine übergebene Datei, diese kann dann mit den weiteren Methoden bearbeitet werden.
    */
   public ExtendedScenarioBuilder(final File source)
         throws JsonParseException, JsonMappingException, IOException {
      data = MAPPER.readValue(source, JSONParametersMultimodel.class);
   }

   public ExtendedScenarioBuilder(final JSONParametersMultimodel data)
         throws JsonParseException, JsonMappingException, IOException {
      this.data = data;
   }

   /**
    * Erstellt bzw. überschreibt die Zieldatei mit den geänderten Daten.
    *
    * @param target Zieldatei, in die gespeichert werden soll.
    */
   public void save(final File target) throws IOException {
      LOG.debug("Speichern unter: {}", target.getAbsoluteFile());
      MAPPER.writeValue(target.getAbsoluteFile(), data);
   }

   /**
    * Fügt eine Kopie des durch den Index gegebenen Jahres am Ende der Datei hinzu.
    *
    * @param yearIndex Index des zu kopierenden Jahres
    * @return Dieses Werkzeug, um Verkettung zu ermöglichen
    */
   public ExtendedScenarioBuilder addMirroredYear(final int yearIndex)
         throws JsonParseException, JsonMappingException, IOException {
      data.getModels().get(0).getYears().add(data.getModels().get(0).getYears().get(yearIndex));
      return this;
   }

   /**
    * Fügt nach dem indizierten Jahr die gegebene Anzahl an null Jahren hinzu, z.B. für Interpolation.
    *
    * @param yearBeforeIndex Index des Jahres, nach dem die leeren Jahre hinzugefügt werden sollen
    * @param nullYears Anzahl neuer leerer Jahre
    * @return Dieses Werkzeug, um Verkettung zu ermöglichen
    */
   public ExtendedScenarioBuilder addNullYears(final int yearBeforeIndex,
         final int nullYears) throws JsonParseException,
         JsonMappingException, IOException {
      for (int i = 1; i <= nullYears; i++) {
         data.getModels().get(0).getYears().add(yearBeforeIndex + i, (YearData) null);
      }
      return this;
   }

   /**
    * Addiert im indizierten Jahr und zur gegebenen Zeitreihe die zweite gegebene Zeitreihe.
    *
    * @param yearIndex Index des zu ändernden Jahres
    * @param timeseriesName Der Name der Zeitreihe, erster String muss "timeseries", "sets", oder "table" sein, danach kommen die Namen der tieferen Level der Bezeichnung (z.B.
    *           Setname, SetElementname, Parametername).
    * @param timeseriesToAdd Die zu addierende Zeitreihe
    * @return Dieses Werkzeug, um Verkettung zu ermöglichen
    */
   public ExtendedScenarioBuilder addToSingleTimeseries(final int yearIndex,
         final String[] timeseriesName, final List<Number> timeseriesToAdd)
         throws JsonParseException, JsonMappingException, IOException {
      final BackendParametersYearData yearData = new BackendParametersYearData(
            data.getModels().get(0).getYears().get(yearIndex));
      List<Number> timeseriesBefore = null;
      switch (timeseriesName[0]) {
      case "timeseries":
         timeseriesBefore = yearData.getTimeseries().get(timeseriesName[1])
               .getData();
         yearData.getTimeseries()
               .get(timeseriesName[1])
               .setData(addToTimeseries(timeseriesToAdd, timeseriesBefore));
         break;
      case "sets":
         timeseriesBefore = yearData.getSetWithName(timeseriesName[1])
               .getElement(timeseriesName[2]).getTimeseries()
               .get(timeseriesName[3]).getData();
         yearData.getSetWithName(timeseriesName[1])
               .getElement(timeseriesName[2])
               .getTimeseries()
               .put(timeseriesName[3],
                     Timeseries.build(addToTimeseries(timeseriesToAdd,
                           timeseriesBefore)));
         break;
      case "tables":
         final Timeseries timeseries = yearData.getTableTimeseries()
               .get(timeseriesName[1]).get(timeseriesName[2])
               .get(timeseriesName[3]);
         timeseries.setData(addToTimeseries(timeseriesToAdd,
               timeseries.getData()));
         break;
      default:
         break;
      }
      data.getModels().get(0).getYears().remove(yearIndex);
      data.getModels().get(0).getYears().add(yearIndex, yearData.createJSONParameters());
      return this;
   }

   /**
    * @param timeseriesToAdd Die zu addierende Zeitreihe
    * @param timeseriesBefore Die Zeitreihe, zu der addiert wird
    * @return Die Summenzeitreihe
    */
   private List<Number> addToTimeseries(final List<Number> timeseriesToAdd,
         final List<Number> timeseriesBefore) {
      final List<Number> timeseriesAfter = new ArrayList<>();
      final Iterator<Number> toAddIterator = timeseriesToAdd.iterator();
      for (final Number numBef : timeseriesBefore) {
         if (toAddIterator.hasNext()) {
            timeseriesAfter.add((double) numBef
                  + (double) toAddIterator.next());
         }
      }
      return timeseriesAfter;
   }

   /**
    * Ändert das Datum des indizierten Jahres.
    *
    * @param yearIndex Index des zu ändernden Jahres
    * @param yearDate Neues Jahresdatum
    * @return Dieses Werkzeug, um Verkettung zu ermöglichen
    */
   public ExtendedScenarioBuilder changeYearDate(final int yearIndex,
         final int yearDate) throws JsonParseException,
         JsonMappingException, IOException {
      data.getModels().get(0).getYears().get(yearIndex).getConfig().setYear(yearDate);
      return this;
   }

   /**
    * Verdoppelt die Werte der Zeitreihe.
    *
    * @param yearIndex Index des zu ändernden Jahres
    * @param timeseriesName Der Name der Zeitreihe, erster String muss "timeseries", "sets", oder "table" sein, danach kommen die Namen der tieferen Level der Bezeichnung (z.B.
    *           Setname, SetElementname, Parametername).
    * @return Dieses Werkzeug, um Verkettung zu ermöglichen
    */
   public ExtendedScenarioBuilder doubleSingleTimeseries(final int yearIndex,
         final String[] timeseriesName) throws JsonParseException,
         JsonMappingException, IOException {
      final BackendParametersYearData yearData = new BackendParametersYearData(
            data.getModels().get(0).getYears().get(yearIndex));
      List<Number> timeseriesToAdd = null;
      switch (timeseriesName[0]) {
      case "timeseries":
         timeseriesToAdd = yearData.getTimeseries().get(timeseriesName[1])
               .getData();
         break;
      case "sets":
         timeseriesToAdd = yearData.getSetWithName(timeseriesName[1])
               .getElement(timeseriesName[2]).getTimeseries()
               .get(timeseriesName[3]).getData();
         break;
      case "tables":
         timeseriesToAdd = yearData.getTableTimeseries()
               .get(timeseriesName[1]).get(timeseriesName[2])
               .get(timeseriesName[2]).getData();
         break;
      default:
         break;
      }
      return addToSingleTimeseries(yearIndex, timeseriesName, timeseriesToAdd);
   }

   /**
    * @param days Anzahl der Tage, die jede Zeitreihe haben soll.
    * @return Dieses Werkzeug, um Verkettung zu ermöglichen
    * @throws TimeseriesTooShortException
    */
   public ExtendedScenarioBuilder changeNumberOfDays(final int days) throws JsonParseException, JsonMappingException, IOException, TimeseriesTooShortException {
      final BackendParametersMultiModel bpData = new BackendParametersMultiModel(data);
      final int dayLength = (int) (24 / data.fetchConfig().getTimestep());
      for (final BackendParametersYearData yearData : bpData.getModels()[0].getYeardata()) {

         yearData.getConfig().setSimulationlength(dayLength * days);
         yearData.getTimeseries().values().forEach(timeseries -> changeTimeseriesLength(timeseries, days));

         yearData.getSets().values().forEach(set -> set.getElements().forEach(element -> {
            element.getTimeseries().values().forEach(timeseries -> changeTimeseriesLength(timeseries, days));
         }));

         yearData.executeOnTableTimeseries((tableName, firstDep, secondDep, timeseries) -> {
            changeTimeseriesLength(timeseries, days);
         });
      }
      data = bpData.createJSONParameters();
      return this;
   }

   /**
    * Ändert die Zeitreihe auf die gegebene Zahl von Tagen.
    *
    * @param timeseries Ur-Zeitreihe
    * @param days Anzahl Tage
    */
   public void changeTimeseriesLength(final Timeseries timeseries, final int days) {
      if (!(timeseries.getSeriesname() == Timeseries.ZEROTIMESERIES_REFERENCE.getSeriesname())) {
         final int dayLength = (int) (24 / data.fetchConfig().getTimestep());
         final int daysBefore = timeseries.size() / dayLength;
         final List<Number> newTimeseries = new ArrayList<>();
         for (int dayIndex = 0; dayIndex < days; dayIndex++) {
            final int startIndex = dayIndex % daysBefore * dayLength;
            newTimeseries.addAll(timeseries.getData().subList(startIndex, startIndex + dayLength));
         }
         timeseries.setData(newTimeseries);
      }

   }

   /**
    * Fügt eine neue Kundengruppe hinzu.
    *
    * @param setName Name Des zu erweiternden Setsfinal Map<String, ?> removedSet =
    * @param count Anzahl zusätzlicher Elemente
    * @param modelName Modellname
    * @return Dieses Werkzeug, um Verkettung zu ermöglichen
    */
   public ExtendedScenarioBuilder extendSet(final String setName,
         final int count) throws JsonParseException,
         JsonMappingException, IOException {
      final BackendParametersMultiModel bpData = new BackendParametersMultiModel(data);
      for (final BackendParametersYearData yearData : bpData.getModels()[0].getYeardata()) {

         final List<String> newSetNames = new ArrayList<>();
         final Set set = yearData.getSetWithName(setName);
         if (set == null) {
            return this;
         }
         final SetElement element = set.getElements().get(0);
         final List<String> elementNames = new LinkedList<>();
         set.getElements().forEach(e -> elementNames.add(e.getName()));
         final String name = element.getName();
         newSetNames.add(name);
         for (int i = 0; i < count; i++) {
            final String name1 = findNewName(elementNames, name, i);
            final SetElement newElement = new SetElement();
            set.getElements().add(newElement);
            newElement.setAttributes(element.getAttributes());
            newElement.setName(name1);
            newElement.setTimeseries(element.getTimeseries());
            newSetNames.add(name1);
         }

         extendTableMap(yearData.getTableTimeseries(), setName, count,
               newSetNames);
         extendTableMap(yearData.getTableValues(), setName, count,
               newSetNames);

      }
      return this;
   }

   /**
    * @param map Map<String, Object>
    * @param setName Name des Sets
    * @param count Anzahl zusätzlicher Elemente
    * @param modelName Modellname
    * @param newElementNames Namen, die bei denm Set erstellt wurden, an Stelle null ist der Ausgangsname
    * @return Ob die Map das Set enthielt
    */
   private boolean extendTableMap(final Object map, final String setName,
         final int count, final List<String> newElementNames) {
      if (!(map instanceof Map)) {
         return false;
      }
      @SuppressWarnings("unchecked")
      final Map<String, Map<String, Map<String, Object>>> stringMap = (Map<String, Map<String, Map<String, Object>>>) map;
      for (final Map<String, Map<String, Object>> firstDeps : stringMap
            .values()) {
         for (final Map.Entry<String, Map<String, Object>> firstDep : firstDeps
               .entrySet()) {
            if (firstDep.getKey().equals(newElementNames.get(0))) {
               for (int i = 1; i <= count; i++) {
                  firstDeps.put(newElementNames.get(i),
                        firstDep.getValue());
               }
               break;
            }
            for (final Map.Entry<String, Object> secondDep : firstDep
                  .getValue().entrySet()) {
               if (secondDep.getKey().equals(newElementNames.get(0))) {
                  for (int i = 1; i <= count; i++) {
                     firstDep.getValue().put(newElementNames.get(i),
                           firstDep.getValue());
                  }
                  break;
               }
            }
         }
      }
      return true;
   }

   /**
    * @param names Bereits vorhandene Namen
    * @param name Zu modifizierender Name
    * @param i count
    * @return neuer Name
    */
   private String findNewName(final Collection<String> names,
         final String name, final int i) {
      final char lastChar = name.charAt(name.length() - 1);
      String name1 = name;
      if (Character.isDigit(lastChar)) {
         int number = Integer.valueOf(lastChar) + 1;
         while (names.contains(name1)) {
            number++;
            name1 = name.substring(0, name.length() - 1) + number;
         }
      } else {
         name1 = name + i;
      }
      return name1;
   }

   /**
    * Löscht das Set nur aus der Setliste, aber nicht die Setelemente aus den Tabellen.
    *
    * @param set Name des Sets
    * @return Diesen Builder zur Verkettung
    */
   public ExtendedScenarioBuilder deleteSet(final String set)
         throws JsonParseException, JsonMappingException, IOException {
      for (final YearData year : data.getModels().get(0).getYears()) {
         LOG.debug("Lösche Set {}", year.getSets().get(set));
         year.getSets().remove(set);
      }
      return this;
   }

   public JSONParametersMultimodel getData() {
      return data;
   }

   /**
    * Setzt die BuisinessModelDescription des Modells, diese wird im ServerStarter als Beschreibung für die Metadaten genutzt.
    *
    * @param description Beschreibung des Szenarios.
    * @return Diesen Builder für Verkettung.
    */
   public ExtendedScenarioBuilder describe(final String description) {
      data.getDescription().setBusinessModelDescription(description);
      return this;
   }

}
