package nl.knmi.geoweb.backend.admin;

import java.nio.file.NotDirectoryException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.JSONResponse;

@RestController
@RequestMapping(path="/admin", method=RequestMethod.GET)
public class AdminServices {
	static AdminStore store = null;

	AdminServices () throws NotDirectoryException {
		store = new AdminStore("/tmp/admin");
	}
	//http://bhw485.knmi.nl:8090/admin/read?type=locations&name=locations

	@RequestMapping(path="/create", method=RequestMethod.POST,	produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void createConfigurationItem(HttpServletRequest req, HttpServletResponse response) throws JsonProcessingException {
		Debug.println("admin/create");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			String type = HTTPTools.getHTTPParam(req, "type");
			String name = HTTPTools.getHTTPParam(req, "name");
			String payload = HTTPTools.getHTTPParam(req, "payload");
			JSONObject result = new JSONObject();
			Debug.println("type:" +type);
			Debug.println("name:" +name);
			Debug.println("payload:" +payload);
			store.create(type,name,payload);
			result.put("message", "ok");
			jsonResponse.setMessage(result);
		} catch (Exception e) {
			jsonResponse.setException("create failed",e);
		}
		try {
			jsonResponse.print(response);
		} catch (Exception e1) {
		}
	}

	@RequestMapping(path="/read", method=RequestMethod.GET,	produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void readConfigurationItem(HttpServletRequest req, HttpServletResponse response) throws JsonProcessingException {
		Debug.println("admin/read");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			String type = HTTPTools.getHTTPParam(req, "type");
			String name = HTTPTools.getHTTPParam(req, "name");
			JSONObject result = new JSONObject();
			Debug.println("type:" +type);
			Debug.println("name:" +name);
			String payload = store.read(type,name);
			result.put("message", "ok");
			result.put("payload", payload);
			jsonResponse.setMessage(result);
		} catch (Exception e) {
			Debug.println("Failed");
			jsonResponse.setException("read failed",e);
		}
		
		Debug.println("printing");
		
		try {
			Debug.println("printing" + jsonResponse.getMessage());
			jsonResponse.print(response);
		} catch (Exception e1) {
		}
	}
	
	
}

