package de.unileipzig.irpsim.server.standingdata.endpoints;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.AddData;
import de.unileipzig.irpsim.core.standingdata.StammdatumEntity;
import de.unileipzig.irpsim.core.standingdata.StaticDataUtil;
import de.unileipzig.irpsim.core.standingdata.SzenarioSet;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Datensatz;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.server.data.Responses;
import de.unileipzig.irpsim.server.standingdata.excel.TemplateImporter;

public class DataUploader {

	private static final Logger LOG = LogManager.getLogger(DataUploader.class);

	static class Uploader implements Runnable {
		final Workbook workbook;
		private final int id;
		private Response response;

		public Uploader(final Workbook workbook, final int id) {
			this.workbook = workbook;
			this.id = id;
		}

		private Response readWorkbook() {
			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				LOG.info("Sheet: " + workbook.getSheetName(i));
			}
			
			try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
				Stammdatum stammdatum = em.find(Stammdatum.class, id);
				StaticDataUtil.fillStammdatum(stammdatum);
				if (stammdatum == null) {
					return Responses.badRequestResponse("Stammdatum " + id + " nicht vorhanden.");
				}
				
				final SzenarioSet szenarioSet = em.find(SzenarioSet.class, stammdatum.getBezugsjahr());

				// Für den DB-Import hier AddData-Objekte erstellen und dann DataEndpint.importData nutzen
				final TemplateImporter importer = new TemplateImporter(workbook);
				importer.init(stammdatum, szenarioSet);

				final Stammdatum excelStammdatum = TemplateImporter.fetchStammdatum(workbook);
				for (final StammdatumEntity entity : StammdatumEntity.values()) {
					if (!entity.compare(excelStammdatum, stammdatum)) {
						return Responses.badRequestResponse("Stammdatum ungleich: " + entity.getName() +
								" (" + entity.getValue(excelStammdatum) + " - " + entity.getValue(stammdatum) + ")");
					}
				}

				final AddData[] addDataArray = importer.getAddData();
				if (importer.getWrongDatasets().length() > 0) {
					return Responses.badRequestResponse(importer.getWrongDatasets());
				}

				final List<String> violations = importer.checkForDomainViolations();
				if (violations.size() > 0) {
					return Responses.badRequestResponse("Es wurden Domain-Regeln verletzt! Anzahl: " + violations.size());
				}

				String errorText = "";
				for (int i = 0; i < addDataArray.length; i++) {
					final Datensatz importedDatensatz = StaticDataUtil.importData(addDataArray[i], em, stammdatum);
					if (importedDatensatz instanceof AlgebraicData) {
						errorText += "Jahr " + addDataArray[i].getJahr() + " konnte nicht importiert werden, da bereits algebraischer Datensatz vorhanden ist.\n ";
					}
				}

				StaticDataUtil.updateCompleteness(em, stammdatum);

				if (errorText.length() == 0) {
					return Responses.okResponse("Erfolgreich", "Erfolgreich, Stammdatum " + stammdatum.getId() + " ist zu " + stammdatum.getVollstaendig() + " % vollständig.");
				} else {
					return Responses.badRequestResponse("Problem beim Upload", errorText);
				}
			} catch (final Throwable t) {
				t.printStackTrace();
				return Responses.errorResponse(t);
			}
		}

		@Override
		public void run() {
			response = readWorkbook();
		}

		public Response getResponse() {
			return response;
		}
	}

	private final static Map<Integer, Uploader> currentUploaders = new HashMap<>();

	public static boolean isProcessed(final int id) {
		return currentUploaders.containsKey(id);
	}

	public static Response uploadData(final Workbook create, final int id) throws InterruptedException {
		final Uploader up;
		synchronized (currentUploaders) {
			if (currentUploaders.size() > 4) {
				return Responses
						.badRequestResponse("Es werden bereits 4 Excel-Dateien hochgeladen; ein paralleles Hochladen von mehr Excel-Dateien führt zu Systeminstabilität und wird deshalb verboten.");
			}
			if (currentUploaders.containsKey(id)) {
				return Responses.badRequestResponse("Es werden bereits Daten für " + id + " hochgeladen; ein paralleles Hochladen von Daten ist nicht möglich.");
			}
			up = new Uploader(create, id);
			currentUploaders.put(id, up);
		}

		final Thread thread = new Thread(up);
		thread.start();
		thread.join();
		currentUploaders.remove(id);
		return up.getResponse();
	}

	/**
	 * Gibt an, ob gerade ein Upload stattfindet
	 * 
	 * @return true, falls ein Upload stattfindet, false sonst
	 */
	public static boolean isActive() {
		return currentUploaders.size() > 0;
	}
}
