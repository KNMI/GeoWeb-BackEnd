package nl.knmi.geoweb.backend.services.view;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.knmi.geoweb.backend.product.taf.Taf;

public class StoreTafResult {
	private boolean succeeded;
	private String message;
	@JsonProperty("tafjson")
	private Taf taf;

	public StoreTafResult(boolean succeeded, String message, Taf taf) {
		this.succeeded = succeeded;
		this.message = message;
		this.taf = taf;
	}

	public boolean isSucceeded() {
		return succeeded;
	}

	public String getMessage() {
		return message;
	}

	public Taf getTaf() {
		return taf;
	}

	@JsonProperty
	public String getTac() {
		return taf.toTAC();
	}

	@JsonProperty
	public String getUuid() {
		return taf.metadata.getUuid();
	}
}
