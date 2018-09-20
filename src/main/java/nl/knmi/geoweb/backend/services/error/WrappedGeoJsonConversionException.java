package nl.knmi.geoweb.backend.services.error;

public class WrappedGeoJsonConversionException extends GeoJsonConversionException {
	public WrappedGeoJsonConversionException(Throwable cause) {
		super("Unable to convert between org.geojson.GeoJsonObject and org.locationtech.jts.geom.Geometry", cause);
	}
}
