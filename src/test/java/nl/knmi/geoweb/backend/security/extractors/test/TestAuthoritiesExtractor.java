package nl.knmi.geoweb.backend.security.extractors.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import nl.knmi.geoweb.backend.security.models.Privilege;

public class TestAuthoritiesExtractor implements AuthoritiesExtractor {

    @Override
    public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
        List<String> testAuthorities = new ArrayList<String>();
        testAuthorities.add(Privilege.AIRMET_EDIT.getAuthority());
        return AuthorityUtils.commaSeparatedStringToAuthorityList(testAuthorities.stream().collect(Collectors.joining(",")));
    }
}
