package nl.knmi.geoweb.backend.services.error;

public class TafDoesNotMatchPrevious extends GeoWebServiceException {
	private final String uuid;
	private final String previousUuid;

	public TafDoesNotMatchPrevious(String uuid, String previousUuid) {
		super("TAF with UUID " + uuid + " does not match previous TAF with UUID " + previousUuid);
		this.uuid = uuid;
		this.previousUuid = previousUuid;
	}

	public String getUuid() {
		return uuid;
	}

	public String getPreviousUuid() {
		return previousUuid;
	}
}
