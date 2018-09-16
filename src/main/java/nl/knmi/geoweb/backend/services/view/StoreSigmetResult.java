package nl.knmi.geoweb.backend.services.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.knmi.geoweb.backend.product.sigmet.Sigmet;

public class StoreSigmetResult {
	private String uuid;
	@JsonIgnore
	private Sigmet.SigmetStatus status;

	public StoreSigmetResult(String uuid, Sigmet.SigmetStatus status) {
		this.uuid = uuid;
		this.status = status;
	}

	public String getUuid() {
		return uuid;
	}

	public Sigmet.SigmetStatus getStatus() {
		return status;
	}

	@JsonProperty
	public String getMessage() {
		return "sigmet " + uuid + " " + (status == Sigmet.SigmetStatus.concept ? "stored" : status.toString());
	}
}
