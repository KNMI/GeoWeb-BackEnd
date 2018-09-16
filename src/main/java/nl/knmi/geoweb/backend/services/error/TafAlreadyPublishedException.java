package nl.knmi.geoweb.backend.services.error;

public class TafAlreadyPublishedException extends GeoWebServiceException {
	private final String uuid;

	public TafAlreadyPublishedException(String uuid) {
		super("TAF with uuid " + uuid + " already published");
		this.uuid = uuid;
	}

	public String getUuid() {
		return uuid;
	}
}
