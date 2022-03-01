package de.unileipzig.irpsim.gams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OptimisationFolderUtil {

	private static final Logger LOG = LogManager.getLogger(OptimisationFolderUtil.class);

	public static File getSimulationFolder(int modeldefinition) {
		final String pathName = ""+modeldefinition;
		final File productionModelPath = new File("gams", pathName);
		final File developmentModelPath = new File("../backend-gams/src/main/resources/gams", pathName);
		LOG.debug("Suche: {}  und {}", productionModelPath.getAbsolutePath(), developmentModelPath.getAbsolutePath());
		if (productionModelPath.exists()) {
			return productionModelPath;
		} else if (developmentModelPath.exists()) {
			try {
				LOG.debug("Dateien im Source Ordner: {}", Files.list(developmentModelPath.toPath()).collect(Collectors.toList()));
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return developmentModelPath;
		} else {
			throw new RuntimeException("GAMS-Quellcode nicht auffindbar.");
		}
	}
	
	 public static void prepareGAMSDirectory(final File workspaceFolder, int modeldefinition) {
	      try {
	         final List<IOFileFilter> filters = new LinkedList<>();
	         filters.add(new WildcardFileFilter("*.gms"));
	         filters.add(new WildcardFileFilter("*.opt"));
	         filters.add(DirectoryFileFilter.DIRECTORY);
	         FileUtils.copyDirectory(OptimisationFolderUtil.getSimulationFolder(modeldefinition), workspaceFolder, new OrFileFilter(filters));
	      } catch (final IOException e) {
	         e.printStackTrace();
	      }
	   }
}
