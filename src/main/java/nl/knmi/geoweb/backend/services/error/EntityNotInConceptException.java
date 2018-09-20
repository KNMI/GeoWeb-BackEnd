package nl.knmi.geoweb.backend.services.error;

public class EntityNotInConceptException extends GeoWebServiceException {
	private final String type;
	private final String uuid;

	public EntityNotInConceptException(String type, String uuid) {
		super(type + " with uuid " + uuid + " is not in concept. Cannot delete.");
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
