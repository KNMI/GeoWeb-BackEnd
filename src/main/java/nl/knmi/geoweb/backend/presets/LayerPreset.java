package nl.knmi.geoweb.backend.presets;

import java.util.Map;

import lombok.Getter;

@Getter
public class LayerPreset{
	private String layer;
	private Map<String,String> dimensions;
	private boolean active;
	private float opacity;
	public LayerPreset(String layer, Map<String,String> dimensions) {
		this(layer, dimensions, true, 1);
	}
	public LayerPreset(){}
	
	public LayerPreset(String layer, Map<String,String> dimensions, boolean active, float opacity) {
		this.layer=layer;
		this.dimensions=dimensions;
		this.active=active;
		this.opacity=opacity;
	}
}
