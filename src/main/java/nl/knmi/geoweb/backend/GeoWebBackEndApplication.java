package nl.knmi.geoweb.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GeoWebBackEndApplication implements ApplicationRunner {

	@Value("${geoweb.backendVersion}")
	private String backendVersion;

	@Value("${geoweb.messageConverterVersion}")
	private String messageConverterVersion;

	private static final Logger log = LoggerFactory.getLogger(GeoWebBackEndApplication.class);

	public static void main(String[] args) throws Exception {
		SpringApplication.run(GeoWebBackEndApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("Version BackEnd: " + backendVersion);
		log.info("Version MessageConverter: " + messageConverterVersion);
	}
}
