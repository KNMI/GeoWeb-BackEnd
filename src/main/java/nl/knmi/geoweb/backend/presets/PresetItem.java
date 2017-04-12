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

	public static DisplayPreset createDisplayPreset(String type){
		return new DisplayPreset(DisplayPreset.DisplayType.valueOf(type));	
	}

	//	public static PresetItem createLayerPreset(List<String>layers, Map<String,String>[]dimensions) {
	//		return new LayerPreset(layers.toArray(new String[0]), dimensions);
	//	}

	public static LayerPreset createLayerPreset(String layer, Map<String,String>dimensions) {
		boolean active=true;
		float opacity=1;
		return new LayerPreset(layer, dimensions, active, opacity);
	}
}
