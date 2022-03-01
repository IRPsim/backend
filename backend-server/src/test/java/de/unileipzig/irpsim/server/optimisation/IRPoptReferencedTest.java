package de.unileipzig.irpsim.server.optimisation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Testet die Parametrisierung mit einem gegebenen Parameterset.
 */
public class IRPoptReferencedTest extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(IRPoptReferencedTest.class);

	/**
	 * Importiert ein Parameterset, lädt es wieder und vergleicht es mit den ursprünglichen Daten.
	 */
	@Test
	public final void testReferenceParametrization() throws JsonParseException, JsonMappingException, IOException {

		final long parameterId = ServerTestUtils.putScenarioWithREST(TestFiles.TEST.make());

		final String parameterset = RESTCaller.callGet(ServerTestUtils.SZENARIEN_URI + "/" + parameterId);
		Assert.assertNotNull(parameterset);
		LOG.trace("Anfrage: " + parameterset);
		final long jobId = startSimulation(parameterset);
		ServerTestUtils.waitForSimulationEnd(jobId);
		final BackendParametersMultiModel result = ServerTestUtils.fetchResults(jobId);
		final BackendParametersYearData resultYear = result.getModels()[0].getYeardata()[0];

		try {
			Assert.assertEquals("Ausgabwert par_out_IuO_Sector_Cust geändert", 0d, resultYear.getPostprocessing().getScalars().get("par_out_IuO_Sector_Cust").getMin(), 1d);
			Assert.assertEquals("Ausgabwert par_out_IuO_WSector_OrgaSide geändert", 0,
					resultYear.getPostprocessing().getSets().get("set_side").get("NS").get("par_out_IuO_WSector_OrgaSide").getAvg(), 1d);
			Assert.assertEquals("Ausgabwert par_out_PowerMeasurement geändert", 3736d,
					resultYear.getPostprocessing().getSets().get("set_power").get("power_EMarket").get("par_out_PowerMeasurement").getSum(), 1d);
			Assert.assertEquals("Ausgabwert par_out_E_fromPss_toPss geändert", 5.5,
					resultYear.getPostprocessing().getTables().get("par_out_E_fromPss_toPss").get("EGrid").get("load_E1").getMax(), 1d);
		} catch (final NullPointerException n) {
			n.printStackTrace();
			Assert.fail(n.getMessage() + " in line " + n.getStackTrace()[0].getLineNumber());
		}

		final List<Integer> references = collectReferences(resultYear);

		final Map<Integer, List<Number>> resultTimeseries = DataLoader.getTimeseries(references, false);

		Assert.assertEquals("Fehler in par_out_IuO_WSector_OrgaSide", 0, (double) resultTimeseries.get(references.get(0)).get(0), 1d);
		Assert.assertEquals("Fehler in par_out_PowerMeasurement", 5.5, (double) resultTimeseries.get(references.get(1)).get(0), 0.1);
		Assert.assertEquals("Fehler in par_out_E_fromPss_toPss(EGrid, load_E1): ", 2.4, (double) resultTimeseries.get(references.get(2)).get(0), 0.1);
		Assert.assertEquals("par_out_IuO_Sector_Cust", 0, (double) resultTimeseries.get(references.get(3)).get(0), 2d);
	}

	/**
	 * @param resultYear Ergebnisjahr
	 * @return Sammlung von Testreferenzen
	 */
	private List<Integer> collectReferences(final BackendParametersYearData resultYear) {
		final List<Integer> references = new ArrayList<>();
		try {
			references.add(resultYear.getSetWithName("set_side").getElement("NS").getTimeseries().get("par_out_IuO_WSector_OrgaSide").getSeriesname());
			references.add(resultYear.getSetWithName("set_power").getElement("power_EMarket").getTimeseries().get("par_out_PowerMeasurement").getSeriesname());
			references.add(resultYear.getTableTimeseries().get("par_out_E_fromPss_toPss").get("EGrid").get("load_E1").getSeriesname());
			references.add(resultYear.getTimeseries().get("par_out_IuO_Sector_Cust").getSeriesname());
		} catch (final NullPointerException n) {
			n.printStackTrace();
			Assert.fail(n.getMessage() + " in line " + n.getStackTrace()[0].getLineNumber());
		}
		return references;
	}
}
