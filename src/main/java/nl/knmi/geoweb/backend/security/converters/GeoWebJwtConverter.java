package nl.knmi.geoweb.backend.security.converters;

import java.util.Collection;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterConfigurer;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GeoWebJwtConverter extends DefaultAccessTokenConverter implements JwtAccessTokenConverterConfigurer {

    @Autowired
    AuthoritiesExtractor authoritiesExtractor;

    @Autowired
    PrincipalExtractor principalExtractor;

    @Override
    public void configure(JwtAccessTokenConverter converter) {
        converter.setAccessTokenConverter(this);
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
        OAuth2Authentication authentication = super.extractAuthentication(map);
        log.debug("Authentication keyset: " + map.keySet().toString());

        @SuppressWarnings("unchecked")
        Object principal = principalExtractor.extractPrincipal((Map<String, Object>) map);
        @SuppressWarnings("unchecked")
        Collection<? extends GrantedAuthority> authorities = authoritiesExtractor.extractAuthorities((Map<String, Object>) map);
        Authentication user = new UsernamePasswordAuthenticationToken(principal, authentication.getCredentials(), authorities);

        return new OAuth2Authentication(authentication.getOAuth2Request(), user);
    }
}
