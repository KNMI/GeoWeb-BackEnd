package nl.knmi.geoweb.backend.services.error;

import nl.knmi.geoweb.backend.product.sigmet.Sigmet;

public class UnsupportedSigmetStatusException extends GeoWebServiceException {
	private final Sigmet.SigmetStatus status;

	public UnsupportedSigmetStatusException(Sigmet.SigmetStatus status) {
		super("SIGMETs with status " + status.toString() + " are not yet supported");
		this.status = status;
	}

	public Sigmet.SigmetStatus getStatus() {
		return status;
	}
}
