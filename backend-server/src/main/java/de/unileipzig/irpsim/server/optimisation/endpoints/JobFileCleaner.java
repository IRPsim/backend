package de.unileipzig.irpsim.server.optimisation.endpoints;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationYearPersistent;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.server.optimisation.Job;

public class JobFileCleaner {
   
   private static final Logger LOG = LogManager.getLogger(OptimisationJobEndpoint.class);
   
   private final long simulationid;
   private final OptimisationJobPersistent sjp;
   
   public JobFileCleaner(long simulationid, OptimisationJobPersistent sjp) {
      this.simulationid = simulationid;
      this.sjp = sjp;
   }

   public void clean() throws IOException {
      for (final OptimisationYearPersistent syp : sjp.getYears()) {
         File persistentJobDirectory = getJobDirectory(syp);
         if (persistentJobDirectory != null) {
            LOG.debug("persistentJobDirectory: "+ persistentJobDirectory.getPath() + "for " + simulationid);
            if (Integer.parseInt(persistentJobDirectory.getName()) != simulationid) {
               throw new RuntimeException("Der Name des PersistenceOrdners stimmt nicht mit der id des SimulationJobPersistence überein!");
            }
            if (!persistentJobDirectory.exists()) {
               throw new RuntimeException("Kein Ordner unter der Speicheradresse " + persistentJobDirectory + " gefunden");
            } else {
               FileUtils.deleteDirectory(persistentJobDirectory);
               LOG.debug("PersistentFolder {} des SimulationJobs gelöscht.", persistentJobDirectory);
               break;
            }
         } else {
            LOG.error("Kein Dateipfad für {} (Index: {}) hinterlegt!", syp.getId(), syp.getYear());
         }
      }
      if (Job.deleteAfterwards) {
         File jobFolder = PersistenceFolderUtil.getRunningJobFolder(sjp.getId().intValue());
         FileUtils.deleteDirectory(jobFolder);
      }
   }

   /**
    * Get the persistent Year Directory of a Job
    * added option for old jobs
    * @param syp
    * @return
    */
   private File getJobDirectory(final OptimisationYearPersistent syp) {
      File persistentYearDirectory = null;
      if (syp.getLstFile() != null) {
         persistentYearDirectory = new File(syp.getLstFile()).getParentFile().getParentFile().getParentFile();
         // Patch to delete old jobs
         if (persistentYearDirectory.getName().equals("backenddata")){
            persistentYearDirectory = new File(syp.getLstFile()).getParentFile().getParentFile();
         }
      } else if (syp.getGdxparameter() != null) {
         persistentYearDirectory = new File(syp.getGdxparameter()).getParentFile().getParentFile().getParentFile();
         // Patch to delete old jobs
         if (persistentYearDirectory.getName().equals("backenddata")){
            persistentYearDirectory = new File(syp.getGdxparameter()).getParentFile().getParentFile();
         }
      }
      return persistentYearDirectory;
   }
}
