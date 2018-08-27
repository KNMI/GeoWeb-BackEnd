package nl.knmi.geoweb.backend.info;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BackendInfo {
	@Value("${info.version}")
	private String backendVersion;
	
	public String getBackendVersion() {
		return backendVersion;
	}

}
