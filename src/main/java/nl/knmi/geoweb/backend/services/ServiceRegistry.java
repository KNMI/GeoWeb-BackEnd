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

//		services=new ArrayList<Service>();
//		services.add(new Service("RADAR", "RADAR","http://birdexp07.knmi.nl/cgi-bin/geoweb/adaguc.RADAR.cgi?", new String[]{"MET"}));
//		services.add(new Service("SAT", "SAT","http://birdexp07.knmi.nl/cgi-bin/geoweb/adaguc.SAT.cgi?", new String[]{"MET"}));
//		services.add(new Service("HARM_N25", "HARM_N25","http://birdexp07.knmi.nl/cgi-bin/geoweb/adaguc.HARM_N25.cgi?", new String[]{"MET"}));
//		services.add(new Service("OBS", "OBS","http://birdexp07.knmi.nl/cgi-bin/geoweb/adaguc.OBS.cgi?", new String[]{"MET"}));
////		services.add(new Service("LGT", "LGT","http://bvmlab-218-41.knmi.nl/cgi-bin/WWWRADAR3.cgi?", new String[]{"MET"}));
//		services.add(new Service("LGT", "LGT","http://birdexp07.knmi.nl/cgi-bin/geoweb/adaguc.LGT.cgi?", new String[]{"MET"}));
//		services.add(new Service("HARM_N25_EXT", "HARM_N25_EXT","http://geoservices.knmi.nl/cgi-bin/HARM_N25.cgi?", new String[]{"USER"}));
//		services.add(new Service("RADAR_EXT", "RADAR_EXT","http://geoservices.knmi.nl/cgi-bin/RADNL_OPER_R___25PCPRR_L3.cgi?", new String[]{"USER"}));
//		services.add(new Service("OVL", "OVL","http://birdexp07.knmi.nl/cgi-bin/geoweb/adaguc.OVL.cgi?", Service.ServiceGoal.OVERLAY, new String[]{"USER","MET"}));
//		services.add(new Service("NOWCASTMIX", "NOWCASTMIX","http://birdexp07.knmi.nl/cgi-bin/geoweb/adaguc.NOWCASTMIX.cgi?", new String[]{"MET"}));
	}
	
	private void getServices() {
		ObjectMapper mapper=new ObjectMapper();
		String json="";
		try {
			json = adminStore.read("config", "services.json");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			services=mapper.readValue(json, new TypeReference<List<Service>>(){});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<Service> getWMSServicesForRole(String role) {
		if (services==null) this.getServices();
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
	
	public List<Service> getWMSOverlayServicesForRole(String role) {
		if (services==null) this.getServices();
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
