package nl.knmi.geoweb.backend.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;

@RunWith(SpringRunner.class)
@SpringBootTest(classes= {TestWebConfig.class,TafServicesTestContext.class})
@DirtiesContext
public class TafServicesTest {
	/** Entry point for Spring MVC testing support. */
    private MockMvc mockMvc;
    
    /** The Spring web application context. */
    @Resource
    private WebApplicationContext webApplicationContext;
    
    /** The {@link ObjectMapper} instance to be used. */
    @Autowired
    @Qualifier("tafObjectMapper")
    private ObjectMapper objectMapper;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		Debug.println("Cleaning /tmp/tafs");
		for(File file: new File("/tmp/tafs").listFiles())
			if (!file.isDirectory()) {
				file.delete();
			}
	}

	private String getValidTaf() throws Exception  {
		String taf = Tools.readResource("Taf_valid.json");
		ObjectNode tafJson = (ObjectNode)objectMapper.readTree(taf);

		OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z")).truncatedTo(ChronoUnit.HOURS);
		ObjectNode metadataNode = (ObjectNode)tafJson.findParent("validityStart");
		metadataNode.put("issueTime", now.minusHours(2).format(DateTimeFormatter.ISO_INSTANT));
		metadataNode.put("validityStart", now.minusHours(1).format(DateTimeFormatter.ISO_INSTANT));
		metadataNode.put("validityEnd", now.plusHours(29).format(DateTimeFormatter.ISO_INSTANT));
		metadataNode.put("baseTime", now.minusHours(1).format(DateTimeFormatter.ISO_INSTANT));
		metadataNode.put("uuid",  UUID.randomUUID().toString());
		tafJson.set("metadata", (JsonNode)metadataNode);
		tafJson.set("changegroups", (JsonNode)objectMapper.readTree("[]"));
		return tafJson.toString();
	}
	
	private String addTaf() throws Exception {
		MvcResult result = mockMvc.perform(post("/tafs")
				.contentType(MediaType.APPLICATION_JSON_UTF8).content(getValidTaf()))
				.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();	
		String responseBody = result.getResponse().getContentAsString();
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);

        assertThat(jsonResult.has("error"), is(false));
        assertThat(jsonResult.has("message"), is(true));
        assertThat(jsonResult.get("message").asText().length(), not(0));
        String uuid = jsonResult.get("uuid").asText();
        return uuid;
	}
	 
	@Test
	public void addTAFTest () throws Exception {
		Debug.println("get inactive tafs");
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		String responseBody = result.getResponse().getContentAsString();
		Debug.println("resp: "+responseBody);
		ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		System.err.println("Before add: "+jsonResult);

        assertThat(jsonResult.has("ntafs"), is(true));
        int tafs = jsonResult.get("ntafs").asInt();

        Debug.println("Add taff");
		String uuid = addTaf();
		Debug.println("Add taff done: "+ uuid);
		assert(uuid != null);
		
		result = mockMvc.perform(get("/tafs?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		responseBody = result.getResponse().getContentAsString();
		jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
		System.err.println("After add: "+jsonResult);
        assertThat(jsonResult.has("ntafs"), is(true));
        assertThat(jsonResult.has("tafs"), is(true));
        int new_tafs = jsonResult.get("ntafs").asInt();
        Debug.println("" + new_tafs + " === " + tafs);
        assert(new_tafs == tafs + 1);
	}
	
	@Test
	public void getTafList () throws Exception {
		String uuid=addTaf();
		Debug.println("TAF "+uuid+" stored");
		MvcResult result = mockMvc.perform(get("/tafs?active=false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		String responseBody = result.getResponse().getContentAsString();
		ObjectMapper om=new ObjectMapper();
		ObjectNode jsonResult = (ObjectNode) om.readTree(responseBody);
        assertThat(jsonResult.has("page"), is(true));
        assertThat(jsonResult.has("npages"), is(true));
        assertThat(jsonResult.has("ntafs"), is(true));
        assertThat(jsonResult.has("tafs"), is(true));
        assert(jsonResult.get("ntafs").asInt() >= 1);
        int ntafs=jsonResult.get("ntafs").asInt();
		result = mockMvc.perform(get("/tafs?active=true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
		
		responseBody = result.getResponse().getContentAsString();
		Debug.println("getTafList:"+responseBody);
		jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("page"), is(true));
        assertThat(jsonResult.has("npages"), is(true));
        assertThat(jsonResult.has("ntafs"), is(true));
        assert(ntafs>=jsonResult.get("ntafs").asInt());

	}
	
	@Test
	public void removeTaf () throws Exception {
		String uuid = addTaf();
		System.err.println("TAF with uuid "+uuid+" added");
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
