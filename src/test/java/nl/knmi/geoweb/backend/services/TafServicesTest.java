package nl.knmi.geoweb.backend.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.WebApplicationContext;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.datastore.TafStore;
import nl.knmi.geoweb.backend.product.airmet.Airmet;
import nl.knmi.geoweb.backend.product.taf.Taf;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class TafServicesTest {
    // private String testGeoJsonBox = "{\"type\": \"FeatureCollection\",\"features\":[{\"type\": \"Feature\",\"id\": \"feb7bb38-a341-438d-b8f5-aa83685a0062\","
    //         + " \"properties\": {\"selectionType\": \"box\",\"featureFunction\": \"start\"},\"geometry\": {\"type\": \"Polygon\","
    //         + " \"coordinates\": [[[5.1618,51.4414],[5.1618,51.7424],[5.8444,51.7424],[5.8444,51.4414],[5.1618,51.4414]]]}}]}\"";

    // static String uuid = "b6ea2637-4652-42cc-97ac-4e34548d3cc7";
    // static String phenomenon = "OCNL_TSGR";
    // static String startTimestamp = "2019-02-12T08:00:00Z";
    // OffsetDateTime start = OffsetDateTime.parse(startTimestamp);
    // OffsetDateTime end = OffsetDateTime.parse("2019-02-12T11:00:00Z");

    private MockMvc mockMvc;

    private Taf validTaf;
    private String testTafValid;

    @Value("classpath:Taf_valid.json")
    private Resource validTafResource;

    /** The Spring web application context. */
    @Autowired
    private WebApplicationContext webApplicationContext;

    /** The {@link ObjectMapper} instance to be used. */
    @Autowired
    @Qualifier("tafObjectMapper")
    private ObjectMapper tafObjectMapper;

    @Autowired
    TafStore tafStore;

    @Autowired
    private ProductExporter<Taf> tafExporter;

	@Before
	public void setUp() throws IOException {
        testTafValid = StreamUtils.copyToString(validTafResource.getInputStream(), StandardCharsets.UTF_8);
        validTaf = tafObjectMapper.readValue(testTafValid, Taf.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        reset(tafStore);
        reset(tafExporter);
    }

    @Test
    public void serviceTestPostCorrectTaf() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(post("/tafs/").contentType(MediaType.APPLICATION_JSON_UTF8).content(testTafValid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message", is("Taf with id " + validTaf.metadata.getUuid() + " is stored")))
                .andExpect(jsonPath("$.succeeded", is(true)))
                .andExpect(jsonPath("$.uuid", is(validTaf.metadata.getUuid())));

        verify(tafStore, times(1)).storeTaf(any(Taf.class));
        verify(tafStore, times(1)).isPublished(anyString());
        verifyNoMoreInteractions(tafStore);
    }

	// private String getValidTaf() throws Exception  {
	// 	String taf = Tools.readResource("Taf_valid.json");
	// 	ObjectNode tafJson = (ObjectNode)objectMapper.readTree(taf);

	// 	OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z")).truncatedTo(ChronoUnit.HOURS);
	// 	ObjectNode metadataNode = (ObjectNode)tafJson.findParent("validityStart");
	// 	metadataNode.put("issueTime", now.minusHours(2).format(DateTimeFormatter.ISO_INSTANT));
	// 	metadataNode.put("validityStart", now.minusHours(1).format(DateTimeFormatter.ISO_INSTANT));
	// 	metadataNode.put("validityEnd", now.plusHours(29).format(DateTimeFormatter.ISO_INSTANT));
	// 	metadataNode.put("baseTime", now.minusHours(1).format(DateTimeFormatter.ISO_INSTANT));
	// 	metadataNode.put("uuid",  UUID.randomUUID().toString());
	// 	tafJson.set("metadata", (JsonNode)metadataNode);
	// 	tafJson.set("changegroups", (JsonNode)objectMapper.readTree("[]"));
	// 	return tafJson.toString();
	// }

    @Test
    public void serviceTestGetTafList() throws Exception {
        // given
        String metaField = "metadata";
        ObjectNode adjustedTaf = (ObjectNode) tafObjectMapper.readTree(testTafValid);
        Arrays.asList( "validityStart", "validityEnd", "issueTime", "baseTime" ).forEach(fieldName -> {
            String fieldValue = adjustedTaf.get(metaField).get(fieldName).asText();
            String adjustedFieldValue;
            try {
                adjustedFieldValue = tafObjectMapper.readValue(fieldValue, OffsetDateTime.class).plusHours(6)
                        .format(DateTimeFormatter.ISO_INSTANT);
                ((ObjectNode) adjustedTaf.get(metaField)).put(fieldName, adjustedFieldValue);
            } catch (IOException exception) {
                log.debug(exception.getMessage());
            }
        });

        // when
        when(tafStore.getTafs(false, null, null, null)).thenReturn(new Taf[] { validTaf });
                // .thenReturn(new Taf[] { validTaf, new Taf(validTaf) })
                // .thenReturn(new Airmet[] { airmet, new Airmet(airmet), new Airmet(airmet) });
        // then
        // mockMvc.perform(get("/airmets/?active=false")).andExpect(status().isOk())
        //         .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        //         .andExpect(jsonPath("$.error").doesNotExist()).andExpect(jsonPath("$.page", is(0)))
        //         .andExpect(jsonPath("$.count", is(0))).andExpect(jsonPath("$.nairmets", is(1)))
        //         .andExpect(jsonPath("$.airmets[0].uuid", is(uuid)));
        // mockMvc.perform(get("/airmets/?active=false")).andExpect(status().isOk())
        //         .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        //         .andExpect(jsonPath("$.error").doesNotExist()).andExpect(jsonPath("$.nairmets", is(2)));
        // mockMvc.perform(get("/airmets/?active=false")).andExpect(status().isOk())
        //         .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        //         .andExpect(jsonPath("$.error").doesNotExist()).andExpect(jsonPath("$.nairmets", is(3)));

        verify(tafStore, times(3)).getTafs(anyBoolean(), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(tafStore);
    }

// 	@Test
// 	public void getTafList () throws Exception {
// 		String uuid=addTaf();
// 		log.debug("TAF "+uuid+" stored");
// 		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                 .andReturn();

// 		String responseBody = result.getResponse().getContentAsString();
// 		ObjectMapper om=new ObjectMapper();
// 		ObjectNode jsonResult = (ObjectNode) om.readTree(responseBody);
//         assertThat(jsonResult.has("page"), is(true));
//         assertThat(jsonResult.has("npages"), is(true));
//         assertThat(jsonResult.has("ntafs"), is(true));
//         assertThat(jsonResult.has("tafs"), is(true));
//         assert(jsonResult.get("ntafs").asInt() >= 1);
//         int ntafs=jsonResult.get("ntafs").asInt();
// 		result = mockMvc.perform(get("/tafs?active=true"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                 .andReturn();

// 		responseBody = result.getResponse().getContentAsString();
// 		log.debug("getTafList:"+responseBody);
// 		jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
//         assertThat(jsonResult.has("page"), is(true));
//         assertThat(jsonResult.has("npages"), is(true));
//         assertThat(jsonResult.has("ntafs"), is(true));
//   //      assertThat(jsonResult.has("tafs"), is(false));
//   //      assertThat(jsonResult.get("ntafs").asInt(), is(0));
//         assert(ntafs>=jsonResult.get("ntafs").asInt());

// 	}

// 	@Test
// 	public void removeTaf () throws Exception {
// 		String uuid = addTaf();
// 		log.debug("TAF with uuid "+uuid+" added");
// 		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                 .andReturn();
// 		String responseBody = result.getResponse().getContentAsString();
// 		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
// 		int tafCount = jsonResult.get("ntafs").asInt();

// 		mockMvc.perform(delete("/tafs/" + uuid))
// 			.andExpect(status().isOk())
// 	        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
// 	        .andReturn();
// 		result = mockMvc.perform(get("/tafs?active=false"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                 .andReturn();
// 		responseBody = result.getResponse().getContentAsString();
// 		jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
// 		int newTafCount = jsonResult.get("ntafs").asInt();
// 		assert(newTafCount == tafCount - 1);
// 	}

}
