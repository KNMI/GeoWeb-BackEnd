package nl.knmi.geoweb.backend.admin;

import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.taf.TafSchemaStore;

@RestController
@RequestMapping(path={"/admin", "/store"}, method=RequestMethod.GET)
public class AdminServices {
	static AdminStore store = null;
	static TafSchemaStore schemaStore = null;
	AdminServices () throws IOException {
		store = new AdminStore("/tmp/admin");
		schemaStore = new TafSchemaStore("/tmp/tafs/schemas");
	}
	//http://bhw485.knmi.nl:8090/admin/read?type=locations&name=locations

	@RequestMapping(path="/create", method=RequestMethod.POST,	produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void createConfigurationItem(HttpServletRequest req, HttpServletResponse response, @RequestBody String payload) throws JsonProcessingException {
		Debug.println("admin/create");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			String type = HTTPTools.getHTTPParam(req, "type");
			String name = HTTPTools.getHTTPParam(req, "name");
			JSONObject result = new JSONObject();
			Debug.println("type:" +type);
			Debug.println("name:" +name);
			Debug.println("payload:" +payload);
			store.create(type,name,payload);
			result.put("message", "ok");
			jsonResponse.setMessage(result);
		} catch (Exception e) {
			jsonResponse.setException("create failed " + e.getMessage(),e);
		}
		try {
			jsonResponse.print(response);
		} catch (Exception e1) {
		}
	}
	
	@RequestMapping(path="/validation/schema/{schemaId}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> getSchemaById(@PathVariable String schemaId) throws IOException {
		if("taf".equals(schemaId.toLowerCase())) {
			String schema = schemaStore.getLatestTafSchema();
			return ResponseEntity.status(HttpStatus.OK).body(schema);
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No or wrong ID given");
	}
	
	@RequestMapping(path="/validation/schema/{schemaId}", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_UTF8_VALUE, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> setSchemaContentById(@PathVariable String schemaId, @RequestBody String content) throws JsonProcessingException, IOException {
		System.out.println(schemaId + ": " + content);
		if("taf".equals(schemaId.toLowerCase())) {
			try {
				schemaStore.storeTafSchema(content);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			return ResponseEntity.status(HttpStatus.OK).body(null);
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No or wrong ID given");
	}
	@RequestMapping(path="/example_tafs/{id}", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_UTF8_VALUE, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void storeExampleTaf(HttpServletRequest req, HttpServletResponse response, @PathVariable int id, @RequestBody String payload) {
		Debug.println("updating taf");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			JSONObject result = new JSONObject();
			store.edit("taf", id, payload);
			result.put("message", "ok");
			jsonResponse.setMessage(result);

		} catch(Exception e) {
			jsonResponse.setException("create failed " + e.getMessage(),e);
		}
		try {
			jsonResponse.print(response);
		} catch (Exception e1) {
		}

	}

	@RequestMapping(path="/example_tafs/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void deleteExampleTaf(HttpServletRequest req, HttpServletResponse response, @PathVariable int id) {
		Debug.println("deleting taf");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			JSONObject result = new JSONObject();
			store.deleteByIndex("taf", id);
			result.put("message", "ok");
			jsonResponse.setMessage(result);

		} catch(Exception e) {
			jsonResponse.setException("create failed " + e.getMessage(),e);
		}
		try {
			jsonResponse.print(response);
		} catch (Exception e1) {
		}

	}
	
	@RequestMapping(path="/example_tafs", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_UTF8_VALUE, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void storeExampleTaf(HttpServletRequest req, HttpServletResponse response, @RequestBody String payload) {
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			JSONObject result = new JSONObject();
			long unixTime = System.currentTimeMillis() / 1000L;
			store.create("taf", String.format("example_taf_%d", unixTime), payload);
			result.put("message", "ok");
			jsonResponse.setMessage(result);

		} catch(Exception e) {
			jsonResponse.setException("create failed " + e.getMessage(),e);
		}
		try {
			jsonResponse.print(response);
		} catch (Exception e1) {
		}

	}
	
	@RequestMapping(path="/example_tafs", method=RequestMethod.GET,	produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void readExampleTafs(HttpServletRequest req, HttpServletResponse response) {
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			JSONObject result = new JSONObject();
			List<String> payload = store.readAll("taf", "example");
			System.out.println(payload);
			result.put("message", "ok");
			result.put("payload", payload);
			jsonResponse.setMessage(result);
		} catch (Exception e) {
			Debug.println("Failed");
			jsonResponse.setException("read failed",e);
		}
		try {
			jsonResponse.print(response);
		} catch(Exception e) {}
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

