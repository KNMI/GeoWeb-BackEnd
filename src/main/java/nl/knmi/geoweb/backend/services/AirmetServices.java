package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
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
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.geoweb.backend.admin.AdminStore;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.product.airmet.Airmet;
import nl.knmi.geoweb.backend.product.airmet.Airmet.AirmetStatus;
import nl.knmi.geoweb.backend.product.airmet.AirmetParameters;
import nl.knmi.geoweb.backend.product.airmet.AirmetPhenomenaMapping;
import nl.knmi.geoweb.backend.product.airmet.AirmetStore;
import nl.knmi.geoweb.backend.product.airmet.AirmetValidationResult;
import nl.knmi.geoweb.backend.product.airmet.AirmetValidator;
import nl.knmi.geoweb.backend.product.airmet.ObscuringPhenomenonList;
import nl.knmi.geoweb.backend.product.airmet.converter.AirmetConverter;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;

@RestController
@RequestMapping("/airmets")
public class AirmetServices {
	final static String baseUrl="/airmets";
	
	@Autowired
	AdminStore adminStore;

	AirmetStore airmetStore=null;
	private ProductExporter<Airmet> publishAirmetStore;

	private AirmetValidator airmetValidator;

	AirmetServices(final AirmetStore airmetStore, final AirmetValidator airmetValidator, final ProductExporter<Airmet> publishAirmetStore) throws IOException {
		Debug.println("INITING AirmetServices...");
		this.airmetStore = airmetStore;
		this.airmetValidator=airmetValidator;
		this.publishAirmetStore=publishAirmetStore;
	}

	@Autowired
	@Qualifier("airmetObjectMapper")
	private ObjectMapper airmetObjectMapper;

	@Autowired
	AirmetConverter airmetConverter;

	@Autowired
	private FIRStore firStore;

	//Store airmet, publish or cancel
	@RequestMapping(
			path = "",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public synchronized ResponseEntity<String> storeJSONAirmet(@RequestBody String airmet) { // throws IOException {
        Debug.println("########################################### storeairmet #######################################");
        Debug.println(airmet);
        Airmet am=null;
        try {
            am = airmetObjectMapper.readValue(airmet, Airmet.class);
            
            if (am.getStatus()== SigmetAirmetStatus.concept) {
                //Store
                if (am.getUuid()==null) {
                    am.setUuid(UUID.randomUUID().toString());
                }
                Debug.println("Storing "+am.getUuid());
                try{
                    airmetStore.storeAirmet(am);
                    JSONObject airmetJson= new JSONObject(am.toJSON(airmetObjectMapper));
                    JSONObject json = new JSONObject().put("succeeded", "true").
                            put("message","airmet "+am.getUuid()+" stored").
                            put("uuid",am.getUuid()).
                            put("airmetjson", airmetJson.toString());
                    return ResponseEntity.ok(json.toString());
                }catch(Exception e){
                    try {
                        JSONObject obj=new JSONObject();
                        obj.put("error",e.getMessage());
                        String json = obj.toString();
                        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
                    } catch (JSONException e1) {
                    }
                }
            } else if (am.getStatus()==SigmetAirmetStatus.published) {
                //publish
                am.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
                am.setSequence(airmetStore.getNextSequence(am));
                Debug.println("Publishing "+am.getUuid());
                try{
                    Feature firFeature=firStore.lookup(am.getLocation_indicator_icao(), true);

                    am.setFirFeature(firFeature);
                    synchronized (airmetStore){ //Lock on airmetStore
                        if (airmetStore.isPublished(am.getUuid())) {
                            //Already published
                            JSONObject airmetJson = new JSONObject(am.toJSON(airmetObjectMapper));
                            JSONObject json = new JSONObject().put("succeeded", "false").
                                    put("message", "airmet " + am.getUuid() + " is already published").
                                    put("uuid", am.getUuid()).
                                    put("airmetjson", airmetJson.toString());
                            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json.toString());
                        } else {
                            String result = publishAirmetStore.export(am, airmetConverter, airmetObjectMapper);
                            if (result.equals("OK")) {
								airmetStore.storeAirmet(am);
                                JSONObject airmetJson = new JSONObject(am.toJSON(airmetObjectMapper));
                                JSONObject json = new JSONObject().put("succeeded", "true").
                                        put("message", "airmet " + am.getUuid() + " published").
                                        put("uuid", am.getUuid()).
                                        put("airmetjson", airmetJson.toString());
                                return ResponseEntity.ok(json.toString());
                            } else {
                                JSONObject json = new JSONObject().put("succeeded", "false").
                                        put("message", "airmet " + am.getUuid() + " failed to publish").
                                        put("uuid", am.getUuid());
                                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json.toString());
                            }
                        }
                    }
                }catch(Exception e){
                    Debug.printStackTrace(e);
                    try {
                        JSONObject obj=new JSONObject();
                        obj.put("error",e.getMessage());
                        String json = obj.toString();
                        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
                    } catch (JSONException e1) {
                    }
                }
            } else if (am.getStatus()==SigmetAirmetStatus.canceled) {
                //cancel
                Airmet toBeCancelled = airmetStore.getByUuid(am.getUuid()); //Has to have status published and an uuid
                Airmet cancelAirmet = new Airmet(toBeCancelled);
                toBeCancelled.setStatus(SigmetAirmetStatus.canceled);
                cancelAirmet.setUuid(UUID.randomUUID().toString());
                cancelAirmet.setStatus(SigmetAirmetStatus.published);
                cancelAirmet.setCancels(toBeCancelled.getSequence());
                cancelAirmet.setCancelsStart(toBeCancelled.getValiddate());
                OffsetDateTime start = OffsetDateTime.now(ZoneId.of("Z"));
                cancelAirmet.setValiddate(start);
                cancelAirmet.setValiddate_end(toBeCancelled.getValiddate_end());
                cancelAirmet.setIssuedate(start);
                cancelAirmet.setSequence(airmetStore.getNextSequence(cancelAirmet));
                Debug.println("Canceling "+am.getUuid());
                try{
					airmetStore.storeAirmet(cancelAirmet);
					airmetStore.storeAirmet(toBeCancelled);
                    cancelAirmet.setFirFeature(firStore.lookup(cancelAirmet.getLocation_indicator_icao(), true));
                    publishAirmetStore.export(cancelAirmet, airmetConverter, airmetObjectMapper);
                    JSONObject airmetJson = new JSONObject(am.toJSON(airmetObjectMapper));
                    JSONObject json = new JSONObject().put("succeeded", "true").
                            put("message","airmet "+am.getUuid()+" canceled").
                            put("uuid",am.getUuid()).
                            put("airmetjson", airmetJson.toString()).
                            put("tac","");
                    return ResponseEntity.ok(json.toString());
                }catch(Exception e){
                    try {
                        JSONObject obj=new JSONObject();
                        obj.put("error",e.getMessage());
                        String json = obj.toString();
                        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
                    } catch (JSONException e1) {
                    }
                }
            } else if (am.getStatus()==null) {
                //Empty airmet
                try {
                    JSONObject obj=new JSONObject();
                    obj.put("error", "empty airmet");
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
        Debug.errprintln("Unknown error");
        JSONObject obj=new JSONObject();
        try {
			obj.put("error", "Unknown error");
		} catch (JSONException e) {
		}
        String json = obj.toString();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
    }

	@RequestMapping(path="/{uuid}", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Airmet getAirmetAsJson(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		return airmetStore.getByUuid(uuid);
	}

	@RequestMapping(path="/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.TEXT_PLAIN_VALUE)
	public String getTacById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		Airmet sm = airmetStore.getByUuid(uuid);
		Feature FIR=firStore.lookup(sm.getFirname(), true);
		return sm.toTAC(FIR);
	}

	@RequestMapping(path="/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_XML_VALUE)
	public String getIWXXM21ById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		Airmet airmet=airmetStore.getByUuid(uuid);
		return airmetConverter.ToIWXXM_2_1(airmet);
	}

	/**
	 * Delete an AIRMET by its uuid
	 * @param uuid
	 * @return ok if the AIRMET was successfully deleted, BAD_REQUEST if the AIRMET didn't exist, is not in concept, or if some other error occurred
	 */
	@RequestMapping(path="/{uuid}",
			method = RequestMethod.DELETE,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> deleteAirmetById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		Airmet airmet = airmetStore.getByUuid(uuid);
		if (airmet == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("AIRMET with uuid %s does not exist", uuid));
		}
		boolean airmetIsInConcept = airmet.getStatus() == SigmetAirmetStatus.concept;
		if (airmetIsInConcept != true) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("AIRMET with uuid %s is not in concept. Cannot delete.", uuid));
		}
		boolean ret = airmetStore.deleteAirmetByUuid(uuid);
		if(ret) {
			return ResponseEntity.ok(String.format("deleted %s", uuid));
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
	}

	@RequestMapping(path="/getobscuringphenomena")
	public ResponseEntity<String> getObscuringPhenomena() {
		try {
			return ResponseEntity.ok(airmetObjectMapper.writeValueAsString(ObscuringPhenomenonList.getAllObscuringPhenomena()));
		}catch(Exception e){}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
	}

	@RequestMapping(path="/getairmetparameters")
	public ResponseEntity<String> getAirmetParameters() {
		JSONResponse jsonResponse = new JSONResponse();
		try {
			/* If airmetparameters.json is not available on disk:
			 * airmetparameters.json is defined in src/main/resources/adminstore/config/airmetparameters.json and
			 * is copied to disk location in adminstore
			 */
			
			String validParam = airmetObjectMapper.writeValueAsString(
					airmetObjectMapper.readValue(
							adminStore.read("config", "airmetparameters.json"),
							AirmetParameters.class
							)
					);
			return ResponseEntity.ok(validParam);
		}catch(Exception e){
			Debug.println(e.getMessage());
			jsonResponse.setErrorMessage("Unable to read airmetparameters", 400);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonResponse.getMessage());
		}
				
	}

	@RequestMapping(path="/putairmetparameters")
	public ResponseEntity<String> storeAirmetParameters(String json) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	@RequestMapping("/getairmetphenomena")
	public ResponseEntity<String> AirmetPhenomena() {
		try {
			return ResponseEntity.ok(airmetObjectMapper.writeValueAsString(new AirmetPhenomenaMapping().getPhenomena()));
		}catch(Exception e){}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	@RequestMapping(path = "/verify", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE
	)
	public ResponseEntity<String> verifyAIRMET(@RequestBody String airmetStr) throws IOException, JSONException, ParseException {
	    airmetStr = URLDecoder.decode(airmetStr, "UTF8");
		/* Add TAC */
		String TAC = "unable to create TAC";
		try {
			Airmet airmet = airmetObjectMapper.readValue(airmetStr, Airmet.class);

			Feature fir=airmet.getFirFeature();
			if (fir==null) {
				Debug.println("Adding fir geometry for "+airmet.getLocation_indicator_icao()+" automatically");
			    fir=firStore.lookup(airmet.getLocation_indicator_icao(), true);
                airmet.setFirFeature(fir);
            }
			if (fir!=null) {
				TAC = airmet.toTAC(fir);
			}
		} catch (InvalidFormatException e) {
			String json = new JSONObject().
					put("message", "Unable to parse airmet").toString();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
		}

        try {
            AirmetValidationResult jsonValidation = airmetValidator.validate(airmetStr);
            if (jsonValidation.isSucceeded() == false) {
                ObjectNode errors = jsonValidation.getErrors();
                String finalJson = new JSONObject()
                        .put("succeeded", false)
                        .put("errors", new JSONObject(errors.toString())) //TODO Get errors from validation
                                .put("TAC", TAC)
                                .put("message", "AIRMET is not valid").toString();
                return ResponseEntity.ok(finalJson);
            } else {
                String json = new JSONObject().put("succeeded", true).put("message", "AIRMET is verified.").put("TAC", TAC).toString();
                return ResponseEntity.ok(json);
            }
        } catch (Exception e) {
            Debug.printStackTrace(e);
            String json = new JSONObject().
                    put("message", "Unable to validate airmet").toString();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
        }
    }

	@Getter
	public static class AirmetFeature {
		private String firname;
		private Feature feature;
		public AirmetFeature() {
		}
	}

	@RequestMapping(
			path = "/airmetintersections",
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> AirmetIntersections(@RequestBody AirmetFeature feature) throws IOException {
		String FIRName=feature.getFirname();
		Feature FIR=firStore.lookup(FIRName, true);
		Debug.println("AirmetIntersections for "+FIRName+" "+FIR);

		if (FIR!=null) {
			GeometryFactory gf=new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
			GeoJsonReader reader=new GeoJsonReader(gf);

			String FIRs=airmetObjectMapper.writeValueAsString(FIR.getGeometry()); //FIR as String
			Debug.println("FIRs:"+FIRs);

            String message=null;
			Feature f=feature.getFeature();
			Feature ff=null;
			if ("fir".equals(f.getProperty("selectionType"))) {
				ff=new Feature();
				ff.setGeometry(FIR.getGeometry());
				ff.setProperty("selectionType", "poly");
			}else {
				String os=airmetObjectMapper.writeValueAsString(f.getGeometry()); //Feature as String
				Debug.println("Feature os: "+os);
				try {
					Geometry geom_fir=reader.read(FIRs);
					Geometry geom_s=reader.read(os);
					Geometry geom_new=geom_s.intersection(geom_fir);
					GeoJsonWriter writer=new GeoJsonWriter();
					String geom_news=writer.write(geom_new);
					String selectionType = feature.getFeature().getProperty("selectionType");
					GeoJsonObject intersect_geom=airmetObjectMapper.readValue(geom_news, GeoJsonObject.class);
					ff=new Feature();
					ff.setGeometry(intersect_geom);
					ff.setProperty("selectionType", selectionType);
					try {
						if ((((Polygon) geom_new).getCoordinates().length > 7) && (!"box".equals(f.getProperty("selectionType")))) {
							message="Intersection of the drawn polygon with the FIR-boundary has more than 6 individual points. The drawn polygon will be used for the TAC-code.";
						}
					}catch (Exception e){}
				} catch (org.locationtech.jts.io.ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					Debug.println("Error with os:"+os);
				}
			}
			//		Debug.println(sm.dumpAirmetGeometryInfo());
			JSONObject json;
			try {
				//				json = new JSONObject().put("message","feature "+featureId+" intersected").
				//						 put("feature", new JSONObject(airmetObjectMapper.writeValueAsString(ff))).toString();
				json = new JSONObject().put("succeeded", "true").
						put("feature", new JSONObject(airmetObjectMapper.writeValueAsString(ff).toString()));
				if (message!=null) {
					json.put("message", message);
				}
				return ResponseEntity.ok(json.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
	}

	@Getter
	private class AirmetList {
		Airmet[] airmets;
		int page;
		int npages;
		int nairmets;
		int count;
		AirmetList(Airmet airmets [], Integer page, Integer cnt){
			int numairmets=airmets.length;
			if(cnt == null){
				this.count = 0;
			}
			if(page == null){
				page = 0;
			}
			if (numairmets==0) {
				this.npages=1;
				this.nairmets=0;
				this.airmets=new Airmet[0];
			} else {
				int first;
				int last;
				if (count!=0){
					/* Select all airmets for requested page/count*/
					if (numairmets<=count) {
						first=0;
						last=numairmets;
					}else {
						first=page*count;
						last=Math.min(first+count, numairmets);
					}
					this.npages = (numairmets / count) + ((numairmets % count) > 0 ? 1:0 );
				} else {
					/* Select all airmets when count or page are not set*/
					first=0;
					last=numairmets;
					this.npages = 1;
				}
				if(first < numairmets && first >= 0 && last >= first && page < this.npages){
					this.airmets = Arrays.copyOfRange(airmets, first, last);
				}
				this.page = page;
				this.nairmets = numairmets;
			}
		}
	}

	@RequestMapping(
			path = "",
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> getAirmetList(@RequestParam(value="active", required=true) Boolean active,
			@RequestParam(value="status", required=false) SigmetAirmetStatus status,
			@RequestParam(value="page", required=false) Integer page,
			@RequestParam(value="count", required=false) Integer count) {
		Debug.println("getAirmetList");
		try{
			Airmet[] airmets=airmetStore.getAirmets(active, status);
//			Debug.println("AIRMETLIST has length of "+airmets.length);
			return ResponseEntity.ok(airmetObjectMapper.writeValueAsString(new AirmetList(airmets,page,count)));
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

	@RequestMapping("/getfir")
	public Feature getFirByName(@RequestParam(value="name", required=true) String firName) {
		return firStore.lookup(firName, true);
	}
}
