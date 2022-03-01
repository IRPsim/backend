package de.unileipzig.irpsim.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/**
 * Ensure that all JSON responses have an explicit UTF-8 annotation added to their "Content-Type" header.
 *
 * @author sdienst
 */
@Provider
public final class CharsetResponseFilter implements ContainerResponseFilter {

	@Override
	public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) throws IOException {

		// MediaType contentType = responseContext.getMediaType();
		final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
		final List<Object> cts = headers.get("Content-Type");
		if (cts != null && cts.size() > 0 && MediaType.APPLICATION_JSON_TYPE.equals(cts.get(0))) {
			headers.put("Content-Type", Arrays.asList(MediaType.APPLICATION_JSON + ";charset=UTF-8"));
		}
	}

}