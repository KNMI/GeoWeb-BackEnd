package nl.knmi.geoweb.backend.product.taf;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;

import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.validation.ValidationUtils;

public class TafValidator {

	public static ProcessingReport validate(Taf taf) throws IOException, ProcessingException {
		return validate(taf.toJSON());
	}

	public static ProcessingReport validate(String tafStr) throws IOException, ProcessingException {
		String schemaFile = Tools.getResourceFromClassPath(TafValidator.class, "TafValidatorSchema.json");
		final JsonSchema schemaNode = ValidationUtils.getSchemaNode(schemaFile);
		final JsonNode jsonNode = ValidationUtils.getJsonNode(tafStr);
		return schemaNode.validate(jsonNode);
	}
	

}
