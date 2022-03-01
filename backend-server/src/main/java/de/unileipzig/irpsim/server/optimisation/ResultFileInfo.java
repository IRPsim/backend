package de.unileipzig.irpsim.server.optimisation;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents information about intermediary CSV-files of GAMS-models. The file names may have the following format:
 * 
 * year.timestep.customergroup.modeltype.csv (e.g. a0.ii11521.p1.output_balancing_customermdl) for IRPopt or complex models
 * 
 * or
 * 
 * year.timestep.output.csv (e.g. a0.ii1.output.csv) for Veriflex or simple models.
 * 
 * The information are used for ordering the files, which needs to be deterministic.
 * 
 * @author reichelt
 */
public class ResultFileInfo implements Comparable<ResultFileInfo> {

   private static final Logger LOG = LogManager.getLogger(ResultFileInfo.class);

   private final File file;
   private final int index;
   private final int timestep;

   /**
    * Initialisiert Fileinfos mit einer Zwischenergebnisdatei.
    * 
    * @param outputFile Zwischenergebnisdatei, aus der das Fileinfos-Objekt erstellt werden soll
    */
   public ResultFileInfo(final File outputFile) {
      this.file = outputFile;
      final String[] nameParts = outputFile.getName().split("\\.");
      final String iiNamePart = nameParts[1];
      timestep = Integer.valueOf(iiNamePart.replace("ii", ""));
      final int lastPartIndex = getLastPartIndex(outputFile, nameParts);

      LOG.trace("Name: " + outputFile.getName());
      if (nameParts.length == 5) {
         int custGroupHash = nameParts[2].hashCode() % 1000;
         index = ((timestep - 1) / 96) * 100000 + 10 * custGroupHash + lastPartIndex;
      } else if (nameParts.length == 4) {
         index = ((timestep - 1) / 96) * 100000 + lastPartIndex;
      } else {
         throw new RuntimeException(
               "Unerwartet: Falsche Unterteilung des Ausgabe-CSV-Namens, Name: " + outputFile.getName());
      }
      LOG.trace("Wert: " + index);

   }

   private int getLastPartIndex(final File outputFile, final String[] nameParts) {
      final int lastPartIndex;
      if (nameParts.length == 5) {
         lastPartIndex = getLastPartIndexIRPopt(outputFile, nameParts);
      } else {
         LOG.debug("5 Elements are only allowed for IRPopt-models; otherwise, the parts after year (a0) and timestep (ii1) are ignored");
         lastPartIndex = 0;
      }
      return lastPartIndex;
   }

   private int getLastPartIndexIRPopt(final File outputFile, final String[] nameParts) {
      final int lastPartIndex;
      String lastPart = nameParts[nameParts.length - 2];
      if (lastPart.startsWith("output_accounting")) {
         if (lastPart.endsWith("customermdl")) {
            lastPartIndex = 1;
         } else if (lastPart.endsWith("organizmdl")) {
            lastPartIndex = 2;
         } else {
            throw new RuntimeException("Unerwarteter Start (customdermdl vs. organizmdl), Name: " + outputFile.getName());
         }
      } else if (lastPart.startsWith("output_balancing")) {
         if (lastPart.endsWith("customermdl")) {
            lastPartIndex = 3;
         } else if (lastPart.endsWith("organizmdl")) {
            lastPartIndex = 4;
         } else {
            throw new RuntimeException("Unerwarteter Start (customdermdl vs. organizmdl), Name: " + outputFile.getName());
         }
      } else {
         throw new RuntimeException("Unerwarteter Start, Name: " + outputFile.getName());
      }
      return lastPartIndex;
   }

   /**
    * Gibt den Zeitschritt der Zwischenergebnisdatei innerhalb des Laufes zurück.
    * 
    * @return Nummer des Zeitschrittes
    */
   public final int getTimestep() {
      return timestep;
   }

   @Override
   public final int compareTo(final ResultFileInfo o) {
      return index - o.index;
   }

   /**
    * Gibt den Index der Zwischenergebnisdatei zurück, anhand derer sie sortiert wird.
    * 
    * @return Index der Zwischenergebnisdatei
    */
   public final int getIndex() {
      return index;
   }

   /**
    * Gibt die Zwischenergebnisdatei selbst zurück.
    * 
    * @return Zwischenergebnisdatei
    */
   public final File getFile() {
      return file;
   }

   @Override
   public final int hashCode() {
      return file.hashCode();
   }

   @Override
   public final boolean equals(final Object other) {
      if (other instanceof ResultFileInfo) {
         ResultFileInfo o = (ResultFileInfo) other;
         return o.file.equals(file);
      } else {
         return false;
      }
   }

}