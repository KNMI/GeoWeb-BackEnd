package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;
import nl.knmi.geoweb.backend.product.sigmet.SigmetParameters;
import nl.knmi.geoweb.backend.product.sigmet.SigmetPhenomenaMapping;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;

@RestController
@RequestMapping("/sigmet")
public class SigmetServices {

	SigmetStore sigmetStore = null;

	SigmetServices (final SigmetStore sigmetStore) throws IOException {
		this.sigmetStore = sigmetStore;
	}
	public static final String DATEFORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	public static ObjectMapper getSigmetObjectMapper() {
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new JavaTimeModule());
		om.setTimeZone(TimeZone.getTimeZone("UTC"));
		om.setDateFormat(new SimpleDateFormat(DATEFORMAT_ISO8601));
		om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		return om;
	}
	
	@RequestMapping(
			path = "/storesigmet", 
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> storeJSONSigmet(@RequestBody String sigmet) throws IOException {
		Debug.println("storesigmet");
		ObjectMapper om = getSigmetObjectMapper();
		Sigmet sm = om.readValue(sigmet, Sigmet.class);
		System.out.println(sm);
		sm.setUuid(UUID.randomUUID().toString());
		try{
			sigmetStore.storeSigmet(sm);
			String json = new JSONObject().put("message","sigmet "+sm.getUuid()+" stored").put("uuid",sm.getUuid()).toString();
			return ResponseEntity.ok(json);
		}catch(Exception e){
			try {
				JSONObject obj=new JSONObject();
				obj.put("error",e.getMessage());
				String json = obj.toString();
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
			} catch (JSONException e1) {
			}
		}
		Debug.errprintln("Unknown error");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
	}

	@RequestMapping(path="/getsigmet")
	public Sigmet getSigmet(@RequestParam(value="uuid", required=true) String uuid) {
		return sigmetStore.getByUuid(uuid);
	}

	@RequestMapping(path="/publishsigmet")
	public synchronized String publishSigmet(@RequestParam(value="uuid", required=true) String uuid) {
		Debug.println("publish");
		Sigmet sigmet = sigmetStore.getByUuid(uuid);
		sigmet.setStatus(SigmetStatus.PUBLISHED);
		sigmet.setIssuedate(OffsetDateTime.now());
		sigmet.setSequence(sigmetStore.getNextSequence());
		sigmetStore.storeSigmet(sigmet);
		return "sigmet "+sigmetStore.getByUuid(uuid)+" published";
	}
	
	@RequestMapping(path="/getsigmetparameters")
	public SigmetParameters getSigmetParameters() {
		return new SigmetParameters();
	}
	
	@RequestMapping(path="/putsigmetparameters")
	public ResponseEntity<String> storeSigmetParameters(String json) { 
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}
	
	@RequestMapping("/getsigmetphenomena")
	public ResponseEntity<String> SigmetPhenomena() {
		try {
		  ObjectMapper mapper = new ObjectMapper();
			return ResponseEntity.ok(mapper.writeValueAsString(new SigmetPhenomenaMapping().getPhenomena()));
		}catch(Exception e){}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	
	@Getter
	private class SigmetList {
		Sigmet[] sigmets;
		int page;
		int npages;
		int nsigmets;
		SigmetList(Sigmet sigmets [], Integer page, Integer count){
			int numsigmets=sigmets.length;
			int first;
			int last;
			if(count == null){
				count = 0;
			}
			if(page == null){
				page = 0;
			}
			if (count!=0){
				/* Select all sigmets for requested page/count*/
				if (numsigmets<=count) {
					first=0;
					last=numsigmets;
				}else {
					first=page*count;
					last=Math.min(first+count, numsigmets);
				}
				this.npages = (numsigmets / count) + ((numsigmets % count) > 0 ? 1:0 );
			} else {
				/* Select all sigmets when count or page are not set*/
				first=0;
				last=numsigmets;
				this.npages = 1;
			}
			if(first < numsigmets && first >= 0 && last >= first && page < this.npages){
				this.sigmets = Arrays.copyOfRange(sigmets, first, last);
			}
			this.page = page;
			this.nsigmets = numsigmets;
		}
	}

	@RequestMapping(
			path = "/getsigmetlist",
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> getSigmetList(@RequestParam(value="active", required=true) Boolean active, 
			@RequestParam(value="status", required=false) SigmetStatus status,
			@RequestParam(value="page", required=false) Integer page,
			@RequestParam(value="count", required=false) Integer count) {
		Debug.println("getSigmetList");
		try{
		  Sigmet[] sigmets=sigmetStore.getSigmets(active, status);
		  ObjectMapper mapper = new ObjectMapper();
			return ResponseEntity.ok(mapper.writeValueAsString(new SigmetList(sigmets,page,count)));
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
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);		
	}



	@RequestMapping("/cancelsigmet")
	public String cancelSigmet(String uuid) {
		return "sigmet "+uuid+" canceled";
	}
}
