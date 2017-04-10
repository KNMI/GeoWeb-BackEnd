package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
import nl.knmi.geoweb.backend.product.sigmet.SigmetParameters;
import nl.knmi.geoweb.backend.product.sigmet.SigmetPhenomenaMapping;
import tools.Debug;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;

@RestController
@RequestMapping("/sigmet")
public class SigmetServices {

	final static SigmetStore store =new SigmetStore("/tmp");
	private static final String Sigmet = null;
	@RequestMapping(path="/storesigmet", method=RequestMethod.POST)
	public String storeJSONSigmet(@RequestBody String sigmet) throws JsonParseException, JsonMappingException, IOException{
		Debug.println("storesigmet");
		Sigmet sm=new ObjectMapper().readValue(URLDecoder.decode(sigmet, "UTF-8"), Sigmet.class);
		sm.setUuid(UUID.randomUUID().toString());
		sm.setIssuedate(new Date());
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
	
	@RequestMapping(path="/getsigmetparameters")
	public SigmetParameters getSigmetParameters() {
		return new SigmetParameters();
	}

	@RequestMapping("/getsigmetphenomena")
	public List<SigmetPhenomenaMapping.SigmetPhenomenon> SigmetPhenomena() {
		return new SigmetPhenomenaMapping().getPhenomena();
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
	
	@RequestMapping("/getsigmetlist")
	public SigmetList getSigmetList(@RequestParam(value="active", required=true) Boolean active, 
			@RequestParam(value="status", required=false) SigmetStatus status,
			@RequestParam(value="page", required=false) Integer page,
			@RequestParam(value="count", required=false) Integer count) {
		Debug.println("getSigmetList");
		Sigmet[] sigmets=store.getSigmets(active, status);
	
		return new SigmetList(sigmets,page,count);
	}



	@RequestMapping("/cancelsigmet")
	public String cancelSigmet(String uuid) {
		return "sigmet "+uuid+" canceled";
	}
}
