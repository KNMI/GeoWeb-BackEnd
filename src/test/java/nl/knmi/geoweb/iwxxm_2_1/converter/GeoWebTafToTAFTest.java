package nl.knmi.geoweb.iwxxm_2_1.converter;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.TestConfig;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.converter.TafConverter;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class })
public class GeoWebTafToTAFTest {
    @Autowired
    @Qualifier("tafObjectMapper")
    private ObjectMapper tafObjectMapper;

    @Autowired
    private TafConverter tafConverter;

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
      Taf taf=setTafFromResource("Taf_valid.json");
      log.debug(taf.toTAC());
      String s = tafConverter.ToIWXXM_2_1(taf);
      log.debug("IWXXM:" + s);
	}
}
