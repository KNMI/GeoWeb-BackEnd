package nl.knmi.geoweb.backend.security.configuration;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import nl.knmi.geoweb.backend.security.extractors.test.TestAuthoritiesExtractor;
import nl.knmi.geoweb.backend.security.extractors.test.TestPrincipalExtractor;

@Profile("test")
@Configuration
public class TestSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers("/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().permitAll()
                .and().csrf().disable();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(Arrays.asList(
            HttpMethod.POST.toString(), HttpMethod.HEAD.toString(), HttpMethod.GET.toString(), HttpMethod.OPTIONS.toString(), HttpMethod.DELETE.toString()
        ));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Profile("test")
    public PrincipalExtractor testPrincipalExtractor() {
        return new TestPrincipalExtractor();
    }

    @Bean
    @Profile("test")
    public AuthoritiesExtractor testAuthoritiesExtractor() {
        return new TestAuthoritiesExtractor();
    }
}
