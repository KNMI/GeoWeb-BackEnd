package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.knmi.adaguc.tools.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

	@Autowired
    @Qualifier("geoWebObjectMapper")
    private ObjectMapper objectMapper;

	private ServiceRegistry() {
	}

	private void getServices() throws IOException {
		String json = Tools.readResource("adminstore/config/services.json");
		services=objectMapper.readValue(json, new TypeReference<List<Service>>(){});
	}

	public List<Service> getWMSServicesForRole(String role) throws IOException {
		this.getServices(); //TODO cache temporarily?
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
	    this.getServices(); //TODO cache temporarily?
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
