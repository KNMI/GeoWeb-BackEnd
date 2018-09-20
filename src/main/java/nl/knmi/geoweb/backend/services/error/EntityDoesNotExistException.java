package nl.knmi.geoweb.backend.services.error;

public class EntityDoesNotExistException extends GeoWebServiceException {
	private final String type;
	private final String uuid;

	public EntityDoesNotExistException(String type, String uuid) {
		super(type + " with uuid " + uuid + " does not exist");
		this.type = type;
		this.uuid = uuid;
	}

	public String getType() {
		return type;
	}

	public String getUuid() {
		return uuid;
	}
}
