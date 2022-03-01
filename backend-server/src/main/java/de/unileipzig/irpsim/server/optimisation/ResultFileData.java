package de.unileipzig.irpsim.server.optimisation;

import java.util.ArrayList;
import java.util.List;

public class ResultFileData {
   private final String header;
   private final List<String> lines;
   
   public ResultFileData(String header, int savelength) {
      this.header = header;
      lines = new ArrayList<>(savelength + 1);
   }

   public String getHeader() {
      return header;
   }

   public List<String> getLines() {
      return lines;
   }
   
}
