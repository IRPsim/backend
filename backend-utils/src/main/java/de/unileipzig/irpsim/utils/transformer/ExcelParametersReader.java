package de.unileipzig.irpsim.utils.transformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.monitorjbl.xlsx.StreamingReader;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Globalconfig;
import de.unileipzig.irpsim.core.simulation.data.Set;
import de.unileipzig.irpsim.core.simulation.data.SetElement;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.utils.DataUtils;
import de.unileipzig.irpsim.core.utils.ParameterBaseDependenciesUtil;
import de.unileipzig.irpsim.core.utils.ParameterInputDependenciesUtil;
import de.unileipzig.irpsim.utils.data.excel.ExcelDefinitionsData;
import de.unileipzig.irpsim.utils.data.excel.ExcelPosition;

/**
 * Liest einen bestehenden GAMS-Parametersatz ein und wandelt ihn in das allgemeine JSON-Format um.
 *
 * @author reichelt
 */
public final class ExcelParametersReader {

   private static final Logger LOG = LogManager.getLogger(ExcelParametersReader.class);

   private static final ObjectMapper om = new ObjectMapper();

   static {
      om.enable(SerializationFeature.INDENT_OUTPUT);
   }

   private final List<WrongSetElement> wrongSetElements = new LinkedList<>();
   private final List<String> unusedParameters = new LinkedList<>();

   /**
    * Startet das Einlesen und umwandeln der XLSX-Datei.
    *
    * @param args Programmstartargumente
    * @throws Exception
    */
   public static void main(final String[] args) throws Exception {
      final File excelFile = new File(args[0]);
      final File inputSpecificationFile = new File(args[1]);
      final File resultFile = new File(args[2]);
      final String description = args[3];
      LOG.info("Input-Spec: " + inputSpecificationFile.getAbsolutePath());

      try {
         final ExcelParametersReader readXLSXModel = new ExcelParametersReader(excelFile, inputSpecificationFile, resultFile,
               description, 1);
         readXLSXModel.readXLSXFileToJSON();
      } catch (final InvalidFormatException e) {
         e.printStackTrace();
      }
   }

   private final File excelFile;
   private final File inputSpecificationFile;
   private final File resultFile;
   private final String description;
   private final BackendParametersMultiModel gamsparameters = new BackendParametersMultiModel(1, 1);
   private BackendParametersYearData yeardata;

   public ExcelParametersReader(final File excelFile, final File inputSpecificationFile, final File resultFile,
         final String description, int modeldefinition) {
      super();
      this.excelFile = excelFile;
      this.inputSpecificationFile = inputSpecificationFile;
      this.resultFile = resultFile;
      if (!resultFile.getParentFile().exists()) {
         resultFile.getParentFile().mkdirs();
      }
      this.description = description;

      yeardata = new BackendParametersYearData();
      yeardata.getConfig().setModeldefinition(modeldefinition);
      yeardata.getConfig().setYear(2015);
      yeardata.getConfig().setTimestep(Constants.QUARTER);
      gamsparameters.getModels()[0].getYeardata()[0] = yeardata;
   }

   /**
    * TODO.
    *
    * @param excelFile Die zu lesende Excel-Datei
    * @param inputSpecificationFile TODO
    * @param resultFile TODO
    * @param description Beschreibung des Szenarios
    * @return Die augelesene Datei
    * @throws Exception
    */
   public BackendParametersMultiModel readXLSXFileToJSON() throws Exception {
      LOG.debug("Input-Spec: {}", inputSpecificationFile.getAbsolutePath());

      // final Workbook workbook = WorkbookFactory.create(excelFile);

      readSetContent(inputSpecificationFile, excelFile, yeardata);

      LOG.info("Alle Sets eingelesen, Sets: " + yeardata.getSets().size());

      // checkEqualSetNames();

      readMainContent(excelFile);

      Globalconfig config = yeardata.getConfig();
      final int length = config.getSimulationlength();
      checkLength(length);
      if (config.getOptimizationlength() > length) {
         throw new RuntimeException("Optimizationlength (set_t) needs to be smaller than or equal to the optimizationlength (set_ii)");
      }

      gamsparameters.getDescription().setBusinessModelDescription(description);

      replaceZeroTimeseries();

      om.writeValue(resultFile, gamsparameters.createJSONParameters());

      if (wrongSetElements.size() > 0) {
         throw new RuntimeException("Falsche Elemente: " + wrongSetElements.size() + " "
               + wrongSetElements.subList(0, Math.min(wrongSetElements.size(), 100)).toString());
      }

      if (unusedParameters.size() > 0) {
         LOG.error("Die folgenden Parameter werden nicht genutzt: ");
         for (String parameter : unusedParameters) {
            LOG.error(parameter);
         }
      }

      return gamsparameters;
   }

   private void checkEqualSetNames() {
      String equalElements = "";
      for (final Map.Entry<String, Set> set : yeardata.getSets().entrySet()) {
         for (final Map.Entry<String, Set> otherSet : yeardata.getSets().entrySet()) {
            if (!set.getKey().equals(otherSet.getKey())) {
               final Set set1 = set.getValue();
               final Set set2 = otherSet.getValue();
               int modeldefinition = yeardata.getConfig().getModeldefinition();
               final List<String> setNames1 = ParameterBaseDependenciesUtil.getInstance().getSubsets(set1.getName(),
                     modeldefinition);
               final List<String> setNames2 = ParameterBaseDependenciesUtil.getInstance().getSubsets(set2.getName(),
                     modeldefinition);
               if (!setNames1.contains(set2.getName()) && !setNames2.contains(set1.getName())) {
                  for (final SetElement element1 : set1.getElements()) {
                     for (final SetElement element2 : set2.getElements()) {
                        if (element2.getName().equals(element1.getName())) {
                           equalElements += set1.getName() + " - " + element1.getName() + " ist gleich "
                                 + set2.getName() + " - " + element2.getName() + "\n";
                        }
                     }
                  }
               }
            }
         }
      }
      if (equalElements.length() > 0) {
         throw new RuntimeException(equalElements);
      }
   }

   private void checkLength(final int length) throws TimeseriesTooShortException {
      yeardata.getTimeseries().forEach((parameter, timeseries) -> {
         // LOG.debug("Länge: {}", timeseries.size());
         if (timeseries.size() != length) {
            throw new RuntimeException("Zeitreihe " + parameter + " sollte " + length + " lang sein, ist aber "
                  + timeseries.size() + " lang.");
         }
      });
      yeardata.executeOnSetTimeseries((parameter, first, timeseries) -> {
         // LOG.info("Länge: {}", timeseries.size());
         if (timeseries.size() != length) {
            throw new RuntimeException("Zeitreihe " + parameter + " sollte " + length + " lang sein, ist aber "
                  + timeseries.size() + " lang.");
         }
      });
      yeardata.executeOnTableTimeseries((parameter, first, second, timeseries) -> {
         // LOG.debug("Länge: {}", timeseries.size());
         if (timeseries.size() != length) {
            throw new RuntimeException("Zeitreihe " + parameter + " sollte " + length + " lang sein, ist aber "
                  + timeseries.size() + " lang.");
         }
      });
   }

   private void replaceZeroTimeseries() {
      for (final Map.Entry<String, Timeseries> entry : yeardata.getTimeseries().entrySet()) {
         if (!entry.getValue().isZeroTimeseries()) {
            entry.setValue(Timeseries.ZEROTIMESERIES_REFERENCE);
         }
      }

      for (final Set set : yeardata.getSets().values()) {
         for (final SetElement element : set.getElements()) {
            for (final Map.Entry<String, Timeseries> entry : element.getTimeseries().entrySet()) {
               if (!entry.getValue().isZeroTimeseries()) {
                  entry.setValue(Timeseries.ZEROTIMESERIES_REFERENCE);
               }
            }
         }
      }

      for (final Map.Entry<String, Map<String, Map<String, Timeseries>>> entry1 : yeardata.getTableTimeseries().entrySet()) {
         for (final Map.Entry<String, Map<String, Timeseries>> entry2 : entry1.getValue().entrySet()) {
            for (final Map.Entry<String, Timeseries> entry : entry2.getValue().entrySet()) {
               if (!entry.getValue().isZeroTimeseries()) {
                  entry.setValue(Timeseries.ZEROTIMESERIES_REFERENCE);
               }
            }
         }
      }
   }

   /**
    * TODO.
    *
    * @param file Die zu lesende Datei
    * @param workbook Das Excel-Workbook
    * @param gamsparameters Die GAMS-Parameter
    * @throws FileNotFoundException Tritt auf falls die zu lesende Datei nicht existiert
    * @throws IOException Tritt auf falls Fehler beim Lesen oder Schreiben auftreten
    */
   public void readMainContent(final File excelFile) throws FileNotFoundException, IOException {
      final Map<String, List<ExcelDefinitionsData>> parameterRanges = getParameterMetadata();

      LOG.info("Alle Zeilen vorbereitet");
      for (final Map.Entry<String, List<ExcelDefinitionsData>> definitionsList : parameterRanges.entrySet()) {
         try (final InputStream is = new FileInputStream(excelFile);
               final Workbook workbook = StreamingReader.builder().rowCacheSize(100) // number of rows to keep in
                     // memory (defaults to 10)
                     .bufferSize(4024) // buffer size to use when reading InputStream to file (defaults to 1024)
                     .open(is);) {
            final Sheet currentSheet = workbook.getSheet(definitionsList.getKey());
            try {
               handleLine(definitionsList.getValue(), currentSheet);
            } catch (final Exception e) {
               LOG.error("Fehler beim Lesen von " + definitionsList.getKey() + " in " + inputSpecificationFile);
               throw e;
            }
         }
      }
   }

   private Map<String, List<ExcelDefinitionsData>> getParameterMetadata() throws IOException, FileNotFoundException {
      final Map<String, List<ExcelDefinitionsData>> parameterRanges = new HashMap<>();
      try (BufferedReader br = new BufferedReader(new FileReader(inputSpecificationFile))) {
         String line;
         while ((line = br.readLine()) != null) {
            if (line.length() > 1 && !line.startsWith("*") && !line.startsWith("set")) {
               final ExcelDefinitionsData excelData = new ExcelDefinitionsData(line);
               if (excelData.getDeclaredDimension() != excelData.getRangePositions()[1].getColumnNumber()
                     - excelData.getRangePositions()[0].getColumnNumber()) {
                  LOG.error("Fehler: faktische Dimension von {} ist {}", excelData.getParameterName(),
                        (excelData.getRangePositions()[1].getColumnNumber()
                              - excelData.getRangePositions()[0].getColumnNumber()));
               }
               List<ExcelDefinitionsData> data = parameterRanges.get(excelData.getSheetName());
               if (data == null) {
                  data = new LinkedList<>();
                  parameterRanges.put(excelData.getSheetName(), data);
               }
               if (!excelData.getParameterName().equals("par_Setii")
                     && !excelData.getParameterName().equals("sca_a")) {
                  data.add(excelData);
                  if (excelData.getParameterName().equals("sca_delta_ii")) {
                     LOG.info("Delta ii" + excelData.getParameterName() + " " + excelData.getSheetName());
                  }
               }
            }
         }
      }
      return parameterRanges;
   }

   /**
    * TODO.
    *
    * @param gamsparameters Die Backend GAMS-Parameter
    * @param line Die zu verarbeitende Zeile
    * @param excelData Die Excel-Daten
    * @param rangePositions TODO
    * @param columnNumber Die Spaletennummer
    * @param currentSheet TODO
    */
   public void handleLine(final List<ExcelDefinitionsData> excelDataList, final Sheet currentSheet) {

      int rowIndex = 0;
      for (final Row row : currentSheet) {
         rowIndex++;
         dataLoop: for (final ExcelDefinitionsData excelData : excelDataList) {
            final ExcelPosition[] rangePositions = excelData.getRangePositions();
            if (rowIndex < rangePositions[0].getRow()) {
               continue dataLoop;
            }
            if (rowIndex > rangePositions[1].getRow()) {
               continue dataLoop;
            }
            final int dependentSize = rangePositions[1].getColumnNumber() - rangePositions[0].getColumnNumber();
            if (dependentSize == 2 || dependentSize == 3) {
               readMultiDependent(currentSheet, yeardata, rowIndex, row, excelData, rangePositions, dependentSize);
            } else if (dependentSize == 1) {
               readSingleDependent(yeardata, rowIndex, row, excelData, rangePositions);
            } else if (dependentSize == 0) {
               final Cell c = row.getCell(rangePositions[0].getColumnNumber());
               LOG.debug("Scalar-Wert: " + c + " Zeile: " + rangePositions[1].getRow() + " "
                     + excelData.getParameterName());
               final Double val = c.getNumericCellValue();
               yeardata.getScalars().put(excelData.getParameterName(), val);
            } else {
               throw new RuntimeException("Wrong dependent size: " + dependentSize + " "
                     + excelData.getParameterName()
                     + " - 0 (scalar), 1 (timeseries / set-dependent scalar), 2 (set-dependent-timeseries, table-dependent scalar) or 3 (table-timeseries) are allowed");
            }
         }
      }
   }

   private void readMultiDependent(final Sheet currentSheet, final BackendParametersYearData yearData, int rowIndex,
         final Row row, final ExcelDefinitionsData excelData, final ExcelPosition[] rangePositions,
         final int depententSize) {
      try {
         final List<String> inputParameterDependencies = ParameterInputDependenciesUtil.getInstance()
               .getInputParameterDependencies(excelData.getParameterName(),
                     yearData.getConfig().getModeldefinition());
         final boolean isTimeseries = inputParameterDependencies.contains("set_ii");

         if (row == null) {
            throw new RuntimeException("Achtung: Zeile " + (rowIndex + 1) + " in " + currentSheet.getSheetName()
                  + " ist undefiniert, sollte aber Werte enthalten.");
         }

         final Cell valueCell = row.getCell(rangePositions[1].getColumnNumber());
         if (valueCell == null) {
            LOG.debug("Position:" + rowIndex + " " + rangePositions[0].getColumnNumber() + " "
                  + rangePositions[1].getColumnNumber());
            LOG.error("Zelle nicht gefunden: " + excelData.getParameterName());
         }

         LOG.trace("Set-Cell-Größe: {} ValueCellType: {}", depententSize, valueCell.getCellType());
         final Double currentValue = valueCell.getNumericCellValue();
         LOG.trace("Wert: {} Sheet: {}", currentValue, currentSheet.getSheetName());

         final List<String> setValues = readSetValues(row, rangePositions, depententSize);
         checkSetElement(setValues, excelData.getParameterName());

         if (isTimeseries) {
            handleTimeseries(yearData, excelData, depententSize, setValues, currentValue,
                  excelData.getParameterName());
         } else {
            final Map<String, Map<String, Object>> firstDependents = DataUtils
                  .getOrNewMap(yearData.getTableValues(), excelData.getParameterName());
            final Map<String, Object> secondDependents = DataUtils.getOrNewMap(firstDependents, setValues.get(0));
            secondDependents.put(setValues.get(1), currentValue);
         }
      } catch (final JSONException je) {
         unusedParameters.add(excelData.getParameterName());
         LOG.error("Parameter wird nicht benutzt: ", excelData.getParameterName());
         // je.printStackTrace();
         // throw new RuntimeException("Parameter nicht definiert: " +
         // excelData.getParameterName(), je);
      }
   }

   private List<String> readSetValues(final Row row, final ExcelPosition[] rangePositions, final int depententSize) {
      final List<String> setValues = new LinkedList<>();
      for (int i = 0; i < depententSize; i++) {
         final int index = rangePositions[0].getColumnNumber() + i;
         LOG.trace("Lade: " + index);
         final Cell setCell = row.getCell(index);
         LOG.trace(setCell);
         final String setElementName = setCell.getStringCellValue();
         if (!setElementName.contains("ii")) {
            setValues.add(setElementName);
         }
      }
      return setValues;
   }

   private void readSingleDependent(final BackendParametersYearData yearData, int rowIndex, final Row row,
         final ExcelDefinitionsData excelData, final ExcelPosition[] rangePositions) {
      final Cell setCell = row.getCell(rangePositions[0].getColumnNumber());
      final Cell valueCell = row.getCell(rangePositions[1].getColumnNumber());
      LOG.trace(setCell + " " + valueCell);
      LOG.trace("Pos: " + rowIndex + " Set-Cell-Typ: " + setCell.getCellType() + " ValueCellTyp: "
            + valueCell.getCellType());
      LOG.trace(setCell + " " + valueCell);
      final String setVal = setCell.getStringCellValue();
      final Double valueVal = valueCell.getNumericCellValue();
      LOG.trace("Suche nach: " + setVal + " " + setVal.equals("set_t"));
      if (setVal.contains("ii")) {
         if (!yearData.getTimeseries().containsKey(excelData.getParameterName())) {
            yearData.getTimeseries().put(excelData.getParameterName(), Timeseries.build(new ArrayList<>()));
         }
         yearData.getTimeseries().get(excelData.getParameterName()).getData().add(valueVal);
      } else {
         // get all set names for one parameter
         // set names have super and sub sets
         final List<String> setNames = ParameterInputDependenciesUtil.getInstance()
               .getInputSetNames(excelData.getParameterName(), yearData.getConfig().getModeldefinition());

         LOG.debug("setNames for {} are {}", excelData.getParameterName(), setNames.toString());

         // iterate through set names
         for (final String setName : setNames) {
            final Set set = yearData.getSetWithName(setName);
            // get set element for set value (e.g. tech_ES_1)
            final SetElement se = set.getElement(setVal);
            // only add parameter and value if the set element have the set value
            if (set.fetchElementNames().contains(setVal)) {
               se.getAttributes().put(excelData.getParameterName(), valueVal);
            } else {
               LOG.trace("Set {} besitzt kein Set-Element {}. Füge parameter {} nicht hinzu.", setName, setVal,
                     excelData.getParameterName());
            }
         }
      }
   }

   /**
    * Prüft, ob die übergebenen Set-Elemente für die Sets des Parameters passend sind.
    * 
    * @param setElements
    * @param parameterName
    */
   private void checkSetElement(final List<String> setElements, final String parameterName) {
      final List<String> dependencies = ParameterInputDependenciesUtil.getInstance().getInputParameterDependencies(
            parameterName, yeardata.getConfig().getModeldefinition());
      dependencies.remove("set_ii");

      for (int i = 0; i < dependencies.size(); i++) {
         boolean found = false;
         final String setName = dependencies.get(i);
         final Set set = yeardata.getSetWithName(setName);
         if (set.fetchElementNames().contains(setElements.get(i))) {
            found = true;
         }
         if (!found) {
            wrongSetElements.add(new WrongSetElement(parameterName, setElements.get(i), setName));
         }
      }
   }

   /**
    * TODO.
    *
    * @param gamsparameters TODO
    * @param model Das Modell
    * @param exd Beinhaltet die Daten einer Zeile in einer Input-spezifikation
    * @param size Die Anzahl der Elemente der Zeitreihe
    * @param setValues TODO
    * @param val TODO
    * @param parameterName Der Name des Parameters
    */
   public static void handleTimeseries(final BackendParametersYearData gamsparameters, final ExcelDefinitionsData exd,
         final int size, final List<String> setValues, final Double val, final String parameterName) {
      if (size == 2) {
         // get all set names for one parameter
         // set names have super and sub sets
         final List<String> setNames = ParameterInputDependenciesUtil.getInstance().getInputSetNames(parameterName,
               gamsparameters.getConfig().getModeldefinition());
         // iterate through set names
         for (final String setName : setNames) {
            LOG.trace("Set: {} Wert 0: {} Suche in Set: {}", parameterName, setValues.get(0));
            // get set element for set value (e.g. tech_ES_1)
            final Set set = gamsparameters.getSetWithName(setName);
            final SetElement element = set.getElement(setValues.get(0));
            final Map<String, Timeseries> timeseriesAttributes = element.getTimeseries();
            if (!timeseriesAttributes.containsKey(exd.getParameterName())) {
               timeseriesAttributes.put(exd.getParameterName(), Timeseries.build(new ArrayList<>()));
            }
            timeseriesAttributes.get(exd.getParameterName()).getData().add(val);
         }
      } else if (size == 3) {
         final Map<String, Map<String, Timeseries>> firstDependents = DataUtils
               .getOrNewMap(gamsparameters.getTableTimeseries(), exd.getParameterName());
         final Map<String, Timeseries> secondDependents = DataUtils.getOrNewMap(firstDependents, setValues.get(0));
         if (!secondDependents.containsKey(setValues.get(1))) {
            secondDependents.put(setValues.get(1), Timeseries.build(new ArrayList<>()));
         }
         final Timeseries timeseries = secondDependents.get(setValues.get(1));
         timeseries.getData().add(val);
      }
   }

   /**
    * TODO.
    *
    * @param inputSpecificationFile TODO
    * @param wb Representiert ein Excelworkbook
    * @param parameters Die Backend GAMS-Parameter
    * @throws Exception
    */
   public void readSetContent(final File inputSpecificationFile, final File excelFile,
         final BackendParametersYearData parameters) throws Exception {
      final Map<String, List<ExcelDefinitionsData>> setRanges = new HashMap<>();
      try (BufferedReader br = new BufferedReader(new FileReader(inputSpecificationFile))) {
         String line;
         while ((line = br.readLine()) != null) {
            if (line.length() > 1 && line.startsWith("set")) {
               final ExcelDefinitionsData excelData = new ExcelDefinitionsData(line);
               List<ExcelDefinitionsData> data = setRanges.get(excelData.getSheetName());
               if (data == null) {
                  data = new LinkedList<>();
                  setRanges.put(excelData.getSheetName(), data);
               }
               final int length = Math.abs(
                     excelData.getRangePositions()[0].getRow() - excelData.getRangePositions()[1].getRow())
                     + 1;
               LOG.debug("Length: {}", length);
               if (!Arrays.asList("set_ii", "set_ii_0", "set_optyears", "set_optsteps", "set_t", "set_optstore",
                     "set_optinitial", "set_a").contains(excelData.getParameterName())) {
                  data.add(excelData);
               } else if ("set_optstore".equals(excelData.getParameterName())) {
                  parameters.getConfig().setSavelength(length);
                  parameters.getConfig().setOptimizationlength(length * 2);
               } else if ("set_ii".equals(excelData.getParameterName())) {
                  parameters.getConfig().setSimulationlength(length);
                  if (length == 672) {
                     parameters.getConfig().setResolution(35040);
                  } else if (length == 168) {
                     parameters.getConfig().setResolution(8760);
                  } else {
                     throw new RuntimeException("Unknown length " + length + " should be 168 or 672");
                  }
               } else if ("set_t".equals(excelData.getParameterName())) {
                  parameters.getConfig().setOptimizationlength(length);
               }
            }
         }
      }

      LOG.info("Alle Zeilen vorbereitet");
      readSets(inputSpecificationFile, excelFile, setRanges);

   }

   private void readSets(final File inputSpecificationFile, final File excelFile,
         final Map<String, List<ExcelDefinitionsData>> setRanges)
         throws Exception, IOException, FileNotFoundException {
      for (final Map.Entry<String, List<ExcelDefinitionsData>> definitionsList : setRanges.entrySet()) {
         try (final InputStream is = new FileInputStream(excelFile);
               final Workbook workbook = StreamingReader.builder().rowCacheSize(100) // number of rows to keep in
                     // memory (defaults to 10)
                     .bufferSize(4024) // buffer size to use when reading InputStream to file (defaults to 1024)
                     .open(is);) {
            final Sheet currentSheet = workbook.getSheet(definitionsList.getKey());
            try {
               handleSet(definitionsList.getValue(), currentSheet);
            } catch (final Exception e) {
               LOG.error("Fehler beim Lesen von " + definitionsList.getKey() + " in " + inputSpecificationFile);
               throw e;
            }
         }
      }
   }

   /**
    * TODO.
    *
    * @param gp Die GAMS-Parameter
    * @param name Der Name des Sets
    * @param rangePositions die Position eines Datenelements in einer Excel-Tabelle
    * @param currentSheet Die zentrale Struktur innerhalb des Workbooks
    */
   private void handleSet(final List<ExcelDefinitionsData> excelDataList, final Sheet currentSheet) {

      for (final ExcelDefinitionsData excelData : excelDataList) {
         final Set newSet = new Set();
         newSet.setName(excelData.getParameterName());
         LOG.debug("Bearbeite Set: {} Sheet: {} Spalte: {}", excelData.getParameterName(),
               currentSheet.getSheetName(), excelData.getRangePositions()[0].getColumnNumber());
         yeardata.getSets().put(newSet.getName(), newSet);
      }

      int count = 0;
      for (final Row row : currentSheet) {
         count++;
         for (final ExcelDefinitionsData excelData : excelDataList) {
            if (count < excelData.getRangePositions()[0].getRow()) {
               continue;
            }
            if (count > excelData.getRangePositions()[1].getRow()) {
               continue;
            }
            final int columnNumber = excelData.getRangePositions()[0].getColumnNumber();
            final Cell cell = row.getCell(columnNumber);
            LOG.debug(
                  "Cell: " + cell + " Anzahl: " + count + " Ende: " + excelData.getRangePositions()[1].getRow());
            if (cell.getCellType() == CellType.STRING) {
               final String value = cell.getStringCellValue();
               LOG.debug("Set: {} Element: {}", excelData.getParameterName(), value);
               final SetElement se = new SetElement();
               se.setName(value);
               final Set newSet = yeardata.getSetWithName(excelData.getParameterName());
               newSet.getElements().add(se);
            } else {
               LOG.error("Else-type: " + cell.getCellType());
            }
         }
      }

   }
}
