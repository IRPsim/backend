package de.unileipzig.irpsim.core.simulation.data.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * POJO für GAMS-Daten in dem für die UI gut nutzbaren Format.
 * 
 * @author reichelt
 */
@Deprecated
public class JSONParametersSingleModel extends JSONParameters {

   @JsonInclude(Include.NON_NULL)
	private UserDefinedDescription description = new UserDefinedDescription();

	public final UserDefinedDescription getDescription() {
		return description;
	}

	public final void setDescription(final UserDefinedDescription description) {
		this.description = description;
	}

}
