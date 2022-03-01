package de.unileipzig.irpsim.server.performance;

import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.unileipzig.irpsim.server.marker.DBTest;
import de.unileipzig.irpsim.server.marker.PerformanceTest;
import de.unileipzig.irpsim.server.marker.RESTTest;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * @author reichelt
 */
@Category({ RESTTest.class, DBTest.class, de.unileipzig.irpsim.server.marker.PerformanceTest.class, PerformanceTest.class })
//@RunWith(PerformanceTestRunnerJUnit.class)
public final class GetLoadServerPerformanceTest extends ServerTests {

	@Test
//	@PerformanceTest(executionTimes = 50, warmupExecutions = 50, timeout = 300000)
	public void testGetLoadREST() {
		ServerTestUtils.getInstance();
		final String uri = ServerTestUtils.TIMESERIES_URI;

		final String loadstring = RESTCaller.callGet(uri);

		final JSONArray jso = new JSONArray(loadstring);

		Assert.assertNotNull(jso);
	}
}
