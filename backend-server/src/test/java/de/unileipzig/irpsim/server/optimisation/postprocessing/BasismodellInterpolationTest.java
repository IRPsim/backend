package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 */
public final class BasismodellInterpolationTest extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(BasismodellInterpolationTest.class);

	/**
	 * Init. Testet nur, wenn überhaupt interpoliert werden soll.
	 */
	@BeforeClass
	public static void init() {
		final String interpolation = System.getenv("IRPSIM_INTERPOLATION");
		Assume.assumeNotNull(interpolation);
		Assume.assumeTrue(interpolation.equalsIgnoreCase("ALL") || interpolation.equalsIgnoreCase("POSTPROCESSING"));
	}

	/**
	 *
	 */
	@Test
	public void testPostProcessingInterpolation() throws JsonParseException, JsonMappingException, IOException {
		final long jobId = startSimulation(DatabaseTestUtils.getParameterText(TestFiles.INTERPOLATION.make()));
		final String endString = ServerTestUtils.waitForSimulationEnd(jobId);
		final IntermediarySimulationStatus iss = new ObjectMapper().readValue(endString, IntermediarySimulationStatus.class);
		Assert.assertEquals(State.FINISHED, iss.getState());

		final BackendParametersMultiModel result = ServerTestUtils.fetchResults(jobId);
		final double[][] scalarTest = new double[3][6];
		int i = 0;
		for (final BackendParametersYearData yearResult : result.getModels()[0].getYeardata()) {
			scalarTest[0][i] = yearResult.getPostprocessing().getScalars().get("par_out_IuO_Sector_Cust").getMin();
			scalarTest[0][i] = yearResult.getPostprocessing().getSets().get("set_side").get("NS").get("par_out_IuO_WSector_OrgaSide").getSum();
			scalarTest[1][i] = yearResult.getPostprocessing().getTables().get("par_out_E_fromPss_toPss").get("EGrid").get("load_E1").getMax();
			LOG.debug("Jahr: {}", yearResult.getConfig().getYear());
			i++;
		}
		checkEqualDifferences(scalarTest);
	}

	/**
	 * Überprüft, dass alle Zahlen einen jeweils gleichen Abstand zur nächsten Zahl haben.
	 *
	 * @param scalarTest Die Zahlen
	 */
	private void checkEqualDifferences(final double[][] scalarTest) {
		for (final double[] values : scalarTest) {
			for (int i = 1; i < values.length - 1; i++) {
				Assert.assertEquals(values[i] - values[i - 1], values[i + 1] - values[i], Constants.DOUBLE_DELTA);
			}
		}
	}

}
