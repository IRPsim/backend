package de.unileipzig.irpsim.server.optimisation.comparators;

import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.DependencyType;
import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.GetDependencyType;
import org.junit.Assert;
import org.junit.Test;


public class TestDependencyTypeGetting {
   
   @Test
   public void testScalar() {
      DependencyType type = GetDependencyType.getDependencyType("sca_X_MS_DE_country", true, 1);
      Assert.assertEquals(type, DependencyType.SCALAR);
   }
   
   @Test
   public void testSetScalar() {
      DependencyType type = GetDependencyType.getDependencyType("par_S_DS", true, 1);
      Assert.assertEquals(type, DependencyType.SET_SCALAR);
   }
   
   @Test
   public void testSetTimeseries() {
      DependencyType type = GetDependencyType.getDependencyType("par_L_DS_E", true, 1);
      Assert.assertEquals(type, DependencyType.SET_TIMESERIES);
   }
   
   @Test
   public void testTableScalar() {
      DependencyType type = GetDependencyType.getDependencyType("par_SOH_pss_sector", true, 1);
      Assert.assertEquals(type, DependencyType.TABLE_SCALAR);
   }
   
   @Test
   public void testTableTimeseries() {
      DependencyType type = GetDependencyType.getDependencyType("par_F_E_EGrid_energy", true, 1);
      Assert.assertEquals(type, DependencyType.TABLE_TIMESERIES);
   }
}
