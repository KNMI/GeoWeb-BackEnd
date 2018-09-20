package nl.knmi.geoweb.backend.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.UUID;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportType;

@RunWith(SpringRunner.class)
@SpringBootTest(classes= {TestWebConfig.class,TafServicesTestContext.class})
@DirtiesContext
public class TafServicesLifeCycleTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(TafServicesLifeCycleTest.class);

	/** Entry point for Spring MVC testing support. */
	private MockMvc mockMvc;

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
				.andExpect(status().is4xxClientError()).andReturn();
		
		return "FAIL";
	}

	private String storeTaf(Taf taf) throws Exception {
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		MvcResult result = mockMvc.perform(post("/tafs")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(taf.toJSON(tafObjectMapper)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();
		String responseBody = result.getResponse().getContentAsString();
		LOGGER.debug("resp: {}", responseBody);
		LOGGER.debug("status: {}", result.getResponse().getStatus());
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);

		assertThat(jsonResult.has("error"), is(false));
		assertThat(jsonResult.has("message"), is(true));
		assertThat(jsonResult.get("message").asText().length(), not(0));
		String uuid = jsonResult.get("uuid").asText();

		return uuid;
	}


	static OffsetDateTime actualisedBaseTime;

	private Taf getBaseTaf(boolean actualise) throws JsonProcessingException, IOException {
		LOGGER.debug("getBaseTaf()");
		String taf=
				"{  \"metadata\" : {"+
						"	    \"uuid\" : \"d612cd81-a043-4fdb-b6fd-d043463d451a\","+
						"	    \"validityStart\" : \"2018-06-25T06:00:00Z\","+
						"	    \"validityEnd\" : \"2018-06-26T12:00:00Z\","+
//						"       \"baseTime\" : \"2018-09-12T13:00:00Z\","+
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


		objectMapper.setTimeZone(TimeZone.getTimeZone("UTC"));
		Taf tafObj=objectMapper.readValue(taf, Taf.class);
		tafObj.getMetadata().setUuid(UUID.randomUUID().toString());
		actualisedBaseTime=tafObj.getMetadata().getValidityStart();
		if (actualise) {
			OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z")).truncatedTo(ChronoUnit.HOURS);
			LOGGER.debug("{}", now);
			tafObj.getMetadata().setValidityStart(now.minusHours(1));
			actualisedBaseTime=tafObj.getMetadata().getValidityStart();
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
		LOGGER.debug("testing TAF life cycle");
		//Generate a valid TAF
		Taf baseTaf=getBaseTaf(true);
		LOGGER.debug("baseTaf:{}", baseTaf.toJSON(tafObjectMapper));
		//Store it
		LOGGER.debug("Storing base taf");
		String uuid=addTaf(baseTaf);
		LOGGER.debug("stored:{}", uuid);
		Taf storedTaf=getTaf(uuid);
		LOGGER.debug("from store:{}", storedTaf.toJSON(tafObjectMapper));
		//check if baseTime is set to validityStart
        OffsetDateTime baseTime=storedTaf.getMetadata().getBaseTime();
        assertEquals(storedTaf.getMetadata().getBaseTime(), storedTaf.getMetadata().getValidityStart());
		storedTaf.metadata.setBaseTime(null);
		LOGGER.debug("EQ: {}", baseTaf.equals(storedTaf));
		assertEquals(baseTaf, storedTaf);
		storedTaf.getMetadata().setBaseTime(baseTime);
		
		//Make an amendment with a new UUID. Ths should fail because TAF has not been published
		LOGGER.debug("Amending unpublished base taf");
		storedTaf.metadata.setType(TAFReportType.amendment);
		storedTaf.metadata.setPreviousUuid(uuid);
		storedTaf.metadata.setUuid(UUID.randomUUID().toString());
		storedTaf.metadata.setStatus(TAFReportPublishedConcept.concept);
		storedTaf.getForecast().getWind().setSpeed(20);
		String amendedConceptUuid = publishAndFail(storedTaf);
		LOGGER.debug("amendedUuid: {}", amendedConceptUuid);
		assertEquals(amendedConceptUuid, "FAIL");

		//Publish original TAF
		LOGGER.debug("Publishing base TAF");
		storedTaf=getTaf(uuid);
		storedTaf.metadata.setStatus(TAFReportPublishedConcept.published);
		String publishedUuid=storeTaf(storedTaf);
		LOGGER.debug("published: {}", publishedUuid);
		
		//Make another amendment with a new UUID.
		LOGGER.debug("Amending published base taf");
		storedTaf.metadata.setType(TAFReportType.amendment);
		storedTaf.metadata.setUuid(null);
		storedTaf.metadata.setPreviousUuid(publishedUuid);
		storedTaf.metadata.setStatus(TAFReportPublishedConcept.concept);
		storedTaf.getForecast().getWind().setSpeed(20);
		String amendedUuid=storeTaf(storedTaf);
		LOGGER.debug("amended base taf in concept: {}", amendedUuid);

		LOGGER.debug("Publishing amendment");
		Taf amendedTaf=getTaf(amendedUuid);
		assertNotNull(amendedTaf.getMetadata().getBaseTime());
		assertEquals(amendedTaf.getMetadata().getBaseTime(), baseTime);

		amendedTaf.metadata.setStatus(TAFReportPublishedConcept.published);
		amendedTaf.metadata.setUuid(null);
		String amendedPublishedUuid=storeTaf(amendedTaf);
		LOGGER.debug("Published amendment: {}", amendedPublishedUuid);

		LOGGER.debug("cancelling");
		Taf amendedPublishedTaf=getTaf(amendedPublishedUuid);
        assertNotNull(amendedPublishedTaf.getMetadata().getBaseTime());
        assertEquals(amendedPublishedTaf.getMetadata().getBaseTime(), baseTime);

        amendedPublishedTaf.metadata.setUuid(null);
		amendedPublishedTaf.metadata.setPreviousUuid(amendedPublishedUuid);
		amendedPublishedTaf.metadata.setStatus(TAFReportPublishedConcept.published);
		amendedPublishedTaf.metadata.setType(TAFReportType.canceled);
		String canceledUuid=storeTaf(amendedPublishedTaf);
		LOGGER.debug("Canceled uuid: {}", canceledUuid);

		Taf cancelTaf=getTaf(canceledUuid);
		assertNotNull(cancelTaf.getMetadata().getBaseTime());
        assertEquals(cancelTaf.getMetadata().getBaseTime(), baseTime);
	}

	public void addTAFTest () throws Exception {
		LOGGER.debug("get inactive tafs");
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) tafObjectMapper.readTree(responseBody);

		assertThat(jsonResult.has("ntafs"), is(true));
		assertThat(jsonResult.has("tafs"), is(true));
		int tafs = jsonResult.get("ntafs").asInt();

		LOGGER.debug("Add taff");
		String uuid = "v";//addTaf("");
		LOGGER.debug("Add taff done: {}", uuid);
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
		LOGGER.debug("{} === {}", new_tafs, tafs);
		assert(new_tafs == tafs + 1);
	}

	public void getTafList () throws Exception {
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
