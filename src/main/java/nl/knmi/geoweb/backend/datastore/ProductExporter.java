package nl.knmi.geoweb.backend.datastore;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.IExportable;
import nl.knmi.geoweb.backend.product.ProductConverter;
import nl.knmi.geoweb.backend.product.taf.converter.TafConverter;
import nl.knmi.geoweb.iwxxm_2_1.converter.GeoWebConverter;

@Component
public class ProductExporter<P> {
	private File path;
	
	
	ProductExporter () {
		this("/tmp/exports");
	}

	ProductExporter(@Value(value = "${productexportlocation}") String productexportlocation) {
		if (productexportlocation == null || productexportlocation.length() == 0) {
			productexportlocation = "/tmp/exports";
		}
		this.path = new File(productexportlocation);

		if (!this.path.exists()) {
			try {
				Tools.mksubdirs(path.getAbsolutePath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void export(IExportable product, ProductConverter<P> converter, ObjectMapper om) {
		// TODO Auto-generated method stub
		product.export(path, converter, om);
	}
}
