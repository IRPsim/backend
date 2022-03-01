package de.unileipzig.irpsim.core;

import java.util.*;

import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;

/**
 * @author reichelt
 */
@Ignore // TODO Set-Bezeichnungen aus realen Parameter benuzen oder alternative Dependencies setzen
public final class SchemaTest {

	private static final Logger LOG = LogManager.getLogger(SchemaTest.class);

	private ObjectMapper om;
	private JSONParametersMultimodel pmm;
	private YearData year0;

	/**
	 * Init Objectmapper und Datensatz.
	 */
	@Before
	public void initOM() {
		om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);

		pmm = new JSONParametersMultimodel();
		pmm.getDescription().setBusinessModelDescription("businessModelDescriptionTest");
		pmm.getDescription().setCreator("creatorTest");
		pmm.getDescription().setInvestmentCustomerSide("investmentCustomerSideTest");
		pmm.getDescription().setParameterAttention("parameterAugenmerkTest");

		JSONParameters jp = new JSONParameters();
		year0 = new YearData();
		ArrayList<YearData> yearList = new ArrayList<>();
		yearList.add(year0);

		jp.setYears(yearList);
		pmm.getModels().add(jp);

		year0.getConfig().setSavelength(1);
		year0.getConfig().setSimulationlength(3);
		year0.getConfig().setTimestep(0.25);
	}

	/**
	 *
	 */
	@Test
	public void testJSONGenerationFromPOJO() throws JsonProcessingException {

		addSetContents();

		addTableContents();

		final String parameterjson = om.writeValueAsString(pmm);
		LOG.debug(parameterjson);

		JsonFluentAssert.assertThatJson(parameterjson).node("models[0].years[0].sets.set_p.p1").isPresent();
		JsonFluentAssert.assertThatJson(parameterjson).node("models[0].years[0].sets.set_p.p2").isPresent();
		JsonFluentAssert.assertThatJson(parameterjson).node("models[0].years[0].sets.set_pv.pv1.prop1").isEqualTo(0.5);
		//JsonFluentAssert.assertThatJson(parameterjson).node("models[0].years[0].sets.set_pv.pv1.propts1").isEqualTo(new Double[] { 1.0, 2.0, 3.0 });
		JsonFluentAssert.assertThatJson(parameterjson).node("models[0].years[0].sets.set_pv.pv1.propts2").isEqualTo("\"2\"");

		JsonFluentAssert.assertThatJson(parameterjson).node("models[0].years[0].tables.chp_property.pv1.p2").isEqualTo(0.5);
		JsonFluentAssert.assertThatJson(parameterjson).node("models[0].years[0].tables.pv_property.pv2.p1").isEqualTo(0.25);
	}

	/**
	 * Fügt einige Werte für Sets hinzu.
	 */
	private void addSetContents() {
		createInitialSetMap();

		year0.getTimeseries().put("ts1", Arrays.asList(new Double[] { 0.1, 0.2, 0.3 }));
		year0.getTimeseries().put("ts2", "3");
	}

	/**
	 *
	 */
	@Test
	public void testTimeseriesEquality() throws JsonProcessingException {

		year0.getTimeseries().put("ts1", Arrays.asList(new Double[] { 0.1, 0.2, 0.3 }));
		year0.getTimeseries().put("ts2", "1");

		final String originalJson = om.writeValueAsString(pmm);

		final BackendParametersMultiModel mip = new BackendParametersMultiModel(pmm);
		final String uijson = om.writeValueAsString(mip.createJSONParameters());
		LOG.trace(uijson);
		LOG.trace("Original:\n" + originalJson);
		JsonAssert.assertJsonEquals(originalJson, uijson);
	}

	/**
	 *
	 */
	@Test
	public void testScalarEquality() throws JsonProcessingException {
		year0.getScalars().put("val1", 0.25);
		year0.getScalars().put("val2", 0.5);

		final String originalJson = om.writeValueAsString(pmm);

		final BackendParametersMultiModel mip = new BackendParametersMultiModel(pmm);
		final String uijson = om.writeValueAsString(mip.createJSONParameters());
		JsonAssert.assertJsonEquals(originalJson, uijson);
	}

	/**
	 *
	 */
	@Test
	public void testSetEquality() throws JsonProcessingException {
		createInitialSetMap();

		final String originalJson = om.writeValueAsString(pmm);

		final BackendParametersMultiModel mip = new BackendParametersMultiModel(pmm);
		final String uijson = om.writeValueAsString(mip.createJSONParameters());
		JsonAssert.assertJsonEquals(originalJson, uijson);
	}

	private void createInitialSetMap() {
		LinkedHashMap<String, LinkedHashMap<String, Object>> setMap = new LinkedHashMap<>();
		setMap.put("p1", new LinkedHashMap<>());
		setMap.put("p2", new LinkedHashMap<>());
		year0.getSets().put("set_p", setMap);

		final LinkedHashMap<String, Object> pvProp = new LinkedHashMap<>();
		pvProp.put("prop1", 0.5);
		pvProp.put("prop2", 0.3);
		// pvProp.put("propts1", Arrays.asList(new Double[] { 1.0, 2.0, 3.0 }));
		pvProp.put("propts2", "2");

		setMap = new LinkedHashMap<>();
		setMap.put("pv1", pvProp);
		setMap.put("pv2", pvProp);
		year0.getSets().put("set_pv", setMap);
	}

	/**
	 *
	 */
	@Test
	public void testTableEquality() throws JsonProcessingException {
		addTableContents();

		final String originalJson = om.writeValueAsString(pmm);

		final BackendParametersMultiModel mip = new BackendParametersMultiModel(pmm);
		final String uijson = om.writeValueAsString(mip.createJSONParameters());
		JsonAssert.assertJsonEquals(originalJson, uijson);
	}

	/**
	 *
	 */
	@Test
	public void testTimeseriesTableEquality() throws JsonProcessingException {
		addTimeseriesTableContents();

		final String originalJson = om.writeValueAsString(pmm);

		final BackendParametersMultiModel mip = new BackendParametersMultiModel(pmm);
		final String uijson = om.writeValueAsString(mip.createJSONParameters());
		JsonAssert.assertJsonEquals(originalJson, uijson);
	}

	/**
	 *
	 */
	private void addTableContents() {
		Map<String, Object> contents = new HashMap<>();
		contents = new HashMap<>();
		contents.put("p1", 0.25);
		contents.put("p2", 0.5);
		final Map<String, Map<String, Object>> tablecontents = new HashMap<>();
		tablecontents.put("pv1", contents);
		tablecontents.put("pv2", contents);
		year0.getTables().put("pv_property", tablecontents);
		final Map<String, Map<String, Object>> tablecontents2 = new HashMap<>();
		tablecontents2.putAll(tablecontents);
		year0.getTables().put("chp_property", tablecontents2);
	}

	/**
	 *
	 */
	private void addTimeseriesTableContents() {
		final Map<String, Object> timeseriescontents = new HashMap<>();
		Map<String, Map<String, Object>> parameterNames = new HashMap<>();
		timeseriescontents.put("p1", Arrays.asList(new Double[] { 0.4, 0.5, 0.6 }));
		timeseriescontents.put("p2", Arrays.asList(new Double[] { 0.4, 0.5, 0.6 }));
		timeseriescontents.put("p3", "1");
		parameterNames.put("chp1", timeseriescontents);
		parameterNames.put("chp2", timeseriescontents);
		year0.getTables().put("pv_property", parameterNames);
		parameterNames = new HashMap<>();
		parameterNames.put("chp1", timeseriescontents);
		parameterNames.put("chp2", timeseriescontents);
		year0.getTables().put("chp_property", parameterNames);
	}
}
