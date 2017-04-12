package nl.knmi.geoweb.backend.presets;

import java.util.Map;

import lombok.Getter;

@Getter
public class LayerPreset{
	private String service;
	private String name;
	private Map<String,String> dimensions;
	private boolean active;
	private boolean overlay;
	private float opacity;
	public LayerPreset(String service, String name, Map<String,String> dimensions) {
		this(service, name, dimensions, false, true, 1);
	}
	public LayerPreset(){}
	
	public LayerPreset(String service, String name, Map<String,String> dimensions, boolean overlay, boolean active, float opacity) {
		this.service=service;
		this.name=name;
		this.dimensions=dimensions;
		this.overlay=overlay;
		this.active=active;
		this.opacity=opacity;
	}
}
