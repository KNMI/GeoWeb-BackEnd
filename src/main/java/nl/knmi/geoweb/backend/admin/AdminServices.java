package nl.knmi.geoweb.backend.admin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.tools.HTTPTools;
import nl.knmi.adaguc.tools.JSONResponse;
import nl.knmi.geoweb.backend.product.taf.TafSchemaStore;

@RestController
@RequestMapping(path={"/admin", "/store"})
public class AdminServices {
	private static final Logger LOGGER = LoggerFactory.getLogger(AdminServices.class);

	AdminStore adminStore ;
	TafSchemaStore tafSchemaStore;
	
	@Autowired
	@Qualifier("geoWebObjectMapper")
    private ObjectMapper objectMapper;
	
	//Grab feedback config info from global config
	@Value("${geoweb.feedback.feedbackfromname}")
	private String feedbackFromName;
	@Value("${geoweb.feedback.feedbackfrommail}")
	private String feedbackFromMail;
	@Value("${geoweb.feedback.feedbackmail}")
	private String feedbackMail;

	@Autowired
	JavaMailSender emailSender;

	@Autowired
	public AdminServices (final AdminStore adminStore, final TafSchemaStore tafSchemaStore) throws IOException {
		this.adminStore = adminStore;
		this.tafSchemaStore = tafSchemaStore;
	}



	@RequestMapping(path="/receiveFeedback", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void distributeFeedback(HttpServletRequest req, HttpServletResponse response, @RequestBody String payload) throws MessagingException, UnsupportedEncodingException {
		JsonNode json_payload = null;
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			json_payload = objectMapper.readTree(payload);
		} catch (IOException e) {
			e.printStackTrace();
			jsonResponse.setException("Failed to read payload", e);
		}
		String version = json_payload.at("/config/version").asText();
		String desc_short = json_payload.at("/descriptions/problemSummary").asText();
		String desc_long = json_payload.at("/descriptions/problemDescription").asText();
		String role = json_payload.at("/descriptions/role").asText();
		String whom = json_payload.at("/descriptions/feedbackName").asText();

		String subject;
		if (whom != null && whom.length() > 0) {
			subject = "[" + role + "] Geoweb " + version + " feedback van " + whom + ": " + desc_short;
		} else { 
			subject = "[" + role + "] Geoweb " + version + " feedback: " + desc_short;
		}

		String content = "Hallo Geowebbers,\n\nEr is feedback binnengekomen van " + (whom != null && whom.length() > 0 ? whom : "anonymous👤") + ".\nHet probleem was \"" + desc_long + "\".\n\nGroetjes,\nDe Geoweb feedback verzamlaar.";
		MimeMessage message = emailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);
		helper.setSubject(subject);
		helper.setText(content);
		helper.setTo(feedbackMail);
		helper.setFrom(new InternetAddress(feedbackFromMail, feedbackFromName));
		helper.addAttachment("debug_info.json", new ByteArrayResource(json_payload.toString().getBytes(StandardCharsets.UTF_8.name())));
		emailSender.send(message);

		try {
			jsonResponse.print(response);
		} catch (Exception e1) {
		}
	}

	@RequestMapping(path = "/create", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void createConfigurationItem(HttpServletRequest req, HttpServletResponse response, @RequestBody String payload) throws JsonProcessingException {
		LOGGER.debug("admin/create");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			String type = HTTPTools.getHTTPParam(req, "type");
			String name = HTTPTools.getHTTPParam(req, "name");
			JSONObject result = new JSONObject();
			LOGGER.debug("type:{}", type);
			LOGGER.debug("name:{}", name);
			LOGGER.debug("payload:{}", payload);
			adminStore.create(type,name,payload);
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
			String schema = tafSchemaStore.getLatestTafSchema();
			return ResponseEntity.status(HttpStatus.OK).body(schema);
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No or wrong ID given");
	}

	@RequestMapping(path="/validation/schema/{schemaId}", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_UTF8_VALUE, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> setSchemaContentById(@PathVariable String schemaId, @RequestBody String content) throws JsonProcessingException, IOException {
		LOGGER.debug("{}: {}", schemaId, content);
		if("taf".equals(schemaId.toLowerCase())) {
			try {
				tafSchemaStore.storeTafSchema(content, objectMapper);
			} catch(Exception e) {
				e.printStackTrace();
			}

			return ResponseEntity.status(HttpStatus.OK).body(null);
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No or wrong ID given");
	}
	@RequestMapping(path="/example_tafs/{id}", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_UTF8_VALUE, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void storeExampleTaf(HttpServletRequest req, HttpServletResponse response, @PathVariable int id, @RequestBody String payload) {
		LOGGER.debug("updating taf");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			JSONObject result = new JSONObject();
			adminStore.edit("taf", id, payload);
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
		LOGGER.debug("deleting taf");
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			JSONObject result = new JSONObject();
			adminStore.deleteByIndex("taf", id);
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
			adminStore.create("taf", String.format("example_taf_%d", unixTime), payload);
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
			List<String> payload = adminStore.readAll("taf", "example");
			LOGGER.debug("{}", payload);
			result.put("message", "ok");
			result.put("payload", payload);
			jsonResponse.setMessage(result);
		} catch (Exception e) {
			LOGGER.debug("Failed");
			jsonResponse.setException("read failed",e);
		}
		try {
			jsonResponse.print(response);
		} catch(Exception e) {}
	}


	@RequestMapping(path = "/read", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void readConfigurationItem(HttpServletRequest req, HttpServletResponse response) throws JsonProcessingException {
		JSONResponse jsonResponse = new JSONResponse(req);
		try {
			String type = HTTPTools.getHTTPParam(req, "type");
			String name = HTTPTools.getHTTPParam(req, "name");
			JSONObject result = new JSONObject();
			LOGGER.debug("admin/read type{} and name {}", type, name);

			String payload = adminStore.read(type,name);
			result.put("message", "ok");
			result.put("payload", new JSONArray(payload));
			jsonResponse.setMessage(result);
		} catch (Exception e) {
			LOGGER.error("Failed to read {}", e.getMessage());
			jsonResponse.setException("read failed",e);
		}

		try {
			jsonResponse.print(response);
		} catch (Exception e1) {
		}
	}


}

