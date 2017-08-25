package nl.knmi.geoweb.backend.services;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nl.knmi.geoweb.backend.usermanagement.UserLogin;
import nl.knmi.geoweb.backend.usermanagement.UserStore;

@RestController
public class ServiceRegistryServices {
	@RequestMapping("/getServices")
	public List<Service> getServices(HttpServletRequest req){
		UserStore store=UserStore.getInstance();
		String user=UserLogin.getUserFromRequest(req);
		String[]roles=store.getUserRoles(user);
		if (roles==null) roles=new String[]{"USER"};
		ServiceRegistry reg=ServiceRegistry.getInstance();
		List<Service> foundServices=new ArrayList<Service>();
		for (String role : roles) {
			List<Service>roleServices=reg.getWMSServicesForRole(role);
			foundServices.addAll(roleServices);
		}
		return foundServices;
	}

	@RequestMapping("/getOverlayServices")
	public List<Service> getOverlayServices(HttpServletRequest req){
		UserStore store=UserStore.getInstance();
		String user=UserLogin.getUserFromRequest(req);
		String[]roles=store.getUserRoles(user);
		if (roles==null) roles=new String[]{"USER"};
		ServiceRegistry reg=ServiceRegistry.getInstance();
		List<Service> foundServices=new ArrayList<Service>();
		for (String role : roles) {
			List<Service>roleServices=reg.getWMSOverlayServicesForRole(role);
			foundServices.addAll(roleServices);
		}
		return foundServices;
	}

}
