package nl.knmi.geoweb.backend.presets.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Preset {
	private String name;
	private String[] keywords;
	private List<List<LayerPreset>> layers;
	private DisplayPreset display;
	private AreaPreset area;

	public Preset(
			@JsonProperty("name") String name,
			@JsonProperty("keywords") String[] keywords,
			@JsonProperty("layers") List<List<LayerPreset>> layers,
			@JsonProperty("display") DisplayPreset display,
			@JsonProperty("area") AreaPreset area
	) {
		this.name = name;
		this.keywords = keywords;
		this.layers = layers;
		this.display = display;
		this.area = area;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getKeywords() {
		return keywords;
	}

	public List<List<LayerPreset>> getLayers() {
		return layers;
	}

	public DisplayPreset getDisplay() {
		return display;
	}

	public AreaPreset getArea() {
		return area;
	}
}
