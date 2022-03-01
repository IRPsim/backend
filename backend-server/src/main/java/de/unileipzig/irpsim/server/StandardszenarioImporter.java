package de.unileipzig.irpsim.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.simulationparameters.OptimisationScenario;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.server.data.modeldefinitions.ModelDefinitionsEndpoint;
import de.unileipzig.irpsim.server.data.simulationparameters.MultiScenarioImportHandler;
import de.unileipzig.irpsim.server.data.simulationparameters.ScenarioEndpoint;

public class StandardszenarioImporter {

   public static final Logger LOG = LogManager.getLogger(StandardszenarioImporter.class);

   /**
    * Initialisiert die Datenbank. Anmerkung: Deletable ist aus Sicht des Anwenders gesetzt, da dieser nur die Möglichkeit haben soll, die von Benutzerseite aus erstellten
    * Szenarien zu löschen, nicht jedoch die vom System erstellten. Andererseits der Server soll die von Benutzern erstellten Szenarien nicht löschen können, aber die automatisch
    * erstellten.
    */
   public void initializeScenarios() {

      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {

         final EntityTransaction et = em.getTransaction();

         et.begin();
         final TypedQuery<OptimisationScenario> oldDataQuery = em.createQuery(
               "SELECT s FROM " + OptimisationScenario.class.getSimpleName() + " s WHERE s.deletable=false",
               OptimisationScenario.class);
         final List<OptimisationScenario> oldScenarios = oldDataQuery.getResultList();
         em.flush();

         final String scenario = "scenarios";
         final String[] fileNames = ModelDefinitionsEndpoint.getResourceListing(ServerStarter.class, scenario);
         for (final String filename : fileNames) {
            LOG.debug("Importiere: {}", filename);
            final String text = readScenarioText(scenario, filename);
            initializeScenario(em, oldScenarios, text);

         }
         for (final OptimisationScenario oldScenario : oldScenarios) {
            LOG.debug("Lösche: {}", oldScenario.getId());
            em.remove(oldScenario);
         }
         et.commit();
         LOG.debug("Datenimport erfolgt");
         System.gc();
      } catch (final URISyntaxException | IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   private void initializeScenario(final ClosableEntityManager em, final List<OptimisationScenario> oldScenarios, final String text)
         throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, InterruptedException {
      final OptimisationScenario currentScenario = new OptimisationScenario();

      final JSONParametersMultimodel gpj = createStandardszenario(text, currentScenario);

      importYears(em, oldScenarios, currentScenario, gpj);

      /*
      if (gpj.fetchConfig().getSimulationlength() < ScenarioImportHandler.MIN_REFERENCING_SIZE) {
         currentScenario.setData(text);
         em.persist(currentScenario);
      } else {
         importFullYear(em, oldScenarios, currentScenario, gpj);
      }
         importYears(em, oldScenarios, currentScenario, gpj);
      }
      */
   }

   private String readScenarioText(final String scenario, final String filename) {
      final InputStream is = ScenarioEndpoint.class.getResourceAsStream("/" + scenario + "/" + filename);
      final Scanner scanner = new Scanner(is, "UTF-8");
      final String text = scanner.useDelimiter("\\Z").next();
      scanner.close();
      return text;
   }

   private void importYears(final ClosableEntityManager em, final List<OptimisationScenario> oldScenarios, final OptimisationScenario currentScenario,
         final JSONParametersMultimodel gpj) throws JsonProcessingException, InterruptedException {
      OptimisationScenario old = findOldScenario(oldScenarios, currentScenario);
      if (old == null) {
         importFullYear(em, currentScenario, gpj);
      } else {
         // Wenn die Version gleich geblieben ist, muss keine Aktualisierung stattfinden
         String currentModelVersion = ModelDefinitionsEndpoint.getModelVersion(old.getModeldefinition());
         if (old.getVersion() == null || !old.getVersion().equals(currentModelVersion)) {
            updateFullYear(em, currentScenario, gpj, old, currentModelVersion);
         }
         oldScenarios.remove(old);

      }
   }

   private void updateFullYear(final ClosableEntityManager em, final OptimisationScenario currentScenario, final JSONParametersMultimodel gpj, OptimisationScenario old,
         String currentModelVersion) throws InterruptedException, JsonProcessingException {
      LOG.info("Update necessary: {} {}", old.getVersion(), currentModelVersion);
      old.setCreator(currentScenario.getCreator());
      old.setDate(currentScenario.getDate());
      old.setDeletable(currentScenario.isDeletable());
      old.setVersion(currentModelVersion);
      MultiScenarioImportHandler importHandler = new MultiScenarioImportHandler(gpj, 1, true);
      JSONParametersMultimodel referencedData = importHandler.handleTimeseries();
      final String writeValueAsString = Constants.MAPPER.writeValueAsString(referencedData);
      old.setData(writeValueAsString);
      LOG.debug("Import: {}", writeValueAsString.substring(0, 100));
      em.merge(old);
   }

   private void importFullYear(final ClosableEntityManager em, final OptimisationScenario currentScenario, final JSONParametersMultimodel gpj)
         throws InterruptedException, JsonProcessingException {
      MultiScenarioImportHandler importHandler = new MultiScenarioImportHandler(gpj, 1, true);
      JSONParametersMultimodel referencedData = importHandler.handleTimeseries();
      final String data = Constants.MAPPER.writeValueAsString(referencedData);
      currentScenario.setData(data);
      em.persist(currentScenario);
   }

   private OptimisationScenario findOldScenario(final List<OptimisationScenario> oldScenarios,
         final OptimisationScenario currentScenario) {
      OptimisationScenario old = null;
      for (final OptimisationScenario oldEach : oldScenarios) {
         System.out.println(currentScenario.getName().equals(oldEach.getName()));
         System.out.println(currentScenario.getDescription());
         System.out.println(oldEach.getDescription());
         System.out.println(currentScenario.getDescription().equals(oldEach.getDescription()));
         if (currentScenario.getName().equals(oldEach.getName())
               && currentScenario.getModeldefinition() == oldEach.getModeldefinition()
               && currentScenario.getDescription().equals(oldEach.getDescription())) {
            old = oldEach;
         }
      }
      return old;
   }

   private JSONParametersMultimodel createStandardszenario(final String text, final OptimisationScenario currentScenario)
         throws IOException, JsonParseException, JsonMappingException {
      final JSONParametersMultimodel gpj = Constants.MAPPER.readValue(text, JSONParametersMultimodel.class);
      currentScenario.setDate(new Date());
      currentScenario.setDeletable(false);
      currentScenario.setDescription(gpj.getDescription().getBusinessModelDescription());
      currentScenario.setName("Standardszenario");
      currentScenario.setCreator("System");
      if (gpj.getModels().size() == 1) {
         currentScenario.setModeldefinition(gpj.getModels().get(0).getYears().get(0).getConfig().getModeldefinition());
      } else {
         currentScenario.setModeldefinition(5); // currently fixed, only 5 is valid multimodel
      }

      currentScenario.setVersion(ModelDefinitionsEndpoint.getModelVersion(currentScenario.getModeldefinition()));
      return gpj;
   }
}
