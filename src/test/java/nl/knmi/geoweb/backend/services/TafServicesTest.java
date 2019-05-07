package nl.knmi.geoweb.backend.services;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.WebApplicationContext;

import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.datastore.TafStore;
import nl.knmi.geoweb.backend.product.taf.Taf;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TafServicesTest {
    private static String META_FIELD = "metadata";
    private static List<String> DATE_FIELDS = Arrays.asList("validityStart","validityEnd","issueTime");

    private MockMvc mockMvc;

    private Taf validTaf;
    private String testTafValidRaw;
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
        if (testTafValidRaw == null) {
            testTafValidRaw = StreamUtils.copyToString(validTafResource.getInputStream(), StandardCharsets.UTF_8);
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z"));
        ObjectNode testTafValidNode = (ObjectNode) tafObjectMapper.readTree(testTafValidRaw);
        String testTafValidityStart = testTafValidNode.get(META_FIELD).get(DATE_FIELDS.get(0)).asText();
        long daysOffset = Duration.between(tafObjectMapper.convertValue(testTafValidityStart, OffsetDateTime.class), now)
                .toDays();

        DATE_FIELDS.forEach(fieldName -> {
            String fieldValue = testTafValidNode.get(META_FIELD).get(fieldName).asText();
            String adjustedFieldValue = tafObjectMapper.convertValue(fieldValue, OffsetDateTime.class)
                    .plusDays(daysOffset)
                    .format(DateTimeFormatter.ISO_INSTANT);
            ((ObjectNode) testTafValidNode.get(META_FIELD)).put(fieldName, adjustedFieldValue);
        });
        testTafValid = tafObjectMapper.writeValueAsString(testTafValidNode);
        validTaf = tafObjectMapper.convertValue(testTafValidNode, Taf.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        reset(tafStore);
        reset(tafExporter);
    }

    @Test
    public void serviceTestPostCorrectTaf() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(post("/tafs").contentType(MediaType.APPLICATION_JSON_UTF8).content(testTafValid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message", is("Taf with id " + validTaf.metadata.getUuid() + " is stored")))
                .andExpect(jsonPath("$.succeeded", is(true)))
                .andExpect(jsonPath("$.uuid", is(validTaf.metadata.getUuid())))
                .andExpect(jsonPath("$.tafjson.metadata.baseTime",
                        is(validTaf.metadata.getValidityStart().format(DateTimeFormatter.ISO_INSTANT))));

        verify(tafStore, times(1)).storeTaf(any(Taf.class));
        verify(tafStore, times(1)).isPublished(anyString());
        verifyNoMoreInteractions(tafStore);
    }

    @Test
    public void serviceTestGetTafList() throws Exception {
        // given
        // when
        when(tafStore.getTafs(false, null, null, null)).thenReturn(new Taf[] { validTaf });
        when(tafStore.getTafs(true, null, null, null)).thenReturn(new Taf[0]);
        // then
        mockMvc.perform(get("/tafs/?active=false")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.npages", is(1)))
                .andExpect(jsonPath("$.ntafs", is(1)))
                .andExpect(jsonPath("$.tafs[0].metadata.uuid", is(validTaf.metadata.getUuid())));
        mockMvc.perform(get("/tafs/?active=true")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.npages", is(1)))
                .andExpect(jsonPath("$.ntafs", is(0)));

        verify(tafStore, times(1)).getTafs(false, null, null, null);
        verify(tafStore, times(1)).getTafs(true, null, null, null);
        verifyNoMoreInteractions(tafStore);
    }

    @Test
    public void serviceTestGetTaf() throws Exception {
        // given
        String uuid = validTaf.metadata.getUuid();
        // when
        when(tafStore.getByUuid(uuid)).thenReturn(validTaf);
        // then
        mockMvc.perform(get("/tafs/" + uuid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.metadata.uuid", is(validTaf.metadata.getUuid())));

        verify(tafStore, times(1)).getByUuid(anyString());
        verifyNoMoreInteractions(tafStore);
    }

    @Test
    public void serviceTestRemoveTaf() throws Exception {
        // given
        String uuid = validTaf.metadata.getUuid();
        // when
        when(tafStore.getByUuid(uuid)).thenReturn(validTaf);
        when(tafStore.deleteTafByUuid(uuid)).thenReturn(true);
        // then
        mockMvc.perform(delete("/tafs/" + uuid)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message", is("deleted " + uuid)));

        verify(tafStore, times(1)).getByUuid(anyString());
        verify(tafStore, times(1)).deleteTafByUuid(anyString());
        verifyNoMoreInteractions(tafStore);
    }
}
