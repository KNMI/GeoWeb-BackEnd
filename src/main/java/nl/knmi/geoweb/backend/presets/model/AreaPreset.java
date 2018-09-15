package nl.knmi.geoweb.backend.presets.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AreaPreset {
	private Float top;
	private Float bottom;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Float left;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Float right;
	private String crs;

	public AreaPreset(
			@JsonProperty("top") float top,
			@JsonProperty("bottom") float bottom,
			@JsonProperty("left") Float left,
			@JsonProperty("right") Float right,
			@JsonProperty("crs") String crs
	) {
		this.top = top;
		this.bottom = bottom;
		this.left = left;
		this.right = right;
		this.crs = crs;
	}

	public Float getTop() {
		return top;
	}

	public Float getBottom() {
		return bottom;
	}

	public Float getLeft() {
		return left;
	}

	public Float getRight() {
		return right;
	}

	public String getCrs() {
		return crs;
	}
}
