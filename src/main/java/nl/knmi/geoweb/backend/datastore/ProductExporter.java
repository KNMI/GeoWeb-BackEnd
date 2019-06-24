package nl.knmi.geoweb.backend.datastore;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.IExportable;
import nl.knmi.geoweb.backend.product.ProductConverter;

@Component
public class ProductExporter<P> {
	private File path;

	ProductExporter(@Value("${geoweb.products.exportLocation}") String productexportlocation) throws Exception {
		if (productexportlocation == null) {
			throw new Exception("productexportlocation property is null");
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

	public String export(IExportable product, ProductConverter<P> converter, ObjectMapper om) {
		// TODO Auto-generated method stub
		return product.export(path, converter, om);
	}
}
