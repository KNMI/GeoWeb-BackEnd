package nl.knmi.geoweb.backend.security.extractors.keycloak;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;

@Slf4j
public class KeycloakPrincipalExtractor implements PrincipalExtractor {

    @Override
    public Object extractPrincipal(Map<String, Object> map) {
        log.info("user_name: " + map.get("user_name")); // also available: name, given_name, family_name, email
        return map.get("user_name");
    }
}
