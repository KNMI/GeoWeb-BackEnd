package nl.knmi.geoweb.backend.services.error;

import java.io.IOException;

public class GeoJsonConversionException extends IOException {
	public GeoJsonConversionException(String message) {
		super(message);
	}

	public GeoJsonConversionException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
