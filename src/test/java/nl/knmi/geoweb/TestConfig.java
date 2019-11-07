package nl.knmi.geoweb;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import nl.knmi.geoweb.backend.aviation.AirportStore;
import nl.knmi.geoweb.backend.aviation.FIRStore;

@Configuration
@Import({nl.knmi.geoweb.iwxxm_2_1.converter.conf.GeoWebConverterConfig.class,
  nl.knmi.geoweb.backend.product.taf.converter.TafConverter.class, fi.fmi.avi.converter.json.conf.JSONConverter.class,
  nl.knmi.geoweb.backend.product.airmet.converter.AirmetConverter.class, nl.knmi.geoweb.backend.product.airmet.AirmetStore.class,
  nl.knmi.geoweb.backend.product.sigmet.converter.SigmetConverter.class, nl.knmi.geoweb.backend.product.sigmet.SigmetStore.class
  })

public class TestConfig {
  private static final String DATEFORMAT_ISO8601 = "yyyy-MM-dd'TT'HH:mm:ss'Y'";

  @Value("${geoweb.products.storeLocation}")
  private String storeLocation;

  private static ObjectMapper objectMapper;

  public TestConfig() {
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new Jdk8Module());
    om.registerModule(new JavaTimeModule());
    om.registerModule(new JsonOrgModule());
    om.setDateFormat(new SimpleDateFormat(DATEFORMAT_ISO8601));
    om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    SimpleModule simpleModule = new SimpleModule();
    simpleModule.addSerializer(OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {
      @Override
      public void serialize(OffsetDateTime offsetDateTime, JsonGenerator jsonGenerator,
          SerializerProvider serializerProvider) throws IOException {
        String formattedDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
        jsonGenerator.writeString(formattedDate);
      }
    });
    om.registerModule(simpleModule);
    om.setTimeZone(TimeZone.getTimeZone("UTC"));
    om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    objectMapper = om;
  }
  
  @Bean("sigmetObjectMapper")
  public static ObjectMapper getSigmetObjectMapperBean() {
    return objectMapper;
  }

  @Bean("airmetObjectMapper")
  public static ObjectMapper getAirmetObjectMapperBean() {
    return objectMapper;
  }

  @Bean("tafObjectMapper")
  public static ObjectMapper getTafObjectMapperBean() {
    return objectMapper;
  }


  @Bean("geoWebObjectMapper")
  public static ObjectMapper getGeoWebObjectMapperBean() {
    return objectMapper;
  }

  @Bean("objectMapper")
  @Primary
  public static ObjectMapper getObjectMapperBean() {
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    om.setTimeZone(TimeZone.getTimeZone("UTC"));
    return om;
  }

  @Bean
  @Primary
  public AirportStore getAirportStore() throws IOException {
    AirportStore airportStore = new AirportStore(storeLocation);
    return airportStore;
  }

  @Bean
  public FIRStore getFirStore() {
    FIRStore firStore = new FIRStore(storeLocation);
    return firStore;
  }
}
