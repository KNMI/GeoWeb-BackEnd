package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;
import tools.Debug;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;

@RestController
@RequestMapping("/sigmet")
public class SigmetServices {

	static SigmetStore store = null;

	SigmetServices () throws NotDirectoryException {
		store = new SigmetStore("/tmp");
	}

	private static final String Sigmet = null;
	@RequestMapping(
			path = "/storesigmet", 
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> storeJSONSigmet(@RequestBody String sigmet) {
		Debug.println("storesigmet");
		Sigmet sm = null;
		try {
			sm = new ObjectMapper().readValue(URLDecoder.decode(sigmet, "UTF-8"), Sigmet.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sm.setUuid(UUID.randomUUID().toString());
		sm.setIssuedate(new Date());
		try{
			store.storeSigmet(sm);
			String json = new JSONObject().put("message","sigmet "+sm.getUuid()+" stored").put("uuid",sm.getUuid()).toString();
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

	@RequestMapping(path="/getsigmet")
	public Sigmet getSigmet(@RequestParam(value="uuid", required=true) String uuid) {
		return store.getByUuid(uuid);
	}

	@RequestMapping("/publishsigmet")
	public String publishSigmet(String uuid) {
		return "sigmet "+store.getByUuid(uuid)+" published";
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
		  Sigmet[] sigmets=store.getSigmets(active, status);
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
