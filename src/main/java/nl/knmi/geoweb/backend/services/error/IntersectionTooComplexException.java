package nl.knmi.geoweb.backend.services.error;

public class IntersectionTooComplexException extends GeoJsonConversionException {
	private final int actualNumberOfPoints;
	private final int maximumNumberOfPoints;

	public IntersectionTooComplexException(int actualNumberOfPoints, int maximumNumberOfPoints) {
		super("Polygon has more than " + maximumNumberOfPoints + " points (it has " +  actualNumberOfPoints + ")");
		this.actualNumberOfPoints = actualNumberOfPoints;
		this.maximumNumberOfPoints = maximumNumberOfPoints;
	}

	public int getActualNumberOfPoints() {
		return actualNumberOfPoints;
	}

	public int getMaximumNumberOfPoints() {
		return maximumNumberOfPoints;
	}
}
