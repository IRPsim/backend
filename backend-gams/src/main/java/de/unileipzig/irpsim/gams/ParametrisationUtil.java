package de.unileipzig.irpsim.gams;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;

public class ParametrisationUtil {
	/**
	 * @param gamshandler
	 *            Der zu parametrisierende {@link GAMSHandler}
	 * @param file
	 *            Die Parameterdatei, die Referenzen zu Zeitreihen in der DB enthält
	 */
	public static void parameterizeModel(final GAMSHandler gamshandler, final File file) throws TimeseriesTooShortException {
		final ObjectMapper om = new ObjectMapper();
		try {
			final JSONParametersMultimodel gp = om.readValue(file, JSONParametersMultimodel.class);
			final BackendParametersMultiModel parameters = new BackendParametersMultiModel(gp);
			final BackendParametersYearData yeardata = parameters.getModels()[0].getYeardata()[0];
         final GAMSModelParametrisationUtil gamsModelParametrisationUtil = new GAMSModelParametrisationUtil(gamshandler, yeardata, 0);
			gamsModelParametrisationUtil.loadParameters();
			gamsModelParametrisationUtil.parameterizeModel();
			
			gamshandler.expose();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
