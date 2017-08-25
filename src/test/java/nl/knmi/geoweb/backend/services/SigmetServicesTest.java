package nl.knmi.geoweb.backend.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
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

@RunWith(SpringRunner.class)
@SpringBootTest
public class SigmetServicesTest {
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
}
