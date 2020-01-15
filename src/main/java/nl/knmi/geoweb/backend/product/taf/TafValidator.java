package nl.knmi.geoweb.backend.product.taf;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.core.report.ListReportProvider;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import lombok.Getter;
import lombok.Setter;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentAscendingHeightClouds;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentChangegroupDuration;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentChangegroupsIncreasingInTime;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentCloudNeededRainOrModifierNecessary;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentEndTimes;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentMaxVerticalVisibility;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentMaxVisibility;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentNonRepeatingChanges;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentOverlappingBecomingChangegroups;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentVisibilityWeatherRequired;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentWindEnoughChange;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentWindGust;
import nl.knmi.geoweb.backend.product.taf.augment.AugmentQualifiers;

@Component
public class TafValidator {

	@Autowired
	@Qualifier("tafObjectMapper")
	private ObjectMapper objectMapper;

	TafSchemaStore tafSchemaStore;

	public TafValidator(final TafSchemaStore tafSchemaStore, ObjectMapper om) throws IOException {
		this.tafSchemaStore = tafSchemaStore;
		this.objectMapper=om;
	}

	public TafValidationResult validate(Taf taf)
			throws IOException, ProcessingException, JSONException, ParseException {
		return validate(taf.toJSON(objectMapper));
	}

	static Map<JsonPointer, String> customMessages;
	/**
	 * Identifies the prefix for JSON elements in which GeoWeb directives are
	 * defined
	 */
	public static final String GEOWEB_DIRECTIVES_ELEMENT_PREFIX = "$geoweb::";

	/**
	 * Identifies the JSON element in which the message for validation errors is
	 * defined
	 */
	public static final String GEOWEB_DIRECTIVE_MESSAGE_ELEMENT = GEOWEB_DIRECTIVES_ELEMENT_PREFIX + "messages";

	private static void removeGeowebPrefixedFields(JsonNode jsonNode) {
		jsonNode.findParents(GEOWEB_DIRECTIVE_MESSAGE_ELEMENT).stream()
		.forEach(node -> ((ObjectNode) node).remove(GEOWEB_DIRECTIVE_MESSAGE_ELEMENT));
	}

	private static long modularAbs(long n, long mod) {
		n %= mod;
		if (n < 0)
			n += mod;
		return n;
	}

	/**
	 * Adds two numbers in modulo arithmetic. This function is safe for large
	 * numbers and won't overflow long.
	 *
	 * @param a
	 * @param b
	 * @param mod
	 *            grater than 0
	 * @return (a+b)%mod
	 */
	public static long add(long a, long b, long mod) {
		if (mod <= 0)
			throw new IllegalArgumentException("Mod argument is not grater then 0");
		a = modularAbs(a, mod);
		b = modularAbs(b, mod);
		if (b > mod - a) {
			return b - (mod - a);
		}
		return (a + b) % mod;
	}

	/**
	 * Subtract two numbers in modulo arithmetic. This function is safe for large
	 * numbers and won't overflow or underflow long.
	 *
	 * @param a
	 * @param b
	 * @param mod
	 *            grater than 0
	 * @return (a-b)%mod
	 */
	public static long subtract(long a, long b, long mod) {
		if (mod <= 0)
			throw new IllegalArgumentException("Mod argument is not grater then 0");
		return add(a, -b, mod);
	}

	private static void harvestFields(JsonNode node, Predicate<String> fieldNamePredicate, JsonPointer parentPointer,
			Set<FoundJsonField> harvestedSoFar, boolean shouldVisitSubNodes) {

		final JsonPointer localParentPointer = parentPointer == null ? JsonPointer.empty() : parentPointer;
		final Predicate<String> localFieldNamePredicate = fieldNamePredicate != null ? fieldNamePredicate
				: name -> true;

				if (node == null) {
					return;
				}

				if (node.isObject()) {
					Iterable<Entry<String, JsonNode>> fieldsIterable = () -> node.fields();
					StreamSupport.stream(fieldsIterable.spliterator(), true).forEach(field -> {
						String fieldName = field.getKey();
						JsonPointer childPointer = localParentPointer.append(fieldName);
						if (localFieldNamePredicate.test(fieldName)) {
							harvestedSoFar.add(
									new FoundJsonField(fieldName, childPointer.parent(), field.getValue(), new ObjectMapper()));
						} else if (shouldVisitSubNodes) {
							harvestFields(field.getValue(), localFieldNamePredicate, childPointer, harvestedSoFar,
									shouldVisitSubNodes);
						}
					});
				} else if (node.isArray()) {
					IntStream.range(0, node.size()).forEach(index -> {
						JsonPointer childPointer = localParentPointer.append(index);
						JsonNode childNode = node.get(index);
						if (childNode.isObject() || childNode.isArray()) {
							harvestFields(childNode, localFieldNamePredicate, childPointer, harvestedSoFar,
									shouldVisitSubNodes);
						}
					});
				}
	}

	private static Map<String, Set<String>> pointersOfSchemaErrors(JsonNode schema) {
		Map<String, Set<String>> pointers = new HashMap<String, Set<String>>();
		if (schema.isObject()) {
			if (schema.has("schema") && schema.has("keyword")) {
				JsonNode schemaField = schema.get("schema");
				if (schemaField.has("pointer")) {
					String pointer = schemaField.get("pointer").asText();
					String keyword = schema.get("keyword").asText();
					if (pointers.containsKey(pointer)) {
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
				while (subNames.hasNext()) {
					Map<String, Set<String>> subReportErrors = pointersOfSchemaErrors(subReports.get(subNames.next()));
					subReportErrors.forEach((pointer, values) -> {
						if (pointers.containsKey(pointer)) {
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

		if (schema.isArray()) {
			schema.forEach(s -> {
				Map<String, Set<String>> subReportErrors = pointersOfSchemaErrors(s);
				subReportErrors.forEach((pointer, values) -> {
					if (pointers.containsKey(pointer)) {
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

	private static Set<String> findPathInOriginalJson(JsonNode schema, String path) {
		Set<String> pathSet = new HashSet<>();
		if (schema.isObject()) {
			JsonNode schemaField = schema.get("schema");
			if (schemaField.has("pointer")) {
				if (schemaField.get("pointer").asText().equals(path)) {
					if (schema.get("instance") != null) {
						JsonNode ptr = schema.get("instance").get("pointer");
						if (ptr != null) {
							pathSet.add(ptr.asText());
						}
					}
				}
			}
			if (schema.has("reports")) {
				JsonNode subReports = schema.get("reports");
				Iterator<String> subNames = subReports.fieldNames();
				while (subNames.hasNext()) {
					pathSet.addAll(findPathInOriginalJson(subReports.get(subNames.next()), path));
				}
			}
		}
		if (schema.isArray()) {
			schema.forEach(s -> pathSet.addAll(findPathInOriginalJson(s, path)));
		}
		return pathSet;
	}

	public static int LCSLength(String a, String b) {
		int[][] lengths = new int[a.length() + 1][b.length() + 1];

		// row 0 and column 0 are initialized to 0 already

		for (int i = 0; i < a.length(); i++)
			for (int j = 0; j < b.length(); j++)
				if (a.charAt(i) == b.charAt(j))
					lengths[i + 1][j + 1] = lengths[i][j] + 1;
				else
					lengths[i + 1][j + 1] = Math.max(lengths[i + 1][j], lengths[i][j + 1]);
		return lengths[a.length()][b.length()];
	}

	private static Map<String, Set<String>> convertReportInHumanReadableErrors(ProcessingReport validationReport,
			Map<String, Map<String, String>> messagesMap) {
		Map<String, Set<String>> errorMessages = new HashMap<>();
		validationReport.forEach(report -> {
			Map<String, Set<String>> errors = pointersOfSchemaErrors(report.asJson());

			// TODO: this is not ideal but filters only relevant errors
			// Removes forecast errors iff there exists an error which contains needle
			errors.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue))
			.forEach((pointer, keywords) -> {
				if (!messagesMap.containsKey(pointer)) {
					return;
				}
				Map<String, String> messages = messagesMap.get(pointer);

				keywords.forEach(keyword -> {
					if (!messages.containsKey(keyword)) {
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
		List<String> keys = errorMessages.keySet().stream().collect(Collectors.toList());
		Map<String, Set<String>> finalErrors = new HashMap<>();
		if (keys.size() == 0) {
			return finalErrors;
		}
		Collections.sort(keys);
		final double SAME_RATIO = 1.0;
		for (int i = 0; i < keys.size(); ++i) {
			for (int j = i + 1; j < keys.size(); ++j) {
				int lcs = LCSLength(keys.get(i), keys.get(j));
				if (((double) lcs / (double) keys.get(i).length()) < SAME_RATIO) {
					finalErrors.put(keys.get(i), errorMessages.get(keys.get(i)));
					break;
				}
			}
		}
		String lastKey = keys.get(keys.size() - 1);
		finalErrors.put(lastKey, errorMessages.get(lastKey));
		return finalErrors;
	}

	private static Map<String, Map<String, String>> extractMessagesAndCleanseSchema(List<Resource> schemaNodes) {
		return schemaNodes.stream().map(schemaNodePath -> {
			try {
				return extractMessagesAndCleanseSchema(ValidationUtils.getJsonNode(schemaNodePath.getFile()));
			} catch (IOException e) {
				return null;
			}
		}).collect(Collectors.toList())
				.stream()
				.flatMap(m -> m.entrySet().stream())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
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

	private List<Resource> discoverSchemata(String schemaResourceLocation) {
		List<Resource> resultList = new ArrayList<Resource>();
		try {
			String locationPattern = "file:" + schemaResourceLocation + "**/*.json";
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
					getClass().getClassLoader());
			resultList = Arrays.asList(resolver.getResources(locationPattern));
			if (resultList.isEmpty()) {
				throw new Exception(String.format("Empty files list while discovering schemata"));
			}
		} catch (Exception exception) {
			throw new IllegalStateException(
					String.format("Could not discover the configuration files containing the validation rules"));
		}

		return resultList;
	}

	public boolean validateSchema(JsonNode schema) throws IOException, ProcessingException {
		JsonNode cpy = schema.deepCopy();
		removeGeowebPrefixedFields(cpy);
		String schemaschemaString = tafSchemaStore.getSchemaSchema();
		ObjectMapper om = new ObjectMapper();
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		final JsonSchema schemaschema = factory.getJsonSchema(om.readTree(schemaschemaString));

		ProcessingReport validReport = schemaschema.validate(cpy);

		return validReport.isSuccess();
	}

	public static void enrich(JsonNode input) throws ParseException, JsonProcessingException, IOException {
		AugmentChangegroupsIncreasingInTime.augment(input);
		AugmentOverlappingBecomingChangegroups.augment(input);
		AugmentChangegroupDuration.augment(input);
		AugmentWindGust.augment(input);
		AugmentAscendingHeightClouds.augment(input);				//Done
		//AugmentAmountCoverageClouds.augment(input);
		AugmentEndTimes.augment(input);
		AugmentVisibilityWeatherRequired.augment(input);
		AugmentWindEnoughChange.augment(input);						// FM Done
		AugmentCloudNeededRainOrModifierNecessary.augment(input);
		AugmentMaxVisibility.augment(input);
		AugmentNonRepeatingChanges.augment(input);					// FM Done
		AugmentMaxVerticalVisibility.augment(input);
		AugmentQualifiers.augment(input);
	}

	/**
	 * Returns true if this node has a value
	 * @param node
	 * @return
	 */
	public static boolean checkIfNodeHasValue(JsonNode node){
		if ( node == null || node.isMissingNode() || node.isNull())return false;
		return true;
	}





	public DualReturn performValidation(String schemaFile, String tafStr) throws IOException, ProcessingException {
		return performValidation(schemaFile, ValidationUtils.getJsonNode(tafStr));
	}

	private class DualReturn {
		@Getter
		@Setter
		private ProcessingReport report = null;

		@Getter
		@Setter
		private Map<String, Map<String, String>> messages = null;

		public DualReturn(ProcessingReport report, Map<String, Map<String, String>> messages) {
			this.report = report;
			this.messages = messages;
		}
	}

	public DualReturn performValidation(String schemaFile, JsonNode jsonNode) throws IOException, ProcessingException {
		JsonNode schemaNode = ValidationUtils.getJsonNode(schemaFile);
		// This extracts the custom error messages in the JSONSchema and removes them
		// This is necessary because otherwise the schema is invalid and thus always
		// needs to happen.
		// The messages map is a mapping from a pointer in the JSONSchema to another map
		// This is a map from keyword to human-readable message. So the full structure
		// is something like
		// /definitions/vertical_visibilitiy --> minimum -> "Vertical visibility must be
		// greater than 0 meters"
		// |-> maximum -> "Vertical visibility must be less than 1000 meters"
		// |-> multipleOf -> "Vertical visibility must a multiple of 30 meters"
		Map<String, Map<String, String>> messagesMap = extractMessagesAndCleanseSchema(discoverSchemata(this.tafSchemaStore.getDirectory()));
		// Construct the final schema based on the filtered schema
		// Set the namespace to the tafstore location such that external schemas can be resolved
		URITranslatorConfiguration uribuilder = URITranslatorConfiguration.newBuilder()
				.setNamespace("file:"+this.tafSchemaStore.getDirectory()+"/").freeze();
		LoadingConfiguration config = LoadingConfiguration
				.newBuilder()
				.setURITranslatorConfiguration(uribuilder)
				.freeze();
		final JsonSchemaFactory factory = JsonSchemaFactory
				.newBuilder()
				.setReportProvider(new ListReportProvider(LogLevel.ERROR, LogLevel.FATAL))
				.setLoadingConfiguration(config)
				.freeze();


		final JsonSchema schema = factory.getJsonSchema(schemaNode);
		// Try and validate the TAF

		ProcessingReport validationReport = schema.validate(jsonNode);
		return new DualReturn(validationReport, messagesMap);
	}

	private static void removeLastEmptyChangegroup(JsonNode jsonNode) {
		if (jsonNode == null)
			return;
		JsonNode changegroupsNode = jsonNode.at("/changegroups");
		if (changegroupsNode == null || changegroupsNode.isMissingNode() || changegroupsNode.isNull())
			return;
		ArrayNode changegroups = (ArrayNode) changegroupsNode;
		// If there are no changegroups we are done
		if (changegroups == null || changegroups.size() <= 1)
			return;

		/* Remove all empty changegroups */
		for (int i = changegroups.size() - 1; i >= 0; i--) {
			JsonNode elem = changegroups.get(i);
			if (elem == null || elem.isMissingNode() || elem.isNull() || elem == NullNode.getInstance() || elem.size() == 0) {
				changegroups.remove(i);
			}
		}
		if (changegroups.size() <= 1)
			return;
		JsonNode lastChangegroup = changegroups.get(changegroups.size() - 1);

		// If the last changegroup is null or {} we can throw it away
		if (lastChangegroup == null || lastChangegroup.size() == 0) {
			changegroups.remove(changegroups.size() - 1);
			return;
		}
		;
		ObjectNode lastForecast = (ObjectNode) lastChangegroup.get("forecast");

		// If the forecast in the last changegroup is null or {} we can throw it away
		if (lastForecast == null || lastForecast.size() == 0) {
			changegroups.remove(changegroups.size() - 1);
			return;
		}
		;

		// If it is a well-formed changegroup but has no content, we can throw it away
		if ((!lastChangegroup.has("changeType") || lastChangegroup.get("changeType").asText().equals(""))
				&& !lastChangegroup.has("changeStart") && !lastChangegroup.has("changeEnd")
				&& lastForecast.get("wind").size() == 0 && lastForecast.get("visibility").size() == 0
				&& lastForecast.get("weather").asText().equals("NSW")
				&& lastForecast.get("clouds").asText().equals("NSC")) {
			changegroups.remove(changegroups.size() - 1);
		}
	}

	public TafValidationResult validate(String tafStr)
			throws ProcessingException, JSONException, IOException, ParseException {

		String schemaFile = tafSchemaStore.getLatestTafSchema();
		JsonNode jsonNode = ValidationUtils.getJsonNode(tafStr);
		removeLastEmptyChangegroup(jsonNode);
		DualReturn ret = performValidation(schemaFile, jsonNode);
		ProcessingReport validationReport = ret.getReport();
		Map<String, Map<String, String>> messagesMap = ret.getMessages();

		// If the validation is not successful we try to find all relevant errors
		// They are relevant if the error path in the schema exist in the
		// possibleMessages set
		if (validationReport == null) {
			ObjectMapper om = new ObjectMapper();
			return new TafValidationResult(false,
					(ObjectNode) om.readTree("{\"/forecast/message\": [\"Validation report was null\"]}"), validationReport);
		}

		Map<String, Set<String>> errorMessages = convertReportInHumanReadableErrors(validationReport, messagesMap);
		JsonNode errorJson = new ObjectMapper().readTree("{}");

		if (!validationReport.isSuccess()) {
			//			validationReport.forEach(report -> {
			//
			//				try {
			//					log.debug((new JSONObject(
			//							report.asJson().toString()
			//							)).toString(4));
			//				} catch (JSONException e) {
			//					log.error(e.getMessage());
			//				}
			//
			//			});

			String errorsAsJson = new ObjectMapper().writeValueAsString(errorMessages);
			// Try to find all possible errors and map them to the human-readable variants
			// using the messages map
			((ObjectNode) errorJson).setAll((ObjectNode) (ValidationUtils.getJsonNode(errorsAsJson)));
		}
		// Enrich the JSON with custom data validation, this is validated using a second
		// schema
		enrich(jsonNode);
		String enrichedSchemaFile = tafSchemaStore.getLatestEnrichedTafSchema();
		ret = performValidation(enrichedSchemaFile, jsonNode);
		ProcessingReport enrichedValidationReport = ret.getReport();
		Map<String, Map<String, String>> enrichedMessagesMap = ret.getMessages();
		if (enrichedValidationReport == null) {
			ObjectMapper om = new ObjectMapper();
			return new TafValidationResult(false,
					(ObjectNode) om.readTree("{\"/forecast/message\": [\"Validation report was null\"]}"), validationReport,
					enrichedValidationReport);
		}

		if (!enrichedValidationReport.isSuccess()) {
			// Try to find all possible errors and map them to the human-readable variants
			// using the messages map
			// Append them to any previous errors, if any
			Map<String, Set<String>> enrichedErrorMessages = convertReportInHumanReadableErrors(
					enrichedValidationReport, enrichedMessagesMap);
			String errorsAsJson = new ObjectMapper().writeValueAsString(enrichedErrorMessages);
			((ObjectNode) errorJson).setAll((ObjectNode) ValidationUtils.getJsonNode(errorsAsJson));
		}

		/* Check if we can make a TAC */
		try{
			objectMapper.readValue(tafStr, Taf.class).toTAC();
		}catch(Exception e){
			ObjectMapper om = new ObjectMapper();
			return new TafValidationResult(false,
					(ObjectNode) om.readTree("{\"/forecast/message\": [\"Unable to generate TAC report\"]}"), validationReport,
					enrichedValidationReport);
		}

		// If everything is okay, return true as succeeded with null as errors
		if (enrichedValidationReport.isSuccess() && validationReport.isSuccess()) {
			return new TafValidationResult(true);
		}
		return new TafValidationResult(false, (ObjectNode) errorJson, validationReport, enrichedValidationReport);
	}
}
