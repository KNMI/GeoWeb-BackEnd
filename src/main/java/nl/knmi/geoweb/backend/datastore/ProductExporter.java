package nl.knmi.geoweb.backend.datastore;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Component;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.IExportable;

@Component
public class ProductExporter {
	private File path;
	
	ProductExporter () {
		this(new File("/tmp/exports"));
	}
	
	ProductExporter(File path) {
		this.path = path;

		if (!this.path.exists()) {
			try {
				Tools.mksubdirs(path.getAbsolutePath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void export(IExportable product) {
		// TODO Auto-generated method stub
		product.export(path);
	}
}
