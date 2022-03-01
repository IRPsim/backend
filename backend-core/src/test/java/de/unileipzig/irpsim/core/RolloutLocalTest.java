package de.unileipzig.irpsim.core;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.unileipzig.irpsim.core.simulation.data.TimeInterval;
import de.unileipzig.irpsim.core.standingdata.StammdatenUtil;

public class RolloutLocalTest {
	
   @Test
   public void testRolloutYearToHour() {
      final List<Double> values = new LinkedList<>();
      for (long i = 0; i < 12; i++) {
         values.add((double) i);
      }
      
      List<Double> rolledOut = StammdatenUtil.rolloutTimeseriesTo8760(TimeInterval.MONTH, values);
      Assert.assertEquals(8760, rolledOut.size());
   }
   
   @Test
   public void testRolloutQuarterHourToHour() {
      final List<Double> values = new LinkedList<>();
      for (long i = 0; i < 35040; i++) {
         values.add((double) i);
      }
      
      List<Double> rolledOut = StammdatenUtil.rolloutTimeseriesTo8760(TimeInterval.QUARTERHOUR, values);
      Assert.assertEquals(8760, rolledOut.size());
   }
   
   @Test
   public void testRolloutWeekToHour() {
      final List<Double> values = new LinkedList<>();
      for (long i = 0; i < 53; i++) {
         values.add((double) i);
      }
      
      List<Double> rolledOut = StammdatenUtil.rolloutTimeseriesTo8760(TimeInterval.WEEK, values);
      Assert.assertEquals(8760, rolledOut.size());
   }
   
	@Test
	public void testRolloutYear() {
		final List<Double> values = new LinkedList<>();
		for (long i = 0; i < 12; i++) {
			values.add((double) i);
		}
		
		List<Double> rolledOut = StammdatenUtil.rolloutTimeseriesTo35040(TimeInterval.MONTH, values);
		Assert.assertEquals(35040, rolledOut.size());
	}
	
	@Test
	public void testRolloutDay() {
		final List<Double> values = new LinkedList<>();
		for (long i = 0; i < 365; i++) {
			values.add((double) i);
		}
		
		List<Double> rolledOut = StammdatenUtil.rolloutTimeseriesTo35040(TimeInterval.DAY, values);
		Assert.assertEquals(35040, rolledOut.size());
	}
	
	@Test
	public void testRolloutWeek() {
		final List<Double> values = new LinkedList<>();
		for (long i = 0; i < 52; i++) {
			values.add((double) i);
		}
		
		List<Double> rolledOut = StammdatenUtil.rolloutTimeseriesTo35040(TimeInterval.WEEK, values);
		Assert.assertEquals(35040, rolledOut.size());
	}
}
