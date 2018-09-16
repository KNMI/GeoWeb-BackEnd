package nl.knmi.geoweb.backend.services.error;

public class EmptySigmetException extends GeoWebServiceException {
	public EmptySigmetException() {
		super("Empty SIGMETs cannot be stored.");
	}
}
