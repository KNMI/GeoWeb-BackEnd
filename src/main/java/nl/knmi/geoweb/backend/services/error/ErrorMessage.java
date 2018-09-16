package nl.knmi.geoweb.backend.services.error;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorMessage {
	@JsonProperty("error")
	private String message;

	public ErrorMessage(
			@JsonProperty("error") String message
	) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
