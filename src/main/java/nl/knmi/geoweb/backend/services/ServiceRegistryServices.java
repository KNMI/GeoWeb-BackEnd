package nl.knmi.geoweb.backend.services;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.geoweb.backend.usermanagement.UserLogin;


@RestController
public class ServiceRegistryServices {
	@Autowired
	ServiceRegistry reg;
	@RequestMapping(path="/getServices", method=RequestMethod.GET,	produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void getServices(HttpServletRequest req,  HttpServletResponse response){
		Debug.println("/getServices");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			String[]roles=UserLogin.getUserPrivileges();
			List<Service> foundServices=new ArrayList<Service>();
			for (String role : roles) {
				List<Service>roleServices=reg.getWMSServicesForRole(role);
				foundServices.addAll(roleServices);
			}
			jsonResponse.setMessage(new ObjectMapper().writeValueAsString(foundServices));
		} catch (Exception e){
			jsonResponse.setException("getServices failed " + e.getMessage(),e);		}
		try {
			jsonResponse.print(response);
		} catch (Exception e1) {
		}
	}

	@RequestMapping(path="/getOverlayServices", method=RequestMethod.GET,	produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void getOverlayServices(HttpServletRequest req,   HttpServletResponse response){
		Debug.println("/getOverlayServices");
		JSONResponse jsonResponse = new JSONResponse(req);
		try{
			String[]roles=UserLogin.getUserPrivileges();
			List<Service> foundServices=new ArrayList<Service>();
			for (String role : roles) {
				List<Service>roleServices=reg.getWMSOverlayServicesForRole(role);
				foundServices.addAll(roleServices);
			}
			jsonResponse.setMessage(new ObjectMapper().writeValueAsString(foundServices));
		} catch (Exception e) {
			jsonResponse.setException("getServices failed " + e.getMessage(),e);			
		}
		try {
			jsonResponse.print(response);
		} catch (Exception e1) {
		}
	}

}
