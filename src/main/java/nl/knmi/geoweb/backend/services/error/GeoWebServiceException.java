package nl.knmi.geoweb.backend.services.error;

abstract public class GeoWebServiceException extends Exception {
	protected GeoWebServiceException(String s) {
		super(s);
	}
}
