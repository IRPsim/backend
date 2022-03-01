package de.unileipzig.irpsim.models;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.Constants;

public class ModelInformations {
   
   @JsonFormat(shape=JsonFormat.Shape.ARRAY)
   private List<ModelInformation> modelInformations = new LinkedList<>();

   public List<ModelInformation> getModelInformations() {
      return modelInformations;
   }

   public void setModelInformations(List<ModelInformation> modelInformations) {
      this.modelInformations = modelInformations;
   }
   
   public static ModelInformations deserializeData(String data) throws JsonMappingException, JsonProcessingException {
      ModelInformations infos = new ModelInformations();
      List<?> value = Constants.MAPPER.readValue(data, List.class);
      List<ModelInformation> value2 = Constants.MAPPER.convertValue(value, new TypeReference<List<ModelInformation>>() {});
      infos.setModelInformations(value2);
      return infos;
   }
   
   public static ModelInformations deserializeData(File file) throws JsonParseException, JsonMappingException, IOException {
      ModelInformations infos = new ModelInformations();
      List<?> value = Constants.MAPPER.readValue(file, List.class);
      List<ModelInformation> value2 = Constants.MAPPER.convertValue(value, new TypeReference<List<ModelInformation>>() {});
      infos.setModelInformations(value2);
      return infos;
   }
   
   public static String serializeData(ModelInformations infos) throws JsonProcessingException {
      return Constants.MAPPER.writeValueAsString(infos.getModelInformations());
   }

   public static void serializeData(ModelInformations infos, File modelsFile) throws JsonGenerationException, JsonMappingException, IOException {
      Constants.MAPPER.writeValue(modelsFile, infos.getModelInformations());
   }
}
