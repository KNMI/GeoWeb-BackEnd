package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.knmi.adaguc.tools.MyXMLParser;

@Slf4j
@RestController
public class ServiceHelper {
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
		MappingJackson2HttpMessageConverter converter = 
				new MappingJackson2HttpMessageConverter(mapper);
		return converter;
	}
	@ResponseBody
	@RequestMapping("XML2JSON")
	public void XML2JSON(@RequestParam(value="request")String request,@RequestParam(value="callback", required=false)String callback, HttpServletResponse response){
		/**
		 * Converts XML file pointed with request to JSON file
		 * @param requestStr
		 * @param out1
		 * @param response
		 */
		log.debug("XML2JSON " + request);

		String requestStr;
		OutputStream out;
		try {
			out = response.getOutputStream();
		} catch (IOException e2) {
			log.error(e2.getMessage());
			return;
		}
		try {
			requestStr=URLDecoder.decode(request,"UTF-8");
			MyXMLParser.XMLElement rootElement = new MyXMLParser.XMLElement();
			//Remote XML2JSON request to external WMS service
			log.debug("Converting XML to JSON for " + requestStr);
			rootElement.parse(new URL(requestStr));
			if (callback==null) {
				response.setContentType("application/json");
				out.write(rootElement.toJSON(null).getBytes());
			} else {
				response.setContentType("application/javascript");
				out.write(callback.getBytes());
				out.write("(".getBytes());
				out.write(rootElement.toJSON(null).getBytes());
				out.write(");".getBytes());

			}
		} catch (Exception e) {
			log.error(e.getMessage());
			try {
				if (callback==null) {
					response.setContentType("application/json");
					out.write("{\"error\":\"error\"}".getBytes());
				} else {
					response.setContentType("application/javascript");
					out.write(callback.getBytes());
					out.write("(".getBytes());
					out.write("{\"error\":\"error\"}".getBytes());
					out.write(");".getBytes());

				}
			}catch (Exception e1) {
				response.setStatus(500);
			}
		}
	}
}

