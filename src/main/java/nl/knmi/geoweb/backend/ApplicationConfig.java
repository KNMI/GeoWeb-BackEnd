package nl.knmi.geoweb.backend;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@Order(-100)
public class ApplicationConfig implements WebMvcConfigurer {
    private static final String DATEFORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public ApplicationConfig() {
        log.trace("Constructor ApplicationConfig");
    }

    private void omBaseSettings(ObjectMapper om) {
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


    }


    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper om=new ObjectMapper();
        omBaseSettings(om);
        jsonConverter.setObjectMapper(om);
        converters.add(jsonConverter);
    }

    @Bean("sigmetObjectMapper")
    public ObjectMapper getSigmetObjectMapperBean() {
        log.trace("sigmetObjectMapper");
        ObjectMapper om=new ObjectMapper();
        omBaseSettings(om);
        return om;
    }

    @Bean("airmetObjectMapper")
    public ObjectMapper getAirmetObjectMapperBean() {
        log.trace("airmetObjectMapper");
        ObjectMapper om=new ObjectMapper();
        omBaseSettings(om);
        return om;
    }

    @Bean("tafObjectMapper")
    public ObjectMapper getTafObjectMapperBean() {
        log.trace("tafObjectMapper");
        ObjectMapper om=new ObjectMapper();
        omBaseSettings(om);
        return om;
    }

    @Bean("geoWebObjectMapper")
    public ObjectMapper getGeoWebObjectMapperBean() {
        log.trace("geoWebObjectMapper");
        ObjectMapper om=new ObjectMapper();
        omBaseSettings(om);
        return om;
    }

    @Bean(name = "objectMapper")
    @Primary
    public ObjectMapper getObjectMapperBean() {
        log.trace("ObjectMapper");
        ObjectMapper om=new ObjectMapper();
        omBaseSettings(om); //TODO are all these settings necessary (or too much)???
        om.setTimeZone(TimeZone.getTimeZone("UTC"));
        return om;
    }
}
