package nl.knmi.geoweb.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@TestConfiguration
@Import(nl.knmi.geoweb.iwxxm_2_1.converter.conf.GeoWebConverterConfig.class)
@EnableWebMvc
public class TestWebConfig implements WebMvcConfigurer {

	@Value("${productstorelocation}")
	private String storeLocation;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("POST", "HEAD", "GET", "OPTIONS", "DELETE");
	}

	// This matcher makes request paths case insensitive
	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		AntPathMatcher matcher = new AntPathMatcher();
		matcher.setCaseSensitive(false);
		configurer.setPathMatcher(matcher);
	}
}
