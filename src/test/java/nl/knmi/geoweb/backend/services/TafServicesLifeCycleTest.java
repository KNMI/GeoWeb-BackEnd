package nl.knmi.geoweb.backend.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.UUID;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fi.fmi.avi.model.taf.TAF;
import icao.iwxxm21.TAFReportStatusType;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportType;

@RunWith(SpringRunner.class)
@SpringBootTest(classes= {TestWebConfig.class,TafServicesTestContext.class})
@DirtiesContext
public class TafServicesLifeCycleTest {
	/** Entry point for Spring MVC testing support. */
	private MockMvc mockMvc;

	//    @Autowired
	//    TafStore tafStore;

	/** The Spring web application context. */
	@Resource
	private WebApplicationContext webApplicationContext;

	/** The {@link ObjectMapper} instance to be used. */
	@Autowired
	@Qualifier("tafObjectMapper")
	private ObjectMapper tafObjectMapper;

	@Autowired
	@Qualifier("objectMapper")
	private ObjectMapper objectMapper;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		for(File file: new File("/tmp/tafs").listFiles()) 
		    if (!file.isDirectory()) 
		        file.delete();
	}

	private String addTaf(Taf taf) throws Exception {
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		MvcResult result = mockMvc.perform(post("/tafs")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(taf.toJSON(tafObjectMapper)))
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

	private String publishAndFail(Taf taf) throws Exception {
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		MvcResult result = mockMvc.perform(post("/tafs")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(taf.toJSON(tafObjectMapper)))
				//				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();
				.andExpect(status().is4xxClientError()).andReturn();
		
		return "FAIL";
	}

	private String storeTaf(Taf taf) throws Exception {
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		MvcResult result = mockMvc.perform(post("/tafs")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(taf.toJSON(tafObjectMapper)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();
		String responseBody = result.getResponse().getContentAsString();
		Debug.println("resp: "+responseBody);
		Debug.println("status: "+result.getResponse().getStatus());
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);

		assertThat(jsonResult.has("error"), is(false));
		assertThat(jsonResult.has("message"), is(true));
		assertThat(jsonResult.get("message").asText().length(), not(0));
		String uuid = jsonResult.get("uuid").asText();

		return uuid;
	}


	private Taf getBaseTaf(boolean actualise) throws JsonProcessingException, IOException {
		Debug.println("getBaseTaf()");
		String taf=
				"{  \"metadata\" : {"+
						"	    \"uuid\" : \"d612cd81-a043-4fdb-b6fd-d043463d451a\","+
						"	    \"validityStart\" : \"2018-06-25T06:00:00Z\","+
						"	    \"validityEnd\" : \"2018-06-26T12:00:00Z\","+
						"	    \"location\" : \"EHAM\","+
						"	    \"status\" : \"concept\","+
						"	    \"type\" : \"normal\""+
						"	  },"+
						"	  \"forecast\" : {"+
						"	    \"clouds\" : [ {"+
						"	      \"amount\" : \"OVC\","+
						"	      \"height\" : 100"+
						"	    } ],"+
						"	    \"visibility\" : {"+
						"	      \"value\" : 6000,"+
						"	      \"unit\" : \"M\""+
						"	    },"+
						"	    \"wind\" : {"+
						"	      \"direction\" : 200,"+
						"	      \"speed\" : 12,"+
						"	      \"unit\" : \"KT\""+
						"	    }"+
						"	  },"+
						"	  \"changegroups\" : [ ]"+
						"	}";


		//		ObjectMapper mapper = new ObjectMapper();
		objectMapper.setTimeZone(TimeZone.getTimeZone("UTC"));
		Taf tafObj=objectMapper.readValue(taf, Taf.class);
		//		Debug.println("taf: "+tafObj.toJSON(tafObjectMapper));
		tafObj.getMetadata().setUuid(UUID.randomUUID().toString());
		if (actualise) {
			OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z")).truncatedTo(ChronoUnit.HOURS);
			Debug.println(now.toString());
			tafObj.getMetadata().setValidityStart(now.minusHours(1));
			tafObj.getMetadata().setValidityEnd(now.plusHours(29));
		}
		
		
		return tafObj;
	}

	private Taf getTaf(String uuid) throws Exception {
		MvcResult result = mockMvc.perform(get("/tafs/"+uuid))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();
		return tafObjectMapper.readValue(result.getResponse().getContentAsString(), Taf.class);
	}

	@Test
	public void lifeCycleTest() throws Exception {
		Debug.println("testing TAF life cycle");
		//Generate a valid TAF
		Taf baseTaf=getBaseTaf(true);
		Debug.println("baseTaf:"+baseTaf.toJSON(tafObjectMapper));
		//Store it
		String uuid=addTaf(baseTaf);
		Debug.println("stored:"+uuid);
		Taf storedTaf=getTaf(uuid);
		Debug.println("from store:"+storedTaf.toJSON(tafObjectMapper));
		Debug.println("EQ: "+baseTaf.equals(storedTaf));
		assertEquals(baseTaf, storedTaf);
		
		//Make an amendment with a new UUID. Ths should fail because TAF has not been published
		storedTaf.metadata.setType(TAFReportType.amendment);
		storedTaf.metadata.setPreviousUuid(uuid);
		storedTaf.metadata.setUuid(UUID.randomUUID().toString());
		storedTaf.metadata.setStatus(TAFReportPublishedConcept.concept);
		storedTaf.getForecast().getWind().setSpeed(20);
		String corrUuid = publishAndFail(storedTaf);

		//Publish original TAF
		Debug.println("Publish original TAF");
		storedTaf=getTaf(uuid);
		storedTaf.metadata.setStatus(TAFReportPublishedConcept.published);
		String publishedUuid=storeTaf(storedTaf);
		Debug.println("published: "+publishedUuid);
		
		//Make another amendment with a new UUID. 
		storedTaf.metadata.setType(TAFReportType.amendment);
		storedTaf.metadata.setUuid(null);
		storedTaf.metadata.setPreviousUuid(publishedUuid);
		storedTaf.metadata.setStatus(TAFReportPublishedConcept.concept);
		storedTaf.getForecast().getWind().setSpeed(20);
		String amendedUuid=storeTaf(storedTaf);
		
		Debug.println("amended: "+amendedUuid);
		Taf amendedTaf=getTaf(amendedUuid);
		amendedTaf.metadata.setStatus(TAFReportPublishedConcept.published);
		amendedTaf.metadata.setUuid(null);
		amendedTaf.metadata.setStatus(TAFReportPublishedConcept.concept);
		String amendedPublishedUuid=storeTaf(amendedTaf);
		
		Debug.println("cancelling");
		Taf amendedPublishedTaf=getTaf(amendedPublishedUuid);
		amendedPublishedTaf.metadata.setUuid(null);
		amendedPublishedTaf.metadata.setStatus(TAFReportPublishedConcept.published);
		amendedPublishedTaf.metadata.setType(TAFReportType.canceled);
		String canceledUuid=storeTaf(amendedPublishedTaf);
	}

	public void addTAFTest () throws Exception {
		Debug.println("get inactive tafs");
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) tafObjectMapper.readTree(responseBody);

		assertThat(jsonResult.has("ntafs"), is(true));
		assertThat(jsonResult.has("tafs"), is(true));
		int tafs = jsonResult.get("ntafs").asInt();

		Debug.println("Add taff");
		String uuid = "v";//addTaf("");
		Debug.println("Add taff done: "+ uuid);
		assert(uuid != null);

		result = mockMvc.perform(get("/tafs?active=false"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		responseBody = result.getResponse().getContentAsString();
		jsonResult = (ObjectNode) tafObjectMapper.readTree(responseBody);
		assertThat(jsonResult.has("ntafs"), is(true));
		assertThat(jsonResult.has("tafs"), is(true));
		int new_tafs = jsonResult.get("ntafs").asInt();
		Debug.println("" + new_tafs + " === " + tafs);
		assert(new_tafs == tafs + 1);
	}

	public void getTafList () throws Exception {
		//	addTaf();
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) tafObjectMapper.readTree(responseBody);
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
		jsonResult = (ObjectNode) tafObjectMapper.readTree(responseBody);
		assertThat(jsonResult.has("page"), is(true));
		assertThat(jsonResult.has("npages"), is(true));
		assertThat(jsonResult.has("ntafs"), is(true));
		assertThat(jsonResult.has("tafs"), is(true));
		assertThat(jsonResult.get("ntafs").asInt(), is(0));

	}

	public void removeTaf () throws Exception {
		String uuid =""; //addTaf("");
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();
		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) tafObjectMapper.readTree(responseBody);
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
		jsonResult = (ObjectNode) tafObjectMapper.readTree(responseBody);
		int newTafCount = jsonResult.get("ntafs").asInt();
		assert(newTafCount == tafCount - 1);
	}

}
