package nl.knmi.geoweb.backend.services;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Service {
	public enum ServiceType{WMS,WCS,WPS,WFS};
	public enum ServiceGoal{LAYER,OVERLAY};
	
	private String name;
	private String title;
	private String service;
	private ServiceType type;
	private ServiceGoal goal;
	private String[] roles;
	
	public Service(String name, String title, String service, ServiceType type, ServiceGoal goal,String[] roles){
		this.name=name;
		this.title=title;
		this.service=service;
		this.type=type;
		this.goal=goal;
		this.roles=roles;
	}
	
	public Service(String name, String title, String service) {
		this(name, title, service, ServiceType.WMS, ServiceGoal.LAYER, new String[]{});
	}

	public Service(String name, String title, String service, ServiceGoal goal) {
		this(name, title, service, ServiceType.WMS, goal, new String[]{});
	}
	
	public Service(String name, String title, String service, String[]roles){
		this(name, title, service, ServiceType.WMS, ServiceGoal.LAYER, roles);
	}
	
	public Service(String name, String title, String service, ServiceGoal goal, String[]roles){
		this(name, title, service, ServiceType.WMS, goal, roles);
	}
	
	public Service(){
	}
}
