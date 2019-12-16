package nl.knmi.geoweb.backend.security.configuration;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import nl.knmi.geoweb.backend.security.extractors.generic.GenericAuthoritiesExtractor;
import nl.knmi.geoweb.backend.security.extractors.generic.GenericPrincipalExtractor;

@Profile("generic")
@Configuration
public class GenericSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("nl/knmi/geoweb/security/rolesToPrivilegesMapping.json")
    private ClassPathResource mappingResource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/**").authorizeRequests().anyRequest().permitAll()
                .and().cors()
                .and().csrf().disable()
                .logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout/geoweb"));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Profile("generic")
    public PrincipalExtractor genericPrincipalExtractor() {
        return new GenericPrincipalExtractor();
    }

    @Bean
    @Profile("generic")
    public AuthoritiesExtractor genericAuthoritiesExtractor() {
        return new GenericAuthoritiesExtractor(objectMapper, mappingResource);
    }
}
