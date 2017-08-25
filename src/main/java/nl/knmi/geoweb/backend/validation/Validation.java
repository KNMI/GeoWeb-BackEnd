package nl.knmi.geoweb.backend.validation;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;


public class Validation {

	public static void main(String[] args) throws ProcessingException, IOException {
		File schemaFile = new File("/nobackup/users/plieger/code/github/maartenplieger/GeoWeb-BackEnd/schemas/taf.jsonschema");
	    File jsonFile = new File("/nobackup/users/plieger/code/github/maartenplieger/GeoWeb-BackEnd/taf.json");
	    	
		final JsonSchema schemaNode = ValidationUtils.getSchemaNode(schemaFile);
		final JsonNode jsonNode = ValidationUtils.getJsonNode(jsonFile);
		
	    ProcessingReport report = schemaNode.validate(jsonNode);
	    	    System.out.println("Validation succes: " + report.isSuccess());
		System.out.println("Report" + report.toString());
	}

}
