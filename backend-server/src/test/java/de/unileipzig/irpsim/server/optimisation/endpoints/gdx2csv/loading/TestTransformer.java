package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading;


import java.util.stream.DoubleStream;

import org.junit.Assert;
import org.junit.Test;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.ParameterdataWriter;

public class TestTransformer {
   @Test
   public void testViertelstunde() throws Exception {
      double[] timeSeries = getTimeSeries();
      double[] viertelstundeTransformed = Transformer.transform(ParameterdataWriter.VIERTELSTUNDENWERTE, timeSeries);
      Assert.assertEquals(0, viertelstundeTransformed[0], 0);
      Assert.assertEquals(5, viertelstundeTransformed[5], 0);
      Assert.assertEquals(35039, viertelstundeTransformed[35039], 0);
      Assert.assertEquals(613883280L, DoubleStream.of(viertelstundeTransformed).sum(), 0);
   }

   @Test
   public void testSumme() throws Exception {
      double[] timeSeries = getTimeSeries();
      double[] summeTransformed = Transformer.transform(ParameterdataWriter.SUMME, timeSeries);
      Assert.assertEquals(613883280L, summeTransformed[0], 0);
   }

   @Test
   public void testDurchschnitt() throws Exception {
      double[] timeSeries = getTimeSeries();
      double[] durchschnittTransformed = Transformer.transform(ParameterdataWriter.DURCHSCHNITT, timeSeries);
      Assert.assertEquals(17519.5, durchschnittTransformed[0], 0);
   }

   @Test
   public void testStundenDurchschnitt() throws Exception {
      double[] timeSeries = getTimeSeries();
      double[] stundenDurchschnittTransformed = Transformer.transform(ParameterdataWriter.STUNDEN_DURCHSCHNITT, timeSeries);
      Assert.assertEquals(1.5, stundenDurchschnittTransformed[0], 0);
      Assert.assertEquals(5.5, stundenDurchschnittTransformed[1], 0);
      Assert.assertEquals(35037.5, stundenDurchschnittTransformed[8759], 0);
      Assert.assertEquals(153470820L, DoubleStream.of(stundenDurchschnittTransformed).sum(), 0);
   }

   @Test
   public void testDurchschnittsJahreswoche() throws Exception {
      double[] timeSeries = getTimeSeries();
      double[] transformed = Transformer.transform(ParameterdataWriter.DURCHSCHNITTS_JAHRESWOCHE, timeSeries);
      Assert.assertEquals(17472, transformed[0], 0);
      Assert.assertEquals(17567, transformed[95], 0);
      Assert.assertEquals(17232, transformed[96], 0);
      Assert.assertEquals(17807, transformed[671], 0);
      Assert.assertEquals(17472*96 + 4560 + 17232*576 + 165600, DoubleStream.of(transformed).sum(), 0);
   }

   private double[] getTimeSeries() {
      double[] timeSeries = new double[35040];
      for (int i= 0; i < 35040; i++) {
         timeSeries[i] = i;
      }
      return timeSeries;
   }
}
