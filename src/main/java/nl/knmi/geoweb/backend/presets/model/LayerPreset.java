package nl.knmi.geoweb.backend.presets.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LayerPreset {
	private String service;
	private String name;
	private Map<String, String> dimensions;
	private boolean overlay;
	private boolean active;
	private float opacity;

	public LayerPreset(
			@JsonProperty("service") String service,
			@JsonProperty("name") String name,
			@JsonProperty("dimensions") Map<String, String> dimensions,
			@JsonProperty("overlay") boolean overlay,
			@JsonProperty("active") boolean active,
			@JsonProperty("opacity") float opacity
	) {
		this.service = service;
		this.name = name;
		this.dimensions = dimensions;
		this.overlay = overlay;
		this.active = active;
		this.opacity = opacity;
	}

	public String getService() {
		return service;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getDimensions() {
		return dimensions;
	}

	public boolean isOverlay() {
		return overlay;
	}

	public boolean isActive() {
		return active;
	}

	public float getOpacity() {
		return opacity;
	}
}
