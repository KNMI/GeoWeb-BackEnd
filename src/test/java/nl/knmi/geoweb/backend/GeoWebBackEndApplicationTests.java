package nl.knmi.geoweb.backend;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.Date;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.Phenomenon;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetChange;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GeoWebBackEndApplicationTests {
	
	/** Entry point for Spring MVC testing support. */
    private MockMvc mockMvc;
    
    
    /** The Spring web application context. */
    @Resource
    private WebApplicationContext webApplicationContext;
    
    /** The {@link ObjectMapper} instance to be used. */
    @Autowired
    private ObjectMapper objectMapper;
    
	 @Before
    public void setUp() {

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	public final String sigmetStoreLocation = "/tmp/junit/geowebbackendstore/";
	
	static String testGeoJson="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]},\"properties\":{\"prop0\":\"value0\",\"prop1\":{\"this\":\"that\"}}}]}";

	static String testSigmet="{\"geojson\":"
			+"{\"type\":\"FeatureCollection\",\"features\":"+"[{\"type\":\"Feature\",\"properties\":{\"prop0\":\"value0\",\"prop1\":{\"this\":\"that\"}},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]}}]},"
			+"\"phenomenon\":\"OBSC_TS\","
			+"\"obs_or_forecast\":{\"obs\":true},"
			+"\"level\":{\"lev1\":{\"value\":100.0,\"unit\":\"FL\"}},"
			+"\"movement\":{\"stationary\":true},"
			+"\"change\":\"NC\","
			+"\"issuedate\":\"2017-03-24T15:56:16Z\","
			+"\"validdate\":\"2017-03-24T15:56:16Z\","
			+"\"firname\":\"AMSTERDAM FIR\","
			+"\"location_indicator_icao\":\"EHAA\","
			+"\"location_indicator_mwo\":\"EHDB\"}";
	
	static String testTAF="{\n" + 
			"  \"issueTime\" : \"2017-08-07T09:46:24.59Z\",\n" + 
			"  \"validityStart\" : \"2017-08-04T12:00:00Z\",\n" + 
			"  \"validityEnd\" : \"2017-08-05T18:00:00Z\",\n" + 
			"  \"forecast\" : {\n" + 
			"    \"weather\" : [ ],\n" + 
			"    \"clouds\" : [ ],\n" + 
			"    \"wind\" : {\n" + 
			"      \"direction\" : 200,\n" + 
			"      \"speed\" : 15,\n" + 
			"      \"gusts\" : 25,\n" + 
			"      \"units\" : \"KT\",\n" + 
			"      \"isVariable\" : false\n" + 
			"    },\n" + 
			"    \"caVOK\" : true\n" + 
			"  },\n" + 
			"  \"changeForecasts\" : [ {\n" + 
			"    \"weather\" : [ \"SHRA\", \"TSRA\" ],\n" + 
			"    \"clouds\" : [ {\n" + 
			"      \"type\" : \"FEW\",\n" + 
			"      \"h\" : 900\n" + 
			"    }, {\n" + 
			"      \"type\" : \"SCT\",\n" + 
			"      \"h\" : 4100\n" + 
			"    }, {\n" + 
			"      \"type\" : \"OVC\",\n" + 
			"      \"h\" : 5100\n" + 
			"    } ],\n" + 
			"    \"visibility\" : {\n" + 
			"      \"visibilityRange\" : 9999\n" + 
			"    },\n" + 
			"    \"wind\" : {\n" + 
			"      \"direction\" : 220,\n" + 
			"      \"speed\" : 17,\n" + 
			"      \"gusts\" : 27,\n" + 
			"      \"units\" : \"KT\",\n" + 
			"      \"isVariable\" : false\n" + 
			"    },\n" + 
			"    \"changeType\" : \"BECMG\",\n" + 
			"    \"changeStart\" : \"2017-08-04T16:00:00Z\",\n" + 
			"    \"changeEnd\" : \"2017-08-04T20:00:00Z\",\n" + 
			"    \"caVOK\" : false\n" + 
			"  }, {\n" + 
			"    \"weather\" : [ \"+SHRA\" ],\n" + 
			"    \"clouds\" : [ {\n" + 
			"      \"type\" : \"FEW\",\n" + 
			"      \"h\" : 900\n" + 
			"    }, {\n" + 
			"      \"type\" : \"OVC\",\n" + 
			"      \"mod\" : \"TCU\",\n" + 
			"      \"h\" : 47000\n" + 
			"    } ],\n" + 
			"    \"visibility\" : {\n" + 
			"      \"visibilityRange\" : 9999\n" + 
			"    },\n" + 
			"    \"wind\" : {\n" + 
			"      \"direction\" : 220,\n" + 
			"      \"speed\" : 17,\n" + 
			"      \"gusts\" : 27,\n" + 
			"      \"units\" : \"KT\",\n" + 
			"      \"isVariable\" : false\n" + 
			"    },\n" + 
			"    \"changeType\" : \"PROB30\",\n" + 
			"    \"changeStart\" : \"2017-08-04T16:00:00Z\",\n" + 
			"    \"changeEnd\" : \"2017-08-04T20:00:00Z\",\n" + 
			"    \"caVOK\" : false\n" + 
			"  }, {\n" + 
			"    \"weather\" : [ \"SHRA\", \"TSRA\" ],\n" + 
			"    \"clouds\" : [ {\n" + 
			"      \"type\" : \"FEW\",\n" + 
			"      \"h\" : 1100\n" + 
			"    }, {\n" + 
			"      \"type\" : \"SCT\",\n" + 
			"      \"h\" : 2700\n" + 
			"    } ],\n" + 
			"    \"visibility\" : {\n" + 
			"      \"visibilityRange\" : 9999\n" + 
			"    },\n" + 
			"    \"wind\" : {\n" + 
			"      \"direction\" : 200,\n" + 
			"      \"speed\" : 7,\n" + 
			"      \"gusts\" : 17,\n" + 
			"      \"units\" : \"KT\",\n" + 
			"      \"isVariable\" : false\n" + 
			"    },\n" + 
			"    \"changeType\" : \"BECMG\",\n" + 
			"    \"changeStart\" : \"2017-08-05T03:00:00Z\",\n" + 
			"    \"changeEnd\" : \"2017-08-05T05:00:00Z\",\n" + 
			"    \"caVOK\" : false\n" + 
			"  } ],\n" + 
			"  \"previousReportAerodrome\" : \"EHAM\" \n" + 
			"}";
	
	public Sigmet createSigmet () throws Exception {
		Sigmet sm=new Sigmet("AMSTERDAM FIR", "EHAA", "EHDB", "abcd");
		sm.setPhenomenon(Phenomenon.getPhenomenon("OBSC_TS"));
		sm.setValiddate(new Date(117,2,13,16,0));
		sm.setChange(SigmetChange.NC);
		sm.setGeoFromString(testGeoJson);
		return sm;
	}
	
	public void validateSigmet (Sigmet sm) throws Exception {
		Debug.println("Testing createAndCheckSigmet");
		Debug.println(sm.getValiddate().toString());
		assertThat(sm.getPhenomenon().toString(), is("OBSC_TS"));
	}
	
	@Test 
	public void createAndValidateSigmet () throws Exception {
		Sigmet sm = createSigmet();
		validateSigmet(sm);
	}

	@Test
	public void createSigmetStoreAtEmptyDirCheckException () throws Exception {
		Tools.rmdir(sigmetStoreLocation);
		exception.expect(NotDirectoryException.class);
		new SigmetStore(sigmetStoreLocation);
	}
	
	public SigmetStore createNewStore() throws IOException {
		Tools.rmdir(sigmetStoreLocation);
		Tools.mksubdirs(sigmetStoreLocation);
		SigmetStore store=new SigmetStore(sigmetStoreLocation);
		Sigmet[] sigmets=store.getSigmets(false, SigmetStatus.PRODUCTION);
		assertThat(sigmets.length, is(0));
		return store;
	}
	
	@Test
	public void saveOneSigmet () throws Exception {
		SigmetStore store=createNewStore();
		Sigmet sm = createSigmet();
		store.storeSigmet(sm);
		assertThat(store.getSigmets(false, SigmetStatus.PRODUCTION).length, is(1));
	}
	
	@Test
	public void loadAndValidateSigmet () throws Exception {
		saveOneSigmet();
		SigmetStore storeLoad=new SigmetStore(sigmetStoreLocation);
		Sigmet[] sigmets=storeLoad.getSigmets(false, SigmetStatus.PRODUCTION);
		assertThat(sigmets.length, is(1));
		validateSigmet(sigmets[0]);
	}
	
	@Test
	public void apiTestStoreSigmetEmptyHasErrorMsg () throws Exception {
		MvcResult result = mockMvc.perform(post("/sigmet/storesigmet")
                .contentType(MediaType.APPLICATION_JSON_UTF8).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		String responseBody = result.getResponse().getContentAsString();
		Debug.println(responseBody);
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("error"), is(true));
        assertThat(jsonResult.get("error").asText().length(), not(0));
	}
	
	public String apiTestStoreSigmetOK() throws Exception {
		MvcResult result = mockMvc.perform(post("/sigmet/storesigmet")
                .contentType(MediaType.APPLICATION_JSON_UTF8).content(testSigmet))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();	
		String responseBody =  result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("error"), is(false));
        assertThat(jsonResult.has("message"), is(true));
        assertThat(jsonResult.has("message"), is(true));
        assertThat(jsonResult.get("message").asText().length(), not(0));
        String uuid = jsonResult.get("uuid").asText();
        Debug.println("Sigmet uuid = " + uuid);
        return uuid;
	}
	
	public ObjectNode getSigmetList() throws Exception {
        /*getsigmetlist*/
		MvcResult result = mockMvc.perform(post("/sigmet/getsigmetlist?active=false")
                .contentType(MediaType.APPLICATION_JSON_UTF8).content(testSigmet))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("page"), is(true));
        assertThat(jsonResult.has("npages"), is(true));
        assertThat(jsonResult.has("nsigmets"), is(true));
        assertThat(jsonResult.has("sigmets"), is(true));
        assertThat(jsonResult.has("nsigmets"), is(true));
        return jsonResult;
	}
	
	@Test
	public void apiTestGetSigmetListIncrement () throws Exception {
		ObjectNode jsonResult = getSigmetList();
        int currentNrOfSigmets = jsonResult.get("nsigmets").asInt();
		apiTestStoreSigmetOK();
		jsonResult = getSigmetList();
		int newNrOfSigmets = jsonResult.get("nsigmets").asInt();
		assertThat(newNrOfSigmets, is(currentNrOfSigmets + 1));
	}
	@Test
	public void apiTestGetSigmetByUUID () throws Exception {
		String sigmetUUID = apiTestStoreSigmetOK();
		
		/*getsigmet by uuid*/
		MvcResult result = mockMvc.perform(post("/sigmet/getsigmet?uuid=" + sigmetUUID)
                .contentType(MediaType.APPLICATION_JSON_UTF8).content(testSigmet))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.get("uuid").asText(), is(sigmetUUID));
        assertThat(jsonResult.get("phenomenon").asText(), is("OBSC_TS"));
        assertThat(jsonResult.get("obs_or_forecast").get("obs").asBoolean(), is(true));
        assertThat(jsonResult.get("level").get("lev1").get("value").asDouble(), is(100.0));
        assertThat(jsonResult.get("level").get("lev1").get("unit").asText(), is("FL"));
        assertThat(jsonResult.get("movement").get("stationary").asBoolean(), is(true)); 
        assertThat(jsonResult.get("change").asText(), is("NC"));
        assertThat(jsonResult.get("validdate").asText(), is("2017-03-24T15:56:16Z"));
        assertThat(jsonResult.get("firname").asText(), is("AMSTERDAM FIR"));
        assertThat(jsonResult.get("location_indicator_icao").asText(), is("EHAA"));
        assertThat(jsonResult.get("location_indicator_mwo").asText(), is("EHDB"));
        assertThat(jsonResult.get("status").asText(), is("PRODUCTION"));
        assertThat(jsonResult.get("sequence").asInt(), is(0));
        assertThat(jsonResult.has("geojson"), is(true));
        Debug.println(responseBody);	
	}
	public String addTaf() throws Exception {
		MvcResult result = mockMvc.perform(post("/tafs")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(testTAF))
				.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();	
		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);

        assertThat(jsonResult.has("error"), is(false));
        assertThat(jsonResult.has("message"), is(true));
        assertThat(jsonResult.has("message"), is(true));
        assertThat(jsonResult.get("message").asText().length(), not(0));
        String uuid = jsonResult.get("uuid").asText();
        return uuid;
	}
	@Test
	public void addTAFTest () throws Exception {
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("ntafs"), is(true));
        assertThat(jsonResult.has("tafs"), is(true));
        int tafs = jsonResult.get("ntafs").asInt();

		String uuid = addTaf();
		assert(uuid != null);
		
		result = mockMvc.perform(get("/tafs?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		responseBody = result.getResponse().getContentAsString();
		jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("ntafs"), is(true));
        assertThat(jsonResult.has("tafs"), is(true));
        int new_tafs = jsonResult.get("ntafs").asInt();
        assert(new_tafs == tafs + 1);
	}
	
	@Test
	public void getTafList () throws Exception {
		String uuid = addTaf();
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("page"), is(true));
        assertThat(jsonResult.has("npages"), is(true));
        assertThat(jsonResult.has("ntafs"), is(true));
        assertThat(jsonResult.has("tafs"), is(true));
        assert(jsonResult.get("ntafs").asInt() >= 1);
        
		result = mockMvc.perform(get("/tafs?active=true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		responseBody = result.getResponse().getContentAsString();
		jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("page"), is(true));
        assertThat(jsonResult.has("npages"), is(true));
        assertThat(jsonResult.has("ntafs"), is(true));
        assertThat(jsonResult.has("tafs"), is(true));
        assertThat(jsonResult.get("ntafs").asInt(), is(0));

	}
	
	@Test
	public void removeTaf () throws Exception {
		String uuid = addTaf();
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		int tafCount = jsonResult.get("ntafs").asInt();
		mockMvc.perform(delete("/tafs/" + uuid))                
			.andExpect(status().isOk())
	        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
	        .andReturn();
		result = mockMvc.perform(get("/tafs?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		responseBody = result.getResponse().getContentAsString();
		jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		int newTafCount = jsonResult.get("ntafs").asInt();
		assert(newTafCount == tafCount - 1);
	}
}
