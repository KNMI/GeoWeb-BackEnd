package nl.knmi.geoweb.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class ServicesConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicesConfig.class);

	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		return new MappingJackson2HttpMessageConverter(getObjectMapper());
	}

	private ObjectMapper getObjectMapper() {
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new JavaTimeModule());
		om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

		return om;
	}

	@Bean("sigmetObjectMapper")
	public ObjectMapper getSigmetObjectMapperBean() {
		LOGGER.debug("Init SigmetObjectMapperBean (services)");
		return getObjectMapper();

	}

	@Bean("tafObjectMapper")
	public ObjectMapper getTafObjectMapperBean() {
		LOGGER.debug("Init TafObjectMapperBean (services)");
		return getObjectMapper();
	}
	
	@Bean("geoWebObjectMapper")
	public ObjectMapper getGeoWebObjectMapperBean() {
		LOGGER.debug("Init GeoWebObjectMapperBean (services)");
		return getObjectMapper();
	}
	
	@Bean("objectMapper")
	@Primary
	public ObjectMapper getObjectMapperBean() {
		LOGGER.debug("Init ObjectMapperBean (services)");
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new JavaTimeModule());
		return om;
	}
}
