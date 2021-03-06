package nl.knmi.geoweb.backend.presets;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.usermanagement.UserLogin;

@Slf4j
@RestController
@RequestMapping("/preset")
public class PresetServices {

	@Autowired
	private ObjectMapper objectMapper;

	PresetStore presetStore;

	PresetServices (final PresetStore presetStore) throws IOException {
		this.presetStore = presetStore;
	}
	@RequestMapping(
		path="/getpresets",
		method = RequestMethod.GET,
		produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<List<Preset>> getPresets(@RequestParam(value="system", required=false, defaultValue="false")Boolean system, HttpServletRequest req) throws JsonProcessingException {
		List<Preset>presets=presetStore.readSystemPresets();
		if (!system) {
			String user=UserLogin.getUserName();
			String[]roles=UserLogin.getUserPrivileges();
			List<Preset>userPresets=presetStore.readUserPresets(user);
			presets.addAll(userPresets);
			for (String role: roles) {
				List<Preset>rolePresets=presetStore.readRolePresets(role);
				presets.addAll(rolePresets);
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body(presets);			
	}

	@RequestMapping(path="/getpreset")
	public ResponseEntity<String> getPreset(@RequestParam("name")String name, @RequestParam(value="system", required=false, defaultValue="false")Boolean system, HttpServletRequest req) throws JsonProcessingException {
		List<Preset>presets=presetStore.readSystemPresets();
		if (!system) {
			String user=UserLogin.getUserName();
			String[]roles=UserLogin.getUserPrivileges();
			List<Preset>userPresets=presetStore.readUserPresets(user);
			presets.addAll(userPresets);
			for (String role: roles) {
				List<Preset>rolePresets=presetStore.readRolePresets(role);
				presets.addAll(rolePresets);
			}
			List<Preset>sharedPresets=presetStore.readSharedPresets();
			for (Preset sharedPreset: sharedPresets) {
				if (sharedPreset.getName().equals(name)) {
					presets.add(sharedPreset);
				}
			}
		}
		for (Preset preset : presets) {
			if (preset.getName().equals(name)) {
				String json= objectMapper.writeValueAsString(preset);
				return ResponseEntity.status(HttpStatus.OK).body(json);
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body("{}");
	}

	@RequestMapping(path="/putsystempreset", method=RequestMethod.POST,	produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> storeSystemPreset(@RequestParam("name")String name, @RequestBody String preset) {
		Preset pr = presetStore.loadJsonPreset(preset);
		if (pr!=null) {
			pr.setName(name);
			try {
				presetStore.storeSystemPreset(pr);
				return ResponseEntity.status(HttpStatus.OK).body("{ \"test\": \"OK\"}");				

			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ERR");				
	}

	@RequestMapping(path="/putrolespreset", method=RequestMethod.POST)
	public ResponseEntity<String> storeRolesPreset(@RequestParam("name")String name, @RequestParam("roles")String roles, @RequestBody String preset, HttpServletRequest req) {
		Preset pr = presetStore.loadJsonPreset(preset);
		if (pr!=null) {
			pr.setName(name);

			try {
				presetStore.storeRolePreset(Arrays.asList(roles.split(",")), pr);
				return ResponseEntity.status(HttpStatus.OK).body("Role preset "+name+" stored");				

			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	@RequestMapping(path="/putuserpreset", method=RequestMethod.POST)
	public ResponseEntity<String> storeUserPreset(@RequestParam("name")String name, @RequestBody Preset preset, HttpServletRequest req) {

		if (preset!=null) {
			preset.setName(name);

			String user=UserLogin.getUserName();
			try {
				presetStore.storeUserPreset(user, preset);
				return ResponseEntity.status(HttpStatus.OK).body("User preset "+name+" stored");				

			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	@RequestMapping(path="/putsharedpreset", method=RequestMethod.POST)
	public ResponseEntity<String> storeSharedPreset(@RequestParam("name")String name, @RequestBody Preset preset, HttpServletRequest req) {
		if (preset!=null) {
			preset.setName(name);
			try {
				presetStore.storeSharedPreset(preset);
				return ResponseEntity.status(HttpStatus.OK).body("{\"message\": \"ok\"}");					

			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);			
	}

}

