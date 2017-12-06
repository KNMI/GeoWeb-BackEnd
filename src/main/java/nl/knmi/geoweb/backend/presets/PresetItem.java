package nl.knmi.geoweb.backend.presets;


import java.util.Map;

public class PresetItem {
	public enum PresetType {LAYER, AREA, DISPLAY};

	PresetType presettype;
	public PresetItem(PresetType presettype) {
		this.presettype=presettype;
	};

	public static AreaPreset createAreaPreset(float top, float bottom, String crs) {
		return new AreaPreset(top, bottom, crs);
	}
	
	public static AreaPreset createAreaPreset(float top, float bottom, float left, float right, String crs) {
		return new AreaPreset(top, bottom, left, right, crs);
	}

	public static DisplayPreset createDisplayPreset(String type, int npanels){
		return new DisplayPreset(type, npanels);
	}

	//	public static PresetItem createLayerPreset(List<String>layers, Map<String,String>[]dimensions) {
	//		return new LayerPreset(layers.toArray(new String[0]), dimensions);
	//	}

	public static LayerPreset createLayerPreset(String service, String name, Map<String,String>dimensions) {
		boolean active=true;
		boolean overlay=false;
		float opacity=1;
		return new LayerPreset(service, name, dimensions, overlay, active, opacity);
	}
}
