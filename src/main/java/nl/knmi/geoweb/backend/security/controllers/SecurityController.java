package nl.knmi.geoweb.backend.security.controllers;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.security.services.SecurityServices;

@Slf4j
@Controller
@RequestMapping("/")
public class SecurityController {

    @Autowired
    SecurityServices securityServices;

    @Value("${client.logoutUri}")
    private String oauth2LogoutUrl;

    @Value("${client.frontendURL}")
    private String frontendURL;

    @RequestMapping(method = RequestMethod.GET, path = "/")
    public String index() {
        log.info("Request received @[/]");
        return "redirect:" + frontendURL;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/login/geoweb")
    public String getGeoWebLogin() {
        log.info("Request received @[/login/geoweb]");
        return "login";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/login/options", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> getLoginOptions() {
        log.info("Request received @[/login/options]");
        return securityServices.getLoginOptions();
    }

    @Value("${security.oauth2.client.clientId}")
    private String clientId;

    
    @GetMapping(path = "/logout")
    public String logOut(@RequestHeader(value = "Referer", required = false) Optional<String> referer,
            HttpServletRequest servletRequest) {
        log.info("Request received @[/logout]");
        return "redirect:" + oauth2LogoutUrl + "?client_id=" + clientId + "&logout_uri=" + securityServices.getLogoutRedirect(referer, servletRequest) + "/logout/geoweb";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/logout/options", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> getLogoutOptions() {
        log.info("Request received @[/logout/options]");
        return securityServices.getLogoutOptions();
    }

    @RequestMapping(method = RequestMethod.GET, path = "/status", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getStatus() {
        log.info("Request received @[/status]");
        return securityServices.getStatus();
    }

    @RequestMapping(method = RequestMethod.GET, path = "/testOnly")
    public String test() {
        log.info("Request received @[/testOnly]");
        return "test.html";
    }
}
