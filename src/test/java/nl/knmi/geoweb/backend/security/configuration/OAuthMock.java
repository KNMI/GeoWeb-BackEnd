
package nl.knmi.geoweb.backend.security.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class OAuthMock {

    @Value("${security.oauth2.client.clientId}")
    private String clientId;

    @Autowired
    AuthorizationServerTokenServices tokenservice;

    public RequestPostProcessor addBearerToken(final String username, String... authorities) {
        return mockRequest -> {
            // Create OAuth2 token
            OAuth2Request oauth2Request = new OAuth2Request(null, clientId, null, true, null,
                    null, null, null, null);
            Authentication userauth = new TestingAuthenticationToken(username, null, authorities);
            OAuth2Authentication oauth2auth = new OAuth2Authentication(oauth2Request, userauth);
            OAuth2AccessToken token = tokenservice.createAccessToken(oauth2auth);

            // Set Authorization header to use Bearer
            mockRequest.addHeader("Authorization", "Bearer " + token.getValue());
            return mockRequest;
        };
    }
}
