package nl.knmi.geoweb.backend.product.taf;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.TestConfig;
import nl.knmi.geoweb.iwxxm_2_1.converter.GeoWebTafInConverter;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class })
public class TAFToGeoWebTaf {
    @Autowired
    @Qualifier("tafObjectMapper")
    private ObjectMapper tafObjectMapper;

    @Autowired
    @Qualifier("aviTafInSpecificMessageConverter")
    private GeoWebTafInConverter geowebTafInConverter;
    
    @Value("classpath:nl/knmi/geoweb/backend/product/taf/TAFToGeoWebTaf1.json")
    Resource tafResource;

    @Test
    public void testTaftoGeoWebTaf() {
        TAF taf=null;
        try {
            taf = tafObjectMapper.readValue(tafResource.getInputStream(), TAFImpl.class);
            log.info("TAF:" + taf);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        TAF completedTaf= TAFImpl.Builder.from(taf).withAllTimesComplete(ZonedDateTime.now()).build();
        log.debug("completedTAF:" + completedTaf);
        ConversionHints hints=new ConversionHints();
        ConversionResult<Taf> result=geowebTafInConverter.convertMessage(completedTaf, hints);
        log.debug("Conversion status: " + result.getStatus());
        log.debug(result.getConvertedMessage().get().toTAC());
    }
}
