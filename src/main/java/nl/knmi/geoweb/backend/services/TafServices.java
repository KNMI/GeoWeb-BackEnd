package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.backend.datastore.TafStore;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportType;
import nl.knmi.geoweb.backend.product.taf.TafSchemaStore;
import nl.knmi.geoweb.backend.product.taf.TafValidator;
import nl.knmi.geoweb.backend.product.taf.converter.TafConverter;
import nl.knmi.geoweb.backend.product.taf.TafValidationResult;
@RestController
public class TafServices {
	
	TafStore tafStore;
	TafSchemaStore tafSchemaStore;
	TafValidator tafValidator;
	
	@Autowired
	private TafConverter tafConverter;
	
	TafServices (final TafStore tafStore, final TafSchemaStore tafSchemaStore, final TafValidator tafValidator) throws Exception {
		this.tafStore = tafStore;
		this.tafSchemaStore = tafSchemaStore;
		this.tafValidator = tafValidator;
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
				String json = new JSONObject().put("succeeded", true).put("message","taf verified").toString();
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
//		if(enableDebug)Debug.println("TAF from String: " + tafStr);
//		try {
//			if(enableDebug)Debug.println("start taf validation");
//			JsonNode jsonValidation = tafValidator.validate(tafStr);
//			if(jsonValidation.get("succeeded").asBoolean() == false){
//				Debug.errprintln("TAF validation failed");
//				String finalJson = new JSONObject().
//				put("succeeded", false).
//				put("errors", jsonValidation.toString()).
//				put("message","TAF is not valid").toString();
//				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(finalJson);
//			}
//		} catch (ProcessingException e3) {
//			if(enableDebug)Debug.println("TAF validator exception " + e3.getMessage());
//			e3.printStackTrace();
//			String json = null;
//			try {
//				json = new JSONObject().
//						put("message","Unable to validate taf").toString();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
//		}
		ObjectMapper objectMapper = null;
		try {
			objectMapper=Taf.getTafObjectMapperBean().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
			taf = objectMapper.readValue(tafStr, Taf.class);
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
		if(enableDebug)Debug.println("TAF from Object: " + taf.toJSON());
		// Assert that the JSONs are equal regardless of order
		final JsonNode tree1 = objectMapper.readTree(tafStr);
		final JsonNode tree2 = objectMapper.readTree(taf.toJSON());
		if(!tree1.equals(tree2)) {
			throw new IllegalArgumentException("TAF JSON is different from origional JSON");
		} else {
			Debug.println("OK: Incoming TAF string is equal to serialized and deserialized TAF string");
		}
		if(taf.metadata.getUuid() != null){
			// TODO Check if existing TAF in store is not in published state
			Debug.println("Overwriting TAF with uuid ["+taf.metadata.getUuid()+"]");
		} else {
			taf.metadata.setUuid(UUID.randomUUID().toString());
		}
		if (taf.metadata.getType() == null) {
			taf.metadata.setType(TAFReportType.normal);
		}
		
		if (taf.metadata.getStatus() == null) {
			taf.metadata.setStatus(TAFReportPublishedConcept.concept);
		}
		taf.metadata.setIssueTime(OffsetDateTime.now(ZoneId.of("UTC"))); //Set only during concept

		if (taf.metadata.getStatus() != TAFReportPublishedConcept.concept ){
			try {
				// We enforce this to check our TAF code, should always validate <-- But not for saving concept tafs.
				TafValidationResult tafValidationReport = tafValidator.validate(taf);
				if(tafValidationReport.isSucceeded() == false){
					Debug.errprintln(tafValidationReport.toString());
					try {
						String json = new JSONObject().
								put("validationreport", new JSONObject(tafValidationReport.getErrors().toString())).
								put("succeeded", false).
								put("message","Saving TAF has failed: Unable to validate.").
								put("uuid",taf.metadata.getUuid()).toString();
						
						Debug.errprintln(tafValidationReport.toString());
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			} catch (ProcessingException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		
		try{
			tafStore.storeTaf(taf);
			String tacString = "<Unable to generate TAC>";
			tacString = taf.toTAC();
			String json = new JSONObject().put("succeeded", true).put("message","Taf with id "+taf.metadata.getUuid()+" is stored").put("tac", tacString).put("uuid",taf.metadata.getUuid()).toString();
			return ResponseEntity.ok(json);
		}catch(Exception e){
		    e.printStackTrace();
			try {
				JSONObject obj=new JSONObject();
				obj.put("error",e.getMessage());
				String json = obj.toString();
				Debug.errprintln("Method not allowed" + json);
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
			} catch (JSONException e1) {
			}
		}
		Debug.errprintln("Unknown error");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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
			Taf[] tafs=tafStore.getTafs(active, status,uuid,location);
			ObjectMapper mapper = Taf.getObjectMapperBean();
			return ResponseEntity.ok(mapper.writeValueAsString(new TafList(tafs,page,count)));
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
		if (tafIsInConcept == false) {
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
