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
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.WebApplicationContext;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportType;
import nl.knmi.geoweb.backend.product.taf.converter.TafConverter;

@Slf4j
@RunWith(SpringRunner.class)
@ActiveProfiles({"test", "lifecycle"})
@SpringBootTest
@DirtiesContext
public class TafServicesLifeCycleTest {
    /** Entry point for Spring MVC testing support. */
    private MockMvc mockMvc;

    /** The Spring web application context. */
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Value("classpath:Taf_valid.json")
    private Resource validTafResource;

    @Value("${geoweb.products.storelocation}")
    private String productstorelocation;

    @Autowired
    private TafConverter tafConverter;

    /** The {@link ObjectMapper} instance to be used. */
    @Autowired
    @Qualifier("tafObjectMapper")
    private ObjectMapper tafObjectMapper;

    @Autowired
    @Qualifier("objectMapper")
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        if (productstorelocation == null) {
			throw new Exception("geoweb.products.storeLocation property for testing is null");
        }
        File[] tafFiles = new File(productstorelocation + "/tafs").listFiles();
        if (tafFiles != null) {
            for(File file: tafFiles) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
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
		mockMvc.perform(post("/tafs")
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
		log.debug("resp: "+responseBody);
		log.debug("status: "+result.getResponse().getStatus());
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);

		assertThat(jsonResult.has("error"), is(false));
		assertThat(jsonResult.has("message"), is(true));
		assertThat(jsonResult.get("message").asText().length(), not(0));
		String uuid = jsonResult.get("uuid").asText();

		return uuid;
	}

	static OffsetDateTime actualisedBaseTime;

	private Taf getBaseTaf(boolean actualise) throws JsonProcessingException, IOException {
		log.trace("getBaseTaf");
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
		tafObj.getMetadata().setUuid(UUID.randomUUID().toString());
		actualisedBaseTime=tafObj.getMetadata().getValidityStart();
		if (actualise) {
			OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z")).truncatedTo(ChronoUnit.HOURS);
			log.debug(now.toString());
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
		log.trace("Testing TAF life cycle");
		//Generate a valid TAF
		Taf baseTaf=getBaseTaf(true);
		log.debug("baseTaf:"+baseTaf.toJSON(tafObjectMapper));
		//Store it
		log.debug("Storing base taf");
		String uuid=addTaf(baseTaf);
		log.debug("stored:" + uuid);
		Taf storedTaf=getTaf(uuid);
		log.debug("from store:" + storedTaf.toJSON(tafObjectMapper));
		//check if baseTime is set to validityStart
        OffsetDateTime baseTime=storedTaf.getMetadata().getBaseTime();
        assertEquals(storedTaf.getMetadata().getBaseTime(), storedTaf.getMetadata().getValidityStart());
		storedTaf.metadata.setBaseTime(null);
		log.debug("EQ: " + baseTaf.equals(storedTaf));
		assertEquals(baseTaf, storedTaf);
		storedTaf.getMetadata().setBaseTime(baseTime);

		//Make an amendment with a new UUID. Ths should fail because TAF has not been published
		log.trace("Amending unpublished base taf");
		storedTaf.metadata.setType(TAFReportType.amendment);
		storedTaf.metadata.setPreviousUuid(uuid);
		storedTaf.metadata.setUuid(UUID.randomUUID().toString());
		storedTaf.metadata.setStatus(TAFReportPublishedConcept.concept);
		storedTaf.getForecast().getWind().setSpeed(20);
		String amendedConceptUuid = publishAndFail(storedTaf);
		log.debug("amendedUuid: " + amendedConceptUuid);
		assertEquals(amendedConceptUuid, "FAIL");

		//Publish original TAF
		log.trace("Publishing base TAF");
		storedTaf=getTaf(uuid);
		storedTaf.metadata.setStatus(TAFReportPublishedConcept.published);
		String publishedUuid=storeTaf(storedTaf);
		log.debug("published: " + publishedUuid);

		//Make another amendment with a new UUID.
		log.trace("Amending published base taf");
		storedTaf.metadata.setType(TAFReportType.amendment);
		storedTaf.metadata.setUuid(null);
		storedTaf.metadata.setPreviousUuid(publishedUuid);
		storedTaf.metadata.setStatus(TAFReportPublishedConcept.concept);
		storedTaf.getForecast().getWind().setSpeed(20);
		String amendedUuid=storeTaf(storedTaf);
		log.debug("amended base taf in concept: "+amendedUuid);

		log.trace("Publishing amendment");
		Taf amendedTaf=getTaf(amendedUuid);
		assertNotNull(amendedTaf.getMetadata().getBaseTime());
		assertEquals(amendedTaf.getMetadata().getBaseTime(), baseTime);

		amendedTaf.metadata.setStatus(TAFReportPublishedConcept.published);
		amendedTaf.metadata.setUuid(null);
		String amendedPublishedUuid=storeTaf(amendedTaf);
		log.debug("Published amendment: "+amendedPublishedUuid);

		log.trace("cancelling");
		Taf amendedPublishedTaf=getTaf(amendedPublishedUuid);
        assertNotNull(amendedPublishedTaf.getMetadata().getBaseTime());
        assertEquals(amendedPublishedTaf.getMetadata().getBaseTime(), baseTime);

        amendedPublishedTaf.metadata.setUuid(null);
		amendedPublishedTaf.metadata.setPreviousUuid(amendedPublishedUuid);
		amendedPublishedTaf.metadata.setStatus(TAFReportPublishedConcept.published);
		amendedPublishedTaf.metadata.setType(TAFReportType.canceled);
		String canceledUuid=storeTaf(amendedPublishedTaf);
		log.debug("Canceled uuid: " + canceledUuid);

		Taf cancelTaf=getTaf(canceledUuid);
		assertNotNull(cancelTaf.getMetadata().getBaseTime());
        assertEquals(cancelTaf.getMetadata().getBaseTime(), baseTime);
	}

	public void addTAFTest () throws Exception {
		log.trace("Get inactive tafs");
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) tafObjectMapper.readTree(responseBody);

		assertThat(jsonResult.has("ntafs"), is(true));
		assertThat(jsonResult.has("tafs"), is(true));
		int tafs = jsonResult.get("ntafs").asInt();

		log.trace("Add taf");
		String uuid = "v";//addTaf("");
		log.debug("Add taf done: "+ uuid);
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
		log.debug("" + new_tafs + " === " + tafs);
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

 	@Test
	public void TafToTAFTest1() throws IOException {
        String tafTAC = "TAF EHAM 041101Z 0412/0518 20015G25KT CAVOK\n"
                + "BECMG 0416/0420 23017G27KT 9000 SHRA TSRA FEW009 SCT015 OVC720CB\n"
                + "PROB30 0416/0420 22037G47KT 9999 FEW009 OVC015CB\n"
                + "BECMG 0503/0505 VRB07G17KT 9999 SHRA FEW010CB";
        Taf taf = null;
        String json = null;
        json = Tools.readResource("Taf_valid.json");
	    if (json != null && !json.equals("")) {
            taf = tafObjectMapper.readValue(json, Taf.class);
        } else {
            log.error("json null");
        }

        assertEquals(tafTAC, taf.toTAC());
        log.debug(taf.toTAC());
        String s = tafConverter.ToIWXXM_2_1(taf);
        log.debug("S:"+s);


        String testTafValidRaw = StreamUtils.copyToString(validTafResource.getInputStream(), StandardCharsets.UTF_8);
        // OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z"));
        ObjectNode testTafValidNode = (ObjectNode) tafObjectMapper.readTree(testTafValidRaw);
        // String testTafValidityStart = testTafValidNode.get(META_FIELD).get(DATE_FIELDS.get(0)).asText();
        // long daysOffset = Duration
        //         .between(tafObjectMapper.convertValue(testTafValidityStart, OffsetDateTime.class), now).toDays();

        // DATE_FIELDS.forEach(fieldName -> {
        //     String fieldValue = testTafValidNode.get(META_FIELD).get(fieldName).asText();
        //     String adjustedFieldValue = tafObjectMapper.convertValue(fieldValue, OffsetDateTime.class)
        //             .plusDays(daysOffset).format(DateTimeFormatter.ISO_INSTANT);
        //     ((ObjectNode) testTafValidNode.get(META_FIELD)).put(fieldName, adjustedFieldValue);
        // });
        // testTafValid = tafObjectMapper.writeValueAsString(testTafValidNode);
        Taf validTaf = tafObjectMapper.convertValue(testTafValidNode, Taf.class);
        log.debug(validTaf.toTAC());
        assertEquals(tafTAC, validTaf.toTAC());
        log.debug("iWXXM:" + tafConverter.ToIWXXM_2_1(validTaf));
    }

    public Taf setTafFromResource(String fn) {
        String json = "";
        try {
            json = Tools.readResource(fn);
        } catch (IOException e) {
            log.error("Can't read resource " + fn);
        }
        return setTafFromString(json);
    }

    public Taf setTafFromString(String json) {
        Taf taf = null;
        try {
            taf = tafObjectMapper.readValue(json, Taf.class);
            return taf;
        } catch (JsonParseException | JsonMappingException e) {

        } catch (IOException e) {
            log.error(e.getMessage());
        }
        log.error("set TAF from string [" + json + "] failed");
        return null;
    }

    @Test
    public void TafToTAFTest() throws JsonProcessingException {
        Taf taf = setTafFromResource("Taf_valid.json");
        log.debug(taf.toTAC());
        log.debug(tafObjectMapper.writeValueAsString(taf));
        String s = tafConverter.ToIWXXM_2_1(taf);
        log.debug("IWXXM:" + s);
    }

}
