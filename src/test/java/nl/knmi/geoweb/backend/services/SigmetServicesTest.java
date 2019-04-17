package nl.knmi.geoweb.backend.services;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import nl.knmi.geoweb.backend.ApplicationConfig;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;
import nl.knmi.geoweb.backend.product.sigmet.converter.SigmetConverter;
import nl.knmi.geoweb.backend.product.sigmetairmet.ObsFc;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetChange;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetLevel;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetMovement;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetType;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(ApplicationConfig.class)
public class SigmetServicesTest {
    private String testGeoJsonBox = "{\"type\": \"FeatureCollection\",\"features\":[{\"type\": \"Feature\",\"id\": \"feb7bb38-a341-438d-b8f5-aa83685a0062\","
            + " \"properties\": {\"selectionType\": \"box\",\"featureFunction\": \"start\"},\"geometry\": {\"type\": \"Polygon\","
            + " \"coordinates\": [[[5.1618,51.4414],[5.1618,51.7424],[5.8444,51.7424],[5.8444,51.4414],[5.1618,51.4414]]]}}]}\"";

	static String uuid = "b6ea2637-4652-42cc-97ac-4e34548d3cc7";
    static String phenomenon = "OBSC_TS";
    static String startTimestamp = "2019-02-12T08:00:00Z";
    OffsetDateTime start = OffsetDateTime.parse(startTimestamp);
    OffsetDateTime end = OffsetDateTime.parse("2019-02-12T11:00:00Z");

    private Sigmet sigmet;

    private MockMvc mockMvc;

    /** The Spring web application context. */
    @Autowired
    private WebApplicationContext webApplicationContext;

    /** The {@link ObjectMapper} instance to be used. */
    @Autowired
    @Qualifier("sigmetObjectMapper")
    private ObjectMapper sigmetObjectMapper;

    @Autowired
    SigmetStore sigmetStore;

    @Autowired
    FIRStore firStore;

    @Autowired
    private ProductExporter<Sigmet> sigmetExporter;

	@Before
	public void setUp() {
        Sigmet sm = new Sigmet("AMSTERDAM FIR", "EHAA", "EHDB", uuid);
        sm.setStatus(SigmetAirmetStatus.concept);
        sm.setType(SigmetAirmetType.normal);
        sm.setPhenomenon(Sigmet.Phenomenon.getPhenomenon(phenomenon));
        sm.setValiddate(start);
        sm.setValiddate_end(end);
        sm.setObs_or_forecast(new ObsFc(true));
        sm.setChange(SigmetAirmetChange.NC);
        sm.setMovement_type(Sigmet.SigmetMovementType.STATIONARY);
        sm.setMovement(new SigmetAirmetMovement("NNE", 4, "KT"));
        sm.setLevelinfo(new SigmetAirmetLevel(
                new SigmetAirmetLevel.SigmetAirmetPart(SigmetAirmetLevel.SigmetAirmetLevelUnit.FL, 100),SigmetAirmetLevel.SigmetAirmetLevelMode.ABV));
        sm.setGeojson(mapJsonToGeoObject(testGeoJsonBox));
        sigmet = sm;
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(sigmetStore);
        reset(firStore);
        reset(sigmetExporter);
	}

	static String features="["
			+"{\"type\":\"Feature\", \"id\":\"geom-1\", \"properties\":{\"featureFunction\":\"start\", \"selectionType\":\"box\"}, \"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-4,52],[4.5,52],[4.5,55.3],[-4,55.3],[-4,52]]]}}"
			+ ",{\"type\":\"Feature\",\"id\":\"geom-2\", \"properties\":{\"featureFunction\":\"intersection\", \"selectionType\":\"poly\", \"relatesTo\":\"geom-1\"}, \"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-4,52],[4.5,52],[4.5,56],[-4,56],[-4,52]]]}}"
			+"]";

	static String testSigmet="{\"geojson\":"
			+"{\"type\":\"FeatureCollection\",\"features\":"+features+"},"
			+"\"phenomenon\":\"OBSC_TS\","
            +"\"obs_or_forecast\":{\"obs\":true},"
            + "\"uuid\": \"" + uuid + "\","
			+"\"levelinfo\":{\"levels\":[{\"value\":100.0,\"unit\":\"FL\"}], \"mode\": \"ABV\"},"
			+"\"movement_type\":\"STATIONARY\","
			+"\"change\":\"NC\","
			+"\"status\":\"concept\","
			+ "\"validdate\":\"" + startTimestamp + "\","
			+"\"validdate_end\":\"2017-03-24T15:56:16Z\","
			+"\"firname\":\"AMSTERDAM FIR\","
			+"\"location_indicator_icao\":\"EHAA\","
            +"\"location_indicator_mwo\":\"EHDB\"}";

    static String firFeature = "{"
            + "\"type\":\"Feature\","
            + "\"id\":\"geom-1\","
            + "\"properties\":{"
                + "\"FIRname\":\"AMSTERDAM FIR\""
            + "},"
            + "\"geometry\":{"
                + "\"type\":\"Polygon\","
                + "\"coordinates\":[["
                    + "[5.0,55.0],"
                    + "[2.0,51.5],"
                    + "[3.36305556,51.37305556],"
                    + "[3.33493217690869,51.234594500845624],"
                    + "[3.896195277533492,51.171788046083975],"
                    + "[4.5522567126603395,51.39763031152693],"
                    + "[5.77839523122809,51.10068033523261],"
                    + "[5.574491972044032,50.87107535958279],"
                    + "[5.6253494476352595,50.726426280074286],"
                    + "[6.14,50.74],"
                    + "[6.327379484546739,51.37958259119304],"
                    + "[6.054376944264307,51.79106877248906],"
                    + "[6.792675005827036,51.84],"
                    + "[7.102884275391208,52.21882119152389],"
                    + "[7.30,53.25],"
                    + "[6.5,53.66666667],"
                    + "[6.5,55.0],"
                    + "[5.0,55.0]"
                + "]]"
            + "}"
        + "}";

    static String testFeatureFIR = "{"
            + "\"type\":\"Feature\","
            + "\"id\":\"geom-1\","
            + "\"properties\":{"
                + "\"featureFunction\":\"start\","
                + "\"selectionType\":\"fir\""
            + "}"
        +"}";

    static String testFeature6points = "{"
            + "\"type\":\"Feature\","
            + " \"id\":\"geom-1\","
            + "\"properties\":{"
                + "\"featureFunction\":\"start\","
                + "\"selectionType\":\"poly\""
            +"},"
            + "\"geometry\":{"
                + "\"type\":\"Polygon\","
                + "\"coordinates\":[[[4,51],[4.25,51.25],[4.5,51.5],[5.5,51.5],[5.25,51.25],[5,51],[4,51]]]"
            +"}"
        + "}";

    static String testFeature8points = "{"
            + "\"type\":\"Feature\","
            + "\"id\":\"geom-1\","
            + " \"properties\":{"
                + "\"featureFunction\":\"start\","
                + " \"selectionType\":\"poly\""
            + "},"
            + "\"geometry\":{"
                + "\"type\":\"Polygon\","
                + "\"coordinates\":[[[4,51],[4.25,51.25],[4.5,51.5],[4.75,51.75],[5.75,51.75],[5.5,51.5],[5.25,51.25],[5,51],[4,51]]]"
            +"}"
        + "}";

    private GeoJsonObject mapJsonToGeoObject(String json) {
        GeoJsonObject result;
        try {
            result = sigmetObjectMapper.readValue(json, GeoJsonObject.class);
        } catch (IOException e) {
            result = null;
        }
        return result;
    }

    @Test
    public void serviceTestPostEmptySigmet() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(post("/sigmets").contentType(MediaType.APPLICATION_JSON_UTF8).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error", is("empty sigmet")));

        verifyNoMoreInteractions(sigmetStore);
    }

    @Test
    public void serviceTestPostCorrectSigmet() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(post("/sigmets/").contentType(MediaType.APPLICATION_JSON_UTF8).content(testSigmet))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message", is("sigmet " + uuid + " stored")))
                .andExpect(jsonPath("$.succeeded", is("true")))
                .andExpect(jsonPath("$.sigmetjson.uuid", is(uuid)))
                .andExpect(jsonPath("$.sigmetjson.validdate", is(startTimestamp)));

        verify(sigmetStore, times(1)).storeSigmet(any(Sigmet.class));
        verifyNoMoreInteractions(sigmetStore);
    }

    @Test
    public void serviceTestGetSigmetList() throws Exception {
        // given
        // when
        when(sigmetStore.getSigmets(false, null))
                .thenReturn(new Sigmet[] { sigmet })
                .thenReturn(new Sigmet[] { sigmet, new Sigmet(sigmet) })
                .thenReturn(new Sigmet[] { sigmet, new Sigmet(sigmet), new Sigmet(sigmet) });
        // then
        mockMvc.perform(get("/sigmets/?active=false")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.count", is(0)))
                .andExpect(jsonPath("$.nsigmets", is(1)))
                .andExpect(jsonPath("$.sigmets[0].uuid", is(uuid)));
        mockMvc.perform(get("/sigmets/?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.nsigmets", is(2)));
        mockMvc.perform(get("/sigmets/?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.nsigmets", is(3)));

        verify(sigmetStore, times(3)).getSigmets(anyBoolean(), isNull());
        verifyNoMoreInteractions(sigmetStore);
    }

    @Test
    public void serviceTestGetSigmetByUUID() throws Exception {
        // given
        // when
        when(sigmetStore.getByUuid(any(String.class))).thenReturn(sigmet);

        // then
        mockMvc.perform(get("/sigmets/" + uuid)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.uuid", is(uuid)))
                .andExpect(jsonPath("$.phenomenon", is(phenomenon)))
                .andExpect(jsonPath("$.obs_or_forecast.obs", is(true)))
                .andExpect(jsonPath("$.levelinfo.levels[0].value", is(100)))
                .andExpect(jsonPath("$.levelinfo.levels[0].unit", is("FL")))
                .andExpect(jsonPath("$.levelinfo.mode", is("ABV")))
                .andExpect(jsonPath("$.movement_type", is("STATIONARY")))
                .andExpect(jsonPath("$.change", is("NC")))
                .andExpect(jsonPath("$.validdate", is(startTimestamp)))
                .andExpect(jsonPath("$.firname", is("AMSTERDAM FIR")))
                .andExpect(jsonPath("$.location_indicator_icao", is("EHAA")))
                .andExpect(jsonPath("$.location_indicator_mwo", is("EHDB")))
                .andExpect(jsonPath("$.status", is("concept")))
                .andExpect(jsonPath("$.sequence", is(-1)))
                .andExpect(jsonPath("$.geojson").exists());

        verify(sigmetStore, times(1)).getByUuid(any(String.class));
        verifyNoMoreInteractions(sigmetStore);
    }

    @Test
    public void serviceTestPublishSigmet() throws Exception {
        // given
        String adjustedSigmet = testSigmet.replace("\"status\":\"concept\"", "\"status\":\"published\"");

        // when
        when(sigmetExporter.export(any(Sigmet.class), any(SigmetConverter.class), any(ObjectMapper.class))).thenReturn("OK");
        when(firStore.lookup(anyString(), anyBoolean())).thenReturn(new Feature());
        when(sigmetStore.isPublished(anyString())).thenReturn(false);
        when(sigmetStore.getNextSequence(any(Sigmet.class))).thenReturn(1);

        // then
        mockMvc.perform(post("/sigmets/").contentType(MediaType.APPLICATION_JSON_UTF8).content(adjustedSigmet))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.succeeded", is("true")))
                .andExpect(jsonPath("$.message", is("sigmet " + uuid + " published")))
                .andExpect(jsonPath("$.uuid", is(uuid)))
                .andExpect(jsonPath("$.sigmetjson.uuid", is(uuid)))
                .andExpect(jsonPath("$.sigmetjson.status", is("published")))
                .andExpect(jsonPath("$.sigmetjson.sequence", is(1)));

        verify(sigmetExporter, times(1)).export(any(Sigmet.class), any(SigmetConverter.class), any(ObjectMapper.class));
        verify(firStore, times(1)).lookup(anyString(), anyBoolean());
        verify(sigmetStore, times(1)).isPublished(any(String.class));
        verify(sigmetStore, times(1)).getNextSequence(any(Sigmet.class));
        verify(sigmetStore, times(1)).storeSigmet(any(Sigmet.class));
        verifyNoMoreInteractions(sigmetExporter);
        verifyNoMoreInteractions(firStore);
        verifyNoMoreInteractions(sigmetStore);
    }

    @Test
    public void serviceTestPublishAlreadyPublishedSigmet() throws Exception {
        // given
        String adjustedAirmet = testSigmet.replace("\"status\":\"concept\"", "\"status\":\"published\"");

        // when
        when(firStore.lookup(anyString(), anyBoolean())).thenReturn(new Feature());
        when(sigmetStore.isPublished(anyString())).thenReturn(true);
        when(sigmetStore.getNextSequence(any(Sigmet.class))).thenReturn(1);

        // then
        mockMvc.perform(post("/sigmets/").contentType(MediaType.APPLICATION_JSON_UTF8).content(adjustedAirmet))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.succeeded", is("false")))
                .andExpect(jsonPath("$.message", is("sigmet " + uuid + " is already published")))
                .andExpect(jsonPath("$.uuid", is(uuid)))
                .andExpect(jsonPath("$.sigmetjson.uuid", is(uuid)))
                .andExpect(jsonPath("$.sigmetjson.status", is("published")))
                .andExpect(jsonPath("$.sigmetjson.sequence", is(1)));

        verify(firStore, times(1)).lookup(anyString(), anyBoolean());
        verify(sigmetStore, times(1)).isPublished(any(String.class));
        verify(sigmetStore, times(1)).getNextSequence(any(Sigmet.class));
        verifyNoMoreInteractions(sigmetExporter);
        verifyNoMoreInteractions(firStore);
        verifyNoMoreInteractions(sigmetStore);
    }

    @Test
    public void serviceTestCancelSigmet() throws Exception {
        // given
        String adjustedSigmet = testSigmet.replace("\"status\":\"concept\"", "\"status\":\"canceled\"");

        // when
        when(sigmetExporter.export(any(Sigmet.class), any(SigmetConverter.class), any(ObjectMapper.class))).thenReturn("OK");
        when(firStore.lookup(anyString(), anyBoolean())).thenReturn(new Feature());
        when(sigmetStore.getByUuid(any(String.class))).thenReturn(sigmet);
        when(sigmetStore.getNextSequence(any(Sigmet.class))).thenReturn(1);

        // then
        mockMvc.perform(post("/sigmets/").contentType(MediaType.APPLICATION_JSON_UTF8).content(adjustedSigmet))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.succeeded", is("true")))
                .andExpect(jsonPath("$.message", is("sigmet " + uuid + " canceled")))
                .andExpect(jsonPath("$.uuid", is(uuid)))
                .andExpect(jsonPath("$.sigmetjson.uuid", is(uuid)))
                .andExpect(jsonPath("$.sigmetjson.status", is("canceled")));

        verify(sigmetExporter, times(1)).export(any(Sigmet.class), any(SigmetConverter.class), any(ObjectMapper.class));
        verify(firStore, times(1)).lookup(anyString(), anyBoolean());
        verify(sigmetStore, times(1)).getByUuid(anyString());
        verify(sigmetStore, times(1)).getNextSequence(any(Sigmet.class));
        verify(sigmetStore, times(2)).storeSigmet(any(Sigmet.class));
        verifyNoMoreInteractions(sigmetExporter);
        verifyNoMoreInteractions(firStore);
        verifyNoMoreInteractions(sigmetStore);
    }

    @Test
    public void serviceTestIntersections() throws Exception {
        // given
        String feature="{"
            + "\"firname\": \"AMSTERDAM FIR\","
            + "\"feature\": " + testFeatureFIR
        + "}";
        Feature fir = sigmetObjectMapper.readValue(firFeature, Feature.class);
        // when
        when(firStore.lookup(anyString(), anyBoolean())).thenReturn(fir);
        // then
        mockMvc.perform(post("/sigmets/sigmetintersections").contentType(MediaType.APPLICATION_JSON_UTF8).content(feature))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));

        verify(firStore, times(1)).lookup(anyString(), anyBoolean());
        verifyNoMoreInteractions(firStore);
    }

	@Test
	public void serviceTestIntersections6points() throws Exception {
        // given
        String feature = "{"
            + "\"firname\": \"AMSTERDAM FIR\","
            + "\"feature\": " + testFeature6points
        + "}";
        Feature fir = sigmetObjectMapper.readValue(firFeature, Feature.class);
        // when
        when(firStore.lookup(anyString(), anyBoolean())).thenReturn(fir);
        // then
        mockMvc.perform(post("/sigmets/sigmetintersections")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(feature))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.feature").exists());

        verify(firStore, times(1)).lookup(anyString(), anyBoolean());
        verifyNoMoreInteractions(firStore);
    }

    @Test
    public void serviceTestIntersections8points() throws Exception {
        // given
        String feature = "{"
            + "\"firname\": \"AMSTERDAM FIR\","
            + "\"feature\": " + testFeature8points
        + "}";
        Feature fir = sigmetObjectMapper.readValue(firFeature, Feature.class);
        // when
        when(firStore.lookup(anyString(), anyBoolean())).thenReturn(fir);
        // then
        mockMvc.perform(
                post("/sigmets/sigmetintersections").contentType(MediaType.APPLICATION_JSON_UTF8).content(feature))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message", containsString("more than")))
                .andExpect(jsonPath("$.feature").exists());

        verify(firStore, times(1)).lookup(anyString(), anyBoolean());
        verifyNoMoreInteractions(firStore);
    }
}
