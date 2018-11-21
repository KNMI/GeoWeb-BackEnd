package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.geoweb.backend.admin.AdminStore;
import nl.knmi.geoweb.backend.services.Service.ServiceGoal;
import nl.knmi.geoweb.backend.services.Service.ServiceType;

@Component
public class ServiceRegistry {
	List<Service>services;

	@Autowired
	AdminStore adminStore;
	
	private ServiceRegistry() {
	}
	
	private void getServices() throws IOException {
		ObjectMapper mapper=new ObjectMapper();
		String json="";
		json = adminStore.read("config", "services.json");
		services=mapper.readValue(json, new TypeReference<List<Service>>(){});
	}

	public List<Service> getWMSServicesForRole(String role) throws IOException {
		/* if (services==null) */ this.getServices(); //TODO chache temporarily?
		List<Service>foundServices=new ArrayList<Service>();
		for (Service srv: services) {
			if (srv.getType().equals(ServiceType.WMS) && srv.getGoal().equals(ServiceGoal.LAYER)){
				for (String servRole: srv.getRoles()) {
					if (servRole.equals(role)) {
						foundServices.add(srv);
						break;
					}
				}
			}
		}
		return foundServices;
	}
	
	public List<Service> getWMSOverlayServicesForRole(String role) throws IOException {
		/* if (services==null) */ this.getServices(); //TODO chache temporarily?
		List<Service>foundServices=new ArrayList<Service>();
		for (Service srv: services) {
			if (srv.getType().equals(ServiceType.WMS)&&srv.getGoal().equals(ServiceGoal.OVERLAY)){
				for (String servRole: srv.getRoles()) {
					if (servRole.equals(role)) {
						foundServices.add(srv);
						break;
					}
				}
			}
		}
		return foundServices;
	}
}
