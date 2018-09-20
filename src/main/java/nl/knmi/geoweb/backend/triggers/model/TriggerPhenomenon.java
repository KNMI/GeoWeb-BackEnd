package nl.knmi.geoweb.backend.triggers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TriggerPhenomenon {
	private String parameter;
	private String operator;
	private double threshold;
	private String units;
	private String source;

	public TriggerPhenomenon(
			@JsonProperty("parameter") String parameter,
			@JsonProperty("operator") String operator,
			@JsonProperty("threshold") double threshold,
			@JsonProperty("units") String units,
			@JsonProperty("source") String source
	) {
		this.parameter = parameter;
		this.operator = operator;
		this.threshold = threshold;
		this.units = units;
		this.source = source;
	}

	public String getParameter() {
		return parameter;
	}

	public String getOperator() {
		return operator;
	}

	public double getThreshold() {
		return threshold;
	}

	public String getUnits() {
		return units;
	}

	public String getSource() {
		return source;
	}
}
