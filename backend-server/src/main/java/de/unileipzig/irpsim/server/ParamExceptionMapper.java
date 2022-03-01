package de.unileipzig.irpsim.server;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ParamException;

import de.unileipzig.irpsim.server.data.Responses;

@Provider
public class ParamExceptionMapper implements ExceptionMapper<ParamException> {
	@Override
	public Response toResponse(final ParamException exception) {
		return Responses.badRequestResponse("Parameter " + exception.getParameterName() + " hat inkorrekten Typ. Erwartet: " + exception.getParameterType());
	}
}
