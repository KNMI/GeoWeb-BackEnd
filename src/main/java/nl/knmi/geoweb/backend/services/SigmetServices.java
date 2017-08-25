package nl.knmi.geoweb.backend.services;

import java.io.IOException;
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

import com.fasterxml.jackson.databind.ObjectMapper;

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

	static SigmetStore store = null;

	SigmetServices () throws IOException {
		store = new SigmetStore("/tmp/sigmets/");
	}
	@RequestMapping(
			path = "/storesigmet", 
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> storeJSONSigmet(@RequestBody Sigmet sigmet) throws IOException {
		Debug.println("storesigmet");
		//ObjectMapper om = new ObjectMapper();
		Sigmet sm = null;
//		sigmet = URLDecoder.decode(sigmet,"UTF-8");
//		try {
//			sm = om.readValue(sigmet, Sigmet.class);
//		} catch (IOException e2) {
//			e2.printStackTrace();
//			throw e2;
//		}
		sm = sigmet;

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
