package nl.knmi.geoweb.backend.presets.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DisplayPreset {
	private String type;
	private int npanels;

	public DisplayPreset(
			@JsonProperty("type") String type,
			@JsonProperty("npanels") int npanels
	) {
		this.type = type;
		this.npanels = npanels;
	}

	public String getType() {
		return type;
	}

	public int getNpanels() {
		return npanels;
	}
}