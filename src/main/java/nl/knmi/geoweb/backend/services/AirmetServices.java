package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
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
import nl.knmi.geoweb.backend.admin.AdminStore;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.product.airmet.Airmet;
import nl.knmi.geoweb.backend.product.airmet.AirmetParameters;
import nl.knmi.geoweb.backend.product.airmet.AirmetPhenomenaMapping;
import nl.knmi.geoweb.backend.product.airmet.AirmetPhenomenaMapping.AirmetPhenomenon;
import nl.knmi.geoweb.backend.product.airmet.ObscuringPhenomenonList.ObscuringPhenomenon;
import nl.knmi.geoweb.backend.product.airmet.AirmetStore;
import nl.knmi.geoweb.backend.product.airmet.AirmetValidationResult;
import nl.knmi.geoweb.backend.product.airmet.AirmetValidator;
import nl.knmi.geoweb.backend.product.airmet.ObscuringPhenomenonList;
import nl.knmi.geoweb.backend.product.airmet.converter.AirmetConverter;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;

@Slf4j
@RestController
@RequestMapping("/airmets")
public class AirmetServices {
    @Autowired
    private AdminStore adminStore;

    @Autowired
    private AirmetStore airmetStore;

    @Autowired
    private AirmetValidator airmetValidator;

    @Autowired
    private ProductExporter<Airmet> publishAirmetStore;

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
    public synchronized ResponseEntity<JSONObject> storeJSONAirmet(@RequestBody JsonNode airmet) { // throws IOException {
        log.debug("########################################### storeairmet #######################################");
        Airmet am=null;
        try {
            am = airmetObjectMapper.treeToValue(airmet, Airmet.class);

            if (am.getStatus()== SigmetAirmetStatus.concept) {
                //Store
                if (am.getUuid()==null) {
                    am.setUuid(UUID.randomUUID().toString());
                }
                log.debug("Storing "+am.getUuid());
                try{
                    airmetStore.storeAirmet(am);
                    JSONObject airmetJson= new JSONObject(am.toJSON(airmetObjectMapper));
                    JSONObject json = new JSONObject()
                            .put("succeeded", true)
                            .put("message","airmet "+am.getUuid()+" stored")
                            .put("uuid",am.getUuid())
                            .put("airmetjson", airmetJson);
                    return ResponseEntity.ok(json);
                }catch(Exception e){
                    try {
                        JSONObject obj=new JSONObject();
                        obj.put("error",e.getMessage());
                        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(obj);
                    } catch (JSONException e1) {
                    }
                }
            } else if (am.getStatus()==SigmetAirmetStatus.published) {
                //publish
                am.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
                am.setSequence(airmetStore.getNextSequence(am));
                log.debug("Publishing "+am.getUuid());
                try{
                    Feature firFeature=firStore.lookup(am.getLocation_indicator_icao(), true);

                    am.setFirFeature(firFeature);
                    synchronized (airmetStore){ //Lock on airmetStore
                        if (airmetStore.isPublished(am.getUuid())) {
                            //Already published
                            JSONObject airmetJson = new JSONObject(am.toJSON(airmetObjectMapper));
                            JSONObject json = new JSONObject()
                                    .put("succeeded", false)
                                    .put("message", "airmet " + am.getUuid() + " is already published")
                                    .put("uuid", am.getUuid())
                                    .put("airmetjson", airmetJson);
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
                        } else {
                            String result = publishAirmetStore.export(am, airmetConverter, airmetObjectMapper);
                            if (result.equals("OK")) {
                                airmetStore.storeAirmet(am);
                                JSONObject airmetJson = new JSONObject(am.toJSON(airmetObjectMapper));
                                JSONObject json = new JSONObject()
                                        .put("succeeded", true)
                                        .put("message", "airmet " + am.getUuid() + " published")
                                        .put("uuid", am.getUuid())
                                        .put("airmetjson", airmetJson);
                                return ResponseEntity.ok(json);
                            } else {
                                JSONObject json = new JSONObject()
                                        .put("succeeded", false)
                                        .put("message", "airmet " + am.getUuid() + " failed to publish")
                                        .put("uuid", am.getUuid());
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
            } else if (am.getStatus()==SigmetAirmetStatus.canceled) {
                //cancel
                Airmet toBeCancelled = airmetStore.getByUuid(am.getUuid()); //Has to have status published and an uuid
                Airmet cancelAirmet = new Airmet(toBeCancelled);
                toBeCancelled.setStatus(SigmetAirmetStatus.canceled);
                cancelAirmet.setUuid(UUID.randomUUID().toString());
                cancelAirmet.setStatus(SigmetAirmetStatus.published);
                cancelAirmet.setCancels(toBeCancelled.getSequence());
                cancelAirmet.setCancelsStart(toBeCancelled.getValiddate());
                OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z"));
                OffsetDateTime start = toBeCancelled.getValiddate();
                if (now.isBefore(start)) {
                    cancelAirmet.setValiddate(start);
                }
                else {
                    cancelAirmet.setValiddate(now);
                }
                cancelAirmet.setValiddate_end(toBeCancelled.getValiddate_end());
                cancelAirmet.setIssuedate(now);
                cancelAirmet.setSequence(airmetStore.getNextSequence(cancelAirmet));
                log.debug("Canceling "+am.getUuid());
                try{
                    airmetStore.storeAirmet(cancelAirmet);
                    airmetStore.storeAirmet(toBeCancelled);
                    cancelAirmet.setFirFeature(firStore.lookup(cancelAirmet.getLocation_indicator_icao(), true));
                    publishAirmetStore.export(cancelAirmet, airmetConverter, airmetObjectMapper);
                    JSONObject airmetJson = new JSONObject(am.toJSON(airmetObjectMapper));
                    JSONObject json = new JSONObject()
                            .put("succeeded", true)
                            .put("message","airmet "+am.getUuid()+" canceled")
                            .put("uuid",am.getUuid())
                            .put("airmetjson", airmetJson)
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
            } else if (am.getStatus()==null) {
                //Empty airmet
                try {
                    JSONObject obj=new JSONObject();
                    obj.put("error", "empty airmet");
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
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
    public ResponseEntity<JSONObject> deleteAirmetById(@PathVariable String uuid) throws JsonParseException, JsonMappingException, IOException {
        Airmet airmet = airmetStore.getByUuid(uuid);
        if (airmet == null) {
            JSONObject json = new JSONObject()
                    .put("message", String.format("AIRMET with uuid %s does not exist", uuid));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
        }
        boolean airmetIsInConcept = airmet.getStatus() == SigmetAirmetStatus.concept;
        if (airmetIsInConcept != true) {
            JSONObject json = new JSONObject()
                    .put("message", String.format("AIRMET with uuid %s is not in concept. Cannot delete.", uuid));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
        }
        boolean ret = airmetStore.deleteAirmetByUuid(uuid);
        if(ret) {
            JSONObject json = new JSONObject()
                    .put("message", String.format("deleted %s", uuid));
            return ResponseEntity.ok(json);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @RequestMapping(path="/getobscuringphenomena")
    public ResponseEntity<List<ObscuringPhenomenon>> getObscuringPhenomena() {
        try {
            List<ObscuringPhenomenon> obsPhenomena = ObscuringPhenomenonList.getAllObscuringPhenomena();
            return ResponseEntity.ok(obsPhenomena);
        }catch(Exception e){}
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @RequestMapping(path="/getairmetparameters")
    public ResponseEntity<JSONObject> getAirmetParameters() {
        try {
            /* If airmetparameters.json is not available on disk:
             * airmetparameters.json is defined in src/main/resources/adminstore/config/airmetparameters.json and
             * is copied to disk location in adminstore
             */

            String paramsFromFile = adminStore.read("config", "airmetparameters.json");
            AirmetParameters params = airmetObjectMapper.readValue(paramsFromFile, AirmetParameters.class);
            JSONObject validParam = airmetObjectMapper.convertValue(params, JSONObject.class);
            return ResponseEntity.ok(validParam);
        }catch(Exception e){
            log.debug(e.getMessage());
            JSONObject jsonResponse = new JSONObject()
                .put("message", "Unable to read airmetparameters");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResponse);
        }

    }

    @RequestMapping(path="/putairmetparameters")
    public ResponseEntity<JSONObject> storeAirmetParameters(@RequestBody AirmetParameters parameters) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @RequestMapping("/getairmetphenomena")
    public ResponseEntity<List<AirmetPhenomenon>> AirmetPhenomena() {
        try {
            List<AirmetPhenomenon> phenomena = new AirmetPhenomenaMapping().getPhenomena();
            return ResponseEntity.ok(phenomena);
        }catch(Exception e){}
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

    @RequestMapping(path = "/verify",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<JSONObject> verifyAIRMET(@RequestBody JsonNode airmetStr) throws IOException, JSONException, ParseException {
        /* Add TAC */
        String TAC = "unable to create TAC";
        Airmet airmet;
        try {
            airmet = airmetObjectMapper.treeToValue(airmetStr, Airmet.class);

            Feature fir=airmet.getFirFeature();
            if (fir==null) {
                log.debug("Adding fir geometry for "+airmet.getLocation_indicator_icao()+" automatically");
                fir=firStore.lookup(airmet.getLocation_indicator_icao(), true);
                airmet.setFirFeature(fir);
            }
            if (fir!=null) {
                TAC = airmet.toTAC(fir);
            }
        } catch (InvalidFormatException e) {
            JSONObject json = new JSONObject()
                .put("message", "Unable to parse airmet");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
        }

        try {
            AirmetValidationResult jsonValidation = airmetValidator.validate(airmetObjectMapper.writeValueAsString(airmetStr));
            if (jsonValidation.isSucceeded() == false) {
                ObjectNode errors = jsonValidation.getErrors();
                JSONObject finalJson = new JSONObject()
                        .put("succeeded", false)
                        .put("errors", airmetObjectMapper.convertValue(errors, JSONObject.class))
                        .put("TAC", TAC)
                        .put("message", "AIRMET is not valid");
                return ResponseEntity.ok(finalJson);
            } else {
                JSONObject json = new JSONObject()
                        .put("succeeded", true)
                        .put("message", "AIRMET is verified.")
                        .put("TAC", TAC);
                return ResponseEntity.ok(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject json = new JSONObject()
                    .put("message", "Unable to validate airmet");
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
    public ResponseEntity<JSONObject> AirmetIntersections(@RequestBody AirmetFeature feature) throws IOException {
        String FIRName=feature.getFirname();
        Feature FIR=firStore.lookup(FIRName, true);
        log.debug("AirmetIntersections for "+FIRName+" "+FIR);

        if (FIR!=null) {
            GeometryFactory gf=new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
            GeoJsonReader reader=new GeoJsonReader(gf);

            String FIRs=airmetObjectMapper.writeValueAsString(FIR.getGeometry()); //FIR as String
            log.debug("FIRs:"+FIRs);

            String message=null;
            Feature f=feature.getFeature();
            Feature ff=null;
            if ("fir".equals(f.getProperty("selectionType"))) {
                ff=new Feature();
                ff.setGeometry(FIR.getGeometry());
                ff.setProperty("selectionType", "poly");
            }else {
                String os=airmetObjectMapper.writeValueAsString(f.getGeometry()); //Feature as String
                log.debug("Feature os: "+os);
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
                    // TODO: Auto-generated catch block
                    e1.printStackTrace();
                    log.debug("Error with os:"+os);
                }
            }
            //		log.debug(sm.dumpAirmetGeometryInfo());
            JSONObject json;
            try {
                //				json = new JSONObject().put("message","feature "+featureId+" intersected").
                //						 put("feature", new JSONObject(airmetObjectMapper.writeValueAsString(ff))).toString();
                json = new JSONObject()
                        .put("succeeded", true)
                        .put("feature", airmetObjectMapper.convertValue(ff, JSONObject.class));
                if (message!=null) {
                    json.put("message", message);
                }
                return ResponseEntity.ok(json);
            } catch (JSONException e) {
                // TODO: Auto-generated catch block
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
    public ResponseEntity<JSONObject> getAirmetList(@RequestParam(value="active", required=true) Boolean active,
                                                @RequestParam(value="status", required=false) SigmetAirmetStatus status,
                                                @RequestParam(value="page", required=false) Integer page,
                                                @RequestParam(value="count", required=false) Integer count) {
        log.debug("getAirmetList");
        try{
            Airmet[] airmets=airmetStore.getAirmets(active, status);
            return ResponseEntity.ok(airmetObjectMapper.convertValue(new AirmetList(airmets, page, count), JSONObject.class));
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
