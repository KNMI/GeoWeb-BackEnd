package nl.knmi.geoweb.backend.services;

import nl.knmi.geoweb.backend.ApplicationConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(ApplicationConfig.class)
public class ExportedProductServicesTest {
    @Value("${geoweb.products.exportLocation}")
    private String productexportlocation;

    private MockMvc mockMvc;

    /** The Spring web application context. */
    @Autowired
    private WebApplicationContext webApplicationContext;

    private void saveFile(String contents, String fn) throws IOException {
        Path p = Paths.get(productexportlocation,fn);
        Files.write(p, contents.getBytes());
    }

    @Before
    public static void clean() {
        File dir = new File(productexportlocation);
        for (File file:dir.listFiles()) {
            file.delete();
        }
    }

    private static String json1="[{\"key\": \"KEY\"}]";
    private static String json1Name="test1_20191105140000.json";
    private static String xml1="<?xml version=\"1.0\"><start>20191105140000</start>";
    private static String xml1Name="test1_20191105140000.xml";
    private static String tac1="test1";
    private static String tac1Name="test1_20191105140000.tac";
    private static String xml2="<?xml version=\"1.0\"><start>20191105150000</start>";
    private static String xml2Name="test1_20191105150000.xml";

    @Before
    public void setupTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

@Test
  public void testListExports() throws Exception {

    saveFile(xml2, xml2Name);
    saveFile(json1, json1Name);
    saveFile(tac1, tac1Name);
    saveFile(xml1, xml1Name);

    mockMvc.perform(get("/exportedproducts/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$[0]", is(json1Name)))
                .andExpect(jsonPath("$[1]", is(tac1Name)))
                .andExpect(jsonPath("$[2]", is(xml1Name)))
                .andExpect(jsonPath("$[3]", is(xml2Name)))
        ;
  }

  @Test
    public void testGetExportedFile() throws Exception {
      saveFile(tac1, tac1Name);
      mockMvc.perform(get("/exportedproducts/get?file="+tac1Name))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.TEXT_PLAIN))
          .andExpect(content().string(tac1))
          ;

      saveFile(json1, json1Name);
      mockMvc.perform(get("/exportedproducts/get?file="+json1Name))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
          .andExpect(content().string(json1))
          ;

      saveFile(xml1, xml1Name);
      mockMvc.perform(get("/exportedproducts/get?file="+xml1Name))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_XML_VALUE))
          .andExpect(content().string(xml1))
          ;

      mockMvc.perform(get("/exportedproducts/get?file="+"absentfile.xml"))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType(MediaType.APPLICATION_XML_VALUE))
      ;

  }
}
