package nl.knmi.geoweb.backend.security.configuration;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import nl.knmi.geoweb.backend.security.converters.GeoWebJwtConverter;
import nl.knmi.geoweb.backend.security.extractors.github.GithubAuthoritiesExtractor;
import nl.knmi.geoweb.backend.security.extractors.github.GithubPrincipalExtractor;
import nl.knmi.geoweb.backend.security.extractors.keycloak.KeycloakAuthoritiesExtractor;
import nl.knmi.geoweb.backend.security.extractors.keycloak.KeycloakPrincipalExtractor;
import nl.knmi.geoweb.backend.security.models.Privilege;

@Configuration
@EnableOAuth2Sso
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:nl/knmi/geoweb/security/rolesToPrivilegesMapping.json")
    private Resource mappingResource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // The order of the rules matters and the more specific request matchers should go first.
        // The first match in the list below will be evaluated
        http.antMatcher("/**").authorizeRequests()
                .antMatchers(HttpMethod.GET, "/login", "/login/geoweb", "/login/options", "/logout", "/logout/options", "/status").permitAll()
                .antMatchers(HttpMethod.GET, "/sigmets/**").hasAuthority(Privilege.SIGMET_READ.getAuthority())
                .antMatchers(HttpMethod.GET, "/tafs/**").hasAuthority(Privilege.TAF_READ.getAuthority())
                .antMatchers(HttpMethod.GET, "/airmets/**").hasAuthority(Privilege.AIRMET_READ.getAuthority())
                .antMatchers(HttpMethod.POST, "/sigmets/**").hasAuthority(Privilege.SIGMET_EDIT.getAuthority())
                .antMatchers(HttpMethod.POST, "/tafs/**").hasAuthority(Privilege.TAF_EDIT.getAuthority())
                .antMatchers(HttpMethod.POST, "/airmets/**").hasAuthority(Privilege.AIRMET_EDIT.getAuthority())
                .antMatchers(HttpMethod.GET, "/testOnly/**").hasAuthority(Privilege.TEST.getAuthority())
                .anyRequest().authenticated()
                .and()
                .logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout/geoweb")).logoutSuccessUrl("/logout");
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
    @Primary
    public RemoteTokenServices tokenServices(GeoWebJwtConverter geowebJwtConverter,
            @Value("${security.oauth2.resource.tokenInfoUri}") String checkTokenUrl,
            @Value("${security.oauth2.resource.param:token}") String checkTokenParam,
            @Value("${security.oauth2.client.clientId}") String clientId,
            @Value("${security.oauth2.client.clientSecret}") String secret) {
        RemoteTokenServices services = new RemoteTokenServices();
        services.setCheckTokenEndpointUrl(checkTokenUrl);
        services.setTokenName(checkTokenParam);
        services.setClientId(clientId);
        services.setClientSecret(secret);
        services.setAccessTokenConverter(geowebJwtConverter);
        return services;
    }

    @Bean
    @Profile("oauth2-github")
    public PrincipalExtractor githubPrincipalExtractor() {
        return new GithubPrincipalExtractor();
    }

    @Bean
    @Profile("oauth2-github")
    public AuthoritiesExtractor githubAuthoritiesExtractor() {
        return new GithubAuthoritiesExtractor();
    }

    @Bean
    @Profile("oauth2-keycloak")
    public PrincipalExtractor keycloakPrincipalExtractor() {
        return new KeycloakPrincipalExtractor();
    }

    @Bean
    @Profile("oauth2-keycloak")
    public AuthoritiesExtractor keycloakAuthoritiesExtractor() {
        return new KeycloakAuthoritiesExtractor(objectMapper, mappingResource);
    }
}
