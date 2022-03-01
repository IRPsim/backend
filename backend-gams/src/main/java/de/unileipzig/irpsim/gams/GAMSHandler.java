package de.unileipzig.irpsim.gams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.unileipzig.irpsim.core.Constants;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gams.api.GAMSDatabase;
import com.gams.api.GAMSException;
import com.gams.api.GAMSExecutionException;
import com.gams.api.GAMSGlobals.DebugLevel;
import com.gams.api.GAMSJob;
import com.gams.api.GAMSOptions;
import com.gams.api.GAMSParameter;
import com.gams.api.GAMSParameterRecord;
import com.gams.api.GAMSSet;
import com.gams.api.GAMSWorkspace;
import com.gams.api.GAMSWorkspaceInfo;

/**
 * Verwaltet den Zugriff auf die GAMS-Java-API und die dafür notwendigen Parameter.
 *
 * @author reichelt
 */
public class GAMSHandler {

	public static final String GAMSRESULTFILE = "irpsimresult.gdx";
	public static final String GAMSPARAMETERFILE = "gamsirpsim.gdx";
	public static final String GAMSPARAMETERDBNAME = "gamsirpsim";

	private static final Logger LOG = LogManager.getLogger(GAMSHandler.class);

	private final GAMSWorkspace workspace;
	private final GAMSDatabase parameterDB;
	private final File workspaceFolder;
	private final Map<String, GAMSParameter> existentParameters = new HashMap<>();

	private GAMSJob gamsjob;

	/**
	 * Parameter für die Modellauf-Parametrisierung, initialisiert den GAMS Arbeitsbereich und die GAMS Datenbank.
	 *
	 * @param workspaceFolder Ordner, in dem der GAMS-Lauf durchgeführt werden soll
	 */
	public GAMSHandler(final File workspaceFolder) {
		this.workspaceFolder = workspaceFolder;

		LOG.info("Erstelle GAMS-Workspace-Info: {}", workspaceFolder);
		final GAMSWorkspaceInfo wsInfo = new GAMSWorkspaceInfo();
		wsInfo.setWorkingDirectory(workspaceFolder.getAbsolutePath());
		String gamsPath = GAMSHandler.findGAMSDirFromEnv();
		wsInfo.setSystemDirectory(gamsPath);

		LOG.debug("Erstelle GAMS-Workspace: {}", gamsPath);
		workspace = new GAMSWorkspace(wsInfo);
		wsInfo.setDebugLevel(DebugLevel.KEEP_FILES);
		LOG.trace("GAMS-Workspace erfolgreich erstellt: {}", workspace);
		parameterDB = workspace.addDatabase(GAMSPARAMETERDBNAME);

		LOG.info("ParameterDB: {}", parameterDB.getName());
	}

  

	public void expose() {
		parameterDB.export(GAMSPARAMETERDBNAME); // write GDX file
	}

	/**
	 * Startet den angegebenen GAMS-Job.
	 * 
	 * @throws IOException
	 */
	public final void startBlocking() throws GAMSExecutionException, IOException {
		final GAMSDatabase parameterDB = workspace.addDatabaseFromGDX(GAMSPARAMETERDBNAME);

		final File gamsFile = new File(workspaceFolder, "main.gms");
		LOG.trace("WS: {}", workspace);

		LOG.info("Führe GAMS in {} GDX: {}", workspaceFolder.getAbsoluteFile(), GAMSPARAMETERFILE);
		LOG.trace("Symbole in DB {} ({}): {}", GAMSPARAMETERFILE, parameterDB, parameterDB.getNumberOfSymbols());

		final GAMSOptions options = workspace.addOptions();
		options.defines("gdxincname", GAMSPARAMETERFILE);
		LOG.info("GAMS Job Start, Name der Datenbank: {} Symbole: {}", GAMSPARAMETERFILE, parameterDB.getNumberOfSymbols());

		// opt.setGDX(workspaceFolder + File.separator + "gamsexchange.gdx");

		gamsjob = workspace.addJobFromFile(gamsFile.getAbsolutePath());
		LOG.debug("Starte mit GDX: {}", options.getGDX());

		if (Thread.currentThread().isInterrupted()) {
			return;
		}

		checkResults(options);

		LOG.info("Run beendet in {}", workspaceFolder.getAbsoluteFile());
	}

   private void checkResults(final GAMSOptions options) {
      try {
			final PrintStream printStream = new PrintStream(new File(workspaceFolder, "log.txt"));
			gamsjob.run(options, printStream, true);
			LOG.info("Lauf beendet");
		} catch (final GAMSExecutionException e) {
			LOG.info("Fehler Lauf in {}", workspaceFolder.getAbsoluteFile());
			e.printStackTrace();
			// FileUtils.copyFile(new File(workspaceFolder, "_gams_java_gjo1.lst"), new File("job_" + workspaceFolder.getName() + ".lst"));
			throw e;
		} catch (final FileNotFoundException e) {
			LOG.info("Fehler Lauf in {}", workspaceFolder.getAbsoluteFile());
			e.printStackTrace();
		} finally {
			LOG.info("Ende Lauf in {}, Schreibe out-DB", workspaceFolder.getAbsoluteFile());
			final File file = new File(workspaceFolder, GAMSRESULTFILE);
			final GAMSDatabase outDB = gamsjob.OutDB();
			outDB.export(file.getAbsolutePath());
			outDB.dispose();
			cleanUp();
		}
   }

	/**
	 * Beendet den aktuellen Simulationslauf, und gibt zurück, ob das Beenden erfolgreich war.
	 *
	 * @return Ob das Beenden erfolgreich war
	 */
	public boolean kill() {
		boolean interrupted = false;
		LOG.info("Beende {}", gamsjob);
		if (gamsjob != null) {
			interrupted = gamsjob.interrupt();
			try {
				Thread.sleep(10);
			} catch (final InterruptedException e) {
				LOG.error(e);
			}
			try {
				final Field privateStringField = GAMSJob.class.getDeclaredField("currentPID");
				privateStringField.setAccessible(true);

				final String fieldValue = (String) privateStringField.get(gamsjob);
				LOG.info("fieldValue = " + fieldValue);

				Runtime.getRuntime().exec("kill -9 " + fieldValue);

			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			} catch (final IllegalAccessException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		LOG.info("Beendet");
		return interrupted;
	}

	/**
	 * Fügt einen Skalar zu GAMS hinzu.
	 *
	 * @param parametername Name des hinzuzufügenden Parameters
	 * @param value Wert des Skalars
	 */
	public void addScalarParameter(final String parametername, final double value) {
		final GAMSParameter p = parameterDB.addParameter(parametername, 0);
		p.addRecord().setValue(value);
	}

	/**
	 * Fügt ein Set mit einer Menge von Elementen zu GAMS hinzu.
	 *
	 * @param setName Name des hinzuzuzfügenden GAMS-Sets.
	 * @param values (Geordnete) Namen der Werte.
	 */
	public void addSetParameter(final String setName, final List<String> values) {
		String usedSetName = setName;
		if (!setName.contains("(*)") && setName.contains("(")) {
			usedSetName = setName.substring(0, setName.indexOf("(")) + "(*)";
		}
		LOG.trace("Füge set hinzu: {}", usedSetName);
		final GAMSSet gs = parameterDB.addSet(usedSetName, 1);
		LOG.trace("Befülle Set: {} {}", usedSetName, values.size());
		for (final String val : values) {
			LOG.trace("Befülle mit: {}", val);
			gs.addRecord(val);
		}
	}

	/**
	 * Fügt einen Parameter zu GAMS hinzu, der von einem Set abhängt.
	 *
	 * @param parametername Der Name des Parameters.
	 * @param list Die Abbildung von Set-Elementen auf die Werte, die den Elementen zugeordnet werden.
	 */
	public void addSingleDependentParameter(final String parametername, final Map<String, Number> list) {
		LOG.trace("Füge zu GAMS-DB hinzu: {}", parametername);
		final GAMSParameter gparam = parameterDB.addParameter(parametername, 1);
		for (final Map.Entry<String, Number> entry : list.entrySet()) {
			if (entry.getValue() == null) {
				LOG.error("Wert ist nicht vorhanden!");
			}
			final GAMSParameterRecord r = gparam.addRecord(entry.getKey());
			r.setValue(entry.getValue().doubleValue());
		}
	}

	/**
	 * Fügt einen Parameter zu GAMS hizu, dessen Werte zwei oder mehr Abhängigkeiten haben.
	 *
	 * @param parametername Name des hinzuzufügenden Parameters
	 * @param map Abbildung der Abhängigen Parameter auf die Parameterwerte
	 */
	public void addMultiDependentParameter(final String parametername, final Map<Vector<String>, Number> map) {
		LOG.trace("Parametrisiere: {} Elemente: {}", parametername, map.size());
		final Vector<String> first = map.keySet().iterator().next();
		final int size = first.size();
		if (size == 3) {
			LOG.trace("Füge hinzu: {} {} Größe: {}", parametername, first, size);
		}
		GAMSParameter gparam = existentParameters.get(parametername);
		if (gparam == null) {
			gparam = parameterDB.addParameter(parametername, size);
			existentParameters.put(parametername, gparam);
		}
		for (final Map.Entry<Vector<String>, Number> entry : map.entrySet()) {
			if (parametername.equals("par_A_DES_PV")) {
				LOG.debug("Wert: {} Keys: {}", entry.getValue().doubleValue(), entry.getKey());
			}
			if (entry.getValue().doubleValue() != 0d) {
				final Vector<String> keys = entry.getKey();
				try {
					if (keys.size() > 2) {
						LOG.trace("Füge hinzu: {} {}", keys, entry.getValue().doubleValue());
					}
					final GAMSParameterRecord r = gparam.addRecord(keys);
					r.setValue(entry.getValue().doubleValue());
				} catch (final GAMSException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void addMultiDependentScalar(final String parametername, final String firstDependent, final String secondDependent, final Number value) {
		GAMSParameter gparam = existentParameters.get(parametername);
		if (gparam == null) {
			gparam = parameterDB.addParameter(parametername, 2);
			existentParameters.put(parametername, gparam);
		}
		final GAMSParameterRecord r = gparam.addRecord(firstDependent, secondDependent);
		r.setValue(value.doubleValue());
	}

	public void cleanUp() {
		if (workspace != null) {
			workspace.finalize();
		}
		if (parameterDB != null && !parameterDB.isDisposed()) {
			parameterDB.dispose();
		}
		LOG.info("Workspace bereinigt");
	}

	public final GAMSDatabase getResults() {
		LOG.info("Lese Ergebnisse aus");
		
		return workspace.addDatabaseFromGDX(getGDXResultFile().getAbsolutePath());
	}

	public GAMSDatabase getParameterDatabase() {
		return parameterDB;
	}
	
	public File getGDXResultFile() {
		File gdxResultFile = new File(workspaceFolder, GAMSRESULTFILE);
		return gdxResultFile;
	}

	public final File getGDXParametersFile() {
		final File resultFile = new File(workspaceFolder, GAMSPARAMETERFILE);
		return resultFile;
	}

	public File getLstFile() {
		for (final File file : workspaceFolder.listFiles((FilenameFilter) new WildcardFileFilter("*.lst"))) {
			return file;
		}
		return null;
	}

	public File getWorkspace() {
		return workspaceFolder;
	}

	/**
	 * Funktion zum finden des korrekten GAMS-Path falls mehrere Möglichkeiten angegeben sind
	 * @param envVar String mit Pfad zur GAMS Installation
	 * @return String mit Pfad zur GAMS Installation falls vorhanden
	 */
	public static String findGAMSDirFromEnv(){
		final String envVar = System.getenv(Constants.LD_GAMS_PATH);
		if (envVar.contains(":")){
			String[] paths = envVar.split(":");
			for (String path: paths) {
				File gams = new File(path, "gams");
				if (gams.exists()){
					return path;
				}
			}
			// TODO: raise error?
			return "";
		} else {
			return envVar;
		}
	}
	
}
