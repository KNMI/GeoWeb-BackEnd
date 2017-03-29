package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;

@RestController
@RequestMapping("/sigmet")
public class SigmetServices {

	final static SigmetStore store =new SigmetStore("/tmp");

	@RequestMapping(path="/storesigmet", method=RequestMethod.POST)
	public String storeSigmet(@RequestParam(value="sigmet", required=true) String sigmet) throws JsonParseException, JsonMappingException, IOException{
		Sigmet sm=new ObjectMapper().readValue(sigmet, Sigmet.class);
		sm.setUuid(UUID.randomUUID().toString());
		sm.setIssuedate(new Date());
		System.out.println("SIGMET PHENOMENON: " + sm.getPhenomenon());
		store.storeSigmet(sm);
		return "sigmet "+sm.getUuid()+" stored";
	}

	@RequestMapping(path="/getsigmet")
    public Sigmet getSigmet(@RequestParam(value="uuid", required=true) String uuid) {
    	return store.getByUuid(uuid);
    }
    
	@RequestMapping("/publishsigmet")
	public String publishSigmet(String uuid) {
		return "sigmet "+store.getByUuid(uuid)+" published";
	}

	@RequestMapping("/getsigmetlist")
	public Sigmet[] getSigmetList(@RequestParam(value="active", required=true) Boolean active, 
			@RequestParam(value="status", required=false) SigmetStatus status,
			@RequestParam(value="page", required=false) Integer page,
			@RequestParam(value="count", required=false) Integer count) {
		Sigmet[] sigmets=store.getSigmets(active, status);
		int len=sigmets.length;
		int first;
		int last;
		if ((count!=null)&&(page!=null)){
			if (len<=count) {
				first=0;
				last=len;
			}else {
				first=page*count;
				last=Math.min(first+count, len);
			}
		} else {
			first=0;
			last=len;
		}
		return Arrays.copyOfRange(sigmets, first, last);
	}

	@RequestMapping("/cancelsigmet")
	public String cancelSigmet(String uuid) {
		return "sigmet "+uuid+" canceled";
	}
}
