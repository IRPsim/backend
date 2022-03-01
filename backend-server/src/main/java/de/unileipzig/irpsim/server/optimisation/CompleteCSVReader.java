package de.unileipzig.irpsim.server.optimisation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompleteCSVReader {

   private static final Logger LOG = LogManager.getLogger(CompleteCSVReader.class);

   public File readCompleteCSV(final File currentWorkspace, final List<ResultFileInfo> fileinfos) {
      final File returnFile = new File(currentWorkspace, "/output/totalresults.csv");
      if (returnFile.getParentFile().exists()) {
         try (BufferedWriter bw = new BufferedWriter(new FileWriter(returnFile))) {
            final Map<Integer, List<ResultFileInfo>> fileInfoMap = OptimisationJobUtils.createFileInfoMap(fileinfos);
            boolean alreadyWrittenHeaderLine = false;
            for (final Entry<Integer, List<ResultFileInfo>> info : fileInfoMap.entrySet()) {
               LOG.debug("Lese für {}", info.getKey());
               info.getValue().stream().forEach(val -> LOG.trace(val.getFile() + " " + val.getIndex()));
               final BufferedReader[] readers = openFiles(info);
               alreadyWrittenHeaderLine = readFiles(bw, alreadyWrittenHeaderLine, readers);

               closeFiles(readers);
            }
         } catch (final IOException e1) {
            e1.printStackTrace();
         }
         return returnFile;
      } else {
         return null;
      }
   }

   private void closeFiles(final BufferedReader[] readers) throws IOException {
      for (BufferedReader reader : readers) {
         reader.close();
      }
   }

   private BufferedReader[] openFiles(final Entry<Integer, List<ResultFileInfo>> info) throws FileNotFoundException {
      final BufferedReader[] readers = new BufferedReader[info.getValue().size()];

      int i = 0;
      for (final ResultFileInfo fileinfo : info.getValue()) {
         readers[i] = new BufferedReader(new FileReader(fileinfo.getFile()));
         i++;
      }
      return readers;
   }

   private boolean readFiles(BufferedWriter bw, boolean alreadyWrittenHeaderLine, final BufferedReader[] readers) throws IOException {
      boolean finished = false;
      while (!finished) {
         int i = 0;
         boolean somethingWasWritten = false;
         for (final BufferedReader br : readers) {
            final String line = br.readLine();
            if (line == null) {
               finished = true;
            } else {
               if (line.length() > 3) {
                  final String shortenedLine = line.replace(" ", "");
                  if (line.startsWith("#")) {
                     if (!alreadyWrittenHeaderLine) {
                        if (i == readers.length - 1) {
                           alreadyWrittenHeaderLine = true;
                        }
                        bw.write(shortenedLine);
                        somethingWasWritten = true;
                     }
                  } else {
                     bw.write(shortenedLine);
                     somethingWasWritten = true;
                  }
               }
            }
            i++;
         }
         if (somethingWasWritten) {
            bw.write("\n");
         }
      }
      return alreadyWrittenHeaderLine;
   }
}
