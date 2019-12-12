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
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.Phenomenon;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetChange;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class })
public class SigmetStoreTest {
	@Autowired
	@Qualifier("sigmetObjectMapper")
	private ObjectMapper sigmetObjectMapper;
	
	@Value("${geoweb.products.storeLocation}")
	private String sigmetStoreLocation;
	
	static String testGeoJson="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]},\"properties\":{\"prop0\":\"value0\",\"prop1\":{\"this\":\"that\"}}}]}";

	static String testSigmet="{\"geojson\":"
			+"{\"type\":\"FeatureCollection\",\"features\":"+"[{\"type\":\"Feature\",\"properties\":{\"prop0\":\"value0\",\"prop1\":{\"this\":\"that\"}},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]}}]},"
			+"\"phenomenon\":\"OBSC_TS\","
			+"\"obs_or_forecast\":{\"obs\":true},"
			+"\"level\":{\"lev1\":{\"value\":100.0,\"unit\":\"FL\"}},"
			+"\"movement_type\":\"STATIONARY\","
			+"\"movement\":{\"stationary\":true},"
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
	
	public Sigmet createSigmet () throws Exception {
		Sigmet sm=new Sigmet("AMSTERDAM FIR", "EHAA", "EHDB", "abcd");
		sm.setPhenomenon(Phenomenon.getPhenomenon("OBSC_TS"));
		sm.setValiddate(OffsetDateTime.now(ZoneId.of("Z")).minusHours(1));
		sm.setChange(SigmetAirmetChange.NC);
		setGeoFromString(sm, testGeoJson);
		return sm;
	}
	
	public void setGeoFromString(Sigmet sm, String json) {
		log.trace("setGeoFromString " + json);
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
		log.debug(sm.getValiddate().toString());
		assertThat(sm.getPhenomenon().toString(), is("OBSC_TS"));
	}
	
	@Test 
	public void createAndValidateSigmet () throws Exception {
		Sigmet sm = createSigmet();
		validateSigmet(sm);
	}

	@Autowired
	SigmetStore testSigmetStore;
	
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
		Sigmet sm = createSigmet();
		assertThat(store.getOM(),notNullValue());
		
		store.storeSigmet(sm);
		assertThat(store.getSigmets(false, SigmetAirmetStatus.concept).length, is(1));
	}
	
	@Test
	public void loadAndValidateSigmet () throws Exception {
		SigmetStore store=createNewStore();
		Sigmet sm = createSigmet();
		assertThat(store.getOM(),notNullValue());
		store.storeSigmet(sm);
		
		Sigmet[] sigmets=store.getSigmets(false, SigmetAirmetStatus.concept);
		assertThat(sigmets.length, is(1));
		validateSigmet(sigmets[0]);
	}
	
}
