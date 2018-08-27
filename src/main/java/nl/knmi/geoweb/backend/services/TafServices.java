package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.datastore.TafStore;
import nl.knmi.geoweb.backend.product.taf.TAFtoTACMaps;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportType;
import nl.knmi.geoweb.backend.product.taf.TafSchemaStore;
import nl.knmi.geoweb.backend.product.taf.TafValidationResult;
import nl.knmi.geoweb.backend.product.taf.TafValidator;
import nl.knmi.geoweb.backend.product.taf.converter.TafConverter;
@RestController
public class TafServices {
	@Autowired
	@Qualifier("tafObjectMapper")
	private ObjectMapper tafObjectMapper;

	@Autowired
	TafStore tafStore;
	ProductExporter<Taf> publishTafStore;
	TafSchemaStore tafSchemaStore;
	TafValidator tafValidator;

	@Autowired
	private TafConverter tafConverter;

	TafServices (final TafStore tafStore, final TafSchemaStore tafSchemaStore, final TafValidator tafValidator, final ProductExporter<Taf> publishTafStore) throws Exception {
		this.tafStore = tafStore;
		this.tafSchemaStore = tafSchemaStore;
		this.tafValidator = tafValidator;
		this.publishTafStore = publishTafStore;
	}	

	static TafSchemaStore schemaStore = null;

	boolean enableDebug = false;


	@RequestMapping(path="/tafs/verify", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE
			)
	public ResponseEntity<String> verifyTAF(@RequestBody String tafStr) throws IOException, JSONException, ParseException {
		tafStr = URLDecoder.decode(tafStr,"UTF8");
		try {
			TafValidationResult jsonValidation = tafValidator.validate(tafStr);
			if(jsonValidation.isSucceeded() == false){
				ObjectNode errors = jsonValidation.getErrors();
				Debug.errprintln("/tafs/verify: TAF validation failed");
				String finalJson = new JSONObject()
						.put("succeeded", false)
						.put("errors", new JSONObject(errors.toString()))
						.put("message","TAF is not valid").toString();
				return ResponseEntity.ok(finalJson);
			} else {
				// If there is already a taf published for this location and airport; only for type==normal
				Taf taf = tafObjectMapper.readValue(tafStr, Taf.class);
				Taf[] tafs = tafStore.getTafs(true, TAFReportPublishedConcept.published, null, taf.metadata.getLocation());
				if (taf.metadata.getStatus() != TAFReportPublishedConcept.published && taf.metadata.getType()==TAFReportType.normal &&
						Arrays.stream(tafs).anyMatch(publishedTaf -> publishedTaf.metadata.getLocation().equals(taf.metadata.getLocation()) &&
								publishedTaf.metadata.getValidityStart().isEqual(taf.metadata.getValidityStart()) /* &&
								(taf.metadata.getPreviousUuid()==null || !taf.metadata.getPreviousUuid().equals(publishedTaf.metadata.getUuid()))*/ )) {
					String finalJson = new JSONObject()
							.put("succeeded", false)
							.put("message","There is already a published TAF for " + taf.metadata.getLocation() + " at " + TAFtoTACMaps.toDDHH(taf.metadata.getValidityStart())).toString();
					return ResponseEntity.ok(finalJson);
				}

				String TAC = "unable to create TAC";
				try{
					TAC = tafObjectMapper.readValue(tafStr, Taf.class).toTAC();
				}catch(Exception e){
					Debug.printStackTrace(e);
				}
				String json = new JSONObject().put("succeeded", true).put("message","TAF is verified.").put("TAC", TAC).toString();
				return ResponseEntity.ok(json);
			}
		} catch (ProcessingException e) {
			Debug.printStackTrace(e);
			// TODO Auto-generated catch block
			String json = new JSONObject().
					put("message","Unable to validate taf").toString();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
		}
	}
	/**
	 * POST a TAF to the product store
	 * @param tafStr
	 * @return
	 * @throws IOException
	 * @throws JSONException 
	 * @throws ParseException 
	 */
	@RequestMapping(
			path = "/tafs", 
			method = RequestMethod.POST, 
			consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE
			)
	public ResponseEntity<String> storeTAF(@RequestBody String  tafStr) throws IOException, JSONException, ParseException {
		Debug.println("storetaf");
		Taf taf = null;
		tafStr = URLDecoder.decode(tafStr,"UTF8");

		try {
			//			objectMapper=Taf.getTafObjectMapperBean().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
			taf = tafObjectMapper.readValue(tafStr, Taf.class);
		} catch (Exception e2) {
			Debug.errprintln("Error parsing taf ["+tafStr+"]");
			Debug.printStackTrace(e2);
			try {
				JSONObject obj=new JSONObject();
				obj.put("error","Error parsing taf").put("exception", e2.getMessage());
				String json = obj.toString();
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
			} catch (JSONException e1) {
			}
		}
		if(enableDebug)Debug.println("TAF from Object: " + taf.toJSON(tafObjectMapper));
		// Assert that the JSONs are equal regardless of order
		final JsonNode tree1 = tafObjectMapper.readTree(tafStr);
		final JsonNode tree2 = tafObjectMapper.readTree(taf.toJSON(tafObjectMapper));
		if(!tree1.equals(tree2)) {
			throw new IllegalArgumentException("TAF JSON is different from origional JSON");
		} else {
			Debug.println("OK: Incoming TAF string is equal to serialized and deserialized TAF string");
		}


		//		if(taf.metadata.getUuid() != null){
		//			// Check if TAF to publish is not already published
		//			if (taf.metadata.getStatus()==TAFReportPublishedConcept.published) {
		//				Taf storedTaf=tafStore.getByUuid(taf.metadata.getUuid());
		//				if ((storedTaf!=null)&&storedTaf.metadata.getUuid().equals(taf.metadata.getUuid())
		//						&&(storedTaf.metadata.getStatus()==TAFReportPublishedConcept.published)){
		//					try {
		//						JSONObject obj=new JSONObject();
		//						obj.put("error", "TAF with uuid "+taf.metadata.getUuid()+"already published");
		//						String json = obj.toString();
		//						return ResponseEntity.status(HttpStatus.FORBIDDEN).body(json);
		//					} catch (JSONException e1) {
		//					}				
		//				}
		//			}
		//			Debug.println("Overwriting TAF with uuid ["+taf.metadata.getUuid()+"]");
		//		} else {
		if(taf.metadata.getUuid() == null){			//Generate random uuid
			taf.metadata.setUuid(UUID.randomUUID().toString());
		}

		if (taf.metadata.getType() == null) {
			taf.metadata.setType(TAFReportType.normal);
		}

		if (taf.metadata.getStatus() == null) {
			taf.metadata.setStatus(TAFReportPublishedConcept.concept);
		}


		Debug.println("Posting TAF of type "+taf.metadata.getType());
		switch (taf.metadata.getType()) {
		case normal:
			//Check if TAF with uuid is already published
			if (tafStore.isPublished(taf.getMetadata().getUuid())) {
				//Error
				try {
					JSONObject obj=new JSONObject();
					obj.put("error", "TAF with uuid "+taf.metadata.getUuid()+"already published");
					String json = obj.toString();
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(json);
				} catch (JSONException e1) {
				}	
			}
			if (taf.getMetadata().getStatus()==TAFReportPublishedConcept.concept) {
				taf.metadata.setIssueTime(null);
				tafStore.storeTaf(taf);
			} else if (taf.getMetadata().getStatus()==TAFReportPublishedConcept.published) {
				if (tafStore.isPublished(taf.getMetadata().getLocation(), taf.getMetadata().getValidityStart(),taf.getMetadata().getValidityEnd())) {
					//Error
					try {
						JSONObject obj=new JSONObject();
						obj.put("error", "TAF for "+taf.metadata.getLocation()+" already published");
						String json = obj.toString();
						return ResponseEntity.status(HttpStatus.FORBIDDEN).body(json);
					} catch (JSONException e1) {
					}	
				}
				// Publish it
				if (taf.metadata.getBaseTime() == null) {
					taf.metadata.setBaseTime(taf.metadata.getValidityStart());
				}
				//Set issuetime
				taf.metadata.setIssueTime(OffsetDateTime.now(ZoneId.of("UTC")));
				String result=this.publishTafStore.export(taf, tafConverter, tafObjectMapper);
				if ("OK".equals(result)) {
					//Only set to published if export has succeeded
					tafStore.storeTaf(taf);
					
					JSONObject tafjson = new JSONObject(taf.toJSON(tafObjectMapper));
					String json = new JSONObject().put("succeeded", true).put("message","Taf with id "+taf.metadata.getUuid()+" is published").put("tac", taf.toTAC()).put("tafjson", tafjson).put("uuid",taf.metadata.getUuid()).toString();
					return ResponseEntity.ok(json);
				}else {
					//result contains error message
					JSONObject obj=new JSONObject();
					obj.put("error", "TAF for "+taf.metadata.getLocation()+" failed to get published: "+result);
					String json = obj.toString();
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(json);
				}
			}
			JSONObject tafjson = new JSONObject(taf.toJSON(tafObjectMapper));
			String json = new JSONObject().put("succeeded", true).put("message","Taf with id "+taf.metadata.getUuid()+" is stored").put("tac", taf.toTAC()).put("tafjson", tafjson).put("uuid",taf.metadata.getUuid()).toString();
			return ResponseEntity.ok(json);
		case amendment:
		case correction:
			if (tafStore.isPublished(taf.getMetadata().getUuid())) {
				//Error
				Debug.println("Err: TAF "+taf.getMetadata().getUuid()+" alreay published");
				try {
					JSONObject obj=new JSONObject();
					obj.put("error", "TAF "+taf.getMetadata().getUuid()+" already published");
					json = obj.toString();
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(json);
				} catch (JSONException e1) {
				}		
			}
			if (!tafStore.isPublished(taf.getMetadata().getPreviousUuid())) {
				//Error
				Debug.println("Err: previous TAF "+taf.getMetadata().getPreviousUuid()+" not published");
				try {
					JSONObject obj=new JSONObject();
					obj.put("error", "previous TAF "+taf.getMetadata().getPreviousUuid()+" not published");
					json = obj.toString();
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(json);
				} catch (JSONException e1) {
				}	
			}
			Taf previousTaf=tafStore.getByUuid(taf.getMetadata().getPreviousUuid());
			if (taf.getMetadata().getStatus().equals(TAFReportPublishedConcept.concept)) {
				if (previousTaf.getMetadata().getLocation().equals(taf.getMetadata().getLocation())&&
						previousTaf.getMetadata().getValidityEnd().equals(taf.getMetadata().getValidityEnd())) {

					//					taf.getMetadata().setUuid(UUID.randomUUID().toString());
					Instant iValidityStart=Instant.now().truncatedTo(ChronoUnit.HOURS);
//TODO enable this when frontend handles this
					//TODO taf.getMetadata().setValidityStart(iValidityStart.atOffset(ZoneOffset.of("Z")));
					taf.getMetadata().setIssueTime(null);
					tafStore.storeTaf(taf);
				}
			} else if (taf.getMetadata().getStatus().equals(TAFReportPublishedConcept.published)) {
				if (previousTaf.getMetadata().getLocation().equals(taf.getMetadata().getLocation())&&
						previousTaf.getMetadata().getValidityEnd().equals(taf.getMetadata().getValidityEnd())) {

					if (previousTaf.getMetadata().getValidityEnd().isAfter(OffsetDateTime.now(ZoneId.of("UTC")))){
						taf.metadata.setIssueTime(OffsetDateTime.now(ZoneId.of("UTC")));
						tafStore.storeTaf(taf);
						// Publish it
						if (taf.metadata.getBaseTime() == null) {
							taf.metadata.setBaseTime(taf.metadata.getValidityStart());
						}
						this.publishTafStore.export(taf, tafConverter, tafObjectMapper);
						tafjson = new JSONObject(taf.toJSON(tafObjectMapper));
						json = new JSONObject().put("succeeded", true).put("message","Taf with id "+taf.metadata.getUuid()+" is amended").put("tac", taf.toTAC()).put("tafjson", tafjson).put("uuid",taf.metadata.getUuid()).toString();
						return ResponseEntity.ok(json);
					} else {
						//Error
						Debug.println("Error: COR/AMD for old TAF");
					}
				} else {
					//Error
					Debug.println("Error: COR/AMD do not match with previousTaf");				}
			}
			tafjson = new JSONObject(taf.toJSON(tafObjectMapper));
			json = new JSONObject().put("succeeded", true).put("message","Taf with id "+taf.metadata.getUuid()+" is stored").put("tac", taf.toTAC()).put("tafjson", tafjson).put("uuid",taf.metadata.getUuid()).toString();
			return ResponseEntity.ok(json);
		case canceled:
			if (tafStore.isPublished(taf.getMetadata().getUuid())) {
				//Error
				Debug.println("Err: TAF "+taf.getMetadata().getUuid()+" alreay published");
				try {
					JSONObject obj=new JSONObject();
					obj.put("error", "TAF "+taf.getMetadata().getUuid()+" already published");
					json = obj.toString();
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(json);
				} catch (JSONException e1) {
				}		
			}
			if (!tafStore.isPublished(taf.getMetadata().getPreviousUuid())) {
				//Error
				Debug.println("Err: previous TAF "+taf.getMetadata().getPreviousUuid()+" not published");
				try {
					JSONObject obj=new JSONObject();
					obj.put("error", "previous TAF "+taf.getMetadata().getPreviousUuid()+" not published");
					json = obj.toString();
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(json);
				} catch (JSONException e1) {
				}	
			}

			previousTaf=tafStore.getByUuid(taf.getMetadata().getPreviousUuid());
			if (previousTaf.getMetadata().getStatus().equals(TAFReportPublishedConcept.published)) {
				if (previousTaf.getMetadata().getLocation().equals(taf.getMetadata().getLocation())&&
						previousTaf.getMetadata().getValidityEnd().equals(taf.getMetadata().getValidityEnd())) {

					//					taf.getMetadata().setUuid(UUID.randomUUID().toString());
					taf.getMetadata().setIssueTime(OffsetDateTime.now(ZoneId.of("UTC")));
					taf.setForecast(null);
					taf.setChangegroups(null);
					if (taf.metadata.getBaseTime() == null) {
						taf.metadata.setBaseTime(taf.metadata.getValidityStart());
					}
					Debug.println("storing cancel");
					tafStore.storeTaf(taf);
					Debug.println("publishing cancel");

					this.publishTafStore.export(taf, tafConverter, tafObjectMapper);
					tafjson = new JSONObject(taf.toJSON(tafObjectMapper));
					json = new JSONObject().put("succeeded", true).put("message","Taf with id "+taf.metadata.getUuid()+" is canceled").put("tac", taf.toTAC()).put("tafjson", tafjson).put("uuid",taf.metadata.getUuid()).toString();
					return ResponseEntity.ok(json);
				}
			}
			//Error
			Debug.println("Err: cancel of "+taf.getMetadata().getPreviousUuid()+" failed");
			try {
				JSONObject obj=new JSONObject();
				obj.put("error", "cancel of "+taf.getMetadata().getPreviousUuid()+" failed");
				json = obj.toString();
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(json);
			} catch (JSONException e1) {
			}	
			break;
		default:
			break;
		}


		Debug.errprintln("Unknown error");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
	}


	boolean publishTaf(Taf taf) {
		if (taf.metadata.getBaseTime() == null) {
			taf.metadata.setBaseTime(taf.metadata.getValidityStart());
		}
		this.publishTafStore.export(taf, tafConverter, tafObjectMapper);
		return true;
	}

	@Getter
	private class TafList {
		Taf[] tafs;
		int page;
		int npages;
		int ntafs;
		TafList(Taf tafs [], Integer page, Integer count){
			int numtafs=tafs.length;
			int first;
			int last;
			if(count == null){
				count = 0;
			}
			if(page == null){
				page = 0;
			}
			if (count!=0){
				/* Select all tafs for requested page/count*/
				if (numtafs<=count) {
					first=0;
					last=numtafs;
				}else {
					first=page*count;
					last=Math.min(first+count, numtafs);
				}
				this.npages = (numtafs / count) + ((numtafs % count) > 0 ? 1:0 );
			} else {
				/* Select all tafs when count or page are not set*/
				first=0;
				last=numtafs;
				this.npages = 1;
			}
			if(first < numtafs && first >= 0 && last >= first && page < this.npages){
				this.tafs = Arrays.copyOfRange(tafs, first, last);
			}
			this.page = page;
			this.ntafs = numtafs;
		}

	}

	/**
	 * Get list of tafs
	 * @param active
	 * @param status
	 * @param page
	 * @param count
	 * @return
	 */
	@RequestMapping(
			path = "/tafs",
			method = RequestMethod.GET, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> getTafList(@RequestParam(value="active", required=true) Boolean active, 
			@RequestParam(value="status", required=false) TAFReportPublishedConcept status,
			@RequestParam(value="uuid", required=false) String uuid,
			@RequestParam(value="location", required=false) String location,
			@RequestParam(value="page", required=false) Integer page,
			@RequestParam(value="count", required=false) Integer count) {
		Debug.println("getTafList");
		try{
			final Taf[] tafs=tafStore.getTafs(active, status,uuid,location);
			Taf[] filteredTafs = (Taf[])Arrays.stream(tafs).filter(
					// The TAF is still valid....
					taf -> taf.metadata.getValidityEnd().isAfter(OffsetDateTime.now(ZoneId.of("Z"))) &&
					// And there is no other taf...
					Arrays.stream(tafs).noneMatch(
							otherTaf -> (!otherTaf.equals(taf) &&
									// For this location 
									otherTaf.metadata.getLocation().equals(taf.metadata.getLocation()) &&
									// Such that the other TAF has a validity start later than *this* TAF...
									otherTaf.metadata.getValidityStart().isAfter(taf.metadata.getValidityStart()) &&
									// And the other TAF is already in its validity window
									otherTaf.metadata.getValidityStart().isBefore(OffsetDateTime.now(ZoneId.of("Z"))))
							)).toArray(Taf[]::new);

			return ResponseEntity.ok(tafObjectMapper.writeValueAsString(new TafList(filteredTafs,page,count)));
		}catch(Exception e){
			try {
				JSONObject obj=new JSONObject();
				obj.put("error",e.getMessage());
				String json = obj.toString();
				Debug.errprintln("Method not allowed");
				Debug.printStackTrace(e);
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
			} catch (JSONException e1) {
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);		
	}

	/**
	 * Delete a TAF by its uuid
	 * @param uuid
	 * @return ok if the TAF was successfully deleted, BAD_REQUEST if the taf didn't exist, is not in concept, or if some other error occurred
	 */
	@RequestMapping(path="/tafs/{uuid}",
			method = RequestMethod.DELETE,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> deleteTafById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		Taf taf = tafStore.getByUuid(uuid);
		if (taf == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("TAF with uuid %s does not exist", uuid));
		}
		boolean tafIsInConcept = taf.metadata.getStatus() == TAFReportPublishedConcept.concept;
		if (tafIsInConcept != true) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("TAF with uuid %s is not in concept. Cannot delete.", uuid));
		}
		boolean ret = tafStore.deleteTafByUuid(uuid);
		if(ret) {
			return ResponseEntity.ok(String.format("deleted %s", uuid));
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
	}


	@RequestMapping(path="/tafs/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Taf getTafById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		return tafStore.getByUuid(uuid);
	}

	@RequestMapping(path="/tafs/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.TEXT_PLAIN_VALUE)
	public String getTacById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		return tafStore.getByUuid(uuid).toTAC();
	}

	@RequestMapping(path="/tafs/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_XML_VALUE)
	public String getIWXXM21ById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		Taf taf=tafStore.getByUuid(uuid);
		return tafConverter.ToIWXXM_2_1(taf);
	}

	/* Deprecated */
	@RequestMapping(path="/gettaf")
	public Taf getTaf(@RequestParam(value="uuid", required=true) String uuid) throws JsonParseException, JsonMappingException, IOException {
		return tafStore.getByUuid(uuid);
	}

	@RequestMapping("/publishtaf")
	public String publishTaf(String uuid) throws JsonParseException, JsonMappingException, IOException {
		return "taf "+tafStore.getByUuid(uuid)+" published";
	}
}
