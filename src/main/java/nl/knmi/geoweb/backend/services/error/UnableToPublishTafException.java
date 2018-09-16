package nl.knmi.geoweb.backend.services.error;

public class UnableToPublishTafException extends GeoWebServiceException {
	private final String location;
	private final String reason;

	public UnableToPublishTafException(String location, String reason) {
		super("TAF for " + location + " failed to get published: " + reason);
		this.location = location;
		this.reason = reason;
	}

	public String getLocation() {
		return location;
	}

	public String getReason() {
		return reason;
	}
}
