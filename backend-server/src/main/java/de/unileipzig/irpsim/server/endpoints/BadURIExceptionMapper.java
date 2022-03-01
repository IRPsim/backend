package de.unileipzig.irpsim.server.endpoints;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import de.unileipzig.irpsim.server.data.Responses;

/**
 * Jersey provider, fängt Fehler auf Anfragen ab, deren Ressourcen nicht gefunden wurden.
 */
@Provider
public class BadURIExceptionMapper implements ExceptionMapper<NotFoundException> {

	@Override
	public Response toResponse(final NotFoundException exception) {
		return Responses.notFoundResponse(exception);
	}
}
