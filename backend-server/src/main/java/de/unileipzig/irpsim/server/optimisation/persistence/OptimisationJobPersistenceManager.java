package de.unileipzig.irpsim.server.optimisation.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationYearPersistent;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.server.modelstart.JavaModelStarter;
import de.unileipzig.irpsim.server.modelstart.ModelStarter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import javax.persistence.EntityTransaction;
import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Verwaltung und Persistierung der zu persistierenden Daten eines Simulationsjobs.
 *
 * @author reichelt
 */
public class OptimisationJobPersistenceManager {

   

   private static final Logger LOG = LogManager.getLogger(OptimisationJobPersistenceManager.class);
   private final long jobid;
   // private OptimisationJobPersistent persistentJob;
   private final File persistenceJobFolder;

   /**
    * Erzeugt neue Instanz des SimulationJobPersistanceManagers, legt falls nicht vorhanden einen neuen Ordner für die Ausführung an.
    *
    * @param persistentJob Der zu Persistierende SimulationJob
    */
   public OptimisationJobPersistenceManager(final OptimisationJobPersistent persistentJob) {
      // Der Job muss zuerst initial persistiert werden, bevor die Id für den Ordnernamen ausgelesen werden kann!
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         em.persist(persistentJob.getDescription());
         em.persist(persistentJob);
         transaction.commit();
      }
      LOG.trace("PersistentJobID nach dem ersten persistieren: {}", persistentJob.getId());
      persistenceJobFolder = PersistenceFolderUtil.getPersistentJobFolder(persistentJob.getId().intValue());
      if (!persistenceJobFolder.exists()) {
         persistenceJobFolder.mkdir();
      }
      LOG.debug("Speichere Ergebnis in {}", persistenceJobFolder);
      jobid = persistentJob.getId();
   }

   /**
    * Persistiert die Simulationsdaten(LST-, GDX-, CSV-Daten) eines bestimmten Jahres.
    *
    * @param yearIndex Der Index des Jahres
    * @param result Die Ergebnisse im Format {@link JSONParametersMultimodel}
    * @param csvResultFile Die CSV-Ergebnisdatei
    * @throws IOException
    */
   public final synchronized void persistYear(int modelIndex, final int yearIndex, final ModelStarter caller
         , final JSONParametersMultimodel result, final File csvResultFile) throws IOException {
      final long start = System.nanoTime();
      persistLogs(modelIndex, yearIndex, caller);
      Serializable persistedYear = null;
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent persistentJob = em.find(OptimisationJobPersistent.class, jobid);
         LOG.debug("Persistiere Simulationsergebnisse in: {}", persistenceJobFolder);
         final OptimisationYearPersistent year = persistentJob.getYearDataOfSimulatedYear(yearIndex, modelIndex);

         LOG.debug("LST-File: {}", caller.getListingFile());
         persistLST(caller.getListingFile(), year, modelIndex, yearIndex);
         persistCSV(csvResultFile, year, modelIndex, yearIndex);
         try {
            LOG.debug("Schreibe JSON...");
            final String value = new ObjectMapper().writeValueAsString(result);
            LOG.debug("JSON: ", value.substring(0, 300));
            persistentJob.setJsonResult(value);
         } catch (final JsonProcessingException e) {
            LOG.error(e);
            e.printStackTrace();
         }

         if (caller instanceof JavaModelStarter) {
            File imageFolder = new File(caller.getParameterizeLog().getParentFile().toString() + File.separator + "images");
            LOG.debug("Persistiere Image Folder{}", imageFolder);
            persistImageFolder(imageFolder, year, modelIndex, yearIndex);
         }

         LOG.debug("Persistiere GDX-Parameter");
         persistGDXParameter(caller.getGDXParameterFile(), year, modelIndex, yearIndex);
         LOG.debug("Persistiere GDX-Ergebnis");
         persistGDXResult(caller.getGDXResultFile(), year, modelIndex, yearIndex);

         LOG.debug("Persisting SQlite");
         persistResultFile(caller.getSQLiteFile(), year, modelIndex, yearIndex);

         LOG.debug("Persistiere Job und Jahr, Instanz: {}", this);
         final EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         final Session session = em.unwrap(Session.class);
         // em.
         persistedYear = session.save(year);
         session.update(persistentJob);
         transaction.commit();
         // session.flush();
      }
      if (persistedYear instanceof Integer) {
         final int newId = (Integer) persistedYear;
         LOG.debug("Persistiertes Jahr-Id: {}", newId);
      } else {
         LOG.debug("{}", persistedYear.getClass());
      }
      final long end = System.nanoTime();
      LOG.debug("Simulationsjob persistiert, Gesamtdauer: {}", (end - start) / Constants.MEGA);
   }

   private void persistIRPACTPutputfile(File outputJson, OptimisationYearPersistent year, int modelIndex, int yearIndex) throws IOException {
      if (outputJson.exists()){
         final File destinationOutputJson = new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
               "year-" + yearIndex + File.separator +  outputJson.getName());
         FileUtils.copyFile(outputJson, destinationOutputJson);
      }
   }

   private void persistLogs(int modelIndex, final int yearIndex, final ModelStarter caller) throws IOException {
      if (caller.getParameterizeLog() != null && caller.getParameterizeLog().exists()) {
         final File destinationParameterize = new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
               "year-" + yearIndex + File.separator +  caller.getParameterizeLog().getName());
         FileUtils.copyFile(caller.getParameterizeLog(), destinationParameterize);
      }
      if (caller.getOptimizeLog().exists()) {
         final File destinationOptimize = new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
               "year-" + yearIndex + File.separator +  caller.getOptimizeLog().getName());
         FileUtils.copyFile(caller.getOptimizeLog(), destinationOptimize);
      }
   }

   /**
    * Speichert ein Jahr, wenn es einen Fehler bei der Berechnung gab.
    *
    * @param yearIndex Index des Jahres
    * @param gdxParameterFile Die GAMS Ergebnisdatei
    * @throws IOException
    */
   public final void persistErrorYear(int modelIndex, final int yearIndex, final ModelStarter caller, final File gdxParameterFile) throws IOException {
      try {
         persistLogs(modelIndex, yearIndex, caller);
      } catch (ClosedByInterruptException e) {
         e.printStackTrace();
         return;
      }

      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent persistentJob = em.find(OptimisationJobPersistent.class, jobid);

         final long start = System.nanoTime();
         LOG.debug("Persistiere Simulationsergebnisse in: {}", persistenceJobFolder);
         final OptimisationYearPersistent year = persistentJob.getYearDataOfSimulatedYear(yearIndex, modelIndex);
         LOG.debug("LST-File: {}", caller.getListingFile());
         persistLST(caller.getListingFile(), year, modelIndex, yearIndex);

         persistGDXParameter(gdxParameterFile, year, modelIndex, yearIndex);

         final long end = System.nanoTime();
         LOG.debug("Simulationsjob persistiert, Gesamtdauer: {}", (end - start) / Constants.MEGA);

         final EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         final Session s = (Session) em.getDelegate();
         s.save(year);
         s.update(persistentJob);
         transaction.commit();
      }

   }

   /**
    * Persistiert lst-Daten.
    *
    * @param listingFile Die GAMS Datei mit der Ausgabedokumentation
    * @param year Das Jahr des SimulationJobs
    * @param yearIndex Der Index des Jahres
    */
   public final void persistLST(final File listingFile, final OptimisationYearPersistent year, final int modelIndex, final int yearIndex) {
      if (listingFile != null && listingFile.exists()) {
         if (listingFile.length() / 10E8 > 1) {
            LOG.info("Listing-Datei ist größer als 1 GB - Lösche Datei ohne Persistierung");
            listingFile.delete();
            year.setLstFile("Listing-Datei war " + (listingFile.length() / 10E8) + " GB groß und wurde deshalb gelöscht.");
         } else {
            final File destination = new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
                  "year-" + yearIndex + File.separator + "listing.zip");
            zipFile(listingFile, destination);
            LOG.debug("Setzte LST-Datei: {}", destination.getAbsolutePath());
            year.setLstFile(destination.getAbsolutePath());
         }

      }
   }

   private void zipFile(final File source, final File destination) {
      if (!destination.getParentFile().exists()) {
         destination.getParentFile().mkdirs();
      }
      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destination))) {
         final ZipEntry ze = new ZipEntry(source.getName());
         zos.putNextEntry(ze);
         final FileInputStream in = new FileInputStream(source);

         int len;
         final byte[] buffer = new byte[1024];
         while ((len = in.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
         }

         in.close();
         zos.closeEntry();

      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Persistiert CSV-Daten.
    *
    * @param csvFile Die CSV-Daten
    * @param year Das Jahr des SimulationsJobs
    * @param yearIndex Der Index des Jahres
    * @throws IOException
    */
   public final void persistCSV(final File csvFile, final OptimisationYearPersistent year, final int modelIndex, final int yearIndex) throws IOException {
      if (csvFile != null && csvFile.exists()) {
         LOG.debug("CSV-File: {} {}", csvFile.exists(), csvFile.getAbsolutePath());
         final File destination = new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
               "year-" + yearIndex + File.separator + "csvdata.csv");
         FileUtils.copyFile(csvFile, destination);
         year.setCsvTotalData(destination.getAbsolutePath());
      }
   }

   // TODO Eigentlich müsste man mal die Dateien alle gleich speichern und dann überall davon ausgehen, dass new File(yearfolder, Constants.XX) die jeweilige Datei ist, statt alle
   // einzeln zu behandeln
   public final void persistResultFile(final File resultFile, final OptimisationYearPersistent year, final int modelIndex, final int yearIndex) throws IOException {
      if (resultFile != null) {
         final File destination = new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
               "year-" + yearIndex + File.separator + resultFile.getName());
         FileUtils.copyFile(resultFile, destination);
         year.setSqlite(destination.getAbsolutePath());
      }
   }

   /**
    * Persistiert GDX-Ausgabedatenbank.
    *
    * @param gdxResultFile Die GDX-Ergebnisdatei
    * @param year Das Jahr des SimulationsJobs
    * @param yearIndex Der Index des Jahres
    * @throws IOException
    */
   public final void persistGDXResult(final File gdxResultFile, final OptimisationYearPersistent year, final int modelIndex, final int yearIndex) throws IOException {
      if (gdxResultFile != null) {
         final File destination = new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
               "year-" + yearIndex + File.separator + "gdxresult.gdx");
         FileUtils.copyFile(gdxResultFile, destination);
         year.setGdxresult(destination.getAbsolutePath());
      }
   }

   /**
    * Persistiert GDX-Eingabedatenbank.
    *
    * @param gdxParameterFile Die GDX-Parameterdatei
    * @param year Das Jahr des SimulationsJobs
    * @param yearIndex Der Index des Jahres
    * @throws IOException
    */
   public final void persistGDXParameter(final File gdxParameterFile, final OptimisationYearPersistent year, int modelIndex, final int yearIndex) throws IOException {
      if (gdxParameterFile != null) {
         final File destination = new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
               "year-" + yearIndex + File.separator + 
               gdxParameterFile.getName());
         FileUtils.copyFile(gdxParameterFile, destination);
         year.setGdxparameter(destination.getAbsolutePath());
      }
   }

   /**
    * Persistiert Daten mehrerer interpolierter Jahre.
    *
    * @param numberOfInterpolYears Anzahl zu persistierender Jahre
    * @param result Das Ergebnis im Format {@link JSONParametersMultimodel}
    */
   public final void persistInterpolatedYears(final int numberOfInterpolYears, final JSONParametersMultimodel result) {
      final long start = System.nanoTime();
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent persistentJob = em.find(OptimisationJobPersistent.class, jobid);

         LOG.debug("Persistiere Interpolationsergebnisse in: {}", persistenceJobFolder);

         // for (int index = yearIndex - numberOfInterpolYears; index < yearIndex; index++) {
         // final OptimisationYearPersistent year = new OptimisationYearPersistent();
         // persistentJob.getYears().add(year);
         // year.setYear(result.getYears().get(yearIndex).getConfig().getYear());
         // year.setSimulatedYearIndex(yearIndex);
         // year.setJob(persistentJob);
         try {
            final String value = new ObjectMapper().writeValueAsString(result);
            persistentJob.setJsonResult(value);
         } catch (final JsonProcessingException e1) {
            e1.printStackTrace();
         }

         final EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         final Session s = (Session) em.getDelegate();
         // s.save(year);
         s.update(persistentJob);
         transaction.commit();
         // }
      }

      final long end = System.nanoTime();
      LOG.debug("Simulationsjob persistiert, Gesamtdauer: {}", (end - start) / Constants.MEGA);
   }

   public void persistEnd(final Date date) {
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent persistentJob = em.find(OptimisationJobPersistent.class, jobid);
         persistentJob.setEnd(date);

         final EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         final Session s = (Session) em.getDelegate();
         s.save(persistentJob);
         s.update(persistentJob);
         transaction.commit();
      }

   }

   /**
    * Persistiert die Image Dateien in einem Archiv
    *
    * @param imageSourceFolder
    * @param modelIndex of the persisting job
    * @param yearIndex of the persisting job
    */
   public final void persistImageFolder(File imageSourceFolder, final OptimisationYearPersistent year, final int modelIndex, final int yearIndex) {
      if (!imageSourceFolder.exists()) {
         return;
      }
      try {
         // copy folder
         FileUtils.copyDirectory(imageSourceFolder, new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
               "year-" + yearIndex + File.separator + "images"));
      } catch (IOException e) {
         e.printStackTrace();
      }
      final File destination = new File(persistenceJobFolder, "model-" + modelIndex + File.separator +
            "year-" + yearIndex + File.separator + "images.zip");
      zipFolder(imageSourceFolder, destination);

      LOG.debug("Setzte Image-Archiv: {}", destination.getAbsolutePath());
      // TODO
      //year.setImageArchive(destination.getAbsolutePath());
   }

   private void zipFolder(final File source, final File destination) {
      if (!destination.getParentFile().exists()){
         destination.getParentFile().mkdir();
      }
      try {
         ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destination)));
         BufferedInputStream in = null;
         byte[] data = new byte[1024];
         String files[] = source.list();
         for (int i =0; i <files.length; i++) {
            in = new BufferedInputStream(new FileInputStream(source.getPath() + "/" + files[i]), 1024);
            int count;
            outputStream.putNextEntry(new ZipEntry(files[i]));
            while((count = in.read(data, 0, 1024)) != -1){
               outputStream.write(data, 0, count);
            }
            outputStream.closeEntry();
         }
         outputStream.flush();
         outputStream.close();

      } catch (Exception e){
         e.printStackTrace();
      }
   }

}
