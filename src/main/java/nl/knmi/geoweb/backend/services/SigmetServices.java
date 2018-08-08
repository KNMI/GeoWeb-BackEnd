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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;
import nl.knmi.geoweb.backend.product.sigmet.SigmetParameters;
import nl.knmi.geoweb.backend.product.sigmet.SigmetPhenomenaMapping;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;
import nl.knmi.geoweb.backend.product.sigmet.converter.SigmetConverter;


@RestController
@RequestMapping("/sigmets")
public class SigmetServices {
	final static String baseUrl="/sigmets";

	SigmetStore sigmetStore=null;
	private ProductExporter<Sigmet> publishSigmetStore;

	SigmetServices (final SigmetStore sigmetStore, final ProductExporter<Sigmet> publishSigmetStore) throws IOException {
		Debug.println("INITING SigmetServices...");
		this.sigmetStore = sigmetStore;
		this.publishSigmetStore=publishSigmetStore;
	}

	@Autowired
	@Qualifier("sigmetObjectMapper")
	private ObjectMapper sigmetObjectMapper;

	@Autowired
	SigmetConverter sigmetConverter;

	@Autowired
	private FIRStore firStore;

	//Store sigmet, publish or cancel
	@RequestMapping(
			path = "", 
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> storeJSONSigmet(@RequestBody String sigmet) { // throws IOException {
		Debug.println("storesigmet: "+sigmet);
		Sigmet sm=null;
		try {
			sm = sigmetObjectMapper.readValue(sigmet, Sigmet.class);

			if (sm.getStatus()==SigmetStatus.concept) {
				//Store
				if (sm.getUuid()==null) {
				  sm.setUuid(UUID.randomUUID().toString());
				}
				Debug.println("Storing "+sm.getUuid());
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
			} else if (sm.getStatus()==SigmetStatus.published) {
				//publish
				sm.setIssuedate(OffsetDateTime.now());
				sm.setSequence(sigmetStore.getNextSequence());
				Debug.println("Publishing "+sm.getUuid());
				try{
					sigmetStore.storeSigmet(sm);
					sm.setFirFeature(firStore.lookup(sm.getLocation_indicator_icao(), true));
					publishSigmetStore.export(sm, sigmetConverter, sigmetObjectMapper);
					String json = new JSONObject().put("message","sigmet "+sm.getUuid()+" published").put("uuid",sm.getUuid()).toString();
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
			} else if (sm.getStatus()==SigmetStatus.canceled) {
				//cancel
				Sigmet toBeCancelled = sigmetStore.getByUuid(sm.getUuid()); //Has to have status published and an uuid
				Sigmet cancelSigmet = new Sigmet(toBeCancelled);
				toBeCancelled.setStatus(SigmetStatus.canceled);
				cancelSigmet.setUuid(UUID.randomUUID().toString());
				cancelSigmet.setStatus(SigmetStatus.published);
				cancelSigmet.setCancels(toBeCancelled.getSequence());
				cancelSigmet.setCancelsStart(toBeCancelled.getValiddate());
				OffsetDateTime start = OffsetDateTime.now();
				cancelSigmet.setValiddate(start);
				cancelSigmet.setValiddate_end(toBeCancelled.getValiddate_end());
				cancelSigmet.setIssuedate(start);
				cancelSigmet.setSequence(sigmetStore.getNextSequence());
				Debug.println("Canceling "+sm.getUuid());
				try{
					sigmetStore.storeSigmet(cancelSigmet);
					sigmetStore.storeSigmet(toBeCancelled);
					cancelSigmet.setFirFeature(firStore.lookup(cancelSigmet.getLocation_indicator_icao(), true));
					publishSigmetStore.export(toBeCancelled, sigmetConverter, sigmetObjectMapper);
					String json = new JSONObject().put("message","sigmet "+sm.getUuid()+" canceled").put("uuid",sm.getUuid()).toString();
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
			} else if (sm.getStatus()==null) {
				//Empty sigmet
				try {
					JSONObject obj=new JSONObject();
					obj.put("error", "empty sigmet");
					String json = obj.toString();
					return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
				} catch (JSONException e1) {
				}
			}
		} catch (JsonParseException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (JsonMappingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
//		try{
//			sigmetStore.storeSigmet(sm);
//			String json = new JSONObject().put("message","sigmet "+sm.getUuid()+" stored").put("uuid",sm.getUuid()).toString();
//			return ResponseEntity.ok(json);
//		}catch(Exception e){
//			try {
//				JSONObject obj=new JSONObject();
//				obj.put("error",e.getMessage());
//				String json = obj.toString();
//				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
//			} catch (JSONException e1) {
//			}
//		}
		Debug.errprintln("Unknown error");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
	}

	@RequestMapping(path="/{uuid}", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Sigmet getSigmetAsJson(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		return sigmetStore.getByUuid(uuid);
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

//	@RequestMapping(path="/publishsigmet") //TODO use store
//	public synchronized String publishSigmet(@RequestParam(value="uuid", required=true) String uuid) {
//		Debug.println("publish");
//		Sigmet sigmet = sigmetStore.getByUuid(uuid);
//		sigmet.setStatus(SigmetStatus.published);
//		sigmet.setIssuedate(OffsetDateTime.now());
//		sigmet.setSequence(sigmetStore.getNextSequence());
//		sigmetStore.storeSigmet(sigmet);
//		return "sigmet "+sigmetStore.getByUuid(uuid)+" published";
//	}

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

	@Getter
	public static class SigmetFeature {
		private String firname;
		private Feature feature;
		public SigmetFeature() {
		}
	}

	@RequestMapping(
			path = "/sigmetintersections", 
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> SigmetIntersections(@RequestBody SigmetFeature feature) throws IOException {
		String FIRName=feature.getFirname();
		Feature FIR=firStore.lookup(FIRName, true);
		Debug.println("SigmetIntersections for "+FIRName+" "+FIR);

		if (FIR!=null) {
			GeometryFactory gf=new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
			GeoJsonReader reader=new GeoJsonReader(gf);

			String FIRs=sigmetObjectMapper.writeValueAsString(FIR.getGeometry()); //FIR as String

			Feature f=feature.getFeature();

			String os=sigmetObjectMapper.writeValueAsString(f.getGeometry()); //Feature as String
			Debug.println("FIRs:"+FIRs);
			Feature ff=null;
			try {
				Geometry geom_fir=reader.read(FIRs);
				Geometry geom_s=reader.read(os);
				Geometry geom_new=geom_s.intersection(geom_fir);
				GeoJsonWriter writer=new GeoJsonWriter();
				String geom_news=writer.write(geom_new);
				GeoJsonObject intersect_geom=sigmetObjectMapper.readValue(geom_news, GeoJsonObject.class);
				ff=new Feature();
				ff.setGeometry(intersect_geom);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.err.println("os:"+os);

			}

			//		Debug.println(sm.dumpSigmetGeometryInfo());		
			String json;
			try {
				//				json = new JSONObject().put("message","feature "+featureId+" intersected").
				//						 put("feature", new JSONObject(sigmetObjectMapper.writeValueAsString(ff))).toString();
				json = new JSONObject(sigmetObjectMapper.writeValueAsString(ff)).toString();
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
		int count;
		SigmetList(Sigmet sigmets [], Integer page, Integer cnt){
			int numsigmets=sigmets.length;
			if(cnt == null){
				this.count = 0;
			}
			if(page == null){
				page = 0;
			}
			if (numsigmets==0) {
				this.npages=1;
				this.nsigmets=0;
				this.sigmets=new Sigmet[0];
			} else {
				int first;
				int last;
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
	}

	@RequestMapping(
			path = "",
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> getSigmetList(@RequestParam(value="active", required=true) Boolean active, 
			@RequestParam(value="status", required=false) SigmetStatus status,
			@RequestParam(value="page", required=false) Integer page,
			@RequestParam(value="count", required=false) Integer count) {
		Debug.println("getSigmetList");
		try{
			Sigmet[] sigmets=sigmetStore.getSigmets(active, status);
			Debug.println("SIGMETLIST has length of "+sigmets.length);
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

//	@RequestMapping("/cancelsigmet") //TODO via store
//	public String cancelSigmet(@RequestParam(value="uuid", required=true) String uuid) throws JsonParseException, JsonMappingException, JsonProcessingException, IOException {
//		Sigmet toBeCancelled = sigmetStore.getByUuid(uuid);
//		Sigmet sm = new Sigmet(toBeCancelled);
//		toBeCancelled.setStatus(SigmetStatus.canceled);
//		sm.setUuid(UUID.randomUUID().toString());
//		sm.setStatus(SigmetStatus.published);
//		sm.setCancels(toBeCancelled.getSequence());
//		sm.setCancelsStart(toBeCancelled.getValiddate());
//		OffsetDateTime start = OffsetDateTime.now();
//		sm.setValiddate(start);
//		sm.setValiddate_end(toBeCancelled.getValiddate_end());
//		sm.setIssuedate(start);
//		sm.setSequence(sigmetStore.getNextSequence());
//		sigmetStore.storeSigmet(sm);
//		sigmetStore.storeSigmet(toBeCancelled);
//
//		return "sigmet "+uuid+" canceled by " + sm.getUuid();
//	}
//

	@RequestMapping("/getfir")
	public Feature getFirByName(@RequestParam(value="name", required=true) String firName) {
		return firStore.lookup(firName, true);
	}
}
