package nl.knmi.geoweb.iwxxm_2_1.converter;

import java.io.IOException;

import org.geojson.GeoJsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.TestConfig;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.product.airmet.Airmet;
import nl.knmi.geoweb.backend.product.airmet.converter.AirmetConverter;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class })
public class AirmetToIWXXMTest {
	@Autowired
	@Qualifier("airmetObjectMapper")
	private ObjectMapper airmetObjectMapper;

	@Autowired
	private AirmetConverter airmetConverter;

	@Autowired
	private FIRStore firStore;

	static String[] testAirmets= new String[] {
			getStringFromFile("nl/knmi/geoweb/iwxxm_2_1/converter/testairmet1.json"),
			getStringFromFile("nl/knmi/geoweb/iwxxm_2_1/converter/testairmet2.json"),
			getStringFromFile("nl/knmi/geoweb/iwxxm_2_1/converter/cnltestairmet2.json")
	};

	public static String getStringFromFile(String fn) {
		try {
			return Tools.readResource(fn);
		} catch (IOException e) {
			log.error("Can't read resource " + fn);
		}
		return "";
    }

	public void setGeoFromString2(Airmet am, String json) {
		GeoJsonObject geo;
		try {
			geo = airmetObjectMapper.readValue(json, GeoJsonObject.class);
			am.setGeojson(geo);
			return;
		} catch (JsonParseException e) {
		} catch (JsonMappingException e) {
		} catch (IOException e) {
		}
		log.error("setGeoFromString on ["+json+"] failed");
		am.setGeojson(null);
	}

	public void TestConversion(String s) {
		Airmet am = null;
		try {
			am=airmetObjectMapper.readValue(s, Airmet.class);
		} catch (Exception e) {
			log.error(e.getMessage());
		}


		String res=airmetConverter.ToIWXXM_2_1(am);
		log.debug(res);
		log.debug("TAC: " + am.toTAC(firStore.lookup(am.getFirname(), true)));
	}

	@Test
	public void TestConversions(){
		for (String am: testAirmets) {
			TestConversion(am);
		}
	}

}
