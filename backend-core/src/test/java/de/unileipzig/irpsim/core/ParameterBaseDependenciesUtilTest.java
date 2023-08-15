package de.unileipzig.irpsim.core;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.unileipzig.irpsim.core.utils.ParameterBaseDependenciesUtil;
import de.unileipzig.irpsim.core.utils.ParameterInputDependenciesUtil;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

import de.unileipzig.irpsim.core.testutils.UseAlternateDependencies;

/**
 * @author krauss
 */
public final class ParameterBaseDependenciesUtilTest {

	public static final File ALTERNATE_DEPENDS = new File(Constants.CORE_MODULE_PATH, "src/test/resources/alternative_dependencies.json");

	@Rule
	public UseAlternateDependencies alt = new UseAlternateDependencies(ALTERNATE_DEPENDS);

	/**
	 * Testet ob die Ausgabeabhängigkeiten geladen werden, wenn sie vorher null sind.
	 */
	@Test
	public void testLoadDependencies() {
		// Assert.assertNull(ParameterDependenciesUtil.getInstance().getOutputDependencies());
		ParameterOutputDependenciesUtil.getInstance().getAllOutputParameters(UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);
		Assert.assertNotNull(ParameterOutputDependenciesUtil.getInstance().getNoOverviewData(UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID));
	}

	/**
	 * Testet ob die Eingaben korrekt geladen werden.
	 */
	@Test
	public void testInputParams() {
		final Set<String> jsonInputs = ParameterInputDependenciesUtil.getInstance().getJSONInputs(UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);
		MatcherAssert.assertThat(jsonInputs, Matchers.containsInAnyOrder("par_A_DES_PV", "par_E_SS_PHS_respos_schedule", "par_F_DES_E_DSESdirect", "par_C_pss_relativeStatus"));
	}

	/**
	 * Testet ob der richtige Setname zurückgegeben wird.
	 */
	@Test
	public void testInputSetName() {
		final List<String> inputSetName = ParameterInputDependenciesUtil.getInstance().getInputSetNames("par_E_SS_PHS_respos_schedule", UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);
		MatcherAssert.assertThat(inputSetName, Matchers.contains("set_tech_SS_PHS"));
	}

	/**
	 * Testet ob die richtigen Setnames zurückgegeben werden.
	 */
	@Test
	public void testInputSetNames() {
		final List<String> inputSetNames = ParameterInputDependenciesUtil.getInstance().getInputSetNames("par_C_pss_relativeStatus", UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);
		MatcherAssert.assertThat(inputSetNames, Matchers.containsInAnyOrder("set_pss", "set_tech_DES_ES"));
	}

	/**
	 * Testet ob die richtigen Setnamen zurückgegeben werden.
	 */
	@Test
	public void testInputTableNames() {
		final List<String> inputTableNames = ParameterInputDependenciesUtil.getInstance().getInputTableNames("par_F_DES_E_DSESdirect", UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);
		MatcherAssert.assertThat(inputTableNames, Matchers.containsInAnyOrder("set_ii", "set_side_cust", "set_tech_DES_ES"));
	}

	/**
	 * Testet ob die Ausgaben korrekt geladen werden.
	 */
	@Test
	public void testOutputParameters() {
		final List<String> allOutputParameters = ParameterOutputDependenciesUtil.getInstance().getAllOutputParameters(UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);
		MatcherAssert.assertThat(allOutputParameters, Matchers.hasItems("par_out_C_MS_E", "par_E_out_DES_load_self", "par_out_E_DES_EB_resneg"));
	}

	/**
	 * Testet ob die Ausgabeabhängigkeiten korrekt ausgegeben werden.
	 */
	@Test
	public void testOutputDependencies() {
		final Map<String, List<String>> allOutputDependencies = ParameterOutputDependenciesUtil.getInstance().getAllOutputDependencies(UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);
		MatcherAssert.assertThat(allOutputDependencies.get("par_out_C_MS_E"), Matchers.contains("set_ii"));
		MatcherAssert.assertThat(allOutputDependencies.get("par_E_out_DES_load_self"), Matchers.containsInAnyOrder("set_ii", "set_p_DS"));
		MatcherAssert.assertThat(allOutputDependencies.get("par_out_E_DES_EB_resneg"), Matchers.containsInAnyOrder("set_ii", "set_p_DS", "set_r"));
	}
	
	@Test
   public void testAllInputDependencies() {
	   Assume.assumeTrue(ParameterBaseDependenciesUtil.getInstance().getModelStream(1) != null);
	   
      Assert.assertNotNull(ParameterInputDependenciesUtil.getInstance().getAllInputDependencies(1));
   }
}
