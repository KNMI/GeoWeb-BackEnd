package nl.knmi.geoweb.backend.datastore;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;

@Component
public class TafStore {
	private static final Logger LOGGER = LoggerFactory.getLogger(TafStore.class);

	@Autowired
	@Qualifier("tafObjectMapper")
	private ObjectMapper tafObjectMapper;
	
	private String directory = null;
		
	static boolean isCreated = false;
	
	TafStore (@Value(value = "${productstorelocation}") String productstorelocation) throws Exception {
		if(productstorelocation == null) {
			throw new Exception("productstorelocation property is null");
		}
		if(isCreated == true) {
			LOGGER.debug("WARN: TafStore is already created");
		}
		isCreated = true;
		String dir = productstorelocation + "/tafs/";
		LOGGER.debug("TAF STORE at {}", dir);
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			LOGGER.debug("Creating taf store at [{}]", f.getAbsolutePath());
		}
		if(f.isDirectory() == false){
			LOGGER.error("Taf directory location is not a directorty");
			throw new NotDirectoryException("Taf directory location is not a directory");
		}
		
		this.directory=dir;
	}

	public void cleanUp() {
		this.isCreated=false;
	}
	
	public void storeTaf(Taf taf) throws JsonProcessingException, IOException {
		String fn=String.format("%s/taf_%s.json", this.directory, taf.metadata.getUuid());
		if(taf.metadata.getValidityStart() == null || taf.metadata.getValidityEnd() == null) {
			throw new IOException("Validity start end validity end must be specified");
		}
		Tools.writeFile(fn, taf.toJSON(tafObjectMapper));
	}



	public Taf[] getTafs(boolean selectActive, TAFReportPublishedConcept selectStatus, String uuid, String location) throws JsonParseException, JsonMappingException, IOException {
		Comparator<Taf> comp = new Comparator<Taf>() {
			public int compare(Taf lhs, Taf rhs) {
				try{
					return rhs.metadata.getIssueTime().compareTo(lhs.metadata.getIssueTime());
				}catch(Exception e){
					return 0;
				}
			}
		};

		//Scan directory for tafs
		File dir=new File(directory);
		File[] files=dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !name.contains("..") && name.contains("taf_")&&name.endsWith(".json");
			}
		});

		OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));

		if (files!=null) {
			List<Taf> tafs=new ArrayList<Taf>();
			for (File f: files) {
				Taf taf;
				taf = Taf.fromFile(f, tafObjectMapper);
				//Check on UUID
				if(taf.metadata.getUuid()!=null && uuid!=null){
					if(taf.metadata.getUuid().equals(uuid) == false ) continue;
				}
				//Check on Location
				if(location!=null){
					if(taf.metadata.getLocation()!=null){ 
						if(taf.metadata.getLocation().equals(location) == false ) continue;
					}else{
						continue;
					}
				}
				if (selectActive) {
					
					if (
							(taf.metadata.getStatus()==TAFReportPublishedConcept.published) &&
							(taf.metadata.getValidityEnd().compareTo(now)>0)
					) {
						tafs.add(taf);
					}
				}else if (selectStatus != null) {
					if (taf.metadata.getStatus()==selectStatus) {
						tafs.add(taf);
					}
				} else {
					tafs.add(taf);
				}
			}
			tafs.sort(comp);
			return tafs.toArray(new Taf[0]);
		}
		return null;
	}

	public Taf getByUuid(String uuid) throws JsonParseException, JsonMappingException, IOException {
		for (Taf taf: getTafs(false, null, uuid, null)) {
			if (uuid.equals(taf.metadata.getUuid())){
				return taf;
			}
		}
		return null;
	}
	
	public boolean isPublished(String uuid) {
		try {
			for (Taf taf: getTafs(false, null, uuid, null)) {
				if (uuid.equals(taf.metadata.getUuid())){
					return taf.getMetadata().getStatus().equals(TAFReportPublishedConcept.published);
				}
			}
		} catch (IOException e) {
		}
		return false;
	}

	public boolean isPublished(String location, OffsetDateTime start, OffsetDateTime end) {
		try {
			for (Taf taf: getTafs(false, TAFReportPublishedConcept.published, null, location)) {
				if (start.equals(taf.metadata.getValidityStart())&&end.equals(taf.metadata.getValidityEnd())){
					return true;
				}
			}
		} catch (IOException e) {
		}
		return false;
	}

	public boolean deleteTafByUuid(String uuid) throws IOException {
		String fn=String.format("%s/taf_%s.json", this.directory, uuid);
		return Tools.rm(fn);

	}

	public String getLocation() {
		return directory;
	}
}
