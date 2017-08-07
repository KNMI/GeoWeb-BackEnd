package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportStatus;
import nl.knmi.geoweb.backend.product.taf.TafStore;

@RestController

public class TafServices {

	static TafStore store = null;

	TafServices () throws IOException {
		store = new TafStore("/tmp/tafs");
	}


	/**
	 * POST a TAF to the product store
	 * @param tafStr
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(
			path = "/tafs", 
			method = RequestMethod.POST, 
			consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE
			)
	public ResponseEntity<String> storeTAF(@RequestBody String  tafStr) throws IOException {
		Debug.println("storetaf");
		Taf taf = null;
		tafStr = URLDecoder.decode(tafStr,"UTF8");
		try {
			ObjectMapper objectMapper=Taf.getObjectMapperBean();
			taf = objectMapper.readValue(tafStr, Taf.class);
		} catch (IOException e2) {
			Debug.errprintln("Error parsing taf ["+tafStr+"]");
			try {
				JSONObject obj=new JSONObject();
				obj.put("error","Error parsing taf").put("exception", e2.getMessage());
				String json = obj.toString();
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
			} catch (JSONException e1) {
			}
		}
		if(taf.getUuid() != null){
			// TODO Check if existing TAF in store is not in published state
			Debug.println("Overwriting TAF with uuid ["+taf.getUuid()+"]");
		} else {
			taf.setUuid(UUID.randomUUID().toString());
		}
		taf.setIssueTime(OffsetDateTime.now(ZoneId.of("UTC"))); //Set only during concept
		try{
			store.storeTaf(taf);
			String json = new JSONObject().put("message","taf "+taf.getUuid()+" stored").put("uuid",taf.getUuid()).toString();
			return ResponseEntity.ok(json);
		}catch(Exception e){
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
			Taf[] tafs=store.getTafs(active, status,uuid,location);
			ObjectMapper mapper = Taf.getObjectMapperBean();
			return ResponseEntity.ok(mapper.writeValueAsString(new TafList(tafs,page,count)));
		}catch(Exception e){
			try {
				JSONObject obj=new JSONObject();
				obj.put("error",e.getMessage());
				String json = obj.toString();
				Debug.errprintln("Method not allowed" + json);
				Debug.printStackTrace(e);
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
			} catch (JSONException e1) {
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);		
	}

	@RequestMapping(path="/tafs/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Taf getTafById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		return store.getByUuid(uuid);
	}
	
	@RequestMapping(path="/tafs/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.TEXT_PLAIN_VALUE)
	public String getTacById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		return store.getByUuid(uuid).toTAC();
	}
	
	
	/* Deprecated */
	@RequestMapping(path="/gettaf")
	public Taf getTaf(@RequestParam(value="uuid", required=true) String uuid) throws JsonParseException, JsonMappingException, IOException {
		return store.getByUuid(uuid);
	}

	@RequestMapping("/publishtaf")
	public String publishTaf(String uuid) throws JsonParseException, JsonMappingException, IOException {
		return "taf "+store.getByUuid(uuid)+" published";
	}


}
