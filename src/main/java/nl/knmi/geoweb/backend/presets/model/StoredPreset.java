package nl.knmi.geoweb.backend.presets.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StoredPreset {
	private Preset preset;
	private String user;
	private List<String> roles;

	@JsonCreator
	private StoredPreset(
			@JsonProperty("user") String user,
			@JsonProperty("roles") List<String> roles,
			@JsonProperty("preset") Preset preset
	) {
		this.user = user;
		this.roles = roles;
		this.preset = preset;
	}

	public StoredPreset(Preset preset) {
		this.user = null;
		this.roles = null;
		this.preset = preset;
	}

	public StoredPreset(String user, Preset preset) {
		this.user = user;
		this.roles = null;
		this.preset = preset;
	}

	public StoredPreset(List<String> roles, Preset preset) {
		this.user = null;
		this.roles = roles;
		this.preset = preset;
	}

	public Preset getPreset() {
		return preset;
	}

	public String getUser() {
		return user;
	}

	public List<String> getRoles() {
		return roles;
	}

	@JsonIgnore
	public boolean isSystem() {
		return user == null && roles == null;
	}
}
