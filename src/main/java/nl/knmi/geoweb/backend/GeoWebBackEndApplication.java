package nl.knmi.geoweb.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;

import nl.knmi.adaguc.tools.Debug;
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan({"nl.knmi.geoweb.backend"})
public class GeoWebBackEndApplication extends SpringBootServletInitializer {
	
	@Value("${info.version}")
	private String infoVersion;

	@RequestMapping(path="/")
	String home() {
		Debug.println(infoVersion);
		return "GeoWeb Backend version [" + infoVersion + "]";
	}

	public static void main(String[] args) {
		configureApplication(new SpringApplicationBuilder()).run(args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application)  {
		Debug.println(infoVersion);
		return application.sources(GeoWebBackEndApplication.class).properties();
	}

	private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder){
		return builder.sources(GeoWebBackEndApplication.class).bannerMode(Banner.Mode.OFF);
	}
}
