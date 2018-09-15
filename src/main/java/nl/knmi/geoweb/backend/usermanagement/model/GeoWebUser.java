package nl.knmi.geoweb.backend.usermanagement.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GeoWebUser {
	private String username;
	private String password;
	private List<RoleType> roles = new ArrayList<>();

	private GeoWebUser(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public GeoWebUser(String username, String password, RoleType[] roles) {
		this(username, password);
		this.roles.addAll(Arrays.asList(roles));
	}

	public GeoWebUser(String username, String password, List<RoleType> roles) {
		this(username, password);
		this.roles.addAll(roles);
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public List<RoleType> getRoles() {
		return Collections.unmodifiableList(roles);
	}

	public List<String> getRoleNames() {
		List<String> roles = new ArrayList<>();
		for (RoleType r : this.roles) {
			roles.add(r.toString());
		}
		return roles;
	}
}
