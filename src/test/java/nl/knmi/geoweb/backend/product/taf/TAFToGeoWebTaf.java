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
import nl.knmi.geoweb.TestConfig;
import nl.knmi.geoweb.iwxxm_2_1.converter.GeoWebTafInConverter;

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
            System.err.println("TAF:"+taf);
        } catch (IOException e) {
            System.err.println("OhOh "+e);
        }
        TAF completedTaf= TAFImpl.Builder.from(taf).withAllTimesComplete(ZonedDateTime.now()).build();
        System.err.println("completedTAF:"+completedTaf);
        ConversionHints hints=new ConversionHints();
        ConversionResult<Taf> result=geowebTafInConverter.convertMessage(completedTaf, hints);
        System.err.println("Conversion status: "+ result.getStatus());
        System.err.println(result.getConvertedMessage().get().toTAC());
    }
}
