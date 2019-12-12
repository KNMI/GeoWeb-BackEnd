package nl.knmi.geoweb.backend.product.airmet;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import nl.knmi.geoweb.backend.product.airmet.Airmet.Phenomenon;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetMovement;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetType;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestConfig.class})
public class AirmetStoreTest {
	@Autowired
	@Qualifier("airmetObjectMapper")
	private ObjectMapper airmetObjectMapper;

	@Value("${geoweb.products.storeLocation}")
	private String airmetStoreLocation;
	
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

	static String cloudAirmet="";
	static String windAirmet="";
	static String visibilityAirmet="";

	private Airmet getVisibilityAirmet() throws  IOException {
	  Airmet am=new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "vis-"+UUID.randomUUID().toString());
	  am.setStatus(SigmetAirmetStatus.concept);
	  am.setType(SigmetAirmetType.normal);
	  am.setPhenomenon(Phenomenon.SFC_VIS);
	  am.setVisibility(new Airmet.AirmetValue(2000, "m"));
	  List<ObscuringPhenomenonList.ObscuringPhenomenon> obs=new ArrayList<>();
	  obs.add(ObscuringPhenomenonList.of("DZ"));
	  am.setObscuring(obs);
	  am.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
	  am.setSequence(2);
	  OffsetDateTime st=OffsetDateTime.now(ZoneId.of("Z")).plusHours(1).truncatedTo(ChronoUnit.HOURS);
	  am.setValiddate(st);
	  am.setValiddate_end(st.plusHours(4));
	  am.setMovement_type(Airmet.AirmetMovementType.MOVEMENT);
	  am.setMovement(new SigmetAirmetMovement("N", 20, "KT"));
	  am.setGeojson(airmetObjectMapper.readValue(testGeoJson, GeoJsonObject.class));

	  return am;
	}

	private Airmet getWindAirmet() throws  IOException {
		Airmet am=new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "wind-"+UUID.randomUUID().toString());
		am.setPhenomenon(Phenomenon.SFC_WIND);
		am.setWind(new Airmet.AirmetWindInfo(20, 180));
		am.setStatus(SigmetAirmetStatus.concept);
		am.setType(SigmetAirmetType.normal);
		am.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
		am.setSequence(2);
		OffsetDateTime st=OffsetDateTime.now(ZoneId.of("Z")).plusHours(1).truncatedTo(ChronoUnit.HOURS);
		am.setValiddate(st);
		am.setValiddate_end(st.plusHours(4));
		am.setMovement_type(Airmet.AirmetMovementType.MOVEMENT);
		am.setMovement(new SigmetAirmetMovement("N", 20, "KT"));
		am.setGeojson(airmetObjectMapper.readValue(testGeoJson, GeoJsonObject.class));
		return am;
	}

	private Airmet getCloudAirmet() throws  IOException {
		Airmet am=new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "cloud-"+UUID.randomUUID().toString());
		am.setPhenomenon(Phenomenon.BKN_CLD);
		am.setCloudLevels(new Airmet.AirmetCloudLevelInfo(4900));
		am.setStatus(SigmetAirmetStatus.concept);
		am.setType(SigmetAirmetType.normal);
		am.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
		am.setSequence(2);
		OffsetDateTime st=OffsetDateTime.now(ZoneId.of("Z")).plusHours(1).truncatedTo(ChronoUnit.HOURS);
		am.setValiddate(st);
		am.setValiddate_end(st.plusHours(4));
		am.setMovement_type(Airmet.AirmetMovementType.MOVEMENT);
		am.setMovement(new SigmetAirmetMovement("N", 20, "KT"));
		am.setGeojson(airmetObjectMapper.readValue(testGeoJson, GeoJsonObject.class));
		return am;
	}

	@Test
	public void testVisibilityAirmet() throws Exception{
		Airmet am=getVisibilityAirmet();
		AirmetStore store=createNewStore();
		assertThat(store.getOM(),notNullValue());
		store.storeAirmet(am);

		Airmet windAirmet=getWindAirmet();
		store.storeAirmet(windAirmet);

		Airmet cloudAirmet=getCloudAirmet();
		store.storeAirmet(cloudAirmet);

		Airmet[] airmets=store.getAirmets(false, SigmetAirmetStatus.concept);
		assertThat(airmets.length, is(3));
		assertEquals(airmetObjectMapper.writeValueAsString(am), airmetObjectMapper.writeValueAsString(airmets[0]));
	}

	@Test
	public void contextLoads() throws Exception {
		assertThat(airmetObjectMapper,notNullValue());
	}
	
	public Airmet createAirmet () throws Exception {
		Airmet sm=new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "abcd");
		sm.setPhenomenon(Phenomenon.getPhenomenon("ISOL_CB"));
		sm.setValiddate(OffsetDateTime.now(ZoneId.of("Z")).minusHours(1));
//		sm.setChange(SigmetChange.NC);
		setGeoFromString(sm, testGeoJson);
		return sm;
	}
	
	public void setGeoFromString(Airmet am, String json) {
		log.trace("setGeoFromString " + json);
		GeoJsonObject geo;	
		try {
			geo = airmetObjectMapper.readValue(json, GeoJsonObject.class);
			log.debug("setGeoFromString [" + json + "] set");
			return;
		} catch (JsonParseException e) {
		} catch (JsonMappingException e) {
		} catch (IOException e) {
		}
		log.error("setGeoFromString on [" + json + "] failed");
///		am.setGeojson(null);
	}
	
	public void validateAirmet (Airmet sm) throws Exception {
		log.trace("Testing createAndCheckAirmet");
		log.debug(sm.getValiddate().toString());
		assertThat(sm.getPhenomenon().toString(), is("ISOL_CB"));
	}
	
	@Test 
	public void createAndValidateAirmet () throws Exception {
		Airmet sm = createAirmet();
		validateAirmet(sm);
	}

	@Autowired
	AirmetStore testAirmetStore;
	
	public AirmetStore createNewStore() throws IOException {
		Tools.rmdir(airmetStoreLocation+"/airmets");
		Tools.mksubdirs(airmetStoreLocation);
		testAirmetStore.setLocation(airmetStoreLocation);
		Airmet[] airmets=testAirmetStore.getAirmets(false, SigmetAirmetStatus.concept);
		assertThat(airmets.length, is(0));
		return testAirmetStore;
	}
	
	@Test
	public void saveOneAirmet () throws Exception {
		AirmetStore store=createNewStore();
		Airmet sm = createAirmet();
		assertThat(store.getOM(),notNullValue());
		
		store.storeAirmet(sm);
		assertThat(store.getAirmets(false, SigmetAirmetStatus.concept).length, is(1));
	}
	
	@Test
	public void loadAndValidateAirmet () throws Exception {
		AirmetStore store=createNewStore();
		Airmet sm = createAirmet();
		assertThat(store.getOM(),notNullValue());
		store.storeAirmet(sm);
		
		Airmet[] airmets=store.getAirmets(false, SigmetAirmetStatus.concept);
		assertThat(airmets.length, is(1));
		validateAirmet(airmets[0]);
	}
	
}
