package de.unileipzig.irpsim.server.data.timeseries;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.server.data.timeseries.IRPsimHolidays.IRPsimHoliday;

/**
 * Funktionales Interface zum Definieren der Algorithmen zur Transformation von Zeitreihen.
 */
public final class TimeseriesTransformer {

	/**
	 * Hilfsklassenkonstruktor.
	 */
	private TimeseriesTransformer() {
	}

	/**
	 * @param args
	 *            Optionen: -i inYear(int) -o outYear(int)
	 */
	public static void main(final String[] args) {
		final Option inYearOption = Option.builder("i").hasArg().type(Integer.class).required().build();
		final Option outYearOption = Option.builder("o").hasArg().type(Integer.class).required().build();
		final Options options = new Options().addOption(inYearOption).addOption(outYearOption);
		CommandLine line;
		try {
			line = new DefaultParser().parse(options, args);
		} catch (final ParseException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		final int inYear = Integer.parseInt(line.getOptionValue("i"));
		final int outYear = Integer.parseInt(line.getOptionValue("o"));

		final List<Number> input = new ArrayList<>();
		for (int i = 0; i < 365; i++) {
			for (int j = 0; j < 96; j++) {
				input.add(i);
			}
		}

		final TransformationInput source = new TransformationInput(inYear, Timeseries.build(input));
		final TransformationInput target = new TransformationInput(outYear, Timeseries.build(input));
		TimeseriesTransformer.adaptCalendar(source, target);
		final List<Number> output = target.getTimeseries().getData();

		@SuppressWarnings("unchecked")
		final List<Number>[] datas = new List[] { input, output };
		final Path[] files = new Path[] { Paths.get("input.csv"), Paths.get("output.csv") };

		for (int i = 0; i < files.length; i++) {
			final Path file = files[i];
			LOG.info("Ausgabe: {}", file.toAbsolutePath());
			try (BufferedWriter out = Files.newBufferedWriter(file, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING)) {
				final List<Number> list = datas[i];
				for (int j = 0; j < 365; j++) {
					final Number value = list.get(j * 96);
					out.write(value.toString());
					out.newLine();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Diese main soll Zeitreihen aus einer Excel auslesen und umgewandelte Zeitreihen in eine neue Excel schreiben, ist aber im Moment nicht aktuell
	 */
	// public static void main(final String[] args) {
	// final Option inYearOption = Option.builder("i").hasArg().type(Integer.class).required().build();
	// final Option outYearOption = Option.builder("o").hasArg().type(Integer.class).required().build();
	// final Option inputFileOption = Option.builder("e").hasArg().type(String.class).required().build();
	// final Options options = new
	// Options().addOption(inputFileOption).addOption(inYearOption).addOption(outYearOption);
	// CommandLine line;
	// try {
	// line = new DefaultParser().parse(options, args);
	// } catch (final ParseException e) {
	// e.printStackTrace();
	// throw new RuntimeException(e);
	// }
	// final int inYear = Integer.parseInt(line.getOptionValue("i"));
	// final int outYear = Integer.parseInt(line.getOptionValue("o"));
	// final Path excel = Paths.get(line.getOptionValue("e"));
	// final String excelString = excel.toString();
	// final Path output = Paths.get(excelString.substring(0, excelString.length() - 5) + "_output.xlsx");
	// try (final Workbook inExcel = new XSSFWorkbook(Files.newInputStream(excel))) {
	// try (final Workbook outExcel = new XSSFWorkbook()) {
	// sheet:
	// for (int sheetIndex = 0; sheetIndex < inExcel.getNumberOfSheets(); sheetIndex++) {
	// final Sheet sheet = inExcel.getSheetAt(0);
	// final Sheet newSheet = outExcel.createSheet(sheet.getSheetName());
	//
	// // Spalte -> Wert
	// final Map<Integer, List<Number>> values = new HashMap<>();
	// final Map<Integer, Integer> dataStart = new HashMap<>();
	// for (final Row row : sheet) {
	// final Row newRow = newSheet.createRow(row.getRowNum());
	// int physCells = 0;
	// for (int i = 0; i < row.getLastCellNum() && physCells < row.getPhysicalNumberOfCells(); i++) {
	// final Cell cell = row.getCell(i);
	// if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
	// continue;
	// }
	// physCells++;
	// final Cell newCell = newRow.createCell(cell.getColumnIndex(), cell.getCellType());
	// if (cell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
	// dataStart.put(cell.getColumnIndex(), null);
	// switch (cell.getCellType()) {
	// case Cell.CELL_TYPE_BOOLEAN:
	// newCell.setCellValue(cell.getBooleanCellValue());
	// break;
	// default:
	// newCell.setCellValue(cell.getRichStringCellValue());
	// break;
	// }
	// } else {
	// if (DateUtil.isCellDateFormatted(cell)) {
	// newCell.setCellValue(cell.getDateCellValue());
	// continue;
	// }
	// List<Number> list = values.get(cell.getColumnIndex());
	// if (dataStart.get(cell.getColumnIndex()) == null) {
	// dataStart.put(cell.getColumnIndex(), row.getRowNum());
	// list = new ArrayList<>();
	// values.put(cell.getColumnIndex(), list);
	// }
	// list.add(cell.getNumericCellValue());
	// }
	// }
	// }
	// final Map<Integer, List<Number>> transformed = new HashMap<>();
	// for (final Entry<Integer, List<Number>> lists : values.entrySet()) {
	// if (lists.getValue().size() < 35040) {
	// continue sheet;
	// }
	// final TransformationInput source =
	// TransformationInput.builder().year(inYear).timeseries(Timeseries.build(lists.getValue())).build();
	// final TransformationInput target = TransformationInput.builder().year(outYear).build();
	// TimeseriesTransformer.adaptCalendar(source, target);
	// transformed.put(lists.getKey(), target.getTimeseries().getData());
	// }
	// for (final Row row : newSheet) {
	// for (final Cell cell : row) {
	// if (dataStart.get(cell.getColumnIndex()) == null || row.getRowNum() < dataStart.get(cell.getColumnIndex())) {
	// continue;
	// }
	// if (!transformed.containsKey(cell.getColumnIndex())) {
	// continue;
	// }
	// cell.setCellValue((Double) transformed.get(cell.getColumnIndex()).get(row.getRowNum()
	// - dataStart.get(cell.getColumnIndex())));
	// }
	// }
	// }
	// outExcel.write(Files.newOutputStream(output));
	// }
	// } catch (final IOException e) {
	// e.printStackTrace();
	// }
	// }

	private static final Logger LOG = LogManager.getLogger(TimeseriesTransformer.class);

	/**
	 * Bestimmt, ob Eingabezeitreihen aus Schaltjahren zu 365 Tagen gekürzt werden.
	 *
	 * @param cutTargetLeap
	 *            true -> Zeitreihen werden gekürzt
	 * @return true -> Zeitreihen werden gekürzt
	 */
	private static boolean cutTargetLeap = true;

	public static boolean isCutTargetLeap() {
		return cutTargetLeap;
	}

	public static void setCutTargetLeap(final boolean cutTargetLeap) {
		TimeseriesTransformer.cutTargetLeap = cutTargetLeap;
	}

	/**
	 * Dieser Algorithmus gibt eine Kopie der Zeitreihe zurück.
	 *
	 * @param source
	 *            Eingabedaten
	 * @param target
	 *            Ausgabemetadaten
	 */
	public static void copy(final TransformationInput source, final TransformationInput target) {
		if (!checkInputLength(source)) {
			LOG.warn("Für Parameter {} in Jahr {} haben die Daten nicht die richtige Anzahl Werte!", source.getTimeseries().getSeriesname(), source.getYear());
			target.getTimeseries().setData(source.getTimeseries().getData());
		}
		final ArrayList<Number> copiedList;
		if (cutTargetLeap && source.getTimeseries().getData().size() == 366 * 96) {
			copiedList = new ArrayList<Number>(source.getTimeseries().getData().subList(0, 355 * 96 - 1));
		} else {
			copiedList = new ArrayList<Number>(source.getTimeseries().getData());
		}
		target.getTimeseries().setData(copiedList);
	}

	/**
	 * Dieser Algorithmus verschiebt die Daten derart um ganze Tage, dass gleiche Wochentage aufeinander abgebildet werden. Dabei wird um höchstens drei Tage zurück- oder vorverschoben. Die
	 * entstehenden Lücken an Ende oder Anfang werden durch gleiche Wochentage in den Wochen zuvor oder danach aufgefüllt.
	 *
	 * @param source
	 *            die gekapselten Daten und Metadaten des ursprünglichen Jahres
	 * @param target
	 *            die gekapselten Metadaten des Zieljahres
	 */
	public static void shiftWeekdays(final TransformationInput source, final TransformationInput target) {
		if (target.getTimeseries() == null) {
			target.setTimeseries(new Timeseries());
		}
		final List<Number> inputData = source.getTimeseries().getData();
		if (!checkInputLength(source)) {
			LOG.warn("Für Parameter {} in Jahr {} konnten die Daten nicht an Wochentage angepasst werden!", source.getTimeseries().getSeriesname(),
					source.getYear());
			target.getTimeseries().setData(inputData);
		}
		final int smallestDayDifference = dayDiff(source.getYear(), target.getYear());
		final boolean fullLeap = inputData.size() == 366 * 96;
		if (smallestDayDifference == 0) {
			copy(source, target);
		} else if (smallestDayDifference > 0) {
			final List<Number> startDays = new ArrayList<>();
			// Erstelle die Starttage (zB (Mi-> Mo): Werte von Mo(6.1.)und Di(7.1.)
			for (int i = 0; i < smallestDayDifference; i++) {
				final LocalDate fillingDay = findFillingDay(LocalDate.of(source.getYear(), 1, 1).plusDays(i - smallestDayDifference), source.getYear());
				startDays.addAll(inputData.subList((fillingDay.getDayOfYear() - 1) * 96, fillingDay.getDayOfYear() * 96));
			}
			// Füge die restlichen Werte hinzu
			startDays.addAll(inputData.subList(0, inputData.size() - smallestDayDifference * 96));
			if (cutTargetLeap && fullLeap) {
				target.getTimeseries().setData(new ArrayList<>(startDays.subList(0, 365 * 96)));
			} else {
				target.getTimeseries().setData(startDays);
			}
		} else {
			// Schneide erste Tage ab (zB (Mo -> Mi): Liste ohne Mo(1.1.) und Di(2.1)
			final List<Number> cutStart = new ArrayList<>(inputData.subList(-smallestDayDifference * 96, inputData.size()));
			// Füge Endtage hinzu
			for (int i = 1; i <= -smallestDayDifference; i++) {
				final LocalDate fillingDay = findFillingDay(LocalDate.of(source.getYear(), 12, 31).plusDays(i), source.getYear());
				cutStart.addAll(inputData.subList((fillingDay.getDayOfYear() - 1) * 96, fillingDay.getDayOfYear() * 96));
			}
			if (cutTargetLeap && fullLeap) {
				target.getTimeseries().setData(new ArrayList<>(cutStart.subList(0, 365 * 96)));
			} else {
				target.getTimeseries().setData(cutStart);
			}
		}
	}

	/**
	 * Methode führt eine kalendarische Anpassung der Zeitreihe an das Zieljahr durch. Dabei werden die Daten der Feiertage gespeichert. Die entstandenen Lücken werden durch Daten naheliegender
	 * gleicher Wochentage (nicht-Feiertage) aufgefüllt. Die entstandene Zeitreihe wird um höchstens drei Tage verschoben, sodass Wochentage im Ausgangsjahr mit gleichen Wochentagen im Zieljahr
	 * korrespondieren, die nun fehlenden Tage an Anfang oder Ende des Jahres werden wie die Feiertagslücken aufgefüllt. Zuletzt ersetzen die Daten der Feiertage die Daten der entsprechenden Tage.
	 * Diese Zeitreihe wird dann zurückgegeben.
	 *
	 * @param source
	 *            die Daten des Ursprungsjahrs
	 * @param target
	 *            die Daten des Zieljahrs
	 */
	public static void adaptCalendar(final TransformationInput source, final TransformationInput target) {
		final int year = source.getYear();
		if (!checkInputLength(source)) {
			LOG.warn("Für Parameter {} in Jahr {} konnten die Daten nicht an den aktuellen Kalender angepasst werden!",
					source.getTimeseries().getSeriesname(), year);
			target.getTimeseries().setData(source.getTimeseries().getData());
			return;
		}
		final List<Number> data = new ArrayList<>(source.getTimeseries().getData());
		final boolean isLeapData = data.size() == 366 * 96;
		final Map<IRPsimHoliday, List<Number>> savedValues = new HashMap<>();
		for (final IRPsimHoliday holiday : new IRPsimHolidays()) {
			final LocalDate holyDate = holiday.getDate(year);
			final LocalDate fillInDay = findFillingDay(holyDate, holyDate.getYear());
			savedValues.put(holiday, replaceDateValues(data, fillInDay, holyDate, isLeapData));
		}
		final Timeseries series = new Timeseries();
		series.setData(data);
		final TransformationInput weekInput = new TransformationInput(year, series);

		shiftWeekdays(weekInput, target);

		for (final Entry<IRPsimHoliday, List<Number>> savedEntry : savedValues.entrySet()) {
			final LocalDate holyDate = savedEntry.getKey().getDate(target.getYear());
			final ListIterator<Number> replaceIterator = target.getTimeseries().getData().listIterator((holyDate.getDayOfYear() - 1) * 96);
			for (final Number value : savedEntry.getValue()) {
				replaceIterator.next();
				replaceIterator.set(value);
			}
		}
	}

	/**
	 * Rechnet die Wochentagesdifferenz des Neujahrstages aus, gibt Werte im Bereich (-3..3) zurück.
	 *
	 * @param sourceYear
	 *            Eingabejahr
	 * @param targetYear
	 *            Ausgabejahr
	 * @return Differenz der Wochentage, z.B. (source -> Montag, target -> Mittwoch) -> -2
	 */
	static int dayDiff(final int sourceYear, final int targetYear) {
		final DayOfWeek inputFirstDay = LocalDate.of(sourceYear, 1, 1).getDayOfWeek();
		final DayOfWeek outputFirstDay = LocalDate.of(targetYear, 1, 1).getDayOfWeek();
		final int dayDifference = inputFirstDay.getValue() - outputFirstDay.getValue();
		final int floorMod = Math.floorMod(dayDifference, 7);
		return floorMod > 3 ? floorMod - 7 : floorMod;
	}

	/**
	 * Überprüft die Länge der Zeitreihe, wobei diese auch 366 96 betragen darf, wenn das Jahr ein Schaltjahr ist.
	 *
	 * @param source
	 *            die zu überprüfenden Daten
	 * @return true -> passt alles
	 */
	private static boolean checkInputLength(final TransformationInput source) {
		final boolean nonLeap = source.getTimeseries().getData().size() == 365 * 96;
		final boolean leap = LocalDate.of(source.getYear(), 1, 1).isLeapYear() && source.getTimeseries().getData().size() == 366 * 96;
		if (nonLeap || leap) {
			return true;
		}
		return false;
	}

	/**
	 * Findet ein Datum in einer nahen vorherigen oder nachfolgenden Woche mit dem gleichen Wochentag, das kein Feiertag ist.
	 *
	 * @param dateToReplace
	 *            das zu ersetzende Datum
	 * @param year
	 *            das Jahr, in dem ein Ersatzdatum gefunden werden soll
	 * @return das ersetzende Datum
	 */
	private static LocalDate findFillingDay(final LocalDate dateToReplace, final int year) {
		Integer relativeWeek = 0;
		while (relativeWeek < 53) {
			relativeWeek = relativeWeek > 0 ? -1 * relativeWeek : -1 * (relativeWeek - 1);
			final LocalDate relativeDate = dateToReplace.plusWeeks(relativeWeek);
			if (relativeDate.getYear() == year && !IRPsimHolidays.isHoliday(relativeDate)) {
				return relativeDate;
			}
		}
		LOG.warn("Für das Datum {} konnte kein valides Datum zum Ersetzen gefunden werden!", dateToReplace);
		return dateToReplace;
	}

	/**
	 * Ersetzt die Daten eines Datums mit denen eines anderen und gibt die ersetzten Daten aus.
	 *
	 * @param data
	 *            Daten in denen ersetzt werden soll
	 * @param dateToCopy
	 *            das Datum mit dessen Daten die ausgeschnittenen Werte ersetzt werden sollen
	 * @param dateToReplace
	 *            das Datum dessen Werte ersetzt werden sollen
	 * @param isLeapData
	 *            Nur dann auf true zu setzen, wenn es sich in dem Jahr um ein Schaltjahr handelt UND die Daten alle 366 Tage umfassen
	 * @return die Daten des dateToReplace
	 */
	private static List<Number> replaceDateValues(final List<Number> data, final LocalDate dateToCopy,
			final LocalDate dateToReplace, final boolean isLeapData) {
		final ListIterator<Number> replaceIterator = data.listIterator((dateToReplace.getDayOfYear() - 1) * 96);
		final ListIterator<Number> refIterator = new ArrayList<>(data).listIterator((dateToCopy.getDayOfYear() - 1) * 96);
		final List<Number> values = new ArrayList<>();
		for (int i = 0; i < 96; i++) {
			values.add(replaceIterator.next());
			replaceIterator.set(refIterator.next());
		}
		return values;
	}

	// /**
	// * Erstellt einen Iterator an dem Punkt in den Daten, die dem Datum entsprechen. Es wird von Zeitvierteln
	// * ausgegangen.
	// *
	// * @param baseList die Daten
	// * @param date das Datum
	// * @param isLeapData
	// * @return der Listeniterator
	// */
	// private static ListIterator<Number> fetchIterator(@NonNull final List<Number> baseList, @NonNull final LocalDate
	// date, final boolean isLeapData) {
	// int startIndex = 0;
	// final boolean leapYear = LocalDate.of(date.getYear(), 1, 1).isLeapYear();
	// if (leapYear && !isLeapData && date.isAfter(LocalDate.of(date.getYear(), 2, 29))) {
	// startIndex = (date.getDayOfYear() - 2) * 96;
	// } else {
	// startIndex = (date.getDayOfYear() - 1) * 96;
	// }
	// return baseList.listIterator(startIndex);
	// }
}