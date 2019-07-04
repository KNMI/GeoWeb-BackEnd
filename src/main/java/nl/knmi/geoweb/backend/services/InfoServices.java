package nl.knmi.geoweb.backend.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/versioninfo")
public class InfoServices {

	@Value("${geoweb.backendVersion}")
	private String backendVersion;

	@Value("${geoweb.messageConverterVersion}")
	private String messageConverterVersion;

	@RequestMapping(path = "/version", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Map<String, String> getVersionInfo() {
		HashMap<String, String> map = new HashMap<>();
		map.put("backend", backendVersion);
		map.put("messageconverter", messageConverterVersion);
		return map;
	}
}
