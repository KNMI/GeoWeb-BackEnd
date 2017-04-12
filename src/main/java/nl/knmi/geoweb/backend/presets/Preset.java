package nl.knmi.geoweb.backend.presets;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Preset {
	private List<List<LayerPreset>> layers;
	private DisplayPreset display;
	private AreaPreset area;
	private String name;
	private String[] keywords;
	
	public Preset(){
	}
	
	public Preset(String name, String[] keywords) {
		this.layers=new ArrayList<List<LayerPreset>>();
		this.display=null;
		this.area=null;
		this.name=name;
		this.keywords=keywords;
	}

	public Preset(String name, String[] keywords, List<List<LayerPreset>> layers, DisplayPreset display, AreaPreset area){
		this.layers=layers;
		this.display=display;
		this.area=area;
		this.name=name;
		this.keywords=keywords;
	}
	public void addLayers(List<List<LayerPreset>> layers) {
		this.layers=layers;
	}
	public void addDisplay(DisplayPreset display) {
		this.display=display;
	}
	public void addArea(AreaPreset area) {
		this.area=area;
	}
	
}
