package nl.knmi.geoweb.backend.security.extractors.cognito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.security.models.RoleToPrivilegesMapper;

@Slf4j
public class CognitoAuthoritiesExtractor implements AuthoritiesExtractor {

    private List<RoleToPrivilegesMapper> mappingsHolder;

    public CognitoAuthoritiesExtractor(ObjectMapper objectMapper, ClassPathResource mappingResource) {
        /* Read the role to privileges mapping into memory */
        try {
            /** 
             * NOTE: mappingResource.getFile() will only work in dev mode!  
             * Use Tools.readResource, this will work in both production and dev
             */
            String mappingResourceAsString = Tools.readResource(mappingResource.getPath());
            RoleToPrivilegesMapper[] mappings = objectMapper.readValue(mappingResourceAsString,
                    RoleToPrivilegesMapper[].class);
            mappingsHolder = Arrays.asList(mappings);
        } catch (IOException exception) {
            log.error("Could not obtain roles to privilege mappings from resource. " + exception.getMessage());
            mappingsHolder = new ArrayList<RoleToPrivilegesMapper>();
        }
    }

    @Override
    public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
        try {
            ObjectMapper om = new ObjectMapper();
            log.debug("Authority map: " + om.writeValueAsString(map));
        } catch (JsonProcessingException e) {
        }
        
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
        String roleList = "production_full";
        List<String> roles = Arrays.asList(roleList.split(","));
        log.debug("Roles: " + roles);
        return roles;
    }
}
