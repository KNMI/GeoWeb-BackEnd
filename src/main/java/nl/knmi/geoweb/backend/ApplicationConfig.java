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

    private ObjectMapper objectMapper;

    public ApplicationConfig() {
        log.info("Constructor ApplicationConfig");
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

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper);
        converters.add(jsonConverter);
    }

    @Bean("sigmetObjectMapper")
    public ObjectMapper getSigmetObjectMapperBean() {
        log.info("sigmetObjectMapper");
        return objectMapper;
    }

    @Bean("airmetObjectMapper")
    public ObjectMapper getAirmetObjectMapperBean() {
        log.info("airmetObjectMapper");
        return objectMapper;
    }

    @Bean("tafObjectMapper")
    public ObjectMapper getTafObjectMapperBean() {
        log.info("tafObjectMapper");
        return objectMapper;
    }

    @Bean("geoWebObjectMapper")
    public ObjectMapper getGeoWebObjectMapperBean() {
        log.info("geoWebObjectMapper");
        return objectMapper;
    }

    @Bean(name = "objectMapper")
    @Primary
    public ObjectMapper getObjectMapperBean() {
        log.info("ObjectMapper");
        return objectMapper;
    }
}
