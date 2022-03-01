package de.unileipzig.irpsim.core.simulation.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersSingleModel;
import de.unileipzig.irpsim.core.simulation.data.json.UserDefinedDescription;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;

/**
 * GAMS-Parameter in für Backend gut lesbarer Form.
 * 
 * @author reichelt
 * @deprecated Always use BackendParametersMultiModel
 */
@Deprecated()
public class BackendParameters implements GenericParameters {

	private UserDefinedDescription description = new UserDefinedDescription();
	private BackendParametersYearData[] yeardata;
	private PostProcessing postprocessing;

	/**
	 * Erzeugt neue BackendParameterinstanz aus Daten im Format {@link JSONParametersSingleModel}. Dabei werden für jedes Jahr die {@link BackendParametersYearData}
	 * aus den entsprechenden {@link YearData} gebildet und ebenfalls alle weiteren Daten übernommen.
	 * 
	 * @param gpjson Die Daten im Format {@link JSONParametersSingleModel}
	 */
	public BackendParameters(final JSONParametersSingleModel gpjson) {
		description = gpjson.getDescription();
		yeardata = new BackendParametersYearData[gpjson.getYears().size()];
		for (int i = 0; i < yeardata.length; i++) {
			YearData year = gpjson.getYears().get(i);
			if (year != null) {
			   if (year.getConfig().getOptimizationlength() < year.getConfig().getSavelength()) {
	            throw new RuntimeException("Optimizationlenth (" + year.getConfig().getOptimizationlength() + 
	                  ") was smaller than savelength (" + year.getConfig().getSavelength() + ")");
	         }
				yeardata[i] = new BackendParametersYearData(year);
			}
		}
		postprocessing = gpjson.getPostprocessing();
	}

	/**
	 * Erstellt BackendParameters mit der angegebenen Jahresanzahl.
	 * 
	 * @param years Jahresanzahl
	 */
	public BackendParameters(final int years) {
		yeardata = new BackendParametersYearData[years];
		// for (int year = 0; year < years; year++) {
		// yeardata[year] = new BackendParametersYearData();
		// }
	}

	/**
	 * Erzeugt GAMS-Daten.
	 * 
	 * @return Die erzeugten JSON-Parameter als GAMSParametersJSON
	 */
	public final JSONParametersSingleModel createJSONParameters() {
		JSONParametersSingleModel gpjson = new JSONParametersSingleModel();

		List<YearData> years = new ArrayList<YearData>(yeardata.length);

		for (int i = 0; i < yeardata.length; i++) {
			if (yeardata[i] != null) {
				years.add(yeardata[i].createJSONParameters());
			} else {
				years.add(null);
			}
		}
		gpjson.setYears(years);

		gpjson.setPostprocessing(postprocessing);
		gpjson.setDescription(description);

		return gpjson;
	}

	public final UserDefinedDescription getDescription() {
		return description;
	}

	public final void setDescription(final UserDefinedDescription description) {
		this.description = description;
	}

	/**
	 * Liefert BackendParameterYearData.
	 * 
	 * @return Die Jahresdaten als BackendParameterYearData[]
	 */
	public final BackendParametersYearData[] getYeardata() {
		return yeardata;
	}

	/**
	 * Gibt die Gesamtlänge der Simulation über alle Jahre zurück.
	 * 
	 * @return Gesamtlänge der Simulation über alle Jahre
	 */
	@JsonIgnore
	public final int fetchSimulationLength() {
		int simulationYears = 0;
		for (BackendParametersYearData year : yeardata) {
			simulationYears += year == null ? 0 : 1;
		}
		return simulationYears * yeardata[0].getConfig().getSimulationlength();
	}

	public final PostProcessing getPostprocessing() {
		return postprocessing;
	}

	public final void setPostprocessing(final PostProcessing postprocessing) {
		this.postprocessing = postprocessing;
	}

   @Override
   public List<BackendParametersYearData> getAllYears() {
      List<BackendParametersYearData> years = new LinkedList<>();
      for (BackendParametersYearData year : yeardata) {
         years.add(year);
      }
      return years;
   }
}
