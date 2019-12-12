package nl.knmi.geoweb.iwxxm_2_1.converter;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.GeoJsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.TestConfig;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.converter.SigmetConverter;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class })
public class SigmetToIWXXMTest {
    @Autowired
    @Qualifier("sigmetObjectMapper")
    private ObjectMapper sigmetObjectMapper;

    @Autowired
    private SigmetConverter sigmetConverter;

    @Autowired
    private FIRStore firStore;

    static String testGeoJson="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]},\"properties\":{\"prop0\":\"value0\",\"prop1\":{\"this\":\"that\"}}}]}";

    static String testSigmet="{\"geojson\":"
            +"{\"type\":\"FeatureCollection\",\"features\":"+"[{\"type\":\"Feature\",\"properties\":{\"selectionType\": \"poly\", \"featureFunction\": \"start\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722],[4.44963571205923,52.75852934878266]]]}}]},"
            +"\"phenomenon\":\"OBSC_TS\","
            +"\"obs_or_forecast\":{\"obs\":true, \"obsFcTime\":\"2017-03-24T15:50:00Z\"},"
            +"\"levelinfo\":{\"mode\": \"BETW\", \"levels\":[{\"value\":100.0,\"unit\":\"FL\"},{\"value\":300.0,\"unit\":\"FL\"}]},"
            +"\"movement_type\":\"STATIONARY\","
            +"\"change\":\"NC\","
            +"\"issuedate\":\"2017-03-24T15:56:16Z\","
            +"\"validdate\":\"2017-03-24T16:00:00Z\","
            +"\"validdate_end\":\"2017-03-24T22:00:00Z\","
            +"\"firname\":\"AMSTERDAM FIR\","
            +"\"status\":\"published\","
            +"\"type\":\"normal\","
            +"\"location_indicator_icao\":\"EHAA\","
            +"\"location_indicator_mwo\":\"EHDB\"}";

    static String testSigmet2="{" +
            "  \"geojson\": {" +
            "    \"type\": \"FeatureCollection\"," +
            "    \"features\": [" +
            "      {" +
            "        \"type\": \"Feature\"," +
            "        \"id\": \"id-start-1\"," +
            "        \"properties\": {" +
            "          \"featureFunction\": \"start\"," +
            "          \"selectionType\": \"poly\"  " +
            "        }," +
            "        \"geometry\": {" +
            "          \"type\": \"Polygon\"," +
            "          \"coordinates\": [" +
            "            [" +
            "              [" +
            "                4.2," +
            "                51.0" +
            "              ]," +
            "              [" +
            "                5.2," +
            "                52.0" +
            "              ]," +
            "              [" +
            "                5.2," +
            "                51.0" +
            "              ]," +
            "              [" +
            "                4.2," +
            "                51.0" +
            "              ]" +
            "            ]" +
            "          ]" +
            "        }" +
            "      }," +
            "      {" +
            "        \"type\": \"Feature\"," +
            "        \"id\": \"id-end-1\"," +
            "        \"properties\": {" +
            "          \"relatesTo\": \"id-start-1\"," +
            "          \"featureFunction\": \"end\"," +
            "          \"selectionType\": \"poly\"  " +
            "        }," +
            "        \"geometry\": {" +
            "          \"type\": \"Polygon\"," +
            "          \"coordinates\": [" +
            "            [" +
            "              [" +
            "                4.7," +
            "                51.5" +
            "              ]," +
            "              [" +
            "                5.7," +
            "                52.5" +
            "              ]," +
            "              [" +
            "                5.7," +
            "                51.5" +
            "              ]," +
            "              [" +
            "                4.7," +
            "                51.5" +
            "              ]" +
            "            ]" +
            "          ]" +
            "        }" +
            "      }" +
            "" +
            "    ]" +
            "  }," +
            "  \"phenomenon\": \"FRQ_TS\"," +
            "  \"obs_or_forecast\": {" +
            "    \"obs\": true," +
            "    \"obsFcTime\": \"2017-08-07T11:45:00Z\""+
            "  }," +
            "  \"levelinfo\": {" +
            "    \"mode\": \"TOPS\","+
            "    \"levels\": [" +
            "        {\"value\": 100, \"unit\": \"FL\"}" +
            //			"        ,{\"value\": 300, \"unit\": \"FL\"}" +
            "      ]" +
            "  }," +
            "  \"movement_type\":\"FORECAST_POSITION\"," +
            "  \"change\": \"NC\"," +
            "  \"issuedate\": \"2017-08-07T11:31:36Z\"," +
            "  \"validdate\": \"2017-08-07T12:00:00Z\"," +
            "  \"validdate_end\": \"2017-08-07T18:00:00Z\"," +
            "  \"firname\": \"AMSTERDAM FIR\"," +
            "  \"location_indicator_icao\": \"EHAA\"," +
            "  \"location_indicator_mwo\": \"EHDB\"," +
            "  \"uuid\": \"5372bd90-e17d-4824-bc18-7fee25413161\"," +
            "  \"status\": \"concept\"," +
            "  \"type\":\"normal\","+
            "  \"sequence\": 0" +
            "}";
    static String testSigmet3="{\"geojson\":"
            +"{\"type\":\"FeatureCollection\",\"features\":"
            +"[{\"type\":\"Feature\",\"id\":\"1\", \"properties\":{\"selectionType\": \"fir\", \"featureFunction\": \"start\"}}, "
            +"{\"type\":\"Feature\",\"id\":\"2\", \"properties\":{\"selectionType\": \"poly\", \"featureFunction\": \"intersection\", \"relatesTo\":\"1\"},\"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[5.2,52.0],[6.2,53.0],[6.2,52.0],[5.2,52.0]]]}}]},"
            +"\"phenomenon\":\"OBSC_TS\","
            +"\"obs_or_forecast\":{\"obs\":true, \"obsFcTime\":\"2017-03-24T15:50:00Z\"},"
            +"\"levelinfo\":{\"mode\": \"BETW\", \"levels\":[{\"value\":100.0,\"unit\":\"FL\"},{\"value\":300.0,\"unit\":\"FL\"}]},"
            +"\"movement_type\":\"STATIONARY\","
            +"\"change\":\"NC\","
            +"\"issuedate\":\"2017-03-24T15:56:16Z\","
            +"\"validdate\":\"2017-03-24T16:00:00Z\","
            +"\"validdate_end\":\"2017-03-24T22:00:00Z\","
            +"\"firname\":\"AMSTERDAM FIR\","
            +"\"status\":\"published\","
            +"\"type\":\"normal\","
            +"\"location_indicator_icao\":\"EHAA\","
            +"\"location_indicator_mwo\":\"EHDB\"}";

    static String testSigmet4="{\"geojson\":"
            +"{\"type\":\"FeatureCollection\",\"features\":"+"[{\"type\":\"Feature\",\"properties\":{\"selectionType\": \"point\", \"featureFunction\": \"start\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[5.2,52.6]}}]},"
            +"\"phenomenon\":\"OBSC_TS\","
            +"\"obs_or_forecast\":{\"obs\":true, \"obsFcTime\":\"2017-03-24T15:50:00Z\"},"
            +"\"levelinfo\":{\"mode\": \"BETW\", \"levels\":[{\"value\":100.0,\"unit\":\"FL\"},{\"value\":300.0,\"unit\":\"FL\"}]},"
            +"\"movement_type\":\"MOVEMENT\","
            +"\"movement\":{\"speed\":15.0, \"speeduom\": \"m/s\", \"dir\":\"S\"},"
            +"\"change\":\"NC\","
            +"\"issuedate\":\"2017-03-24T15:56:16Z\","
            +"\"validdate\":\"2017-03-24T16:00:00Z\","
            +"\"validdate_end\":\"2017-03-24T22:00:00Z\","
            +"\"firname\":\"AMSTERDAM FIR\","
            +"\"status\":\"published\","
            +"\"type\":\"normal\","
            +"\"location_indicator_icao\":\"EHAA\","
            +"\"location_indicator_mwo\":\"EHDB\"}";

    static String[] testSigmets= new String[] { /* testSigmet, testSigmet2, testSigmet3, testSigmet4 */ getStringFromFile(
            "nl/knmi/geoweb/iwxxm_2_1/converter/failingsigmet.json") };

    public static String getStringFromFile(String fn) {
        try {
            return Tools.readResource(fn);
        } catch (IOException e) {
            log.error("Can't read resource " + fn);
        }
        return "";
    }

    public void setGeoFromString2(Sigmet sm, String json) {
        log.trace("setGeoFromString2 " + json);
        GeoJsonObject geo;
        try {
            geo = sigmetObjectMapper.readValue(json, GeoJsonObject.class);
            sm.setGeojson(geo);
            log.debug("setGeoFromString [" + json + "] set");
            return;
        } catch (JsonParseException e) {
        } catch (JsonMappingException e) {
        } catch (IOException e) {
        }
        log.error("setGeoFromString on [" + json + "] failed");
        sm.setGeojson(null);
    }

    public void TestConversion(String s) {
        Sigmet sm = null;
        try {
            sm=sigmetObjectMapper.readValue(s, Sigmet.class);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        String res=sigmetConverter.ToIWXXM_2_1(sm);
        log.debug(res);
        log.debug("TAC: " + sm.toTAC(firStore.lookup(sm.getFirname(), true)));
    }

    @Test
    public void TestConversions(){
        for (String sm: testSigmets) {
            log.debug("Testing sigmet: " + sm);
            TestConversion(sm);
        }
    }

}
