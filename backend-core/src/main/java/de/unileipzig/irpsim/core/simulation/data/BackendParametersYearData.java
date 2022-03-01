package de.unileipzig.irpsim.core.simulation.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.utils.DataUtils;
import de.unileipzig.irpsim.core.utils.ParameterBaseDependenciesUtil;
import de.unileipzig.irpsim.core.utils.ParameterInputDependenciesUtil;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;

/**
 * Beinhaltet Backendparameter für spezifische Jahredaten.
 *
 * @author reichlet
 */
public class BackendParametersYearData {

   private static final Logger LOG = LogManager.getLogger(BackendParametersYearData.class);

   private Globalconfig config;

   private final Set[] timeseriesSets = new Set[2];
   private Map<String, Set> sets = new HashMap<>();
   private Map<String, Object> scalars = new LinkedHashMap<>();
   private Map<String, Timeseries> timeseries = new LinkedHashMap<>();
   private Map<String, Map<String, Map<String, Object>>> tableValues = new LinkedHashMap<>();
   private Map<String, Map<String, Map<String, Timeseries>>> tableTimeseries = new LinkedHashMap<>();
   private PostProcessing postprocessing = new PostProcessing();

   /**
    * Erstellt neues BackendParametersYearData-Objekt mit den spezifischen Jahresdaten.
    *
    * @param year Die spezifischen Jahresdaten
    */
   public BackendParametersYearData(final YearData year) {
      config = year.getConfig();
      createTimeseriesSets();

      scalars.putAll(year.getScalars());

      initializeTimeseries(year);

      initializeSets(year);

      initializeTables(year);

      this.postprocessing = year.getPostprocessing();
   }

   public void createTimeseriesSets() {
      timeseriesSets[0] = new Set();
      timeseriesSets[0].setName("set_ii");
      for (int i = 1; i <= config.getSimulationlength(); i++) {
         final SetElement se = new SetElement();
         se.setName("ii" + i);
         timeseriesSets[0].getElements().add(se);
      }

      timeseriesSets[1] = new Set();
      timeseriesSets[1].setName("set_ii_0");
      for (int i = 0; i <= config.getSimulationlength(); i++) {
         final SetElement se = new SetElement();
         se.setName("ii" + i);
         timeseriesSets[1].getElements().add(se);
      }
   }

   /**
    * Leerer Konstruktor, erstellt leere {@link Globalconfig}.
    */
   public BackendParametersYearData() {
      config = new Globalconfig();
   }

   /**
    * Initialisiert Zeitreihendaten mit spezifischen Jahresdaten.
    *
    * @param gpjson Die Jahresdaten
    */
   private void initializeTimeseries(final YearData gpjson) {
      for (final Map.Entry<String, Object> entry : gpjson.getTimeseries().entrySet()) {
         final Object value = entry.getValue();
         if (value instanceof String) {
            timeseries.put(entry.getKey(), Timeseries.build(Integer.parseInt((String) value)));
         } else if (value instanceof List<?>) {
            timeseries.put(entry.getKey(), Timeseries.build(new ArrayList<Number>()));
            ((List<?>) value).forEach(number -> {
               if (number instanceof Number) {
                  timeseries.get(entry.getKey()).add((Number) number);
               } else {
                  throw new RuntimeException("Zeitreihenelement in Jahr " + gpjson.getConfig().getYear()
                        + ", Parameter: " + entry.getKey() + " ist nicht vom Typ Number!");
               }
            });
         }
      }
   }

   /**
    * Initialisiert Sets mit Jahresdaten für die BackendParameterYearData.
    *
    * @param gpjson Die Jahresdaten
    */
   private void initializeSets(final YearData gpjson) {
      // Setname -> Element -> Parametername -> Wert
      for (final Map.Entry<String, LinkedHashMap<String, LinkedHashMap<String, Object>>> entry : gpjson.getSets()
            .entrySet()) {
         final Set set = new Set();
         set.setName(entry.getKey());
         LOG.trace("Value: " + entry.getKey());
         for (final Entry<String, LinkedHashMap<String, Object>> setElementMap : entry.getValue().entrySet()) {
            final SetElement setElement = new SetElement();
            setElement.setName(setElementMap.getKey());
            LOG.trace(entry.getKey() + " " + setElementMap.getKey());
            final Map<String, Object> attributeData = setElementMap.getValue();
            if (attributeData != null) {
               for (final Map.Entry<String, Object> attributeElement : attributeData.entrySet()) {
                  final String parameterName = attributeElement.getKey();
                  final List<String> dependencies = getDependencies(parameterName);
                  final Object value = attributeElement.getValue();
                  if (dependencies != null && dependencies.contains("set_ii")) {
                     if (value instanceof String) {
                        setElement.getTimeseries().put(parameterName,
                              Timeseries.build(Integer.parseInt((String) value)));
                     } else if (value instanceof List<?>) {
                        setElement.getTimeseries().put(parameterName,
                              Timeseries.build(new ArrayList<Number>()));
                        ((List<?>) value).forEach(number -> {
                           if (number instanceof Number) {
                              setElement.getTimeseries().get(parameterName).add((Number) number);
                           } else {
                              throw new RuntimeException("Zeitreihenelement in Jahr "
                                    + gpjson.getConfig().getYear() + ", Parameter: " + entry.getKey()
                                    + " ist nicht vom Typ Number!");
                           }
                        });
                     } else {
                        LOG.error("Parameter " + parameterName
                              + " hat Abhängigkeit von set_ii, Typ des Parameters ist aber: "
                              + value.getClass());
                     }
                  } else {
                     if (value instanceof Number) {
                        setElement.getAttributes().put(parameterName, ((Number) value).doubleValue());
                     } else if (value instanceof String) {
                        setElement.getAttributes().put(parameterName, value);
                     } else {
                        LOG.error("Parameter " + parameterName
                              + " hat keine Abhängigkeit von set_ii, Typ des Parameters ist aber: "
                              + value.getClass());
                     }
                  }

               }
            }
            set.getElements().add(setElement);
         }
         sets.put(set.getName(), set);
      }
   }

   private List<String> getDependencies(final String parameterName) {
      final List<String> dependencies = ParameterInputDependenciesUtil.getInstance()
            .getAllInputDependencies(config.getModeldefinition()).get(parameterName) != null
                  ? ParameterInputDependenciesUtil.getInstance().getAllInputDependencies(config.getModeldefinition())
                        .get(parameterName)
                  : ParameterOutputDependenciesUtil.getInstance().getAllOutputDependencies(config.getModeldefinition())
                        .get(parameterName);
      return dependencies;
   }

   /**
    * Initialisiert Zeitreihentabellen mit den übergebenen Jahresdaten.
    *
    * @param gpjson Die zu setzenden Jahresdaten
    */
   private void initializeTables(final YearData gpjson) {
      for (final Entry<String, Map<String, Map<String, Object>>> entry : gpjson.getTables().entrySet()) {
         final String parametername = entry.getKey();
         if (entry.getValue() == null) {
            throw new NullPointerException("Leere Tabelle! " + entry.getKey());
         }
         for (final Entry<String, Map<String, Object>> values : entry.getValue().entrySet()) {
            final String firstDependent = values.getKey();
            for (final Entry<String, Object> secondDependentEntry : values.getValue().entrySet()) {
               final String secondDependent = secondDependentEntry.getKey();
               final Object valueObject = secondDependentEntry.getValue();
               if (valueObject instanceof Number) {
                  final Map<String, Map<String, Object>> scalarFirstDependents = DataUtils.getOrNewMap(tableValues, parametername);
                  final Map<String, Object> scalarSecondDependents = DataUtils.getOrNewMap(scalarFirstDependents, firstDependent);
                  scalarSecondDependents.put(secondDependent, valueObject);
               } else if (valueObject instanceof String) {
                  List<String> dependencyList = ParameterBaseDependenciesUtil.getInstance().getDependencies(parametername, config.getModeldefinition());
                  if (dependencyList != null && !dependencyList.contains("set_ii")) {
                     final Map<String, Map<String, Object>> scalarFirstDependents = DataUtils.getOrNewMap(tableValues, parametername);
                     final Map<String, Object> scalarSecondDependents = DataUtils.getOrNewMap(scalarFirstDependents, firstDependent);
                     scalarSecondDependents.put(secondDependent, valueObject);
                  } else {
                     final Map<String, Map<String, Timeseries>> timeseriesFirstDependents = DataUtils.getOrNewMap(tableTimeseries, parametername);
                     final Map<String, Timeseries> timeseriesSecondDependents = DataUtils.getOrNewMap(timeseriesFirstDependents, firstDependent);
                     timeseriesSecondDependents.put(secondDependent, Timeseries.build(Integer.parseInt((String) valueObject)));
                  }
               } else if (valueObject instanceof List<?>) {
                  final Map<String, Map<String, Timeseries>> timeseriesFirstDependents = DataUtils.getOrNewMap(tableTimeseries, parametername);
                  final Map<String, Timeseries> timeseriesSecondDependents = DataUtils
                        .getOrNewMap(timeseriesFirstDependents, firstDependent);
                  timeseriesSecondDependents.put(secondDependent, Timeseries.build(new ArrayList<Number>()));
                  ((List<?>) valueObject).forEach(number -> {
                     if (number instanceof Number) {
                        timeseriesSecondDependents.get(secondDependent).add((Number) number);
                     } else {
                        throw new RuntimeException("Zeitreihenelement in Jahr " + gpjson.getConfig().getYear()
                              + ", Parameter: " + entry.getKey() + " ist nicht vom Typ Number!");
                     }
                  });
               } else {
                  throw new RuntimeException(
                        "Unerwarteter Datentyp (Number, String oder List erwartet): " + valueObject.getClass());
               }
            }
         }
      }
   }

   /**
    * Liefert eine Abbildung aller Zeitreihen, wahlweise der Refererenz-Zeitreihen oder der Array-Zeitreihen, auf die zugehörigen Parameter.
    * 
    * @param input Ob Eingabe- oder Ausgabezeitreihen ausgegeben werden sollen
    * @param isReference Ob Referenz(true) oder Array(false)-Zeitreihen zurückgegeben werden sollen
    * @return Die Referenzen als Schlüsselset einer Map auf die zugehörigen Parameter
    */
   public final Map<Integer, String> collectTimeseriesReferences(final boolean input) {
      final Map<String, List<String>> dependencies = input
            ? ParameterInputDependenciesUtil.getInstance().getAllInputDependencies(config.getModeldefinition())
            : ParameterOutputDependenciesUtil.getInstance().getAllOutputDependencies(config.getModeldefinition());
      final Map<Integer, String> references = new HashMap<>();
      timeseries.entrySet().stream().filter(t -> t.getValue().hasReference()).forEach(timeseries -> {
         references.put(timeseries.getValue().getSeriesname(), timeseries.getKey());
      });
      final Collection<Set> values = sets.values();
      values.forEach(set -> {
         set.getElements().forEach(setElement -> {
            setElement.getTimeseries().entrySet().stream().filter(t -> t.getValue().hasReference())
                  .forEach(reference -> {
                     final String parameterName = reference.getKey();
                     final List<String> dependencyList = dependencies.get(parameterName);
                     if (dependencyList != null && dependencyList.contains("set_ii")) {
                        references.put(reference.getValue().getSeriesname(), reference.getKey());
                     }
                  });
         });
      });
      tableTimeseries.forEach((paramaterName, firstDependent) -> {
         firstDependent.forEach((secondDependentName, secondDependent) -> {
            secondDependent.entrySet().stream().filter(reference -> reference.getValue().hasReference())
                  .forEach(currentTimeseries -> {
                     final List<String> dependencyList = dependencies.get(paramaterName);
                     if (dependencyList != null && dependencyList.contains("set_ii")) {
                        references.put(currentTimeseries.getValue().getSeriesname(), paramaterName);
                     }
                  });
         });
      });
      return references;
   }

   public final Map<String, List<Number>> collectTimeseries(final boolean input) {
      final Map<String, List<String>> dependencies = input
            ? ParameterInputDependenciesUtil.getInstance().getAllInputDependencies(config.getModeldefinition())
            : ParameterOutputDependenciesUtil.getInstance().getAllOutputDependencies(config.getModeldefinition());
      final Map<String, List<Number>> references = new HashMap<>();
      timeseries.entrySet().stream().filter(t -> !t.getValue().hasReference()).forEach(currentTimeseries -> {
         references.put(currentTimeseries.getKey(), currentTimeseries.getValue().getData());
      });
      final Collection<Set> values = sets.values();
      values.forEach(set -> {
         set.getElements().forEach(setElement -> {
            setElement.getTimeseries().entrySet().stream().filter(t -> !t.getValue().hasReference())
                  .forEach(currentTimeseries -> {
                     final String parameterName = currentTimeseries.getKey();
                     final List<String> dependencyList = dependencies.get(parameterName);
                     if (dependencyList != null && dependencyList.contains("set_ii")) {
                        references.put(parameterName, currentTimeseries.getValue().getData());
                     }
                  });
         });
      });
      tableTimeseries.forEach((paramaterName, firstDependent) -> {
         firstDependent.forEach((secondDependentName, secondDependent) -> {
            secondDependent.entrySet().stream().filter(reference -> !reference.getValue().hasReference())
                  .forEach(currentTimeseries -> {
                     final List<String> dependencyList = dependencies.get(paramaterName);
                     if (dependencyList != null && dependencyList.contains("set_ii")) {
                        references.put(paramaterName, currentTimeseries.getValue().getData());
                     }
                  });
         });
      });
      return references;
   }

   /**
    * Sammelt alle Skalare Referenzen; Skalare Referenzen kommen nur bei Eingabe-Skalaren vor.
    * 
    * @return
    */
   public final java.util.Map<Integer, String> collectScalarReferences() {
      final Map<String, List<String>> dependencies = ParameterInputDependenciesUtil.getInstance()
            .getAllInputDependencies(config.getModeldefinition());
      final Map<Integer, String> references = new HashMap<>();
      scalars.entrySet().stream().filter(t -> t.getValue() instanceof String).forEach(reference -> {
         references.put(Integer.parseInt((String) reference.getValue()), reference.getKey());
      });
      collectSetReferences(dependencies, references);
      collectTableValueReferences(dependencies, references);
      collectTableTimeseriesReferences(dependencies, references);
      return references;
   }

   private void collectTableTimeseriesReferences(final Map<String, List<String>> dependencies,
         final Map<Integer, String> references) {
      LOG.trace("Sammle Tabellenzeitreihenreferenzen..");
      tableTimeseries.forEach((paramaterName, firstDependent) -> {
         firstDependent.forEach((secondDependentName, secondDependent) -> {
            secondDependent.entrySet().stream().filter(reference -> reference.getValue().hasReference())
                  .forEach(reference -> {
                     final List<String> dependencyList = dependencies.get(paramaterName);
                     if (dependencyList != null) {
                        LOG.trace("Merke Tabellen-Zeitreihe-Referenz: {} {}", reference.getValue(), paramaterName);
                        references.put(reference.getValue().getSeriesname(), paramaterName);
                     } else {
                        LOG.trace("Referenz nicht gespeichert: {} {}", paramaterName, dependencyList);
                     }
                  });
         });
      });
   }

   private void collectTableValueReferences(final Map<String, List<String>> dependencies,
         final Map<Integer, String> references) {
      LOG.trace("Sammle Tabellenwertreferenzen..");
      tableValues.forEach((paramaterName, firstDependent) -> {
         firstDependent.forEach((secondDependentName, secondDependent) -> {
            secondDependent.entrySet().stream().filter(reference -> reference.getValue() instanceof String)
                  .forEach(reference -> {
                     final List<String> dependencyList = dependencies.get(paramaterName);
                     if (dependencyList != null) {
                        LOG.trace("Merke Tabellen-Wert-Referenz: {} {}", reference.getValue(), paramaterName);
                        references.put(Integer.parseInt((String) reference.getValue()), paramaterName);
                     } else {
                        LOG.trace("Referenz nicht gespeichert: {} {}", paramaterName, dependencyList);
                     }
                  });
         });
      });
   }

   private void collectSetReferences(final Map<String, List<String>> dependencies,
         final Map<Integer, String> references) {
      LOG.trace("Sammle Set-Referenzen");
      sets.values().forEach(set -> {
         set.getElements().forEach(setElement -> {
            setElement.getAttributes().entrySet().stream().filter(t -> t.getValue() instanceof String)
                  .forEach(reference -> {
                     final String paramaterName = reference.getKey();
                     final List<String> dependencyList = dependencies.get(paramaterName);
                     if (dependencyList != null && !dependencyList.contains("set_ii")) {
                        LOG.trace("Merke Set-Wert-Referenz: {} {}", reference.getValue(), paramaterName);
                        references.put(Integer.parseInt((String) reference.getValue()), paramaterName);
                     } else {
                        LOG.trace("Referenz nicht gespeichert: {} {}", paramaterName, dependencyList);
                     }
                  });
         });
      });
   }

   /**
    * Liefert das erste gefundene Set mit dem angegebenen Namen. Gibt null zurück, wenn kein set gefunden wurde.
    *
    * @param name Der Name der Sets
    * @return Das set als Set
    */
   public final Set getSetWithName(final String name) {
      LOG.trace("Suche nach: {}", name);
      // sets.stream().forEach(s -> LOG.trace(s.getName()));
      try {
         return sets.get(name);
      } catch (final NoSuchElementException nse) {
         return null;
      }
   }

   /**
    * Liefert das timerowSet.
    *
    * @return Das timerowSet als Set
    */
   public final Set[] getTimeseriesSets() {
      return timeseriesSets;
   }

   /**
    * Liefert sets.
    *
    * @return Die sets als List<Set>
    */
   public final Map<String, Set> getSets() {
      return sets;
   }

   /**
    * Setzt sets.
    *
    * @param sets Die zu setzenden sets
    */
   public final void setSets(final Map<String, Set> sets) {
      this.sets = sets;
   }

   /**
    * Liefert scalars.
    *
    * @return Die scalars als Map<String, Number>
    */
   public final Map<String, Object> getScalars() {
      return scalars;
   }

   /**
    * Setzt scalars.
    *
    * @param scalars Die zu setzenden scalars
    */
   public final void setScalars(final Map<String, Object> scalars) {
      this.scalars = scalars;
   }

   public final Map<String, Timeseries> getTimeseries() {
      return timeseries;
   }

   public final void setTimeseries(final Map<String, Timeseries> timeseries) {
      this.timeseries = timeseries;
   }

   public final Map<String, Map<String, Map<String, Timeseries>>> getTableTimeseries() {
      return tableTimeseries;
   }

   @JsonIgnore
   public final void executeOnSetTimeseries(final SetConsumer consumer) {
      sets.forEach((element, data) -> {
         data.getElements().forEach(setElement -> {
            setElement.getTimeseries().forEach((parametername, timeseries) -> {
               consumer.consumeSetData(parametername, element, timeseries);
            });
         });
      });
   }

   @JsonIgnore
   public final void executeOnTableTimeseries(final TableConsumer consumer) throws TimeseriesTooShortException {
      for (final Entry<String, Map<String, Map<String, Timeseries>>> parameterNameEntry : tableTimeseries
            .entrySet()) {
         for (final Entry<String, Map<String, Timeseries>> firstDependentEntry : parameterNameEntry.getValue()
               .entrySet()) {
            for (final Entry<String, Timeseries> secondDependentEntry : firstDependentEntry.getValue().entrySet()) {
               consumer.consumeTableData(parameterNameEntry.getKey(), firstDependentEntry.getKey(),
                     secondDependentEntry.getKey(), secondDependentEntry.getValue());
            }
         }
      }
   }

   public final void setTableTimeseries(final Map<String, Map<String, Map<String, Timeseries>>> tableTimeseries) {
      this.tableTimeseries = tableTimeseries;
   }

   /**
    * Liefert tableValues.
    *
    * @return Die tableValues als Map<String, List<Tableentry>>
    */
   public final Map<String, Map<String, Map<String, Object>>> getTableValues() {
      return tableValues;
   }

   /**
    * Setzt tablesValues.
    *
    * @param tableValues Die zu setzenden tableValues
    */
   public final void setTableValues(final Map<String, Map<String, Map<String, Object>>> tableValues) {
      this.tableValues = tableValues;
   }

   /**
    * Erzeugt JSON-Parameter-Jahresdaten im Frontend-Format aus den Backend-Jahresdaten.
    *
    * @return Erzeugt JSON-Parameter der Jahresdaten.
    */
   public final YearData createJSONParameters() {

      final YearData year = new YearData();

      year.setConfig(config);
      getScalars().forEach((parameter, value) -> {
         year.getScalars().put(parameter, DataUtils.convertToReadable(value));
      });
      sets.values().stream().forEach(set -> year.getSets().put(set.getName(), new LinkedHashMap<>()));

      for (final Set set : sets.values()) {
         final LinkedHashMap<String, LinkedHashMap<String, Object>> setMap = year.getSets().get(set.getName());
         for (final SetElement setElement : set.getElements()) {
            setMap.put(setElement.getName(), new LinkedHashMap<>());
            final Map<String, Object> setMapEntryMap = setMap.get(setElement.getName());
            for (final Map.Entry<String, Object> attribute : setElement.getAttributes().entrySet()) {
               setMapEntryMap.put(attribute.getKey(), DataUtils.convertToReadable(attribute.getValue()));
            }
            for (final Entry<String, Timeseries> attribute : setElement.getTimeseries().entrySet()) {
               if (attribute.getValue().hasReference()) {
                  setMapEntryMap.put(attribute.getKey(), "" + attribute.getValue().getSeriesname());
               } else {
                  setMapEntryMap.put(attribute.getKey(), attribute.getValue().fetchTimeseriesReadable());
               }
            }
         }
      }

      timeseries.forEach((parameter, timeseries) -> {
         if (timeseries.hasReference()) {
            year.getTimeseries().put(parameter, "" + timeseries.getSeriesname());
         } else {
            year.getTimeseries().put(parameter, timeseries.fetchTimeseriesReadable());
         }
      });

      tableValues.forEach((parameter, firstDependents) -> {
         final Map<String, Map<String, Object>> yearFirstDependents = DataUtils.getOrNewMap(year.getTables(),
               parameter);
         firstDependents.forEach((firstDependent, secondDependents) -> {
            final Map<String, Object> yearSecondDependents = DataUtils.getOrNewMap(yearFirstDependents,
                  firstDependent);
            secondDependents.forEach((secondDependent, value) -> {
               yearSecondDependents.put(secondDependent, DataUtils.convertToReadable(value));
            });
         });
      });

      tableTimeseries.forEach((parameter, firstDependents) -> {
         final Map<String, Map<String, Object>> yearFirstDependents = DataUtils.getOrNewMap(year.getTables(),
               parameter);
         firstDependents.forEach((firstDependent, secondDependents) -> {
            final Map<String, Object> yearSecondDependents = DataUtils.getOrNewMap(yearFirstDependents,
                  firstDependent);
            secondDependents.forEach((secondDependent, timeseries) -> {
               if (timeseries.hasReference()) {
                  yearSecondDependents.put(secondDependent, "" + timeseries.getSeriesname());
               } else {
                  yearSecondDependents.put(secondDependent, timeseries.fetchTimeseriesReadable());
               }
            });
         });
      });

      year.setPostprocessing(postprocessing);

      return year;
   }

   public final Globalconfig getConfig() {
      return config;
   }

   public final void setConfig(final Globalconfig config) {
      this.config = config;
   }

   public final PostProcessing getPostprocessing() {
      return postprocessing;
   }

   public final void setPostprocessing(final PostProcessing postprocessing) {
      this.postprocessing = postprocessing;
   }

   /**
    * Map.Entry kann mit .setValue(value) in der hinterlegten Map die {@link Timeseries} durch eine Andere austauschen. Dies ist nicht Threadsicher! TODO Methodendokumentation
    *
    * @param reference Beliebige Zeitreihenreferenz dieses Jahres
    * @return Map.Entry mit der {@link Timeseries}, die die Referenz enthält, oder null, wenn die Referenz nicht existiert.
    */
   public final Entry<String, Timeseries> lookupMapWithReference(final Integer reference) {
      for (final Map.Entry<String, Timeseries> entry : timeseries.entrySet()) {
         if (entry.getValue().getSeriesname().equals(reference)) {
            return entry;
         }
      }
      for (final Set set : sets.values()) {
         for (final SetElement setElement : set.getElements()) {
            for (final Map.Entry<String, Timeseries> entry : setElement.getTimeseries().entrySet()) {
               if (entry.getValue().getSeriesname().equals(reference)) {
                  return entry;
               }
            }
         }
      }
      for (final Map<String, Map<String, Timeseries>> firstDependents : tableTimeseries.values()) {
         for (final Map<String, Timeseries> secondDependents : firstDependents.values()) {
            for (final Map.Entry<String, Timeseries> entry : secondDependents.entrySet()) {
               if (entry.getValue().getSeriesname().equals(reference)) {
                  return entry;
               }
            }
         }
      }
      LOG.error("Zeitreihenreferenz {} existiert nicht!", reference);
      return null;
   }
}
