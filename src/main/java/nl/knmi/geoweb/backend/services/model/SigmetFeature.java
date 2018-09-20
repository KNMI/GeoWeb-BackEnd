package nl.knmi.geoweb.backend.services.model;

import org.geojson.Feature;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SigmetFeature {
	private String firname;
	private Feature feature;

	public SigmetFeature(
			@JsonProperty("firname") String firname,
			@JsonProperty("feature") Feature feature
	) {
		this.firname = firname;
		this.feature = feature;
	}

	public String getFirname() {
		return firname;
	}

	public Feature getFeature() {
		return feature;
	}
}
