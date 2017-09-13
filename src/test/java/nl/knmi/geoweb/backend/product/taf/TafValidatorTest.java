package nl.knmi.geoweb.backend.product.taf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import nl.knmi.adaguc.tools.Tools;

public class TafValidatorTest {
	@Test
	public void testValidateOK () throws Exception {
		String taf = Tools.getResourceFromClassPath(TafValidatorTest.class, "Taf_valid.json");
		
		JSONObject tafAsJSON = new JSONObject(taf);
		JsonNode report = new TafValidator().validate(tafAsJSON.toString());
		assertThat(report.get("succeeded").asBoolean(), is(true));
	}
	@Test
	public void testValidateFails () throws IOException, JSONException, ProcessingException  {
		String taf = Tools.getResourceFromClassPath(TafValidatorTest.class, "./Taf_invalid.json");
		JSONObject tafAsJSON = new JSONObject(taf);
		JsonNode report = new TafValidator().validate(tafAsJSON.toString());
		assertThat(report.get("succeeded").asBoolean(), is(false));
	}
	
}
