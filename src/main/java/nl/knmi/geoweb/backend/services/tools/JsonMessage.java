package nl.knmi.geoweb.backend.services.tools;

import lombok.Getter;

@Getter
public class JsonMessage {
	private String message;
	public JsonMessage(String message) {
		this.message=message;
	}

}
