package nl.knmi.geoweb.backend.security.extractors.keycloak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.security.models.RoleToPrivilegesMapper;

@Slf4j
public class KeycloakAuthoritiesExtractor implements AuthoritiesExtractor {

    private String KEY_APP_ID = "client_id";
    private String KEY_ACCESS = "resource_access";
    private String KEY_ROLES = "roles";

    private List<RoleToPrivilegesMapper> mappingsHolder;

    public KeycloakAuthoritiesExtractor(ObjectMapper objectMapper, Resource mappingResource) {
        try {
            RoleToPrivilegesMapper[] mappings = objectMapper.readValue(mappingResource.getFile(), RoleToPrivilegesMapper[].class);
            mappingsHolder = Arrays.asList(mappings);
        } catch (IOException exception) {
            log.error("Could not obtain roles to privilege mappings from resource. " + exception.getMessage());
            mappingsHolder = new ArrayList<RoleToPrivilegesMapper>();
        }
    }

    @Override
    public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
        List<String> roles = extractRoles(map);

        if (mappingsHolder == null || roles == null) {
            return new ArrayList<GrantedAuthority>();
        }

        SortedSet<String> authoritiesSet = new TreeSet<String>();
        roles.forEach(role -> {
            Optional<RoleToPrivilegesMapper> mapper = mappingsHolder.stream()
                    .filter(potentialMapper -> potentialMapper.getRoleName().equals(role))
                    .findFirst();
            if (mapper.isPresent()) {
                mapper.get().getPrivilegesAsList().forEach(privilege -> {
                    authoritiesSet.add(privilege.getAuthority());
                });
            }
        });

        String authoritiesSeparatedString = authoritiesSet.stream().collect(Collectors.joining(","));
        return AuthorityUtils.commaSeparatedStringToAuthorityList(authoritiesSeparatedString);
    }

    private List<String> extractRoles(Map<String, Object> map) {
        String clientId = null;
        if (map.containsKey(KEY_APP_ID) && map.get(KEY_APP_ID) != null) {
            clientId = map.get(KEY_APP_ID).toString();
        }
        if (clientId == null || !map.containsKey(KEY_ACCESS)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> resourceAccessMap = (Map<String, Object>) map.get(KEY_ACCESS);
        if (resourceAccessMap == null || !resourceAccessMap.containsKey(clientId)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rolesMap = (Map<String, Object>) resourceAccessMap.get(clientId);
        if (rolesMap == null || !rolesMap.containsKey(KEY_ROLES)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) rolesMap.get(KEY_ROLES);
        log.info("Roles: " + roles);
        return roles;
    }
}
