package de.unileipzig.irpsim.server.data.stammdaten;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.ws.rs.core.Response;

import org.hamcrest.MatcherAssert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;
import net.javacrumbs.jsonunit.JsonMatchers;
import net.javacrumbs.jsonunit.core.Option;

public class ZeroTimeseriesTest extends ServerTests {

	@ClassRule
	public static TestRule testRule = new TestRule() {

		@Override
      public Statement apply(final Statement base, final Description description) {
         DatabaseTestUtils.setupDbConnectionHandler();
         try (final Connection connection = DatabaseConnectionHandler.getInstance().getConnection()) {
            connection.createStatement().executeUpdate("DROP TABLE series_data_in");
            connection.createStatement().executeUpdate("DROP TABLE series_data_out");
         } catch (final SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
         return base;
      }
	};

	@Test
	public void testGetZeroTimeseries() throws IOException {
		final Response getResponse = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/concretedata?seriesid=" + 0);
		final String concreteData = getResponse.readEntity(String.class);
		// LOG.debug(concreteData);

		MatcherAssert.assertThat(concreteData, JsonMatchers.jsonNodePresent("" + 0));

		final Response getResponse5 = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/concretedata?seriesid=" + 0 + "&start=01.01.-00:00&end=31.12.-00:00&maxcount=5");
		final String concreteData5 = getResponse5.readEntity(String.class);

		// final String concreteData5 = "{\"0\": [{\"value\": 0}]}";
		MatcherAssert.assertThat(concreteData5, JsonMatchers.jsonEquals("{\"0\": [{\"avg\": 0.0},{\"avg\": 0.0},{\"avg\": 0.0},{\"avg\": 0.0},{\"avg\": 0.0}]}").when(Option.IGNORING_EXTRA_FIELDS));
	}
}
