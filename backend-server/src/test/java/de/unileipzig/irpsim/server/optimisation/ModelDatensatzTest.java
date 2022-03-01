package de.unileipzig.irpsim.server.optimisation;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.AddData;
import de.unileipzig.irpsim.core.standingdata.StaticDataUtil;
import de.unileipzig.irpsim.core.standingdata.TimeseriesValue;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

public class ModelDatensatzTest extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(ModelDatensatzTest.class);

	private static final ObjectMapper om = new ObjectMapper();

	private static BackendParametersMultiModel bpMultiModel;

	@Before
	public void initDB() throws IOException {
		DatabaseTestUtils.cleanUp();
		final String content = DatabaseTestUtils.getParameterText(TestFiles.TEST.make());
		final JSONParametersMultimodel jsonParametersMultimodel = om.readValue(content,JSONParametersMultimodel.class);

		bpMultiModel = new BackendParametersMultiModel(jsonParametersMultimodel);

		final Entry<String, Timeseries> timeseriesEntry = bpMultiModel.getAllYears().get(0).getTimeseries().entrySet().iterator().next();

		final List<Double> values = timeseriesEntry.getValue().getValues();

		final AddData data = new AddData();
		data.setJahr(bpMultiModel.getAllYears().get(0).getConfig().getYear());
		data.setSzenario(1);

		final TimeseriesValue importValues[] = new TimeseriesValue[values.size()];
		final long timestamp = new Date(data.getJahr(), 0, 1).getTime();

		long i = 0;
		for (final Double d : values) {
			importValues[(int) i] = new TimeseriesValue(timestamp + i * 15 * 60 * 1000l, d);
			i++;
		}
		data.setValues(importValues);

		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final StaticData importedData = (StaticData) StaticDataUtil.importData(data, em, null);
			timeseriesEntry.getValue().setSeriesname("" + importedData.getId());
			timeseriesEntry.getValue().setData(new LinkedList<Number>());
		}

	}

	@Test
	public void testDatensatzParametrisation() throws JsonParseException, JsonMappingException, IOException {
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		final String content = objectMapper.writeValueAsString(bpMultiModel.createJSONParameters());
		ServerTestUtils.getInstance();
		final long jobid = ServerTestUtils.startSimulation(content);

		ServerTestUtils.waitForSimulationEnd(jobid);
		String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + jobid + "/";
		String resultstring = RESTCaller.callGet(testURI);
		LOG.info("GET: " + resultstring + " " + resultstring.getClass());
		final JSONObject jso = new JSONObject(resultstring);

		final int steps = (int) jso.get("finishedsteps");
		final int allSteps = (int) jso.get("simulationsteps");

		Assert.assertEquals(allSteps, steps);

		testURI = ServerTestUtils.OPTIMISATION_URI + "/" + jobid + "/results";
		LOG.info("Results-URI: " + testURI);

		resultstring = RESTCaller.callGet(testURI);

		LOG.warn(resultstring);

		final ObjectMapper om = objectMapper;
		final JSONParametersMultimodel result = om.readValue(resultstring, JSONParametersMultimodel.class);
		Assert.assertNotNull(result);

	}
}
