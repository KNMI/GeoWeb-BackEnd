package nl.knmi.geoweb.backend.usermanagement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;
import nl.knmi.geoweb.backend.services.tools.JsonMessage;
import nl.knmi.geoweb.backend.services.tools.StatusCode;
import nl.knmi.geoweb.backend.usermanagement.UserStore.GeoWebUser;

@RestController
public class UserLogin {

	public static String getUserFromRequest(HttpServletRequest req) {
		Cookie[] cookies=req.getCookies();
		if (cookies!=null) {
			for (Cookie cookie: cookies){
				if (cookie.getName().equals("GEOWEB_AUTH")) {
					return cookie.getValue();
				}
			}
		}
		return "anonymous";
	}

	UserStore userstore=UserStore.getInstance();

	@RequestMapping("/getuser")
	public GeoWebUser getGeoWebUser (HttpServletRequest req) {
		String user=getUserFromRequest(req);
		if (!user.equals("anonymous")) {
			GeoWebUser geowebUser=userstore.getUser(user);
			return geowebUser;
		}
		String[] roles={"USER"};
		return userstore.new GeoWebUser("guest","XXX", roles);
	}	

	private Cookie getCookie(GeoWebUser user) {
		Cookie cookie=new Cookie("GEOWEB_AUTH",user.getUsername());
		cookie.setSecure(false);
		cookie.setMaxAge(24*60*60);
		cookie.setPath("/");
		return cookie;
	}


	@RequestMapping("/logout")
	public JsonMessage userLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Cookie []cookies=request.getCookies();
		if (cookies!=null) {
			for(int i = 0; i< cookies.length ; ++i){
				if(cookies[i].getName().equals("GEOWEB_AUTH")){
					cookies[i].setMaxAge(0);
					cookies[i].setPath("/");
					response.addCookie(cookies[i]);
					return new JsonMessage("logout successfull");
				}
			} 
		}
		return new JsonMessage("not logged in");
	}

	@RequestMapping("/login")
	public GeoWebUser userLogin(@RequestParam(value="username", required=true)String name,
			@RequestParam(value="password", required=true)String password,
			HttpServletResponse response) throws IOException {
		GeoWebUser user=userstore.checkUser(name, password);
		if (user==null) {
			List<String>roles=new ArrayList<String>();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);//, "User/password incorrect");
			response.sendError(401, "User/password incorrect");
			return userstore.new GeoWebUser(null, null, roles);

		}
		Cookie cookie=getCookie(user);
		response.addCookie(cookie);
		return user ;
	}
}
