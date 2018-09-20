package nl.knmi.geoweb.backend.services.error;

public class SigmetFeatureFirNotFoundException extends GeoWebServiceException {
	private final String firName;

	public SigmetFeatureFirNotFoundException(String firName) {
		super("FIR with name " + firName + " not found");
		this.firName = firName;
	}

	public String getFirName() {
		return firName;
	}
}
