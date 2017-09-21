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
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;

@Getter
@Component
public class AdminStore {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private String dir = null;
	private String roleDir;
	private String userDir;

	
	
	AdminStore(@Value(value = "${productstorelocation}") String productstorelocation) throws IOException {
		String dir = productstorelocation+"/admin";
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			Debug.println("Creating admin store at ["+f.getAbsolutePath()+"]");
		}
		if(f.isDirectory() == false){
			Debug.errprintln("Admin store directory location is not a directory");
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
		return Tools.readFile(itemdir + name+".dat");
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
