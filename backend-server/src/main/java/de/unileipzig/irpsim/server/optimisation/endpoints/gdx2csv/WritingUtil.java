package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.ws.rs.core.StreamingOutput;

import de.unileipzig.irpsim.core.utils.StreamGobbler;

public class WritingUtil {
   public static Process runGnuplotProcess(File tempFolder, String gnuPlot) throws IOException, InterruptedException {
      ProcessBuilder processBuilder = new ProcessBuilder("gnuplot", gnuPlot);
      processBuilder.environment().put("LD_LIBRARY_PATH", "");
      processBuilder.directory(tempFolder);
      Process process = processBuilder.start();
      StreamGobbler.showFullProcess(process);
      process.waitFor(10, TimeUnit.SECONDS);
      return process;
   }

   public static byte[] getImageData(File tempFolder) throws IOException {
      File outputFile = new File(tempFolder, "download.png");
      BufferedImage image = ImageIO.read(outputFile);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      byte[] imageData = baos.toByteArray();
      return imageData;
   }

   public static File createFiles(final String data, final String gnuPlot) throws IOException, FileNotFoundException {
      final InputStream plotScriptSource = GDXCSVEndpoint.class.getResourceAsStream("/visualisierungen/" + gnuPlot);
      File tempFolder = Files.createTempDirectory("visualization").toFile();

      File downloadCSV = new File(tempFolder, "download.csv");
      try (PrintWriter writer = new PrintWriter(downloadCSV)) {
         writer.write(data);
      }

      File plotfile = new File(tempFolder, gnuPlot);
      try (FileOutputStream writer = new FileOutputStream(plotfile)) {
         plotScriptSource.transferTo(writer);
      }
      return tempFolder;
   }

   public static StreamingOutput writeZIP(final String data, final InputStream plotFile) {
      StreamingOutput streamingOutput = outputStream -> {
         try {
            ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));
            createCSV(data, zipOut);
            createPlotscript(plotFile, zipOut);

            zipOut.close();
            outputStream.flush();
            outputStream.close();
         } catch (Throwable t) {
            t.printStackTrace();
         }
      };
      return streamingOutput;
   }

   private static void createCSV(final String data, ZipOutputStream zipOut) throws IOException {
      ZipEntry zipEntry = new ZipEntry("download.csv");
      zipOut.putNextEntry(zipEntry);
      zipOut.write(data.getBytes(), 0, data.getBytes().length); // you can set the data from another input stream
      zipOut.closeEntry();
   }

   private static void createPlotscript(final InputStream plotFile, ZipOutputStream zipOut) throws IOException {
      ZipEntry skriptEntry = new ZipEntry("plot.plt");
      zipOut.putNextEntry(skriptEntry);
      byte[] dataBlock = new byte[1024];
      int count = plotFile.read(dataBlock, 0, 1024);
      while (count != -1) {
         zipOut.write(dataBlock, 0, count);
         count = plotFile.read(dataBlock, 0, 1024);
      }
      zipOut.closeEntry();
   }
}
