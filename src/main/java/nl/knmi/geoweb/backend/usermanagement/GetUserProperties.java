package nl.knmi.geoweb.backend.usermanagement;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.AbstractJsonpResponseBodyAdvice;

import lombok.Getter;


@RestController
public class GetUserProperties {
	@Getter
	public class UserProperties {
		private String name;
		public UserProperties(){
			name="NONAME";
		}
		public UserProperties(String nm) {
			this.name=nm;
		}
	}
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
