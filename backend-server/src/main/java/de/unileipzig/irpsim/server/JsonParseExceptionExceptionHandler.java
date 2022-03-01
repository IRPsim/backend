package de.unileipzig.irpsim.server;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import com.fasterxml.jackson.core.JsonParseException;

import de.unileipzig.irpsim.server.data.Responses;

@Provider
public final class JsonParseExceptionExceptionHandler implements ExtendedExceptionMapper<JsonParseException> {
	@Override
	public Response toResponse(final JsonParseException exception) {
		exception.printStackTrace();
		return Responses.badRequestResponse("JSON nicht in das korrekte Format parsbar.");
	}

	@Override
	public boolean isMappable(final JsonParseException arg0) {
		return true;
	}
}