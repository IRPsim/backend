package de.unileipzig.irpsim.core.testutils;

import java.io.File;

import org.junit.rules.ExternalResource;

import de.unileipzig.irpsim.core.utils.ParameterBaseDependenciesUtil;

/**
 * Ersetzt die Abhängigkeiten der ParameterDependenciesUtil durch die in der übergebenen Datei angegebenen. Nach dem Test wird der vorherige Stand wieder hergestellt.
 *
 * @author krauss
 */
public final class UseAlternateDependencies extends ExternalResource {

	public static final int ALTERNATIVE_DEPENDENCIES_ID = -1;
	
	private final File alternativeDependency;
	// private JSONObject oldDependencies;
	// private File oldAlternativeFile;

	/**
	 * @param alternativeDependency
	 *            Die zu nutzende Datei
	 */
	public UseAlternateDependencies(final File alternativeDependency) {
		this.alternativeDependency = alternativeDependency;
	}

	@Override
	protected void before() throws Throwable {

		// oldDependencies = ParameterDependenciesUtil.getInstance().getOutputDependencies();
		// oldAlternativeFile = ParameterDependenciesUtil.getInstance().getAlternativeDependencyFile();
		ParameterBaseDependenciesUtil.getInstance().loadDependencies(alternativeDependency, -1);
		// ParameterDependenciesUtil.getInstance().setOutputDependencies(null);
	}

	@Override
	protected void after() {
	   if (ParameterBaseDependenciesUtil.getInstance().getModelStream(1) != null) {
	      ParameterBaseDependenciesUtil.getInstance().loadDependencies(1);
	   }
		
		// ParameterDependenciesUtil.getInstance().setOutputDependencies(oldDependencies);
		// ParameterDependenciesUtil.getInstance().setAlternativeDependencyFile(oldAlternativeFile);
	}
}
