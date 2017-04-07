package nl.knmi.geoweb.backend.config;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nl.knmi.geoweb.backend.usermanagement.UserLogin;
import nl.knmi.geoweb.backend.usermanagement.UserStore;

@RestController
@RequestMapping("/config")
public class ConfigServices {
   private final static Config configStore=new Config("/tmp", "/tmp", "/tmp");
	

	@RequestMapping("getuserconfig")
	public String getConfig(HttpServletRequest req) {
		UserStore store=UserStore.getInstance();
		String user=UserLogin.getUserFromRequest(req);
		String[]roles=store.getUserRoles(user);
		if (roles==null) roles=new String[]{"USER"};

		Properties props=configStore.getConfig(user, roles);
		return props.toString();

	}

}
