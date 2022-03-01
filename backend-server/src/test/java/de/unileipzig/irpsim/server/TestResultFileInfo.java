package de.unileipzig.irpsim.server;

import java.io.File;

import org.junit.Test;

import de.unileipzig.irpsim.server.optimisation.ResultFileInfo;

public class TestResultFileInfo {
   
   @Test
   public void testResultFileInfoName() {
      File testFile = new File("a0.ii11521.p1.output_balancing_customermdl.csv");
      ResultFileInfo test1 = new ResultFileInfo(testFile);
      
      File testFileVeriflex = new File("a0.ii1.output.csv");
      ResultFileInfo testVeriflex = new ResultFileInfo(testFileVeriflex);
   }
   
   
   @Test(expected = RuntimeException.class)
   public void testWrongName() {
      File testFile = new File("a0.ii11521.p1.output_balancing_gehtnichtmodel.csv");
      ResultFileInfo test1 = new ResultFileInfo(testFile);
   }
}
