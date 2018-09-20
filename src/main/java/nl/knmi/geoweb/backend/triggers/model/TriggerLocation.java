package nl.knmi.geoweb.backend.triggers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TriggerLocation {
	private double lat;
	private double lon;
	private String name;
	private String code;
	private double value;

	public TriggerLocation(
			@JsonProperty("lat") double lat,
			@JsonProperty("lon") double lon,
			@JsonProperty("name") String name,
			@JsonProperty("code") String code,
			@JsonProperty("value") double value
	) {
		this.lat = lat;
		this.lon = lon;
		this.name = name;
		this.code = code;
		this.value = value;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public double getValue() {
		return value;
	}
}
