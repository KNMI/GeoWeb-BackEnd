package nl.knmi.geoweb.backend.admin;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.Tools;

@Slf4j
@Getter
@Component
public class AdminStore {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private String dir = null;
	private String roleDir;
	private String userDir;



	AdminStore(@Value("${geoweb.products.storeLocation}") String productstorelocation) throws IOException {
		String dir = productstorelocation+"/admin";
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			log.debug("Creating admin store at ["+f.getAbsolutePath()+"]");
		}
		if(f.isDirectory() == false){
			log.error("Admin store directory location is not a directory");
			throw new NotDirectoryException("Admin store directory location is not a directory");
		}
		this.dir=dir;
	}



	public void create(String type, String name, String payload) throws IOException {
		String itemdir = dir+"/"+type+"/";
		Tools.mksubdirs(itemdir);
		Tools.writeFile(itemdir + name+".dat", payload);
	}

	public void edit(String type, int id, String payload) throws IOException {
		String itemdir = dir+"/"+type+"/";
		File dir=new File(itemdir);
		File[] files=dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String n) {
				return !n.contains("..") && n.contains("example_taf");
			}
		});

		if (files!=null) {
			Arrays.sort(files, (a, b) -> getTimestamp(a.getName()).compareTo(getTimestamp(b.getName())));
			File theFile = files[id];
			Tools.rmfile(theFile.getPath());
			Tools.writeFile(theFile.getPath(), payload);
		}
	}

	public void deleteByIndex(String type, int id) throws IOException {
		String itemdir = dir+"/"+type+"/";
		File dir=new File(itemdir);
		File[] files=dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String n) {
				return !n.contains("..") && n.contains("example_taf");
			}
		});

		if (files!=null) {
			Arrays.sort(files, (a, b) -> getTimestamp(a.getName()).compareTo(getTimestamp(b.getName())));
			File theFile = files[id];
			Tools.rmfile(theFile.getPath());
		}
	}

	public String read(String type, String name) throws IOException{
		String itemdir = dir+"/"+type+"/";
		File file = new File(itemdir + name+".dat");
		if (file.exists() == false){
			String resourceName = "adminstore/" + type + "/" + name;
			if (!resourceName.endsWith(".json")) {
				resourceName += ".json";
			}
			log.error("Unable to load item [" + file.getAbsolutePath() + "] Attempting to read from resources with name [" + resourceName + "]");
			String item = null;
			try {
				item = Tools.readResource(resourceName);
				log.debug("[OK] Read from resource [" + resourceName + "] now writing to store");
			}catch(Exception e) {				
			}
			if (item != null && item.length() > 0) {
				Tools.mksubdirs(file.getParentFile().getAbsolutePath());
				Tools.writeFile(file.getAbsolutePath(), item);
				log.debug("[OK] Write  item [" + file.getAbsolutePath() + "]");
			} else {
				log.error("Unable to find resource " + resourceName);
				throw new java.io.IOException("Unable to locate item " + resourceName);
			}
		}
		log.debug("[ADMINSTORE] Reading item [" + file.getAbsolutePath() + "]");
		return Tools.readFile(file.getAbsolutePath());
	}
	private Long getTimestamp(String fname) {
		return Long.parseLong(fname.replaceAll("\\D+", ""));
	}
	public List<String> readAll(String type, String name) throws IOException {
		String itemdir = dir+"/"+type+"/";
		File dir=new File(itemdir);
		File[] files=dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String n) {
				return !n.contains("..") && n.contains("example_taf");
			}
		});

		if (files!=null) {
			Arrays.sort(files, (a, b) -> getTimestamp(a.getName()).compareTo(getTimestamp(b.getName())));
			List<String> tafs=new ArrayList<String>();
			for (File f: files) {
				byte[] tafBytes = Files.readAllBytes(f.toPath());
				tafs.add(new String(tafBytes, "utf-8"));
			}

			return tafs;
		}
		return null;
	}

	public void createSubseq(String type, String name, String payload) throws IOException {
		String itemdir = dir+"/"+type+"/";
		Tools.mksubdirs(itemdir);
		File directory = new File(itemdir);
		File[] files=directory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String n) {
				return !n.contains("..") && n.contains(name)&&n.endsWith(".json");
			}
		});

		if (files!=null) {
			Tools.writeFile(itemdir + name+"_" + files.length + ".json", payload);
		}
	}
}
