package nl.knmi.geoweb.backend.product.taf;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;

public class TafSchemaStore {
	private String directory;
	public TafSchemaStore(String dir) throws IOException {
		Debug.println("TAF SCHEMA STORE at " + dir);
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			Debug.println("Creating taf schema store at ["+f.getAbsolutePath()+"]");		
		}
		if(f.isDirectory() == false){
			Debug.errprintln("Taf directory location is not a directory");
			throw new NotDirectoryException("Taf directory location is not a directory");
		}
		
		this.directory=dir;
	}
	
	public String getSchemaSchema() throws IOException {
		String s = Tools.readFile(this.directory + "/jsonschema_schema.json");
		System.out.println(s);
		return s;
	}

	public void storeTafSchema(String schema) throws JsonProcessingException, IOException, ProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode asJson = mapper.readTree(schema);
		if (new TafValidator().validateSchema(asJson)) {
			long unixTime = System.currentTimeMillis() / 1000L;
			String fn=String.format("%s/taf_schema_%s.json", this.directory, unixTime);
			Tools.writeFile(fn, asJson.toString());
		} else {
			throw new ProcessingException("Schema is not valid");
		}
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
	
	public String getLatestTafSchema() throws IOException {
		File dir=new File(directory);
		File[] files=dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !name.contains("..") && name.contains("taf_schema")&&name.endsWith(".json");
			}
		});
		
		// Timestamp is in the file so sort files according to this timestamp
		// Oldest are first so pick the final element in the array
		if (files!=null) {
			Arrays.sort(files, (a, b) -> getTimestamp(a.getName()).compareTo(getTimestamp(b.getName())));
			File latest = files[files.length - 1];
			byte[] bytes = Files.readAllBytes(latest.toPath());
			return new String(bytes, "utf-8");
		}
		return null;

	}
}
