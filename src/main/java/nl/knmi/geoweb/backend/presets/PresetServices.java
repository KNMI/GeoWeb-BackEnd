package nl.knmi.geoweb.backend.presets;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

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

import nl.knmi.geoweb.backend.presets.model.Preset;
import nl.knmi.geoweb.backend.usermanagement.UserLogin;
import nl.knmi.geoweb.backend.usermanagement.UserStore;

@RestController
@RequestMapping("/preset")
public class PresetServices {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	PresetStore presetStore;

	PresetServices (final PresetStore presetStore) throws IOException {
		this.presetStore = presetStore;
	}
	@RequestMapping(path = "/getpresets", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public List<Preset> getPresets(@RequestParam(value="system", required=false, defaultValue="false")Boolean system, HttpServletRequest req) throws JsonProcessingException {
		List<Preset>presets=presetStore.readSystemPresets();
		if (!system) {
			UserStore userStore=UserStore.getInstance();
			String user=UserLogin.getUserFromRequest(req);
			String[]roles=userStore.getUserRoles(user);
			if (roles==null) roles=new String[]{"USER"};
			List<Preset>userPresets=presetStore.readUserPresets(user);
			presets.addAll(userPresets);
			for (String role: roles) {
				List<Preset>rolePresets=presetStore.readRolePresets(role);
				presets.addAll(rolePresets);
			}
		}
		return presets;
	}

	@RequestMapping(path="/getpreset", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Preset getPreset(@RequestParam("name")String name, @RequestParam(value="system", required=false, defaultValue="false")Boolean system, HttpServletRequest req) throws JsonProcessingException {
		List<Preset>presets=presetStore.readSystemPresets();
		if (!system) {
			UserStore userStore=UserStore.getInstance();
			String user=UserLogin.getUserFromRequest(req);
			String[]roles=userStore.getUserRoles(user);
			if (roles==null) roles=new String[]{"USER"};
			List<Preset>userPresets=presetStore.readUserPresets(user);
			presets.addAll(userPresets);
			for (String role: roles) {
				List<Preset>rolePresets=presetStore.readRolePresets(role);
				presets.addAll(rolePresets);
			}
		}
		for (Preset preset : presets) {
			if (preset.getName().equals(name)) {
				return preset;
			}
		}
		return Preset.emptyPreset();
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
				// TODO Auto-generated catch block
				e.printStackTrace();
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	@RequestMapping(path="/putuserpreset", method=RequestMethod.POST)
	public ResponseEntity<String> storeUserPreset(@RequestParam("name")String name, @RequestBody String preset, HttpServletRequest req) {
		Preset pr = presetStore.loadJsonPreset(preset);

		if (pr!=null) {
			pr.setName(name);

			String user=UserLogin.getUserFromRequest(req);
			try {
				presetStore.storeUserPreset(user, pr);
				return ResponseEntity.status(HttpStatus.OK).body("User preset "+name+" stored");				

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}
}

