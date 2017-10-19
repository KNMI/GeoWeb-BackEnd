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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;

@Component
public class TafStore {

	private String directory = null;
	
	static boolean isCreated = false;
	
	TafStore (@Value(value = "${productstorelocation}") String productstorelocation) throws Exception {
		if(productstorelocation == null) {
			throw new Exception("productstorelocation property is null");
		}
		if(isCreated == true) {
			throw new Exception("TafStore is already created");
		}
		isCreated = true;
		String dir = productstorelocation + "/tafs/";
		Debug.println("TAF STORE at " + dir);
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			Debug.println("Creating taf store at ["+f.getAbsolutePath()+"]");		
		}
		if(f.isDirectory() == false){
			Debug.errprintln("Taf directory location is not a directorty");
			throw new NotDirectoryException("Taf directory location is not a directorty");
		}
		
		this.directory=dir;
	}

	public void storeTaf(Taf taf) throws JsonProcessingException, IOException {
		Debug.println("Store taf " + this.directory);
		String fn=String.format("%s/taf_%s.json", this.directory, taf.metadata.getUuid());
		Tools.writeFile(fn, taf.toJSON());
	}



	public Taf[] getTafs(boolean selectActive, TAFReportPublishedConcept selectStatus, String uuid, String location) throws JsonParseException, JsonMappingException, IOException {
		Debug.println("directory:"+directory);
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
				taf = Taf.fromFile(f);
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
	
	public boolean deleteTafByUuid(String uuid) throws IOException {
		String fn=String.format("%s/taf_%s.json", this.directory, uuid);
		return Tools.rm(fn);

	}

	public String getLocation() {
		return directory;
	}
}
