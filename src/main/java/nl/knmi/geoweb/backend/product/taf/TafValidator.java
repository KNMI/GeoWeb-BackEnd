package nl.knmi.geoweb.backend.product.taf;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.validation.ValidationUtils;

public class TafValidator {

	public static JsonNode validate(Taf taf) throws IOException, ProcessingException, JSONException {
		return validate(taf.toJSON());
	}
	
	static Map<JsonPointer, String> customMessages; 
    /**
     * Identifies the prefix for JSON elements in which GeoWeb directives are defined
     */
    public static final String GEOWEB_DIRECTIVES_ELEMENT_PREFIX = "$geoweb::";

    /**
     * Identifies the JSON element in which the message for validation errors is defined
     */
    public static final String GEOWEB_DIRECTIVE_MESSAGE_ELEMENT = GEOWEB_DIRECTIVES_ELEMENT_PREFIX + "messages";
    
    private static void removeGeowebPrefixedFields(JsonNode jsonNode) {
    	jsonNode.findParents(GEOWEB_DIRECTIVE_MESSAGE_ELEMENT).stream()
        	.forEach(node -> ((ObjectNode) node).remove(GEOWEB_DIRECTIVE_MESSAGE_ELEMENT));
    }
    
    private static void harvestFields(JsonNode node, Predicate<String> fieldNamePredicate, JsonPointer parentPointer, Set<FoundJsonField> harvestedSoFar, boolean shouldVisitSubNodes) {
        
        final JsonPointer localParentPointer = parentPointer == null ? JsonPointer.empty() : parentPointer;
        final Predicate<String> localFieldNamePredicate = fieldNamePredicate != null ? fieldNamePredicate : name -> true;
        
        if (node == null) {
            return;
        }
        
        if (node.isObject()) {
            Iterable<Map.Entry<String, JsonNode>> fieldsIterable = () -> node.fields();
            StreamSupport.stream(fieldsIterable.spliterator(), true)
                .forEach(field -> {
                    String fieldName = field.getKey();
                    JsonPointer childPointer = localParentPointer.append(fieldName);
                    if (localFieldNamePredicate.test(fieldName)) {
                        harvestedSoFar.add(new FoundJsonField(fieldName, childPointer.parent(), field.getValue(), new ObjectMapper()));
                    } else if (shouldVisitSubNodes) {
                        harvestFields(field.getValue(), localFieldNamePredicate, childPointer, harvestedSoFar, shouldVisitSubNodes);
                    }
                });
        } else if (node.isArray()) {
            IntStream.range(0, node.size())
                .forEach(index -> {
                    JsonPointer childPointer = localParentPointer.append(index);
                    JsonNode childNode = node.get(index);
                    if (childNode.isObject() || childNode.isArray()) {
                        harvestFields(childNode, localFieldNamePredicate, childPointer, harvestedSoFar, shouldVisitSubNodes);
                    }
                });
        }
    }
    
    private static Map<String, Set<String>> pointersOfSchemaErrors(JsonNode schema) {
    	Map<String, Set<String>> pointers = new HashMap<String, Set<String>>();
    	if(schema.isObject()) {
	    	if (schema.has("schema") && schema.has("keyword")) {
	    		JsonNode schemaField = schema.get("schema");
	    		if (schemaField.has("pointer")) {
	    			String pointer = schemaField.get("pointer").asText();
	    			String keyword = schema.get("keyword").asText();
	    			if (pointers.containsKey(pointer) ) {
	    				Set<String> keywords = pointers.get(pointer);
	    				keywords.add(keyword);
	    				pointers.put(pointer, keywords);
	    			} else {
	    				pointers.put(pointer, Stream.of(keyword).collect(Collectors.toSet()));
	    			}
	    		}
	    	}
	    	
	    	if (schema.has("reports")) {
	    		JsonNode subReports = schema.get("reports");
	    		Iterator<String> subNames = subReports.fieldNames();
	    		while(subNames.hasNext()) {
	    			Map<String, Set<String>> subReportErrors = pointersOfSchemaErrors(subReports.get(subNames.next()));
	    			subReportErrors.forEach((pointer, values) -> {
		    			if (pointers.containsKey(pointer) ) {
		    				Set<String> keywords = pointers.get(pointer);
		    				keywords.addAll(values);
		    				pointers.put(pointer, keywords);
		    			} else {
		    				pointers.put(pointer, values);
		    			}
	    			});
	    		}
	    	}
    	}
    	
    	if(schema.isArray()) {
    		schema.forEach(s -> {
    			Map<String, Set<String>> subReportErrors = pointersOfSchemaErrors(s);
    			subReportErrors.forEach((pointer, values) -> {
	    			if (pointers.containsKey(pointer) ) {
	    				Set<String> keywords = pointers.get(pointer);
	    				keywords.addAll(values);
	    				pointers.put(pointer, keywords);
	    			} else {
	    				pointers.put(pointer, values);
	    			}
    			});
    		});
    	}
    	
    	return pointers;
    }
    
    private static boolean existsHashmapKeyContainingString(Map<String, Set<String>> map, List<String> needles) {
    	Iterator<String> it = map.keySet().iterator();
    	while (it.hasNext()) {
    		String key = it.next();
    		for (String needle : needles) {
    			if (key.contains(needle)) 
    				return true;
    		}
    	}
    	return false;
    }
    
    private static Set<String> findPathInOriginalJson(JsonNode schema, String path) {
    	Set<String> pathSet = new HashSet<>();
    	if(schema.isObject()) {
    		JsonNode schemaField = schema.get("schema");
    		if (schemaField.has("pointer")) {
    			if(schemaField.get("pointer").asText().equals(path)) {
    				pathSet.add(schema.get("instance").get("pointer").asText());
    			}
    		}
	    	if (schema.has("reports")) {
	    		JsonNode subReports = schema.get("reports");
	    		Iterator<String> subNames = subReports.fieldNames();
	    		while(subNames.hasNext()) {
	    			pathSet.addAll(findPathInOriginalJson(subReports.get(subNames.next()), path));
	    		}
	    	}
    	}
    	if(schema.isArray()) {
    		schema.forEach(s -> pathSet.addAll(findPathInOriginalJson(s, path)));
    	}
    	return pathSet;
    }
    
    private static Map<String, Set<String>> convertReportInHumanReadableErrors(ProcessingReport validationReport, Map<String, Map<String, String>> messagesMap) {
    	Map<String, Set<String>> errorMessages = new HashMap<>();
    	List<String> needles = Arrays.asList("visibility", "clouds", "wind", "weather");
		validationReport.forEach(report -> {
			Map<String, Set<String>> errors = pointersOfSchemaErrors(report.asJson());
			
			// TODO: this is not ideal but filters only relevant errors
			// Removes forecast errors iff there exists an error which contains needle
			errors.entrySet().stream().filter(error -> 
				!(error.getKey().contains("forecast") && 
				 existsHashmapKeyContainingString(errors, needles))
			)
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue)).forEach((pointer, keywords) -> {
				if(!messagesMap.containsKey(pointer)) {
					return;
				}
				Map<String, String> messages = messagesMap.get(pointer);
				keywords.forEach(keyword -> {
					if(!messages.containsKey(keyword)) {
						return;
					}
					findPathInOriginalJson(report.asJson(), pointer).forEach(path -> {
						if (!errorMessages.containsKey(path)) {
							errorMessages.put(path, new HashSet<String>(Arrays.asList(messages.get(keyword))));
						} else {
							Set<String> set = errorMessages.get(path);
							set.add(messages.get(keyword));
							errorMessages.put(path, set);
						}
					});
				});
			});
		});
		return errorMessages;
    }
    
	@SuppressWarnings("unchecked")
    private static Map<String, Map<String, String>> extractMessagesAndCleanseSchema(JsonNode schemaNode) {
		Predicate<String> pred = name -> name.equals(GEOWEB_DIRECTIVE_MESSAGE_ELEMENT);
		Set<FoundJsonField> harvests = new HashSet<>();

		// Pointer in schema to a keyword/message pair
		Map<String, Map<String, String>> messagesMap = new HashMap<>();
		harvestFields(schemaNode, pred, null, harvests, true);
		ObjectMapper _mapper = new ObjectMapper();
		
		harvests.forEach(harvest -> {
			String path = harvest.getPointer().toString();
			JsonNode rawMessages = harvest.getValue();
			Map<String, String> messages = _mapper.convertValue(rawMessages, HashMap.class);
			messagesMap.put(path, messages);
		});

		// Remove custom fields
		removeGeowebPrefixedFields(schemaNode);
		
		return messagesMap;
    }

	public static JsonNode validate(String tafStr) throws IOException, ProcessingException, JsonMappingException, JSONException {
		// Locate the schema file
		String schemaFile = Tools.getResourceFromClassPath(TafValidator.class, "/TafValidatorSchema.json");
		// Convert the TAF and the validation schema to JSON objects
		JsonNode jsonNode = ValidationUtils.getJsonNode(tafStr);
		JsonNode schemaNode = ValidationUtils.getJsonNode(schemaFile);
		
		// This extracts the custom error messages in the JSONSchema and removes them
		// This is necessary because otherwise the schema is invalid and thus always needs to happen.
		// The messages map is a mapping from a pointer in the JSONSchema to another map
		// This is a map from keyword to human-readable message. So the full structure is something like
		// /definitions/vertical_visibilitiy --> minimum -> "Vertical visibility must be greater than 0 meters"
		//	                                 |-> maximum -> "Vertical visibility must be less than 1000 meters"
		//	                                 |-> multipleOf -> "Vertical visibility must a multiple of 30 meters"
		Map<String, Map<String, String>> messagesMap = extractMessagesAndCleanseSchema(schemaNode);
		
		// Construct the final schema based on the filtered schema
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final JsonSchema schema = factory.getJsonSchema(schemaNode);
        
        // Try and validate the TAF
		ProcessingReport validationReport = schema.validate(jsonNode);
		// If the validation is not successful we try to find all relevant errors
		// They are relevant if the error path in the schema exist in the possibleMessages set
		if(validationReport != null && !validationReport.isSuccess()) {
			// Try to find all possible errors and map them to the human-readable variants using the messages map
			Map<String, Set<String>> errorMessages = convertReportInHumanReadableErrors(validationReport, messagesMap);

			String json = new ObjectMapper().writeValueAsString(errorMessages);
			JsonNode finalJson = ValidationUtils.getJsonNode(json);
			((ObjectNode)finalJson).put("succeeded", "false");
			return finalJson;
		}
		JSONObject succeededObject = new JSONObject();
		succeededObject.put("succeeded", "true");
		return ValidationUtils.getJsonNode(succeededObject.toString());
	}
}
