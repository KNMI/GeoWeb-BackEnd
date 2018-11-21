package nl.knmi.geoweb.backend.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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
//@WebMvcTest(SigmetServices.class)
@DirtiesContext
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

	static String features="["
			+"{\"type\":\"Feature\", \"id\":\"geom-1\", \"properties\":{\"featureFunction\":\"start\", \"selectionType\":\"box\"}, \"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-4,52],[4.5,52],[4.5,55.3],[-4,55.3],[-4,52]]]}}"
			+ ",{\"type\":\"Feature\",\"id\":\"geom-2\", \"properties\":{\"featureFunction\":\"intersection\", \"selectionType\":\"poly\", \"relatesTo\":\"geom-1\"}, \"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-4,52],[4.5,52],[4.5,56],[-4,56],[-4,52]]]}}"
			+"]";
	
	static String testSigmet="{\"geojson\":"
			+"{\"type\":\"FeatureCollection\",\"features\":"+features+"},"
			//+"[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]}}]},"
			+"\"phenomenon\":\"OBSC_TS\","
			+"\"obs_or_forecast\":{\"obs\":true},"
			+"\"levelinfo\":{\"levels\":[{\"value\":100.0,\"unit\":\"FL\"}], \"mode\": \"AT\"},"
			+"\"movement_type\":\"STATIONARY\","
			+"\"change\":\"NC\","
			+"\"status\":\"concept\","
			+"\"validdate\":\"2017-03-24T15:56:16Z\","
			+"\"validdate_end\":\"2017-03-24T15:56:16Z\","
			+"\"firname\":\"AMSTERDAM FIR\","
			+"\"location_indicator_icao\":\"EHAA\","
			+"\"location_indicator_mwo\":\"EHDB\"}";

	static String testSigmetWithDate="{\"geojson\":"
			+"{\"type\":\"FeatureCollection\",\"features\":"+features+"},"
			//"[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]}}]},"
			+"\"phenomenon\":\"OBSC_TS\","
			+"\"obs_or_forecast\":{\"obs\":true},"
			+"\"levelinfo\":{\"levels\":[{\"value\":100.0,\"unit\":\"FL\"}], \"mode\": \"AT\"},"
			+"\"movement_type\":\"STATIONARY\","
			+"\"change\":\"NC\","
			+"\"status\":\"concept\","
			+"\"validdate\":\"%DATETIME%\","
			+"\"validdate_end\":\"%DATETIME_END%\","
			+"\"firname\":\"AMSTERDAM FIR\","
			+"\"location_indicator_icao\":\"EHAA\","
			+"\"location_indicator_mwo\":\"EHDB\"}";

	@Test
	public void apiTestStoreSigmetEmptyHasErrorMsg () throws Exception {
		MvcResult result = mockMvc.perform(post("/sigmets")
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

	public String apiTestStoreSigmetOK(String sigmetText) throws Exception {
		MvcResult result = mockMvc.perform(post("/sigmets/")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(sigmetText))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();	
		String responseBody =  result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		assertThat(jsonResult.has("error"), is(false));
		assertThat(jsonResult.has("message"), is(true));
		assertThat(jsonResult.get("succeeded").asText(), is("true"));
		assertThat(jsonResult.get("message").asText().length(), not(0));
		assertThat(jsonResult.get("sigmetjson").asText().length(), not(0));
		String uuid = jsonResult.get("uuid").asText();
		Debug.println("Sigmet uuid = " + uuid);
		return uuid;
	}

	public ObjectNode getSigmetList() throws Exception {
		/*getsigmetlist*/
		MvcResult result = mockMvc.perform(get("/sigmets/?active=false")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(testSigmet))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		Debug.println("getSigmetList() result:"+responseBody);
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		assertThat(jsonResult.has("page"), is(true));
		assertThat(jsonResult.has("npages"), is(true));
		assertThat(jsonResult.has("sigmets"), is(true));
		assertThat(jsonResult.has("nsigmets"), is(true));
		return jsonResult;
	}

	@Test
	public void apiTestGetSigmetListIncrement () throws Exception {
		apiTestStoreSigmetOK(testSigmet);
		ObjectNode jsonResult = getSigmetList();
		int currentNrOfSigmets = jsonResult.get("nsigmets").asInt();
		apiTestStoreSigmetOK(testSigmet);
		jsonResult = getSigmetList();
		int newNrOfSigmets = jsonResult.get("nsigmets").asInt();
		assertThat(newNrOfSigmets, is(currentNrOfSigmets + 1));
	}

	@Test
	public void apiTestGetSigmetByUUID () throws Exception {
		String sigmetUUID = apiTestStoreSigmetOK(testSigmet);

		/*getsigmet by uuid*/
		MvcResult result = mockMvc.perform(get("/sigmets/"+sigmetUUID))
				//                .contentType(MediaType.APPLICATION_JSON_UTF8).content(testSigmet))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		assertThat(jsonResult.get("uuid").asText(), is(sigmetUUID));
		assertThat(jsonResult.get("phenomenon").asText(), is("OBSC_TS"));
		assertThat(jsonResult.get("obs_or_forecast").get("obs").asBoolean(), is(true));
		assertThat(jsonResult.get("levelinfo").get("levels").get(0).get("value").asDouble(), is(100.0));
		assertThat(jsonResult.get("levelinfo").get("levels").get(0).get("unit").asText(), is("FL"));
		assertThat(jsonResult.get("levelinfo").get("mode").asText(), is("AT"));
		assertThat(jsonResult.get("movement_type").asText(), is("STATIONARY")); 
		assertThat(jsonResult.get("change").asText(), is("NC"));
		assertThat(jsonResult.get("validdate").asText(), is("2017-03-24T15:56:16Z"));
		assertThat(jsonResult.get("firname").asText(), is("AMSTERDAM FIR"));
		assertThat(jsonResult.get("location_indicator_icao").asText(), is("EHAA"));
		assertThat(jsonResult.get("location_indicator_mwo").asText(), is("EHDB"));
		assertThat(jsonResult.get("status").asText(), is("concept"));
		assertThat(jsonResult.get("sequence").asInt(), is(-1));
		assertThat(jsonResult.has("geojson"), is(true));
		Debug.println(responseBody);	
	}

	private String fixDate(String testSigmetWithDate) {
		String now = OffsetDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);
		String end = OffsetDateTime.now(ZoneId.of("UTC")).plusHours(2).format(DateTimeFormatter.ISO_INSTANT);
		return testSigmetWithDate.replaceFirst("%DATETIME%", now).replaceFirst("%DATETIME_END%", end);
	}
	
	@Test
	public void apiTestPublishSigmet () throws Exception {
		String currentTestSigmet=fixDate(testSigmetWithDate);
		String sigmetUUID = apiTestStoreSigmetOK(currentTestSigmet);
		MvcResult result = mockMvc.perform(get("/sigmets/"+sigmetUUID))
				//              .contentType(MediaType.APPLICATION_JSON_UTF8).content(testSigmet))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		jsonResult.put("status",  "published");
		Debug.println("After setting status=published: "+jsonResult.toString());

		result = mockMvc.perform(post("/sigmets/")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(jsonResult.toString()))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();	
		responseBody =  result.getResponse().getContentAsString();
		Debug.println("After publish: "+responseBody);
		//ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
	}
	
	@Test
	public void apiTestCancelSigmet () throws Exception {
		String currentTestSigmet=fixDate(testSigmetWithDate);
		String sigmetUUID = apiTestStoreSigmetOK(currentTestSigmet);
		MvcResult result = mockMvc.perform(get("/sigmets/"+sigmetUUID))
				//              .contentType(MediaType.APPLICATION_JSON_UTF8).content(testSigmet))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		jsonResult.put("status",  "published");
		result = mockMvc.perform(post("/sigmets/")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(jsonResult.toString()))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();	
		responseBody =  result.getResponse().getContentAsString();		
		
		
		jsonResult.put("status",  "canceled");
		Debug.println("After setting status=canceled: "+jsonResult.toString());

		result = mockMvc.perform(post("/sigmets/")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(jsonResult.toString()))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();	
		responseBody =  result.getResponse().getContentAsString();
		Debug.println("After cancel: "+responseBody);
		//ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
	}
	
	static String testFeatureFIR="{\"type\":\"Feature\", \"id\":\"geom-1\", \"properties\":{\"featureFunction\":\"start\", \"selectionType\":\"fir\"}}";
	@Test
	public void apiIntersections() throws Exception {
		String feature="{\"firname\":\"AMSTERDAM FIR\", \"feature\":"+testFeatureFIR+"}";
		Debug.println(feature);
		MvcResult result = mockMvc.perform(post("/sigmets/sigmetintersections")
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.content(feature))
				.andExpect(status().isOk())
				.andReturn();
		
		String responseBody = result.getResponse().getContentAsString();
		Debug.println("After sigmetintersections: "+responseBody);
	}

	static String testIntersection6points="{\"type\":\"Feature\", \"id\":\"geom-1\", \"properties\":{" +
			"\"featureFunction\":\"start\", \"selectionType\":\"poly\"},"+
			"\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4,51],[4.25,51.25],[4.5,51.5],[5.5,51.5],[5.25,51.25],[5,51],[4,51]]]}"+
			"}";

	@Test
	public void apiIntersections6points() throws Exception {
		String feature="{\"firname\":\"AMSTERDAM FIR\", \"feature\":"+testIntersection6points+"}";
		Debug.println(feature);
		MvcResult result = mockMvc.perform(post("/sigmets/sigmetintersections")
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.content(feature))
				.andExpect(status().isOk())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		assertThat(jsonResult.has("error"), is(false));
        assertThat(jsonResult.has("message"), is(false));
        assertThat(jsonResult.has("feature"), is(true));
        Debug.println("After sigmetintersections: "+responseBody);
	}

	static String testIntersection8points="{\"type\":\"Feature\", \"id\":\"geom-1\", \"properties\":{" +
			"\"featureFunction\":\"start\", \"selectionType\":\"poly\"},"+
			"\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4,51],[4.25,51.25],[4.5,51.5],[4.75,51.75],[5.75,51.75],[5.5,51.5],[5.25,51.25],[5,51],[4,51]]]}"+
			"}";

	@Test
	public void apiIntersections8points() throws Exception {
		String feature="{\"firname\":\"AMSTERDAM FIR\", \"feature\":"+testIntersection8points+"}";
		Debug.println(feature);
		MvcResult result = mockMvc.perform(post("/sigmets/sigmetintersections")
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.content(feature))
				.andExpect(status().isOk())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("error"), is(false));
        assertThat(jsonResult.has("message"), is(true));
        assertThat(jsonResult.get("message").asText().contains("more than"), is(true));
        assertThat(jsonResult.has("feature"), is(true));
        Debug.println("After sigmetintersections: "+responseBody);
	}

}
