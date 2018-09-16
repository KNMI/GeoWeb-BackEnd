package nl.knmi.geoweb.backend.services.error;

import nl.knmi.geoweb.backend.product.taf.Taf;

public class UnsupportedTafTypeException extends GeoWebServiceException {
	private final Taf.TAFReportType tafType;

	public UnsupportedTafTypeException(Taf.TAFReportType tafType) {
		super("TAF Reports of type " + tafType.toString() + " are not yet supported");
		this.tafType = tafType;
	}

	public Taf.TAFReportType getTafType() {
		return tafType;
	}
}
