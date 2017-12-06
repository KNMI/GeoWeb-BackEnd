package nl.knmi.geoweb.backend.presets;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
public class AreaPreset {
	Float top;
	Float bottom;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	Float left;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	Float right;
	String crs;

	public AreaPreset(){}
	
	public AreaPreset(float top, float bottom, String crs) {
        this.top=top;
        this.bottom=bottom;
        this.crs=crs;
	}

	public AreaPreset(float top, float bottom, float left, float right, String crs) {
        this.top=top;
        this.bottom=bottom;
        this.left=left;
        this.right=right;
        this.crs=crs;
	}
}
