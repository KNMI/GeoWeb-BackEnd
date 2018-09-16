package nl.knmi.geoweb.backend.services;

import java.io.IOException;

import org.geojson.GeoJsonObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import nl.knmi.geoweb.backend.services.error.GeoJsonConversionException;
import nl.knmi.geoweb.backend.services.error.IntersectionTooComplexException;
import nl.knmi.geoweb.backend.services.error.WrappedGeoJsonConversionException;

public class GeoJsonIntersectionHelper {
	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
	private static final int MAXIMUM_NUMBER_OF_RESULTING_POINTS = 6;
	private static ObjectMapper geoJsonObjectMapper;

	static {
		geoJsonObjectMapper = new ObjectMapper();
		geoJsonObjectMapper.registerModule(new JavaTimeModule());
		geoJsonObjectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		geoJsonObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		geoJsonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		geoJsonObjectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
	}

	public static GeoJsonObject intersection(GeoJsonObject featureGJO, GeoJsonObject firGJO) throws GeoJsonConversionException {
		Geometry featureGeometry = convertGeoJsonObjectToGeometry(featureGJO);
		Geometry firGeometry = convertGeoJsonObjectToGeometry(firGJO);
		Geometry intersectionGeometry = featureGeometry.intersection(firGeometry);

		if (intersectionGeometry.getNumPoints() > MAXIMUM_NUMBER_OF_RESULTING_POINTS) {
			throw new IntersectionTooComplexException(intersectionGeometry.getNumPoints(), MAXIMUM_NUMBER_OF_RESULTING_POINTS);
		}

		return convertGeometryToGeoJsonObject(intersectionGeometry);
	}

	private static GeoJsonObject convertGeometryToGeoJsonObject(Geometry geometry) throws WrappedGeoJsonConversionException {
		try {
			return geoJsonObjectMapper.readValue(new GeoJsonWriter().write(geometry), GeoJsonObject.class);
		} catch (IOException e) {
			throw new WrappedGeoJsonConversionException(e);
		}
	}

	private static Geometry convertGeoJsonObjectToGeometry(GeoJsonObject geoJsonObject) throws WrappedGeoJsonConversionException {
		try {
			return new GeoJsonReader(GEOMETRY_FACTORY).read(geoJsonObjectMapper.writeValueAsString(geoJsonObject));
		} catch (JsonProcessingException | ParseException e) {
			throw new WrappedGeoJsonConversionException(e);
		}
	}
}
