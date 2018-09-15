package nl.knmi.geoweb.backend.usermanagement;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.AbstractJsonpResponseBodyAdvice;

import nl.knmi.geoweb.backend.usermanagement.model.UserProperties;


@RestController
public class GetUserProperties {
    @ControllerAdvice
    static class JsonpAdvice extends AbstractJsonpResponseBodyAdvice {
        public JsonpAdvice() {
            super("callback");
        }
    }
    
	@RequestMapping("/getUserProperties")
	public UserProperties userProperties(HttpServletRequest req) {
		String nm=UserLogin.getUserFromRequest(req);
		return new UserProperties(nm);  
	}
}
