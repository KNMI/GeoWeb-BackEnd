package nl.knmi.geoweb.backend.presets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.presets.model.Preset;
import nl.knmi.geoweb.backend.presets.model.StoredPreset;

@Component
public class PresetStore {
	private static final Logger LOGGER = LoggerFactory.getLogger(PresetStore.class);

	private String directory;
	private String roleDir;
	private String userDir;

	public PresetStore(@Value(value = "${productstorelocation}") String productstorelocation) throws IOException {
		String dir = productstorelocation + "/presets";
		LOGGER.debug("PRESET STORE at {}", dir);
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			LOGGER.debug("Creating presetstore at [{}]", f.getAbsolutePath());
		}
		if(f.isDirectory() == false){
			LOGGER.error("Sigmet directory location is not a directory");
			throw new NotDirectoryException("Sigmet directory location is not a directory");
		}
		this.userDir=dir+"/users";
		f=new File(userDir);
		if(f.exists() == false){
			if (!f.mkdir()) {
				LOGGER.error("Presets directory can not be created");
				throw new NotDirectoryException("Presets directory can not be created");
			}
		}
		this.roleDir=dir+"/roles";
		f=new File(roleDir);
		if(f.exists() == false){
			if (!f.mkdir()) {
				LOGGER.error("Presets directory can not be created");
				throw new NotDirectoryException("Presets directory can not be created");
			}
		}
		this.directory=dir;

	}

	public void store(StoredPreset preset) throws IOException {
		ObjectMapper om =new ObjectMapper();
		String json="";
		try {
			json=om.writeValueAsString(preset);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File f=null;

		if (preset.isSystem()) {
			//Store in sysdir
			f=new File(this.directory,"sys_"+preset.getPreset().getName()+".json");
		}else if (preset.getUser()!=null){
			//Store in userdir
			f=new File(this.userDir,"user_"+preset.getUser()+"_"+preset.getPreset().getName()+".json");
		}else {
			//Store in roledir
			f=new File(this.roleDir,"role_"+preset.getPreset().getName()+".json");
		}
		try (PrintStream out = new PrintStream(new FileOutputStream(f))) {
			out.print(json);
		}
	}

	public void storeUserPreset(String user, Preset preset) throws IOException{
		StoredPreset sp=new StoredPreset(user, preset);
		store(sp);
	}

	public void storeRolePreset(List<String>roles, Preset preset) throws IOException {
		StoredPreset sp=new StoredPreset(roles, preset);
		store(sp);
	}

	public void storeSystemPreset(Preset preset) throws IOException {
		StoredPreset sp=new StoredPreset(preset);
		store(sp);
	}

	public Preset loadJsonPresetFromFile(String fn) throws IOException {
		String json=new String(Files.readAllBytes(Paths.get(fn)));
		return loadJsonPreset(json);

	}
	public Preset loadJsonPreset(String json) {
		ObjectMapper om=new ObjectMapper();
		try {
			  return om.readValue(json, Preset.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public StoredPreset loadJsonStoredPresetFromFile(String fn) throws IOException {
		String json=new String(Files.readAllBytes(Paths.get(fn)));
		return loadJsonStoredPreset(json);

	}
	public StoredPreset loadJsonStoredPreset(String json) {
		ObjectMapper om=new ObjectMapper();
		try {
			return om.readValue(json, StoredPreset.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public List<Preset> readUserPresets(String name) {
		List<Preset> presets=new ArrayList<Preset>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(userDir),
				new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				return entry.getFileName().toString().contains("user_"+name);
			}
		})
				) {

			for (Path path : files) {
				StoredPreset preset=loadJsonStoredPresetFromFile(path.toString());
				presets.add(preset.getPreset());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return presets;
	}

	public List<Preset> readRolePresets(String role) {
		List<Preset> presets=new ArrayList<Preset>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(roleDir),
				new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				return entry.getFileName().toString().contains("role_");
			}
		})
				) {

			for (Path path : files) {
				StoredPreset preset=loadJsonStoredPresetFromFile(path.toString());
				if (preset.getRoles().contains(role)){
					presets.add(preset.getPreset());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return presets;
	}

	public List<Preset> readSystemPresets() {
		List<Preset> presets=new ArrayList<Preset>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(directory),
				new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				return entry.getFileName().toString().contains("sys_");
			}
		})
				) {

			for (Path path : files) {
				StoredPreset preset=loadJsonStoredPresetFromFile(path.toString());
				presets.add(preset.getPreset());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return presets;
	}
}
