package nl.knmi.geoweb.backend.services.error;

public class PreviousTafNotYetPublishedException extends GeoWebServiceException {
	private final String previousUuid;

	public PreviousTafNotYetPublishedException(String previousUuid) {
		super("previous TAF " + previousUuid + " not published");
		this.previousUuid = previousUuid;
	}

	public String getPreviousUuid() {
		return previousUuid;
	}
}
