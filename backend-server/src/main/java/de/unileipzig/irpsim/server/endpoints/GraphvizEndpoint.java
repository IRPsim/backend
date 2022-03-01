package de.unileipzig.irpsim.server.endpoints;

import com.fasterxml.jackson.core.JsonParseException;
import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParameters;
import de.unileipzig.irpsim.server.data.Responses;
import de.unileipzig.irpsim.server.modelstart.JavaModelStarter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static de.unileipzig.irpsim.server.data.Responses.badRequestResponse;

/**
 * Beinhaltet die Schnittstelen um dem Frontend IRPACT-Graphen zur Verfügung zu stellen.
 *
 * @author kluge
 */
@Path("graphviz")
@Api(value = "/graphviz", tags = "Graphviz", description = "Beinhaltet die Graphviz Ansteuerung für IRPACT Modelle in der von der UI benötigten Form.")
public class GraphvizEndpoint {

   private static final Logger LOG = LogManager.getLogger(GraphvizEndpoint.class);

   @POST
   @Path("/initGraphPNG")
   @Produces("image/png")
   @Consumes(MediaType.APPLICATION_JSON)
   @ApiOperation(value = "Liefert Graph zurück", notes = "Initialer Graph eines IRPact Scenarios.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
         @ApiResponse(code = 400, message = "Bad Request") })
   public Response getGraph(final String optimisationScenario) {
      LOG.debug("generiere Graph für Szenario: " + optimisationScenario.substring(0, 500));
      try {
         JSONParameters model = Constants.MAPPER.readValue(optimisationScenario, JSONParameters.class);
         // TODO find better file name (a non overridable one)
         File inputJsonFile = new File("/tmp/input_test.json");
         Constants.MAPPER.writeValue(inputJsonFile, model);
         String imageFile = "/tmp/initImage.png";
         String[] command = buildImageGenerationCommand(inputJsonFile, imageFile);

         // TODO better location and filename needed for image generation log
         File outputFile = new File("/tmp/debugGraphVizOut.log");

         generateImage(command, outputFile);

         File outputImageFile = new File(imageFile);
         LOG.debug("Outputfile exists: {}", outputImageFile.exists());

         BufferedImage bufferedImage = ImageIO.read(outputImageFile);
         byte[] imageBytes = toByteArray(bufferedImage, "png");

         return Response.ok(imageBytes).build();
      } catch (JsonParseException e ){
         e.printStackTrace();
         return Responses.errorResponse("No valid model");
      } catch (IOException e) {
         e.printStackTrace();
         return Responses.errorResponse("Unable to write file.");
      } catch (InterruptedException e) {
         e.printStackTrace();
         return Responses.errorResponse("Process was interrupted");
      }

   }

   /**
    * Builds the command to generate the image with help of the irpact jar.
    *
    * @param inputJsonFile File with location an name of the json Inputfile
    * @param imageFile     File location where the image will be saved
    * @return commands for ProcessBuilder as String Array
    */
   private String[] buildImageGenerationCommand(File inputJsonFile, String imageFile) {
      String JAR_NAME = JavaModelStarter.getJarName();
      File MODEL_PATH = JavaModelStarter.getModelPath();
      File jar = new File(MODEL_PATH, JAR_NAME);

      String[] command = new String[] { "java", "-jar",
            jar.getAbsolutePath(),
            "-i", inputJsonFile.getAbsolutePath(),
            "-o /tmp/act_init_graph_output.json",
            "--image", imageFile,
            "--noSimulation" };

      LOG.debug("Command line: {}", java.util.Arrays.toString(command));
      LOG.debug("{} exists {}", MODEL_PATH, MODEL_PATH.exists());
      LOG.debug("{} exists {}", jar.getAbsolutePath(), jar.exists());
      LOG.debug("{} exists {}", inputJsonFile, inputJsonFile.exists());
      return command;
   }

   /**
    * Runs the image generation process
    *
    * @param command String Array with arguments for the processBuilder
    * @param outputFile File with output name and location
    * @throws IOException default exception
    * @throws InterruptedException default exception
    */
   private void generateImage(String[] command, File outputFile) throws IOException, InterruptedException {
      final ProcessBuilder pb = new ProcessBuilder(command);

      pb.redirectErrorStream(true);
      pb.redirectOutput(outputFile);

      Process process;
      process = pb.start();

      LOG.info("graphviz Called");
      process.waitFor();
      int lastExitCode = process.exitValue();

      LOG.debug("Last Exit Code for image generation{}", lastExitCode);
   }

   @GET
   @Path("/initGraph/{id}")
   @Produces("image/png")
   @ApiOperation(value = "Liefert den initial Graph für ein IRPACT-Scenario zurück"
         , notes = "Initialer Graph eines IRPact Scenarios.")
   @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")
         , @ApiResponse(code = 400, message = "Bad Request") })
   public Response getInitGraph(@PathParam("id") int ScenarioId) {
      try {
         byte[] imageBytes = loadImagesString();
         return Response.ok(imageBytes).build();
      } catch (IOException e) {
         e.printStackTrace();
         return badRequestResponse("Image not found");
      } catch (Exception e) {
         e.printStackTrace();
         return badRequestResponse("Error");
      }

   }

   private byte[] loadImagesString() throws IOException {
      Class c = getClass();
      ClassLoader cl = c.getClassLoader();
      URL resource = cl.getResource("demo.png");
      final BufferedImage image = ImageIO.read(resource);

      return toByteArray(image, "png");
   }


   private byte[] toByteArray(BufferedImage bImage, String format) throws IOException {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      ImageIO.write(bImage, format, stream);
      return stream.toByteArray();
   }
}
