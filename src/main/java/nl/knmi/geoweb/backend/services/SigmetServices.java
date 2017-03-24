package nl.knmi.geoweb.backend.services;

import java.util.Arrays;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;

@RestController
@RequestMapping("/sigmet")
public class SigmetServices {

	final static SigmetStore store =new SigmetStore("/tmp");

	@RequestMapping(path="/storesigmet", method=RequestMethod.POST)
	public String storeSigmet(Sigmet sigmet){
		return "sigmet "+sigmet.getUuid()+" stored";
	}

	@RequestMapping("/publishsigmet")
	public String publishSigmet(String uuid) {
		return "sigmet "+store.getByUuid(uuid)+" published";
	}

	@RequestMapping("/getsigmetlist")
	public Sigmet[] getSigmetList(@RequestParam(value="active", required=true) Boolean active, 
			@RequestParam(value="page", required=false) Integer page,
			@RequestParam(value="count", required=false) Integer count) {
		Sigmet[] sigmets=store.getSigmets(active);
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
