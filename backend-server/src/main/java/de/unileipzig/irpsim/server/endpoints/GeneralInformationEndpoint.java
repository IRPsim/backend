package de.unileipzig.irpsim.server.endpoints;

import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import de.unileipzig.irpsim.server.data.Responses;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Endpunkt für generelle Informationen über die aktuelle Backend-Version.
 * 
 * @author reichelt
 *
 */
@Path("generalinformation")
@Api(value = "/generalinformation", tags="Allgemeine Informationen", description = "Gibt generelle Informationen über die aktuelle Backendversion aus")
public class GeneralInformationEndpoint {

	/**
	 * Gibt die Git-Tags der aktuellen Modell- und Backendversion zurück.
	 * 
	 * @return Git-Tag der aktuellen Backenversion
	 */
	@Path("versions")
	@GET
	@ApiOperation(value = "Gibt die Git-Tags der aktuellen Modell- und Backendversion zurück.", notes = "Gibt die Git-Tags der aktuellen Modell-"
			+ " und Backendversion in einem JSON-Objekt zurück. Diese "
			+ "werden zur Buildzeit in das Projekt geschrieben und für die Ausgabe beim Aufruf des Services wieder ausgelesen.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	@Produces(MediaType.APPLICATION_JSON)
	public final Response getBackendVersion() {
		try {
			final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("git.properties");
			final Properties prop = new Properties();
			prop.load(inputStream);
//			final String modelversion = ModelDefinitionsEndpoint.getModelVersion();
			final String backendversion = (String) prop.get("git.commit.id");
			final JSONObject jso = new JSONObject();
//			jso.put("modelversion", modelversion);
			jso.put("backendversion", backendversion);
			return Response.ok(jso.toString()).build();
		} catch (final Throwable t) {
			t.printStackTrace();
			return Responses.errorResponse(t);
		}
	}
}
