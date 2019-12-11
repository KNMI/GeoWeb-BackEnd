package nl.knmi.geoweb.backend.security.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.usermanagement.UserLogin;

@Slf4j
@Service
public class SecurityServices {
    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Value("${client.name}")
    private String clientName;

    private String loginUri = "/login";
    private String logoutUri = "/logout/geoweb";

    /**
     * Provide the options for logging in
     *
     * @return the available options as a {@link Map<String, String>}
     */
    public Map<String, String> getLoginOptions() {
        Map<String, String> oauth2AuthenticationUrls = new HashMap<>();

        oauth2AuthenticationUrls.put(clientName, loginUri);
        oauth2AuthenticationUrls.put("type", "oauth2");

        return oauth2AuthenticationUrls;
    }

    /**
     * Provide the options for logging out
     *
     * @return the available options as a {@link Map<String, String>}
     */
    public Map<String, String> getLogoutOptions() {
        Map<String, String> oauth2AuthenticationUrls = new HashMap<>();

        oauth2AuthenticationUrls.put(clientName, logoutUri);

        return oauth2AuthenticationUrls;
    }

    /**
     * Provide the redirect to be used after logging out
     *
     * @return the url as a {@link String}
     */
    public String getLogoutRedirect(Optional<String> referer, HttpServletRequest servletRequest) {
        UriComponents rootComponents = ServletUriComponentsBuilder.fromRequest(servletRequest)
                .replacePath(null)
                .replaceQuery(null)
                .build();
        if (!referer.isPresent()) {
            log.info("root: " + rootComponents.toUriString());
            return rootComponents.toUriString();
        }
        log.info("ref: " + UriComponentsBuilder.fromUriString(referer.get())
                .scheme(rootComponents.getScheme())
                .build()
                .toUriString());
        return UriComponentsBuilder.fromUriString(referer.get())
                .scheme(rootComponents.getScheme())
                .build()
                .toUriString();
    }

    /**
     * Provide the status info
     *
     * @return the status info as a {@link Map<String, Object>}
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        if(SecurityServices.isProfileActive(activeProfiles, "generic")) {
            ArrayList<String> privileges = new ArrayList<String>(Arrays.asList("AIRMET_edit", "AIRMET_read", "AIRMET_settings_read", 
                                        "SIGMET_edit", "SIGMET_read", "SIGMET_settings_read", "TAF_edit", "TAF_read", "TAF_settings_read"));
            status.put("userName", UserLogin.getUserName());
            status.put("privileges", privileges);
            status.put("isLoggedIn", true);
        }else{
            if (SecurityContextHolder.getContext().getAuthentication() == null
                    || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                    || (SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
                status.put("userName", null);
                status.put("privileges", new ArrayList<>());
                status.put("isLoggedIn", false);
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                status.put("userName", auth.getName());
                status.put("privileges", auth.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(Collectors.toList()));
                status.put("isLoggedIn", true);
            }
        }
        return status;
    }

    public static boolean isProfileActive(String profiles, String requestedProfile){
        for (String profileName : profiles.split(",")) {
            if(profileName.equals(requestedProfile)){
                return true;
            }
        }
        return false;
    }
}
