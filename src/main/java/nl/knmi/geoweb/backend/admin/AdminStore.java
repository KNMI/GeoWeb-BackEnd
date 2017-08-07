package nl.knmi.geoweb.backend.admin;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;

@Getter
public class AdminStore {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private String dir;
	private String roleDir;
	private String userDir;

	public AdminStore() {}

	public AdminStore(String dir) throws IOException {
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
	
	public String read(String type, String name) throws IOException{
		String itemdir = dir+"/"+type+"/";
		return Tools.readFile(itemdir + name+".dat");
	}
}
