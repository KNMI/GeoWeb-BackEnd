package nl.knmi.geoweb.backend.presets;

import lombok.Getter;

@Getter
public class AreaPreset {
	float top;
	float bottom;
	String crs;

	public AreaPreset(){}
	
	public AreaPreset(float top, float bottom, String crs) {
		//super(PresetType.AREA);
        this.top=top;
        this.bottom=bottom;
        this.crs=crs;
	}
}
