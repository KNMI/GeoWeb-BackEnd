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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;


@Getter
public class PresetStore {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Getter
	public static class StoredPreset {
		Preset preset;
		private boolean system=false;
		private String user;
		private List<String> roles;
		public StoredPreset(Preset preset) {
			this.system=true;
			this.user=null;
			this.roles=null;
			this.preset=preset;
		}
		public StoredPreset(String user, Preset preset) {
			this.system=false;
			this.user=user;
			this.roles=null;
			this.preset=preset;
		}
		public StoredPreset(List<String>roles, Preset preset) {
			this.system=false;
			this.user=null;
			this.roles=roles;
			this.preset=preset;
		}
		public StoredPreset(){}
	}

	private String dir;
	private String roleDir;
	private String userDir;

	public PresetStore() {}

	public PresetStore(String dir) throws IOException {
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			Debug.println("Creating presetstore at ["+f.getAbsolutePath()+"]");
		}
		if(f.isDirectory() == false){
			Debug.errprintln("Sigmet directory location is not a directory");
			throw new NotDirectoryException("Sigmet directory location is not a directory");
		}
		this.userDir=dir+"/users";
		f=new File(userDir);
		if(f.exists() == false){
			if (!f.mkdir()) {
				Debug.errprintln("Presets directory can not be created");
				throw new NotDirectoryException("Presets directory can not be created");
			}
		}
		this.roleDir=dir+"/roles";
		f=new File(roleDir);
		if(f.exists() == false){
			if (!f.mkdir()) {
				Debug.errprintln("Presets directory can not be created");
				throw new NotDirectoryException("Presets directory can not be created");
			}
		}
		this.dir=dir;

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
			f=new File(this.dir,"sys_"+preset.getPreset().getName()+".json");
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
		try (DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dir),
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
	
	public static void main(String[]args) {
		PresetStore ps=null;
		try {
			ps=new PresetStore("/tmp/presets");
		} catch (NotDirectoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AreaPreset pi1=PresetItem.createAreaPreset(60, 50, "EPSG:4326");
		DisplayPreset pi2=PresetItem.createDisplayPreset("QUADCOL", 4);

		Map<String, String> dims=new HashMap<String, String>();
		String[] layers={"layer1", "layer2"};
		String[] services={"http://service.knmi.nl/service1", "http://service.knmi.nl/service1"};
		LayerPreset pi3=PresetItem.createLayerPreset(services[0], layers[0],  dims);
		List<LayerPreset>lyrs=new ArrayList<LayerPreset>();
		lyrs.add(pi3);
		LayerPreset pi4=PresetItem.createLayerPreset(services[1], layers[1],  dims);
		lyrs.add(pi4);
		try {
			System.err.println(new ObjectMapper().writeValueAsString(lyrs));
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		List<List<LayerPreset>>lyrslist=new ArrayList<List<LayerPreset>>();
		lyrslist.add(lyrs);
		String[]keywords={"SIGMET", "AVIATION"};
		//	public Preset(String name, String[] keywords, List<List<LayerPreset>> layers, DisplayPreset display, AreaPreset area){
		Preset p1=new Preset("preset1", keywords, lyrslist, pi2, pi1);
		try {
			ps.storeSystemPreset(p1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Preset p2=new Preset("preset2", keywords, lyrslist, pi2, pi1);
		try {
			ps.storeUserPreset("ernst", p2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Preset p3=new Preset("preset3", keywords, lyrslist, pi2, pi1);
		try {
			String[]roles={"met", "admin"};
			ps.storeRolePreset(Arrays.asList(roles), p3);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			System.err.println(new ObjectMapper().writeValueAsString(ps.readUserPresets("ernst")));
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			System.err.println(new ObjectMapper().writeValueAsString(ps.readRolePresets("met")));
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			System.err.println("sys:"+new ObjectMapper().writeValueAsString(ps.readSystemPresets()));
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
