package de.unileipzig.irpsim.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersSingleModel;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.utils.ParameterBaseDependenciesUtil;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.models.ModelInformation;
import de.unileipzig.irpsim.utils.transformer.ExtendedScenarioBuilder;
import de.unileipzig.irpsim.utils.transformer.GAMSParserCaller;

public class JavaTransformer {

   private final File modelDir;
   private final ModelInformation metadata;
   
   public JavaTransformer(File folder, ModelInformation metadata) throws IOException {
      this.metadata = metadata;
      modelDir = new File(folder, "src/main/resources/");
   }

   public void transform() throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      final String source = modelDir.getAbsolutePath();
      final String outputDependenciesName = "../backend-core/src/main/resources/gams-dependencies-" + metadata.getId() + ".json";
      GAMSParserCaller.createBackendDependencies(source, outputDependenciesName);
      ParameterBaseDependenciesUtil.getInstance().loadDependencies(new File(outputDependenciesName), metadata.getId());
      GAMSParserCaller.createFrontendData(source, Constants.SERVER_MODULE_PATH + "src/main/resources/modeldefinitions/" + metadata.getId() + ".json");

      createScenarioFiles();
   }

   private void createScenarioFiles() throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      File scenarioFile = new File(modelDir, "scenarios" + File.separator + "default.json");

      JSONParametersSingleModel jsonparameters = new ObjectMapper().readValue(scenarioFile, JSONParametersSingleModel.class);
      
      JSONParametersMultimodel result = new JSONParametersMultimodel();
      
      JSONParameters model = new JSONParameters();
      result.getModels().add(model);
      model.setYears(jsonparameters.getYears());
      
      List<YearData> parametersNew = new LinkedList<>();
      parametersNew.add(model.getYears().get(0));
      model.setYears(parametersNew);
      
      for (YearData year : model.getYears()) {
         year.getConfig().setModeldefinition(metadata.getId());
      }
      
      final BackendParametersMultiModel backendparameters = new BackendParametersMultiModel(result); 
      
      backendparameters.getDescription().setCreator(TransformConstants.DEFAULTSCENARIO_CREATOR);
      backendparameters.getDescription().setBusinessModelDescription(TransformConstants.DEFAULTSCENARIO_YEAR_TITLE + metadata.getName());

      File parameterFileJSON = new File(TransformConstants.SCENARIO_FOLDER, metadata.getId() + ".json");
      ExtendedScenarioBuilder.MAPPER.writeValue(parameterFileJSON, backendparameters.createJSONParameters());
      ExtendedScenarioBuilder.MAPPER.writeValue(new File(TransformConstants.SCENARIO_FOLDER, metadata.getId() + "_full.json"), backendparameters.createJSONParameters());
   
      final File baseFile = TestFiles.IRPACT.make();
      FileUtils.copyFile(parameterFileJSON, baseFile);

      final ExtendedScenarioBuilder builder2 = new ExtendedScenarioBuilder(parameterFileJSON);
      builder2.addMirroredYear(0).changeYearDate(1, 2016).addMirroredYear(0).changeYearDate(2, 2017).save(TestFiles.IRPACT_MULTIPLE_YEAR.make());

   }
}
