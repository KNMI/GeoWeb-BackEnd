package nl.knmi.geoweb.backend.services;

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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import nl.knmi.geoweb.backend.ApplicationConfig;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.product.airmet.Airmet;
import nl.knmi.geoweb.backend.product.airmet.AirmetStore;
import nl.knmi.geoweb.backend.product.airmet.converter.AirmetConverter;
import nl.knmi.geoweb.backend.product.sigmetairmet.ObsFc;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetChange;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetLevel;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetMovement;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetType;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(ApplicationConfig.class)
@DirtiesContext
public class AirmetServicesTest {
    private String testGeoJsonBox = "{\"type\": \"FeatureCollection\",\"features\":[{\"type\": \"Feature\",\"id\": \"feb7bb38-a341-438d-b8f5-aa83685a0062\","
    + " \"properties\": {\"selectionType\": \"box\",\"featureFunction\": \"start\"},\"geometry\": {\"type\": \"Polygon\","
    + " \"coordinates\": [[[5.1618,51.4414],[5.1618,51.7424],[5.8444,51.7424],[5.8444,51.4414],[5.1618,51.4414]]]}}]}\"";

    static String uuid = "b6ea2637-4652-42cc-97ac-4e34548d3cc7";
    static String phenomenon = "OCNL_TSGR";
    static String startTimestamp = "2019-02-12T08:00:00Z";
    OffsetDateTime start = OffsetDateTime.parse(startTimestamp);
    OffsetDateTime end = OffsetDateTime.parse("2019-02-12T11:00:00Z");

    private Airmet airmet;

    private MockMvc mockMvc;

    /** The Spring web application context. */
    @Autowired
    private WebApplicationContext webApplicationContext;

    /** The {@link ObjectMapper} instance to be used. */
    @Autowired
    @Qualifier("airmetObjectMapper")
    private ObjectMapper airmetObjectMapper;

    @Autowired
    AirmetStore airmetStore;

    @Autowired
    FIRStore firStore;

    @Autowired
    private ProductExporter<Airmet> airmetExporter;

    // @Autowired
    // private OAuthHelper authHelper;

    @Before
    public void setUp() {
        Airmet am = new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", uuid);
        am.setStatus(SigmetAirmetStatus.concept);
        am.setType(SigmetAirmetType.normal);
        am.setPhenomenon(Airmet.Phenomenon.getPhenomenon(phenomenon));
        am.setValiddate(start);
        am.setValiddate_end(end);
        am.setObs_or_forecast(new ObsFc(true));
        am.setChange(SigmetAirmetChange.INTSF);
        am.setMovement_type(Airmet.AirmetMovementType.MOVEMENT);
        am.setMovement(new SigmetAirmetMovement("NNE", 4, "KT"));
        am.setLevelinfo(new SigmetAirmetLevel(
                new SigmetAirmetLevel.SigmetAirmetPart(SigmetAirmetLevel.SigmetAirmetLevelUnit.FL, 30),
                SigmetAirmetLevel.SigmetAirmetLevelMode.ABV));
        am.setGeojson(mapJsonToGeoObject(testGeoJsonBox));
        airmet = am;
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        reset(airmetStore);
        reset(firStore);
        reset(airmetExporter);
    }

    static String features = "["
            + "{\"type\":\"Feature\", \"id\":\"geom-1\", \"properties\":{\"featureFunction\":\"start\", \"selectionType\":\"box\"}, \"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-4,52],[4.5,52],[4.5,55.3],[-4,55.3],[-4,52]]]}}"
            + ",{\"type\":\"Feature\",\"id\":\"geom-2\", \"properties\":{\"featureFunction\":\"intersection\", \"selectionType\":\"poly\", \"relatesTo\":\"geom-1\"}, \"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-4,52],[4.5,52],[4.5,56],[-4,56],[-4,52]]]}}"
            + "]";

    static String testAirmet = "{\"geojson\":" + "{\"type\":\"FeatureCollection\",\"features\":" + features + "},"
            + "\"phenomenon\":\"" + phenomenon + "\"," + "\"obs_or_forecast\":{\"obs\":true}," + "\"uuid\": \"" + uuid + "\","
            + "\"levelinfo\":{\"levels\":[{\"value\":100.0,\"unit\":\"FL\"}], \"mode\": \"AT\"},"
            + "\"movement_type\":\"STATIONARY\"," + "\"change\":\"NC\"," + "\"status\":\"concept\","
            + "\"validdate\":\"" + startTimestamp + "\"," + "\"validdate_end\":\"2017-03-24T15:56:16Z\","
            + "\"firname\":\"AMSTERDAM FIR\"," + "\"location_indicator_icao\":\"EHAA\","
            + "\"location_indicator_mwo\":\"EHDB\"}";

    static String testAirmetWithDate = "{\"geojson\":" + "{\"type\":\"FeatureCollection\",\"features\":" + features
            + "},"
            // "[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]}}]},"
            + "\"phenomenon\":\"OCNL_TS\"," + "\"obs_or_forecast\":{\"obs\":true},"
            + "\"levelinfo\":{\"levels\":[{\"value\":100.0,\"unit\":\"FL\"}], \"mode\": \"AT\"},"
            + "\"movement_type\":\"STATIONARY\"," + "\"change\":\"NC\"," + "\"status\":\"concept\","
            + "\"validdate\":\"%DATETIME%\"," + "\"validdate_end\":\"%DATETIME_END%\","
            + "\"firname\":\"AMSTERDAM FIR\"," + "\"location_indicator_icao\":\"EHAA\","
            + "\"location_indicator_mwo\":\"EHDB\"}";

    private GeoJsonObject mapJsonToGeoObject(String json) {
        GeoJsonObject result;
        try {
            result = airmetObjectMapper.readValue(json, GeoJsonObject.class);
        } catch (IOException e) {
            result = null;
        }
        return result;
    }

    @Test
    public void serviceTestGetAirmetByUUID() throws Exception {
        // given
        // when
        when(airmetStore.getByUuid(any(String.class))).thenReturn(airmet);

        // then
        mockMvc.perform(get("/airmets/" + uuid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.uuid", is(uuid)))
                .andExpect(jsonPath("$.phenomenon", is(phenomenon)))
                .andExpect(jsonPath("$.validdate", is(startTimestamp)))
                .andExpect(jsonPath("$.firname", is("AMSTERDAM FIR")))
                .andExpect(jsonPath("$.location_indicator_icao", is("EHAA")))
                .andExpect(jsonPath("$.location_indicator_mwo", is("EHDB")))
                .andExpect(jsonPath("$.status", is("concept")))
                .andExpect(jsonPath("$.sequence", is(-1)));

        verify(airmetStore, times(1)).getByUuid(any(String.class));
        verifyNoMoreInteractions(airmetStore);
    }

    @Test
    public void serviceTestPostEmptyAirmet() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(post("/airmets").contentType(MediaType.APPLICATION_JSON_UTF8).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error", is("empty airmet")));

        verifyNoMoreInteractions(airmetStore);
    }

    @Test
    public void serviceTestPostCorrectAirmet() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(post("/airmets/").contentType(MediaType.APPLICATION_JSON_UTF8).content(testAirmet))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message", is("airmet " + uuid + " stored")))
                .andExpect(jsonPath("$.succeeded", is("true")))
                .andExpect(jsonPath("$.airmetjson.uuid", is(uuid)))
                .andExpect(jsonPath("$.airmetjson.validdate", is(startTimestamp)));

        verify(airmetStore, times(1)).storeAirmet(any(Airmet.class));
        verifyNoMoreInteractions(airmetStore);
    }

    @Test
    public void serviceTestGetAirmetList() throws Exception {
        // given
        // when
        when(airmetStore.getAirmets(false, null)).thenReturn(new Airmet[] { airmet })
                .thenReturn(new Airmet[] { airmet, new Airmet(airmet) })
                .thenReturn(new Airmet[] { airmet, new Airmet(airmet), new Airmet(airmet) });
        // then
        mockMvc.perform(get("/airmets/?active=false"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.count", is(0)))
                .andExpect(jsonPath("$.nairmets", is(1)))
                .andExpect(jsonPath("$.airmets[0].uuid", is(uuid)));
        mockMvc.perform(get("/airmets/?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.nairmets", is(2)));
        mockMvc.perform(get("/airmets/?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.nairmets", is(3)));

        verify(airmetStore, times(3)).getAirmets(anyBoolean(), isNull());
        verifyNoMoreInteractions(airmetStore);
    }

    @Test
    public void serviceTestPublishAirmet() throws Exception {
        // given
        String adjustedAirmet = testAirmet.replace("\"status\":\"concept\"", "\"status\":\"published\"");

        // when
        when(airmetExporter.export(any(Airmet.class), any(AirmetConverter.class), any(ObjectMapper.class))).thenReturn("OK");
        when(firStore.lookup(anyString(), anyBoolean())).thenReturn(new Feature());
        when(airmetStore.isPublished(anyString())).thenReturn(false);
        when(airmetStore.getNextSequence(any(Airmet.class))).thenReturn(1);

        // then
        mockMvc.perform(post("/airmets/").contentType(MediaType.APPLICATION_JSON_UTF8).content(adjustedAirmet))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.succeeded", is("true")))
                .andExpect(jsonPath("$.message", is("airmet " + uuid + " published")))
                .andExpect(jsonPath("$.uuid", is(uuid)))
                .andExpect(jsonPath("$.airmetjson.uuid", is(uuid)))
                .andExpect(jsonPath("$.airmetjson.status", is("published")))
                .andExpect(jsonPath("$.airmetjson.sequence", is(1)));

        verify(airmetExporter, times(1)).export(any(Airmet.class), any(AirmetConverter.class), any(ObjectMapper.class));
        verify(firStore, times(1)).lookup(anyString(), anyBoolean());
        verify(airmetStore, times(1)).isPublished(any(String.class));
        verify(airmetStore, times(1)).getNextSequence(any(Airmet.class));
        verify(airmetStore, times(1)).storeAirmet(any(Airmet.class));
        verifyNoMoreInteractions(airmetExporter);
        verifyNoMoreInteractions(firStore);
        verifyNoMoreInteractions(airmetStore);
    }

    @Test
    public void serviceTestPublishAlreadyPublishedAirmet() throws Exception {
        // given
        String adjustedAirmet = testAirmet.replace("\"status\":\"concept\"", "\"status\":\"published\"");

        // when
        when(firStore.lookup(anyString(), anyBoolean())).thenReturn(new Feature());
        when(airmetStore.isPublished(anyString())).thenReturn(true);
        when(airmetStore.getNextSequence(any(Airmet.class))).thenReturn(1);

        // then
        mockMvc.perform(post("/airmets/").contentType(MediaType.APPLICATION_JSON_UTF8).content(adjustedAirmet))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.succeeded", is("false")))
                .andExpect(jsonPath("$.message", is("airmet " + uuid + " is already published")))
                .andExpect(jsonPath("$.uuid", is(uuid)))
                .andExpect(jsonPath("$.airmetjson.uuid", is(uuid)))
                .andExpect(jsonPath("$.airmetjson.status", is("published")))
                .andExpect(jsonPath("$.airmetjson.sequence", is(1)));

        verify(firStore, times(1)).lookup(anyString(), anyBoolean());
        verify(airmetStore, times(1)).isPublished(any(String.class));
        verify(airmetStore, times(1)).getNextSequence(any(Airmet.class));
        verifyNoMoreInteractions(airmetExporter);
        verifyNoMoreInteractions(firStore);
        verifyNoMoreInteractions(airmetStore);
    }

    @Test
    public void serviceTestCancelAirmet() throws Exception {
        // given
        String adjustedAirmet = testAirmet.replace("\"status\":\"concept\"", "\"status\":\"canceled\"");

        // when
        when(airmetExporter.export(any(Airmet.class), any(AirmetConverter.class), any(ObjectMapper.class))).thenReturn("OK");
        when(firStore.lookup(anyString(), anyBoolean())).thenReturn(new Feature());
        when(airmetStore.getByUuid(any(String.class))).thenReturn(airmet);
        when(airmetStore.getNextSequence(any(Airmet.class))).thenReturn(1);

        // then
        mockMvc.perform(post("/airmets/").contentType(MediaType.APPLICATION_JSON_UTF8).content(adjustedAirmet))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist()).andExpect(jsonPath("$.succeeded", is("true")))
                .andExpect(jsonPath("$.message", is("airmet " + uuid + " canceled")))
                .andExpect(jsonPath("$.uuid", is(uuid))).andExpect(jsonPath("$.airmetjson.uuid", is(uuid)))
                .andExpect(jsonPath("$.airmetjson.status", is("canceled")));

        verify(airmetExporter, times(1)).export(any(Airmet.class), any(AirmetConverter.class), any(ObjectMapper.class));
        verify(firStore, times(1)).lookup(anyString(), anyBoolean());
        verify(airmetStore, times(1)).getByUuid(anyString());
        verify(airmetStore, times(1)).getNextSequence(any(Airmet.class));
        verify(airmetStore, times(2)).storeAirmet(any(Airmet.class));
        verifyNoMoreInteractions(airmetExporter);
        verifyNoMoreInteractions(firStore);
        verifyNoMoreInteractions(airmetStore);
    }

    // RequestPostProcessor bearerToken = authHelper.addBearerToken("test",
    // "ROLE_USER");
    // ResultActions resultActions =
    // restMvc.perform(post("/hello").with(bearerToken));

}
