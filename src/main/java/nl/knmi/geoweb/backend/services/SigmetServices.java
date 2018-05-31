package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;
import nl.knmi.geoweb.backend.product.sigmet.SigmetParameters;
import nl.knmi.geoweb.backend.product.sigmet.SigmetPhenomenaMapping;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;
import nl.knmi.geoweb.backend.product.sigmet.converter.SigmetConverter;

@RestController
@RequestMapping("/sigmet")
public class SigmetServices {

	SigmetStore sigmetStore = null;

	SigmetServices (final SigmetStore sigmetStore) throws IOException {
		this.sigmetStore = sigmetStore;
	}

	@Autowired
	@Qualifier("sigmetObjectMapper")
	private ObjectMapper sigmetObjectMapper;

	@Autowired
	SigmetConverter sigmetConverter;
	
	@Autowired
	private FIRStore firStore;

	@RequestMapping(
			path = "/storesigmet", 
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> storeJSONSigmet(@RequestBody String sigmet) throws IOException {
		Debug.println("storesigmet");
		Sigmet sm = sigmetObjectMapper.readValue(sigmet, Sigmet.class);
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

	@RequestMapping(path="/getsigmet", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Sigmet getSigmet(@RequestParam(value="uuid", required=true) String uuid) {
		return sigmetStore.getByUuid(uuid);
	}
	

	@RequestMapping(path="/getsigmet", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getSigmetAsText(@RequestParam(value="uuid", required=true) String uuid) {
		Sigmet sm = sigmetStore.getByUuid(uuid);
		System.out.println(sm);
		Feature FIR=firStore.lookup(sm.getFirname(), true);
		return sm.toTAC(FIR);
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

	@RequestMapping(path="/sendtestsigmet")
	public synchronized String sendTestSigmet() {
		Sigmet sm=new Sigmet("AMSTERDAM FIR", "EHAA", "EHDB", UUID.randomUUID().toString());
		sm.setStatus(SigmetStatus.TEST);
		OffsetDateTime start = OffsetDateTime.now().withHour(11).withMinute(0).withSecond(0).withNano(0);
		sm.setValiddate(start);
		sm.setValiddate_end(start.plusMinutes(5));
		sm.setIssuedate(OffsetDateTime.now());
		sm.setSequence(sigmetStore.getNextSequence());
		sigmetStore.storeSigmet(sm);
		return "Stored test sigmet " + sm.getUuid();
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
			return ResponseEntity.ok(sigmetObjectMapper.writeValueAsString(new SigmetPhenomenaMapping().getPhenomena()));
		}catch(Exception e){}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	@RequestMapping(
			path = "/sigmetintersections", 
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> SigmetIntersections(@RequestBody String sigmet) throws IOException {
		Debug.println("SM:"+sigmet);
		Sigmet sm = sigmetObjectMapper.readValue(sigmet, Sigmet.class);
		Debug.println(sm.dumpSigmetGeometryInfo());
		String FIRName=sm.getFirname();
		Feature FIR=firStore.lookup(FIRName, true);
		Debug.println("SigmetIntersections for "+FIRName+" "+FIR);
		//		sm.putIntersectionGeometry("abcd",FIR);
		//		sm.putIntersectionGeometry("bcde", FIRStore.cloneThroughSerialize(FIR));

		if (FIR!=null) {
			GeometryFactory gf=new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
			GeoJsonReader reader=new GeoJsonReader(gf);

			List<GeoJsonObject> intersectableGeometries=sm.findIntersectableGeometries();
			String FIRs=sigmetObjectMapper.writeValueAsString(FIR.getGeometry()); //FIR as String

			for (GeoJsonObject geom: intersectableGeometries) {
				Feature f=(Feature)geom;
				String startId=f.getId();
				Debug.println("id:"+startId);
				String os=sigmetObjectMapper.writeValueAsString(f.getGeometry()); //Feature as String
				Debug.println("os:"+os);

				Debug.println("FIRs:"+FIRs);
				try {
					Geometry geom_fir=reader.read(FIRs);
					Debug.println("geom_fir:"+geom_fir.toString());
					Geometry geom_s=reader.read(os);
					Debug.println("geom_s:"+geom_s.toString());
					Geometry geom_new=geom_s.intersection(geom_fir);
					GeoJsonWriter writer=new GeoJsonWriter();
					String geom_news=writer.write(geom_new);
					GeoJsonObject intersect_geom=sigmetObjectMapper.readValue(geom_news, GeoJsonObject.class);
					Debug.println(intersect_geom.toString());
					Feature ff=new Feature();
					ff.setGeometry(intersect_geom);
					sm.putIntersectionGeometry(startId, ff);
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}		

			//		Debug.println(sm.dumpSigmetGeometryInfo());		
			String json;
			try {
				json = new JSONObject().put("message","sigmet "+sm.getUuid()+" intersected").put("uuid",sm.getUuid())
						.put("sigmet", new JSONObject(sigmetObjectMapper.writeValueAsString(sm))).toString();
				return ResponseEntity.ok(json);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
			return ResponseEntity.ok(sigmetObjectMapper.writeValueAsString(new SigmetList(sigmets,page,count)));
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
	public String cancelSigmet(@RequestParam(value="uuid", required=true) String uuid) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
		Sigmet toBeCancelled = sigmetStore.getByUuid(uuid);
		Sigmet sm = new Sigmet(toBeCancelled);
		toBeCancelled.setStatus(SigmetStatus.CANCELLED);
		sm.setUuid(UUID.randomUUID().toString());
		sm.setStatus(SigmetStatus.PUBLISHED);
		sm.setCancels(toBeCancelled.getSequence());
		sm.setCancelsStart(toBeCancelled.getValiddate());
		OffsetDateTime start = OffsetDateTime.now();
		sm.setValiddate(start);
		sm.setValiddate_end(toBeCancelled.getValiddate_end());
		sm.setIssuedate(start);
		sm.setSequence(sigmetStore.getNextSequence());
		sigmetStore.storeSigmet(sm);
		sigmetStore.storeSigmet(toBeCancelled);

		return "sigmet "+uuid+" canceled by " + sm.getUuid();
	}
	
	@RequestMapping(path="/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.TEXT_PLAIN_VALUE)
	public String getTacById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		Sigmet sm = sigmetStore.getByUuid(uuid);
		Feature FIR=firStore.lookup(sm.getFirname(), true);
		return sm.toTAC(FIR);
	}
	
	@RequestMapping(path="/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_XML_VALUE)
	public String getIWXXM21ById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		Sigmet sigmet=sigmetStore.getByUuid(uuid);
		return sigmetConverter.ToIWXXM_2_1(sigmet);
	}
  
  @RequestMapping(path="/{uuid}", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Sigmet getSigmetAsJson(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		return sigmetStore.getByUuid(uuid);
	}

	@RequestMapping("/getfir")
	public Feature getFirByName(@RequestParam(value="name", required=true) String firName) {
		return firStore.lookup(firName, true);
	}
}
