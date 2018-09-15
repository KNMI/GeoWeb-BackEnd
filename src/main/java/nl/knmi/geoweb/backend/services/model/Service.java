package nl.knmi.geoweb.backend.services.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Service {
	private String name;
	private String title;
	private String service;
	private ServiceType type;
	private ServiceGoal goal;
	private String[] roles;

	public Service(
			@JsonProperty("name") String name,
			@JsonProperty("title") String title,
			@JsonProperty("service") String service,
			@JsonProperty("type") ServiceType type,
			@JsonProperty("goal") ServiceGoal goal,
			@JsonProperty("roles") String[] roles
	) {
		this.name = name;
		this.title = title;
		this.service = service;
		this.type = type;
		this.goal = goal;
		this.roles = roles;
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public String getService() {
		return service;
	}

	public ServiceType getType() {
		return type;
	}

	public ServiceGoal getGoal() {
		return goal;
	}

	public String[] getRoles() {
		return roles;
	}
}
