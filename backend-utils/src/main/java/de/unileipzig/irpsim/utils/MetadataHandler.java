package de.unileipzig.irpsim.utils;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.models.ModelInformation;
import de.unileipzig.irpsim.models.ModelInformations;

public class MetadataHandler {

   private static final Logger LOG = LogManager.getLogger(MetadataHandler.class);

   private static final File MODELS_FILE = new File(Constants.SERVER_MODULE_PATH + "src/main/resources/models.json");

   private final File metaInformationFile;
   private final ModelInformations currentInformation;

   public MetadataHandler(File metaInformationFile) throws JsonParseException, JsonMappingException, IOException {
      this.metaInformationFile = metaInformationFile;
      if (MODELS_FILE.exists()) {
         currentInformation = ModelInformations.deserializeData(MODELS_FILE);
      } else {
         currentInformation = new ModelInformations();
      }
   }

   public ModelInformation handleMetadata(String version) throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      ModelInformation metadata = Constants.MAPPER.readValue(metaInformationFile, ModelInformation.class);
      metadata.setVersion(version);

      checkModelId(metadata);

      updateModelInformation(metadata);
      updateCombinedModel();

      ModelInformations.serializeData(currentInformation, MODELS_FILE);
      return metadata;
   }

   private void updateCombinedModel() {
      ModelInformation irpoptInformation = findInformationById(1);
      ModelInformation irpactInformation = findInformationById(3);
      ModelInformation combinedModelInformation = findInformationById(5);

      if (irpoptInformation != null && irpactInformation != null) {
         if (combinedModelInformation == null) {
            combinedModelInformation = new ModelInformation();
            combinedModelInformation.setId(5);
            combinedModelInformation.setName("opt-act");
            combinedModelInformation.setDescription("Combination of IRPopt and IRPact");
            combinedModelInformation.setSubmodels(new int[] { 1, 3 });
            combinedModelInformation.setType("COMBINED");
            combinedModelInformation.setVersion(irpoptInformation.getVersion() + " - " + irpactInformation.getVersion());
            currentInformation.getModelInformations().add(combinedModelInformation);
         } else {
            combinedModelInformation.setVersion(irpoptInformation.getVersion() + " - " + irpactInformation.getVersion());
         }
      }
   }

   private void updateModelInformation(ModelInformation metadata) {
      ModelInformation oldInformation = findInformationById(metadata.getId());

      if (oldInformation != null) {
         LOG.info("Model already existing");
         oldInformation.setDescription(metadata.getDescription());
         oldInformation.setName(metadata.getName());
         oldInformation.setType(metadata.getType());
         oldInformation.setVersion(metadata.getVersion());
      } else {
         LOG.info("Adding model");
         currentInformation.getModelInformations().add(metadata);
      }
   }

   private ModelInformation findInformationById(int id) {
      ModelInformation oldInformation = null;
      for (ModelInformation information : currentInformation.getModelInformations()) {
         if (information.getId() == id) {
            oldInformation = information;
         }
      }
      return oldInformation;
   }

   private void checkModelId(ModelInformation metadata) {
      if (metadata.getId() < 1) {
         throw new RuntimeException("Only id > 0 is allowed!");
      }
      if (metadata.getId() == 1 && !metadata.getName().equals("IRPopt")) {
         throw new RuntimeException("Model 1 needs to be IRPopt!");
      }
      if (metadata.getId() == 3 && !metadata.getName().equals("IRPact")) {
         throw new RuntimeException("Model 3 needs to be IRPact!");
      }
      if (metadata.getId() == 5) {
         throw new RuntimeException("Model 3 needs to be opt-act and should not be defined manually!");
      }
   }
}
