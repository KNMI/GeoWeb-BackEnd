package nl.knmi.geoweb.backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.knmi.adaguc.tools.Debug;
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class TriggerServiceTest {
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

    @Test
    public void apiTestCalculateTrigger() throws Exception {
        MvcResult result = mockMvc.perform(get("/triggers/calculatetrigger"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();

        String responseArray = result.getResponse().getContentAsString();
        String responseBody = responseArray.substring(1);
        ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("locations"), is(true));
        assertThat(jsonResult.has("phenomenon"), is(true));
        Debug.println("calculateTrigger() result:" + responseBody);
    }

    @Test
    public void apiTestGetParameters() throws Exception {
        MvcResult result = mockMvc.perform(get("/triggers/parametersget"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Debug.println("getParameters() result:" + responseBody);
    }

    static String testUnit = "{\"parameter\":\"Air Temperature 1 Min Average\"}";
    @Test
    public void apiTestGetUnit() throws Exception {
        MvcResult result = mockMvc.perform(post("/triggers/unitget")
                .contentType(MediaType.APPLICATION_JSON_UTF8).content(testUnit))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("unit"), is(true));
        assertThat(jsonResult.get("unit").asText(), is("degrees Celsius"));
        Debug.println("getUnit() result: " + responseBody);
    }

    @Test
    public void apiTestGetTriggers() throws Exception {
        MvcResult result = mockMvc.perform(get("/triggers/gettriggers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();

        String responseArray = result.getResponse().getContentAsString();
        String responseBody = responseArray.substring(1);
        ObjectNode jsonResult = (ObjectNode) objectMapper.readTree(responseBody);
        assertThat(jsonResult.has("phenomenon"), is(true));
        Debug.println("getTriggers() result:"+responseBody);
    }
}
