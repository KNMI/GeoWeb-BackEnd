package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.geoweb.backend.admin.AdminStore;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.SigmetParameters;
import nl.knmi.geoweb.backend.product.sigmet.SigmetPhenomenaMapping;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;
import nl.knmi.geoweb.backend.product.sigmet.SigmetValidationResult;
import nl.knmi.geoweb.backend.product.sigmet.SigmetValidator;
import nl.knmi.geoweb.backend.product.sigmet.converter.SigmetConverter;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;

@Slf4j
@RestController
@RequestMapping("/sigmets")
public class SigmetServices {
    final static String baseUrl = "/airmets";

    @Autowired
    private AdminStore adminStore;

    @Autowired
    private SigmetStore sigmetStore;

    @Autowired
    private SigmetValidator sigmetValidator;

    @Autowired
    private ProductExporter<Sigmet> publishSigmetStore;

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
    public synchronized ResponseEntity<JSONObject> storeJSONSigmet(@RequestBody JsonNode sigmet) { // throws IOException {
        log.debug("########################################### storesigmet #######################################");
        Sigmet sm=null;
        try {
            sm = sigmetObjectMapper.treeToValue(sigmet, Sigmet.class);

            if (sm.getStatus()==SigmetAirmetStatus.concept) {
                //Store
                if (sm.getUuid()==null) {
                    sm.setUuid(UUID.randomUUID().toString());
                }
                log.debug("Storing "+sm.getUuid());
                try{
                    sigmetStore.storeSigmet(sm);
                    JSONObject sigmetJson = new JSONObject(sm.toJSON(sigmetObjectMapper));
                    JSONObject json = new JSONObject()
                            .put("succeeded", "true")
                            .put("message","sigmet "+sm.getUuid()+" stored")
                            .put("uuid",sm.getUuid())
                            .put("sigmetjson", sigmetJson);
                    return ResponseEntity.ok(json);
                }catch(Exception e){
                    try {
                        JSONObject obj=new JSONObject();
                        obj.put("error",e.getMessage());
                        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(obj);
                    } catch (JSONException e1) {
                    }
                }
            } else if (sm.getStatus()==SigmetAirmetStatus.published) {
                //publish
                sm.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
                sm.setSequence(sigmetStore.getNextSequence(sm));
                log.debug("Publishing "+sm.getUuid());
                try{
                    Feature firFeature=firStore.lookup(sm.getLocation_indicator_icao(), true);

                    sm.setFirFeature(firFeature);
                    synchronized (sigmetStore){ //Lock on sigmetStore
                        if (sigmetStore.isPublished(sm.getUuid())) {
                            //Already published
                            JSONObject sigmetJson = new JSONObject(sm.toJSON(sigmetObjectMapper));
                            JSONObject json = new JSONObject().put("succeeded", "false").
                                    put("message", "sigmet " + sm.getUuid() + " is already published").
                                    put("uuid", sm.getUuid()).
                                    put("sigmetjson", sigmetJson);
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
                        } else {
                            String result = publishSigmetStore.export(sm, sigmetConverter, sigmetObjectMapper);
                            if (result.equals("OK")) {
                                sigmetStore.storeSigmet(sm);
                                JSONObject sigmetJson = new JSONObject(sm.toJSON(sigmetObjectMapper));
                                JSONObject json = new JSONObject().put("succeeded", "true").
                                        put("message", "sigmet " + sm.getUuid() + " published").
                                        put("uuid", sm.getUuid()).
                                        put("sigmetjson", sigmetJson);
                                return ResponseEntity.ok(json);
                            } else {
                                JSONObject json = new JSONObject().put("succeeded", "false").
                                        put("message", "sigmet " + sm.getUuid() + " failed to publish").
                                        put("uuid", sm.getUuid());
                                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
                            }
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    try {
                        JSONObject obj=new JSONObject();
                        obj.put("error",e.getMessage());
                        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(obj);
                    } catch (JSONException e1) {
                    }
                }
            } else if (sm.getStatus()==SigmetAirmetStatus.canceled) {
                //cancel
                Sigmet toBeCancelled = sigmetStore.getByUuid(sm.getUuid()); //Has to have status published and an uuid
                Sigmet cancelSigmet = new Sigmet(toBeCancelled);
                toBeCancelled.setStatus(SigmetAirmetStatus.canceled);
                cancelSigmet.setUuid(UUID.randomUUID().toString());
                cancelSigmet.setStatus(SigmetAirmetStatus.published);
                cancelSigmet.setCancels(toBeCancelled.getSequence());
                cancelSigmet.setCancelsStart(toBeCancelled.getValiddate());
                OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z"));
                OffsetDateTime start = toBeCancelled.getValiddate();
                if (now.isBefore(start)) {
                    cancelSigmet.setValiddate(start);
                }
                else {
                    cancelSigmet.setValiddate(now);
                }
                cancelSigmet.setValiddate_end(toBeCancelled.getValiddate_end());
                cancelSigmet.setIssuedate(now);
                cancelSigmet.setSequence(sigmetStore.getNextSequence(cancelSigmet));
                /* This is done to facilitate move_to, this is the only property which can be adjusted during sigmet cancel */
                cancelSigmet.setVa_extra_fields(sm.getVa_extra_fields());
                log.debug("Canceling "+sm.getUuid());
                try{
                    sigmetStore.storeSigmet(cancelSigmet);
                    sigmetStore.storeSigmet(toBeCancelled);
                    cancelSigmet.setFirFeature(firStore.lookup(cancelSigmet.getLocation_indicator_icao(), true));
                    publishSigmetStore.export(cancelSigmet, sigmetConverter, sigmetObjectMapper);
                    JSONObject sigmetJson = new JSONObject(sm.toJSON(sigmetObjectMapper));
                    JSONObject json = new JSONObject()
                            .put("succeeded", "true")
                            .put("message","sigmet "+sm.getUuid()+" canceled")
                            .put("uuid",sm.getUuid())
                            .put("sigmetjson", sigmetJson)
                            .put("tac","");
                    return ResponseEntity.ok(json);
                }catch(Exception e){
                    try {
                        JSONObject obj=new JSONObject();
                        obj.put("error",e.getMessage());
                        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(obj);
                    } catch (JSONException e1) {
                    }
                }
            } else if (sm.getStatus()==null) {
                //Empty sigmet
                try {
                    JSONObject obj=new JSONObject();
                    obj.put("error", "empty sigmet");
                    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(obj);
                } catch (JSONException e1) {
                }
            }
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        log.debug("Unknown error");
        JSONObject obj=new JSONObject();
        try {
            obj.put("error", "Unknown error");
        } catch (JSONException e) {
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(obj);
    }

    @RequestMapping(path="/{uuid}",
            method=RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
        boolean sigmetIsInConcept = sigmet.getStatus() == SigmetAirmetStatus.concept;
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


    @RequestMapping(path="/getsigmetparameters")
    public ResponseEntity<String> getSigmetParameters() {
        JSONResponse jsonResponse = new JSONResponse();
        try {
            /* If sigmetparameters.json is not available on disk:
             * sigmetparameters.json is defined in src/main/resources/adminstore/config/sigmetparameters.json and
             * is copied to disk location in adminstore
             */

            String validParam = sigmetObjectMapper.writeValueAsString(
                    sigmetObjectMapper.readValue(
                            adminStore.read("config", "sigmetparameters.json"),
                            SigmetParameters.class
                    )
            );
            return ResponseEntity.ok(validParam);
        }catch(Exception e){
            log.debug(e.getMessage());
            jsonResponse.setErrorMessage("Unable to read sigmetparameters", 400);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonResponse.getMessage());
        }

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

    @RequestMapping(path = "/verify", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public ResponseEntity<String> verifySIGMET(@RequestBody String sigmetStr) throws IOException, JSONException, java.text.ParseException {
        sigmetStr = URLDecoder.decode(sigmetStr, "UTF8");
        /* Add TAC */
        String TAC = "unable to create TAC";
        try {
            Sigmet sigmet = sigmetObjectMapper.readValue(sigmetStr, Sigmet.class);

            Feature fir=sigmet.getFirFeature();
            if (fir==null) {
                log.debug("Adding fir geometry for "+sigmet.getLocation_indicator_icao()+" automatically");
                fir=firStore.lookup(sigmet.getLocation_indicator_icao(), true);
                sigmet.setFirFeature(fir);
            }
            if (fir!=null) {
                TAC = sigmet.toTAC(fir);
            }
        } catch (InvalidFormatException e) {
            String json = new JSONObject().
                    put("message", "Unable to parse sigmet").toString();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
        }

        try {
            SigmetValidationResult jsonValidation = sigmetValidator.validate(sigmetStr);
            if (jsonValidation.isSucceeded() == false) {
                ObjectNode errors = jsonValidation.getErrors();
                String finalJson = new JSONObject()
                        .put("succeeded", false)
                        .put("errors", new JSONObject(errors.toString())) //TODO Get errors from validation
                        .put("TAC", TAC)
                        .put("message", "SIGMET is not valid").toString();
                return ResponseEntity.ok(finalJson);
            } else {
                String json = new JSONObject().put("succeeded", true).put("message", "SIGMET is verified.").put("TAC", TAC).toString();
                return ResponseEntity.ok(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String json = new JSONObject().
                    put("message", "Unable to validate sigmet").toString();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
        }
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
        log.debug("SigmetIntersections for "+FIRName+" "+FIR);

        if (FIR!=null) {
            GeometryFactory gf=new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
            GeoJsonReader reader=new GeoJsonReader(gf);

            String FIRs=sigmetObjectMapper.writeValueAsString(FIR.getGeometry()); //FIR as String
            log.debug("FIRs:"+FIRs);

            String message=null;
            Feature f=feature.getFeature();
            Feature ff=null;
            if ("fir".equals(f.getProperty("selectionType"))) {
                ff=new Feature();
                ff.setGeometry(FIR.getGeometry());
                ff.setProperty("selectionType", "poly");
            }else {
                String os=sigmetObjectMapper.writeValueAsString(f.getGeometry()); //Feature as String
                log.debug("Feature os: "+os);
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
                        if ((((Polygon) geom_new).getCoordinates().length > 7) && (!"box".equals(f.getProperty("selectionType")))) {
                            message="Intersection of the drawn polygon with the FIR-boundary has more than 6 individual points. The drawn polygon will be used for the TAC-code.";
                        }
                    }catch (Exception e){}
                } catch (org.locationtech.jts.io.ParseException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    log.debug("Error with os:"+os);
                }
            }
            //		log.debug(sm.dumpSigmetGeometryInfo());
            JSONObject json;
            try {
                //				json = new JSONObject().put("message","feature "+featureId+" intersected").
                //						 put("feature", new JSONObject(sigmetObjectMapper.writeValueAsString(ff))).toString();
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
    public ResponseEntity<JSONObject> getSigmetList(@RequestParam(value="active", required=true) Boolean active,
                                                @RequestParam(value="status", required=false) SigmetAirmetStatus status,
                                                @RequestParam(value="page", required=false) Integer page,
                                                @RequestParam(value="count", required=false) Integer count) {
        log.debug("getSigmetList");
        try{
            Sigmet[] sigmets=sigmetStore.getSigmets(active, status);
            return ResponseEntity.ok(sigmetObjectMapper.convertValue(new SigmetList(sigmets, page, count), JSONObject.class));
        }catch(Exception e){
            try {
                JSONObject obj=new JSONObject();
                obj.put("error",e.getMessage());
                String json = obj.toString();
                log.debug("Method not allowed" + json);
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(obj);
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
