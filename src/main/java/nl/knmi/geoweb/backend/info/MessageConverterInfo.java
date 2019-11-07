package nl.knmi.geoweb.backend.info;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageConverterInfo {
	@Value("${geoweb.messageConverterVersion}")
	private String messageConverterVersion;
	
	public String getMessageConverterVersion() {
		return messageConverterVersion;
	}

}
