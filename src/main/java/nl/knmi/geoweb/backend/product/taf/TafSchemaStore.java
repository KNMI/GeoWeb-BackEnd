package nl.knmi.geoweb.backend.product.taf;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.Tools;

@Slf4j
@Component
@Getter
public class TafSchemaStore {
		
	private String directory = null;
	
	private static boolean schemaCopied=false;
	
	private static boolean enrichedSchemaCopied=false;
/*
	@Autowired
	private TafValidator tafValidator;
*/

	public TafSchemaStore(@Value(value = "${geoweb.products.storeLocation}") String productstorelocation) throws IOException {

		String dir = productstorelocation + "/tafs/schemas";
		log.debug("TafSchemaStore at " + dir);
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			log.debug("Creating TafSchemaStore at ["+f.getAbsolutePath()+"]");
		}
		if(f.isDirectory() == false){
			log.debug("Taf directory location is not a directory");
			throw new NotDirectoryException("Taf directory location is not a directory");
		}

		this.directory=dir;
	}

	public String getSchemaSchema() throws IOException {
		String s = null;
		try {
			s = Tools.readFile(this.directory + "/taf_jsonschema_schema.json");
		} catch (IOException e) {
			log.warn("taf_jsonschema_schema.json missing: writing to store from resource");
			s = Tools.readResource("taf_jsonschema_schema.json");
			Tools.writeFile(this.directory + "/taf_jsonschema_schema.json", s);
		}
		return s;
	}


	private Long getTimestamp(String fname) {
		return Long.parseLong(fname.replaceAll("\\D+", ""));
	}

	public String[] getTafSchemas() throws JsonParseException, JsonMappingException, IOException {
		//Scan directory for tafs
		File dir=new File(directory);
		File[] files=dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !name.contains("..") && name.contains("taf_schema")&&name.endsWith(".json");
			}
		});

		if (files!=null) {
			Arrays.sort(files, (a, b) -> getTimestamp(a.getName()).compareTo(getTimestamp(b.getName())));
			List<String> tafs=new ArrayList<String>();
			for (File f: files) {
				byte[] tafBytes = Files.readAllBytes(f.toPath());
				tafs.add(new String(tafBytes, "utf-8"));
			}
			return (String[])tafs.toArray();
		}
		return null;
	}
	public String getLatestEnrichedTafSchema() throws IOException {
		// Delete any *enriched_taf_schema*.json schemata currently in the directory
		File dir=new File(directory);
		File[] files=dir.listFiles(new FilenameFilter() {

		@Override
			public boolean accept(File dir, String name) {
				return name.contains("enriched_taf_schema")&&name.endsWith(".json");
			}
		});
		for(File file : files){
			file.delete();
		}

        if (!enrichedSchemaCopied) {
            log.warn("No taf enriched schema found, copying from resources dir");
            String s = Tools.readResource("EnrichedTafValidatorSchema.json");
            String fn=String.format("%s/EnrichedTafValidatorSchema.json", this.directory);
			Tools.writeFile(fn, s);
			
            enrichedSchemaCopied=true;
        }
        byte[] bytes = Files.readAllBytes(Paths.get(this.directory, "EnrichedTafValidatorSchema.json"));
        String result = new String(bytes, "utf-8");
        return result;
	}

	public String getLatestTafSchema() throws IOException {

        if (!schemaCopied) {
			// Delete any *taf_schema*.json schemata currently in the directory
			File dir=new File(directory);
			File[] files=dir.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.contains("taf_schema") && !name.contains("enriched") && name.endsWith(".json");
				}
			});
			for(File file : files){
				file.delete();
			}

			log.warn("No taf schemas found, copying one from resources dir");
			String s = Tools.readResource("TafValidatorSchema.json");
			String fn=String.format("%s/TafValidatorSchema.json", this.directory);
			Tools.writeFile(fn, s);

			// Copy subschemas
			s = Tools.readResource("SubSchemas/clouds.json");
			fn=String.format("%s/clouds.json",  this.directory);
			Tools.writeFile(fn, s);

			s = Tools.readResource("SubSchemas/weathergroup.json");
			fn=String.format("%s/weathergroup.json",  this.directory);
			Tools.writeFile(fn, s);

			s = Tools.readResource("SubSchemas/visibility.json");
			fn=String.format("%s/visibility.json",  this.directory);
			Tools.writeFile(fn, s);

			s = Tools.readResource("SubSchemas/wind.json");
			fn=String.format("%s/wind.json",  this.directory);
			Tools.writeFile(fn, s);

			s = Tools.readResource("SubSchemas/forecast.json");
			fn=String.format("%s/forecast.json",  this.directory);
			Tools.writeFile(fn, s);

			s = Tools.readResource("SubSchemas/weather.json");
			fn=String.format("%s/weather.json",  this.directory);
			Tools.writeFile(fn, s);

			s = Tools.readResource("SubSchemas/metadata.json");
			fn=String.format("%s/metadata.json",  this.directory);
			Tools.writeFile(fn, s);

			s = Tools.readResource("SubSchemas/vertical_visibility.json");
			fn=String.format("%s/vertical_visibility.json",  this.directory);
			Tools.writeFile(fn, s);

			s = Tools.readResource("SubSchemas/changegroup.json");
			fn=String.format("%s/changegroup.json",  this.directory);
			Tools.writeFile(fn, s);

			s = Tools.readResource("SubSchemas/temperature.json");
			fn=String.format("%s/temperature.json",  this.directory);
			Tools.writeFile(fn, s);
			schemaCopied=true;
        }
        byte[] bytes = Files.readAllBytes(Paths.get(this.directory, "TafValidatorSchema.json"));
        String result = new String(bytes, "utf-8");
        return result;
	}
}