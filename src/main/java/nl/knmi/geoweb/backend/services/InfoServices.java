package nl.knmi.geoweb.backend.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import nl.knmi.geoweb.backend.info.BackendInfo;
import nl.knmi.geoweb.backend.info.MessageConverterInfo;

@RestController
@RequestMapping("/versioninfo")
public class InfoServices {
	@Autowired
	BackendInfo backendInfo;
	
	@Autowired 
	MessageConverterInfo messageConverterInfo;
	
	@RequestMapping(path="/version", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Map<String,String> getVersionInfo() {
		HashMap<String, String> map=new HashMap<>();
		map.put("backend", backendInfo.getBackendVersion());
		map.put("messageconverter", messageConverterInfo.getMessageConverterVersion());
		return map;
	}
}
