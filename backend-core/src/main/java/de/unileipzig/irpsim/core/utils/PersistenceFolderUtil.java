package de.unileipzig.irpsim.core.utils;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.Constants;

public class PersistenceFolderUtil {

   private static final Logger LOG = LogManager.getLogger(PersistenceFolderUtil.class);

   private static File persistenceFolder;

   static {
      persistenceFolder = System.getenv(Constants.IRPSIM_PERSISTENCEFOLDER) != null ? new File(System.getenv(Constants.IRPSIM_PERSISTENCEFOLDER)) : null;
   }

   public static void setPersistenceFolder(final File staticPersistenceFolder) {
      persistenceFolder = staticPersistenceFolder;
   }

   public static File getWorkspaceFolder(final long id, int modelIndex, final int yearIndex) {
      LOG.debug("Parametrisiere Job: {} Jahr: {}", id, yearIndex);

      final File workspacesFolder = getRunningJobFolder();
      final File concreteJobFolder = new File(workspacesFolder, "job_" + id);
      final File modelFolder = new File(concreteJobFolder, "model_" + modelIndex);
      final File concreteWorkspaceFolder = new File(modelFolder, "year_" + yearIndex);
      return concreteWorkspaceFolder;
   }

   public static File getPersistenceFolder() {
      return persistenceFolder;
   }

   public static File getRunningJobFolder() {
      return new File(persistenceFolder, "running_jobs");
   }

   public static File getPersistentJobFolder(int jobid) {
      return new File(persistenceFolder, "" + jobid);
   }

   public static File getRunningJobFolder(int jobid) {
      return new File(getRunningJobFolder(), "" + jobid);
   }
}
