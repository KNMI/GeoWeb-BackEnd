package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;
import nl.knmi.geoweb.backend.product.sigmet.SigmetParameters;
import nl.knmi.geoweb.backend.product.sigmet.SigmetPhenomenaMapping;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;
import nl.knmi.geoweb.backend.product.sigmet.converter.SigmetConverter;
import nl.knmi.geoweb.backend.services.model.SigmetFeature;
import nl.knmi.geoweb.backend.services.view.SigmetPaginationWrapper;


@RestController
@RequestMapping("/sigmets")
public class SigmetServices {
	private static final Logger LOGGER = LoggerFactory.getLogger(SigmetServices.class);
	private SigmetStore sigmetStore;
	private ProductExporter<Sigmet> publishSigmetStore;

	SigmetServices (final SigmetStore sigmetStore, final ProductExporter<Sigmet> publishSigmetStore) throws IOException {
		LOGGER.debug("INITING SigmetServices...");
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
			path = "/ORG",
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> storeJSONSigmetORG(@RequestBody String sigmet) { // throws IOException {
		LOGGER.debug("storesigmetORG: {}", sigmet);
		Sigmet sm=null;
		try {
			sm = sigmetObjectMapper.readValue(sigmet, Sigmet.class);

			if (sm.getStatus()==SigmetStatus.concept) {
				//Store
				if (sm.getUuid()==null) {
					sm.setUuid(UUID.randomUUID().toString());
				}
				LOGGER.debug("Storing {}", sm.getUuid());
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
				sm.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
				sm.setSequence(sigmetStore.getNextSequence());
				LOGGER.debug("Publishing {}", sm.getUuid());
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
				OffsetDateTime start = OffsetDateTime.now(ZoneId.of("Z"));
				cancelSigmet.setValiddate(start);
				cancelSigmet.setValiddate_end(toBeCancelled.getValiddate_end());
				cancelSigmet.setIssuedate(start);
				cancelSigmet.setSequence(sigmetStore.getNextSequence());
				LOGGER.debug("Canceling {}", sm.getUuid());
				try{
					sigmetStore.storeSigmet(cancelSigmet);
					sigmetStore.storeSigmet(toBeCancelled);
					cancelSigmet.setFirFeature(firStore.lookup(cancelSigmet.getLocation_indicator_icao(), true));
					publishSigmetStore.export(cancelSigmet, sigmetConverter, sigmetObjectMapper);
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
		LOGGER.error("Unknown error");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
	}

	//Store sigmet, publish or cancel
	@RequestMapping(
			path = "",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> storeJSONSigmet(@RequestBody String sigmet) { // throws IOException {
		LOGGER.info("storesigmet: {}", sigmet);
        Sigmet sm=null;
        try {
            sm = sigmetObjectMapper.readValue(sigmet, Sigmet.class);

            if (sm.getStatus()==SigmetStatus.concept) {
                //Store
                if (sm.getUuid()==null) {
                    sm.setUuid(UUID.randomUUID().toString());
                }
				LOGGER.info("Storing {}", sm.getUuid());
                try{
                    sigmetStore.storeSigmet(sm);
                    JSONObject sigmetJson = new JSONObject(sm.toJSON(sigmetObjectMapper));
                    JSONObject json = new JSONObject().put("succeeded", "true").
                            put("message","sigmet "+sm.getUuid()+" stored").
                            put("uuid",sm.getUuid()).
                            put("sigmetjson", sigmetJson.toString());
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
            } else if (sm.getStatus()==SigmetStatus.published) {
                //publish
                sm.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
                sm.setSequence(sigmetStore.getNextSequence());
                LOGGER.info("Publishing {}", sm.getUuid());
                try{
                    sigmetStore.storeSigmet(sm);
                    sm.setFirFeature(firStore.lookup(sm.getLocation_indicator_icao(), true));
                    publishSigmetStore.export(sm, sigmetConverter, sigmetObjectMapper);
                    JSONObject sigmetJson = new JSONObject(sm.toJSON(sigmetObjectMapper));
                    JSONObject json = new JSONObject().put("succeeded", "true").
                            put("message","sigmet "+sm.getUuid()+" published").
                            put("uuid",sm.getUuid()).
                            put("sigmetjson", sigmetJson.toString());
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
            } else if (sm.getStatus()==SigmetStatus.canceled) {
                //cancel
                Sigmet toBeCancelled = sigmetStore.getByUuid(sm.getUuid()); //Has to have status published and an uuid
                Sigmet cancelSigmet = new Sigmet(toBeCancelled);
                toBeCancelled.setStatus(SigmetStatus.canceled);
                cancelSigmet.setUuid(UUID.randomUUID().toString());
                cancelSigmet.setStatus(SigmetStatus.published);
                cancelSigmet.setCancels(toBeCancelled.getSequence());
                cancelSigmet.setCancelsStart(toBeCancelled.getValiddate());
                OffsetDateTime start = OffsetDateTime.now(ZoneId.of("Z"));
                cancelSigmet.setValiddate(start);
                cancelSigmet.setValiddate_end(toBeCancelled.getValiddate_end());
                cancelSigmet.setIssuedate(start);
                cancelSigmet.setSequence(sigmetStore.getNextSequence());
                LOGGER.info("Canceling {}", sm.getUuid());
                try{
                    sigmetStore.storeSigmet(cancelSigmet);
                    sigmetStore.storeSigmet(toBeCancelled);
                    cancelSigmet.setFirFeature(firStore.lookup(cancelSigmet.getLocation_indicator_icao(), true));
                    publishSigmetStore.export(cancelSigmet, sigmetConverter, sigmetObjectMapper);
                    JSONObject sigmetJson = new JSONObject(sm.toJSON(sigmetObjectMapper));
                    JSONObject json = new JSONObject().put("succeeded", "true").
                            put("message","sigmet "+sm.getUuid()+" canceled").
                            put("uuid",sm.getUuid()).
                            put("sigmetjson", sigmetJson.toString()).
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
        LOGGER.error("Unknown error");
        JSONObject obj=new JSONObject();
        obj.put("error", "Unknown error");
        String json = obj.toString();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
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

	/**
	 * Delete a SIGMET by its uuid
	 * @param uuid
	 * @return ok if the SIGMET was successfully deleted, BAD_REQUEST if the SIGMET didn't exist, is not in concept, or if some other error occurred
	 */
	@RequestMapping(path="/{uuid}",
			method = RequestMethod.DELETE,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> deleteSigmetById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
		Sigmet sigmet = sigmetStore.getByUuid(uuid);
		if (sigmet == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("SIGMET with uuid %s does not exist", uuid));
		}
		boolean sigmetIsInConcept = sigmet.getStatus() == SigmetStatus.concept;
		if (sigmetIsInConcept != true) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("SIGMET with uuid %s is not in concept. Cannot delete.", uuid));
		}
		boolean ret = sigmetStore.deleteSigmetByUuid(uuid);
		if(ret) {
			return ResponseEntity.ok(String.format("deleted %s", uuid));
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}
	}

	static SigmetParameters sigmetParameters;
	@RequestMapping(path="/getsigmetparameters", method=RequestMethod.GET)
	public SigmetParameters getSigmetParameters() {
		if (sigmetParameters==null) {
			sigmetParameters=new SigmetParameters();
		}
		return sigmetParameters;
	}

	@RequestMapping(path="/putsigmetparameters", method=RequestMethod.GET)
	public ResponseEntity<String> storeSigmetParameters(String json) { 
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	@RequestMapping(path="/getsigmetphenomena", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
	public ResponseEntity<String> SigmetIntersections(@RequestBody SigmetFeature feature) throws IOException {
		String FIRName=feature.getFirname();
		Feature FIR=firStore.lookup(FIRName, true);
		LOGGER.debug("SigmetIntersections for {} {}", FIRName, FIR);

		if (FIR!=null) {
			GeometryFactory gf=new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
			GeoJsonReader reader=new GeoJsonReader(gf);

			String FIRs=sigmetObjectMapper.writeValueAsString(FIR.getGeometry()); //FIR as String
			LOGGER.debug("FIRs:{}", FIRs);

            String message=null;
			Feature f=feature.getFeature();
			Feature ff=null;
			if ("fir".equals(f.getProperty("selectionType"))) {
				ff=new Feature();
				ff.setGeometry(FIR.getGeometry());
				ff.setProperty("selectionType", "poly");
			}else {
				String os=sigmetObjectMapper.writeValueAsString(f.getGeometry()); //Feature as String
				LOGGER.debug("Feature os: {}", os);
				try {
					Geometry geom_fir=reader.read(FIRs);
					Geometry geom_s=reader.read(os);
					Geometry geom_new=geom_s.intersection(geom_fir);
					GeoJsonWriter writer=new GeoJsonWriter();
					String geom_news=writer.write(geom_new);
					String selectionType = feature.getFeature().getProperty("selectionType");
					GeoJsonObject intersect_geom=sigmetObjectMapper.readValue(geom_news, GeoJsonObject.class);
					ff=new Feature();
					ff.setGeometry(intersect_geom);
					ff.setProperty("selectionType", selectionType);
					try {
						if (((Polygon) geom_new).getCoordinates().length > 7) {
							message="Polygon has more than 6 points";
						}
					}catch (Exception e){}
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					LOGGER.debug("Error with os:{}", os);
				}
			}
			JSONObject json;
			try {
				json = new JSONObject().put("succeeded", "true").
						put("feature", new JSONObject(sigmetObjectMapper.writeValueAsString(ff).toString()));
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

	@RequestMapping(
			path = "",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> getSigmetList(@RequestParam(value="active", required=true) Boolean active, 
			@RequestParam(value="status", required=false) SigmetStatus status,
			@RequestParam(value="page", required=false) Integer page,
			@RequestParam(value="count", required=false) Integer count) {
		LOGGER.debug("getSigmetList");
		try{
			Sigmet[] sigmets=sigmetStore.getSigmets(active, status);
			LOGGER.debug("SIGMETLIST has length of {}", sigmets.length);
			return ResponseEntity.ok(sigmetObjectMapper.writeValueAsString(new SigmetPaginationWrapper(sigmets,page,count)));
		}catch(Exception e){
			try {
				JSONObject obj=new JSONObject();
				obj.put("error",e.getMessage());
				String json = obj.toString();
				LOGGER.error("Method not allowed{}", json);
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
			} catch (JSONException e1) {
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);		
	}

	@RequestMapping(path="/getfir", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Feature getFirByName(@RequestParam(value="name", required=true) String firName) {
		return firStore.lookup(firName, true);
	}
}
