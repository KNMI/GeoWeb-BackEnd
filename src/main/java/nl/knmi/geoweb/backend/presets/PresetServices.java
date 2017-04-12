package nl.knmi.geoweb.backend.presets;

import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/preset")
public class PresetServices {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	static PresetStore store = null;

	PresetServices () throws NotDirectoryException {
		store = new PresetStore("/tmp/presets");
	}
	@RequestMapping(path="/getpresets")
	public ResponseEntity<String> getPresets(@RequestParam("system")Boolean system) throws JsonProcessingException {
		if (system) {
			List<Preset>presets=store.readSystemPresets();
			String json=new ObjectMapper().writeValueAsString(presets);
			return ResponseEntity.status(HttpStatus.OK).body(json);
		} else {
			List<Preset>presets=store.readUserPresets("ernst");
			List<Preset>rolepresets=store.readRolePresets("met");
			presets.addAll(rolepresets);
			String json=new ObjectMapper().writeValueAsString(presets);
			return ResponseEntity.status(HttpStatus.OK).body(json);
		}
//		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error");				
	}

	@RequestMapping(path="/putsystempreset", method=RequestMethod.POST,	produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> storeSystemPreset(@RequestParam("name")String name, @RequestBody String preset) {
		logger.error("PRESET:"+name+" "+preset);
		Preset pr = store.loadJsonPreset(preset);
		if (pr!=null) {
			pr.setName(name);
			try {
				store.storeSystemPreset(pr);
				return ResponseEntity.status(HttpStatus.OK).body("{ \"test\": \"OK\"}");				

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ERR");				
	}

	@RequestMapping(path="/putsrolespreset", method=RequestMethod.POST)
	public ResponseEntity<String> storeRolesPreset(@RequestParam("name")String name, @RequestParam("roles")String roles, @RequestBody String preset) {
		Preset pr = store.loadJsonPreset(preset);
		if (pr!=null) {
			pr.setName(name);
			try {
				store.storeRolePreset(Arrays.asList(roles.split(",")), pr);
				return ResponseEntity.status(HttpStatus.OK).body("Role preset "+name+" stored");				

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	@RequestMapping(path="/putuserpreset", method=RequestMethod.POST)
	public ResponseEntity<String> storeUserPreset(@RequestParam("name")String name, @RequestParam("preset")String preset) {
		Preset pr = store.loadJsonPreset(preset);
		if (pr!=null) {
			pr.setName(name);
			try {
				store.storeUserPreset(name, pr);
				return ResponseEntity.status(HttpStatus.OK).body("User preset "+name+" stored");				

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}
}

