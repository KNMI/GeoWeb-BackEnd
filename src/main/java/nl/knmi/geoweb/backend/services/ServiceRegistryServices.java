package nl.knmi.geoweb.backend.services;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.geoweb.backend.services.model.Service;
import nl.knmi.geoweb.backend.usermanagement.UserLogin;
import nl.knmi.geoweb.backend.usermanagement.UserStore;


@RestController
public class ServiceRegistryServices {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryServices.class);

	@Autowired
	ServiceRegistry reg;
	@RequestMapping(path="/getServices", method=RequestMethod.GET,	produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void getServices(HttpServletRequest req,  HttpServletResponse response){
		LOGGER.debug("/getServices");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			UserStore store=UserStore.getInstance();
			String user=UserLogin.getUserFromRequest(req);
			String[]roles=store.getUserRoles(user);
			if (roles==null) roles=new String[]{"USER", "ANON"};
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
		LOGGER.debug("/getOverlayServices");
		JSONResponse jsonResponse = new JSONResponse(req);
		try{
			UserStore store=UserStore.getInstance();
			String user=UserLogin.getUserFromRequest(req);
			String[]roles=store.getUserRoles(user);
			if (roles==null) roles=new String[]{"USER", "ANON"};
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
