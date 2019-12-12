package nl.knmi.geoweb.backend.product.sigmet;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.GeoJsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.TestConfig;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.Phenomenon;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetChange;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetLevel;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class })
public class SigmetToTACTest {
	@Autowired
	@Qualifier("sigmetObjectMapper")
	private ObjectMapper sigmetObjectMapper;
	
	@Value("${geoweb.products.storeLocation}")
	private String sigmetStoreLocation;

	@Autowired
	private FIRStore firStore;

	@Autowired
	private SigmetStore testSigmetStore;
	
	static String testGeoJson="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]},\"properties\":{}}]}";

	static String testGeoJson1="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,52],[0,60],[10,60],[10,52],[0,52]]]}, \"properties\":{\"featureFunction\":\"start\", \"selectionType\":\"box\"} }]}";

	static String testGeoJson2="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,52],[0,60],[5,60],[5,52],[0,52]]]}, \"properties\":{\"featureFunction\":\"start\", \"selectionType\":\"box\"} }]}";
	
	static String testGeoJson3="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,52],[0,54],[10,54],[10,52],[0,52]]]}, \"properties\":{\"featureFunction\":\"start\", \"selectionType\":\"box\"} }]}";
	
	static String testGeoJson4="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[0,52]}, \"properties\":{\"featureFunction\":\"start\", \"selectionType\":\"point\"} }]}";
	
	static String testGeoJson5="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\", \"properties\":{\"featureFunction\":\"start\", \"selectionType\":\"fir\"} }]}";

	static String testSigmet="{\"geojson\":"
			+"{\"type\":\"FeatureCollection\",\"features\":"+"[{\"type\":\"Feature\",\"properties\":{\"prop0\":\"value0\",\"prop1\":{\"this\":\"that\"}},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]}}]},"
			+"\"phenomenon\":\"OBSC_TS\","
			+"\"obs_or_forecast\":{\"obs\":true},"
			+"\"level\":{\"lev1\":{\"value\":100.0,\"unit\":\"FL\"}},"
			+"\"movement_type\":\"stationary\","
			+"\"change\":\"NC\","
			+"\"issuedate\":\"2017-03-24T15:56:16Z\","
			+"\"validdate\":\"2017-03-24T15:56:16Z\","
			+"\"firname\":\"AMSTERDAM FIR\","
			+"\"location_indicator_icao\":\"EHAA\","
			+"\"location_indicator_mwo\":\"EHDB\"}";
	
	@Test
	public void contextLoads() throws Exception {
		assertThat(sigmetObjectMapper,notNullValue());
	}
	
	public Sigmet createSigmet (String s) throws Exception {
		Sigmet sm=new Sigmet("AMSTERDAM FIR", "EHAA", "EHDB", "abcd");
		sm.setPhenomenon(Phenomenon.getPhenomenon("OBSC_TS"));
		sm.setValiddate(OffsetDateTime.now(ZoneId.of("Z")).minusHours(1));
		sm.setValiddate_end(OffsetDateTime.now(ZoneId.of("Z")).plusHours(3));
		sm.setChange(SigmetAirmetChange.NC);
		sm.setMovement_type(Sigmet.SigmetMovementType.STATIONARY);
		sm.setLevelinfo(new SigmetAirmetLevel(new SigmetAirmetLevel.SigmetAirmetPart(SigmetAirmetLevel.SigmetAirmetLevelUnit.FL, 300),
				SigmetAirmetLevel.SigmetAirmetLevelMode.TOPS_ABV));
		setGeoFromString(sm, s);
		return sm;
	}
	
	public void setGeoFromString(Sigmet sm, String json) {
		log.trace("setGeoFromString "+json);
		GeoJsonObject geo;	
		try {
			geo = sigmetObjectMapper.readValue(json, GeoJsonObject.class);
			sm.setGeojson(geo);
			log.debug("setGeoFromString [" + json + "] set");
			return;
		} catch (JsonParseException e) {
		} catch (JsonMappingException e) {
		} catch (IOException e) {
		}
		log.error("setGeoFromString on [" + json + "] failed");
		sm.setGeojson(null);
	}
	
	public void validateSigmet (Sigmet sm) throws Exception {
		log.trace("Testing createAndCheckSigmet");
		log.error(sm.getValiddate().toString());
		assertThat(sm.getPhenomenon().toString(), is("OBSC_TS"));
	}
	
	@Test 
	public void createAndValidateSigmet () throws Exception {
		Sigmet sm = createSigmet(testGeoJson1);
		validateSigmet(sm);
	}
	
	public SigmetStore createNewStore() throws IOException {
		Tools.rmdir(sigmetStoreLocation+"/sigmets");
		Tools.mksubdirs(sigmetStoreLocation);
		testSigmetStore.setLocation(sigmetStoreLocation);
		Sigmet[] sigmets=testSigmetStore.getSigmets(false, SigmetAirmetStatus.concept);
		assertThat(sigmets.length, is(0));
		return testSigmetStore;
	}
	
	@Test
	public void saveOneSigmet () throws Exception {
		SigmetStore store=createNewStore();
		Sigmet sm = createSigmet(testGeoJson1);
		assertThat(store.getOM(),notNullValue());
		
		store.storeSigmet(sm);
		assertThat(store.getSigmets(false, SigmetAirmetStatus.concept).length, is(1));
	}
	
	@Test
	public void loadAndValidateSigmet () throws Exception {
		SigmetStore store=createNewStore();
		Sigmet sm = createSigmet(testGeoJson5);
		assertThat(store.getOM(),notNullValue());
		store.storeSigmet(sm);
		
		Sigmet[] sigmets=store.getSigmets(false, SigmetAirmetStatus.concept);
		assertThat(sigmets.length, is(1));
		validateSigmet(sigmets[0]);
		log.debug("SIGMET: " + sigmets[0].toString());
		log.debug("  TAC:" + sigmets[0].toTAC(firStore.lookup("EHAA", true)));
	}
	
}
