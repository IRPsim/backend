package de.unileipzig.irpsim.server.data.timeseries;

import java.io.File;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;

public class TestSQLite {
	private final static File sqlite = new File("src/test/resources/example_sqlite.sql");

	@Test
	public void testSQLiteReading() {
		final GDXSQLiteData data = new GDXSQLiteData(7 * 96, sqlite);
		Assert.assertNotNull(data.getData());
		Assert.assertNotNull(data.getData().get("var_energyFlow"));
		Assert.assertTrue(data.getData().get("par_A_DES_PV").isScalar());
		Assert.assertFalse(data.getData().get("var_energyFlow").isScalar());
		Assert.assertTrue(data.getData().get("var_energyFlow").isContainsSetII0());
		System.out.println(data.getData().get("var_energyFlow").getDependencies());
	}

	@Test
	public void testValueReading() {
		final GDXSQLiteData data = new GDXSQLiteData(7 * 96, sqlite);
		double[] values = data.getValues("var_energyFlow", Arrays.asList(new String[] { "E", "EGrid", "load_E1" }));
		Assert.assertNotNull(values);
		Assert.assertNotNull(values[0]);
		Assert.assertEquals(672, values.length);
		System.out.println(Arrays.toString(values));
		
		double[] valuesNonSetII0 = data.getValues("par_out_IuO_ESector_OrgaSide", Arrays.asList(new String[] { "SMS" }));
      Assert.assertNotNull(valuesNonSetII0);
      Assert.assertNotNull(valuesNonSetII0[0]);
      Assert.assertEquals(672, valuesNonSetII0.length);
      System.out.println(Arrays.toString(valuesNonSetII0));
		
		
		
		double[] values2 = data.getValues("par_H_pss_techavail", Arrays.asList(new String[] { "tech_BGP1" }));
		Assert.assertNotNull(values2);
		Assert.assertNotNull(values2[0]);
		System.out.println(Arrays.toString(values2));
		
		
	}
}
