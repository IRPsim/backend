package de.unileipzig.irpsim.server.optimisation.persistence;

import java.sql.SQLException;

import org.junit.Ignore;
import org.junit.Test;

import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.server.optimisation.ImportResultHandler;

/**
 * @author reichelt
 */
@Ignore
// TODO Anpassen an Trennung Eingabe / Ausgabedaten
public final class ImportResultHandlerTest {

	private static final String TEST = "test-zr";
	private static final String TEST2 = "test-zr2";

	/**
	 *
	 */
	@Test
	public void testResultHandler() throws SQLException {
		final BackendParametersYearData yeardata = new BackendParametersYearData();
		yeardata.getConfig().setSimulationlength(35040);
		final ImportResultHandler resultHandler = new ImportResultHandler(yeardata);
		for (int i = 0; i < 35040; i++) {
			resultHandler.manageResult(TEST + "(set_i)", i);
			resultHandler.manageResult(TEST2 + "(set_i)", i);
		}
		resultHandler.executeImports();

		// final Map<Integer, List<Number>> result = LoadDataProvider.getInstance().getTimeseries(Arrays.asList("temp_" + TEST + "_job_0_year_0", "temp_" + TEST2 + "_job_0_year_0"));
		//
		// Assert.assertEquals(35040, result.get("temp_" + TEST + "_job_0_year_0").size());
		// Assert.assertEquals(35040, result.get("temp_" + TEST2 + "_job_0_year_0").size());
		//
		// try (Connection con = DatabaseConnectionHandler.getInstance().getConnection()) {
		// final Statement st = con.createStatement();
		// final ResultSet rs = st.executeQuery("SELECT COUNT(*) count FROM series_data_out WHERE seriesname='temp_" + TEST
		// + "_job_0_year_0' AND unixtimestamp<1356994800000");
		// rs.next();
		// Assert.assertEquals(0, rs.getInt("count"));
		//
		// final ResultSet rs2 = st.executeQuery("SELECT COUNT(*) count FROM series_data_out WHERE seriesname='temp_" + TEST
		// + "_job_0_year_0' AND unixtimestamp>=1356994800000");
		// rs2.next();
		// Assert.assertEquals(35040, rs2.getInt("count"));
		// }

		// TODO Test anpassen
	}
}
