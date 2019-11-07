package nl.knmi.geoweb.backend.product;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface IExportable<GeoWebProduct> {
	public String export(File path, ProductConverter<GeoWebProduct> converter, ObjectMapper om);
}
