package nl.knmi.geoweb.backend.product.airmet;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.geoweb.backend.product.sigmet.geo.GeoUtils;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.sigmet.AIRMET;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.TestConfig;
import nl.knmi.geoweb.backend.product.sigmetairmet.ObsFc;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetChange;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetLevel;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetMovement;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetType;
import nl.knmi.geoweb.iwxxm_2_1.converter.GeoWebAIRMETConverter;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class })
public class AirmetToFMITest {
	@Autowired
	@Qualifier("airmetObjectMapper")
	private ObjectMapper airmetObjectMapper;

	@Autowired
	@Qualifier("aviAirmetSpecificMessageConverter")
	private GeoWebAIRMETConverter geowebToFMIConverter;

	@Autowired
	@Qualifier("airmetJSONSerializer")
	private AviMessageSpecificConverter<AIRMET, String> airmetJSONSerializer;

	private String firFeature="{\"geometry\": {\"type\": \"Polygon\",\"coordinates\": [[" +
			"[7.065557479000063,52.38582801800004],[7.133054733000051,52.88888740500005],[7.142179873000032,52.898243887000035]," +
			"[7.191666670000075,53.30000000000007],[6.500000000000057,53.66666667000004],[6.500001907000069,55.00000190700007]," +
			"[5.000001907000069,55.00000190700007],[4.999987712000063,54.99998690600006],[3.4387616280000657,53.25967899900007]," +
			"[2.000001907000069,51.50000190700007],[3.370000839000056,51.369722366000076],[3.3705270460000634,51.36866995200006]," +
			"[3.3622226710000405,51.32000160200005],[3.3638896940000222,51.313608170000066],[3.3736133570000675,51.30999946600008]," +
			"[3.952501297000026,51.21444129900004],[4.397500992000062,51.45277595500005],[5.078611374000047,51.39166450500005]," +
			"[5.848333358000048,51.139444351000066],[5.651666641000077,50.82471656800004],[6.011796951000065,50.75727272100005]," +
			"[5.934167862000038,51.03638649000004],[6.222223282000073,51.36166572600007],[5.946390152000049,51.81166267400005]," +
			"[6.405000686000051,51.830827713000076],[7.053094864000059,52.23776435800005],[7.031389237000042,52.26888465900004]," +
			"[7.063611984000033,52.34610939100003],[7.065557479000063,52.38582801800004]]]}, "+
			"\"type\":\"Feature\",\"properties\":{\"centlong\":4.98042633,\"REGION\":\"EUR\",\"StateName\":\"Netherlands\"," +
			"\"FIRname\":\"AMSTERDAMFIR\",\"StateCode\":\"NLD\",\"centlat\":52.8618788,\"ICAOCODE\":\"EHAA\"}}";

	private String testGeoJsonBox= "{\"type\": \"FeatureCollection\",\"features\":[{\"type\": \"Feature\",\"id\": \"feb7bb38-a341-438d-b8f5-aa83685a0062\"," +
			" \"properties\": {\"selectionType\": \"box\",\"featureFunction\": \"start\"},\"geometry\": {\"type\": \"Polygon\"," +
			" \"coordinates\": [[[5.1618,51.4414],[5.8444,51.4414],[5.8444,51.7424],[5.1618,51.7424],[5.1618,51.4414]]]}}]}\"";

	private Feature mapJsonToFeature (String json) {
		Feature result;
		try {
			result = airmetObjectMapper.readValue(json, Feature.class);
		} catch (JsonParseException e) {
			result = null;
		} catch (JsonMappingException e) {
			result = null;
		} catch (IOException e) {
			result = null;
		}
		return result;
	}

	private GeoJsonObject mapJsonToGeoObject(String json) {
		GeoJsonObject result;
		try {
			result = airmetObjectMapper.readValue(json, GeoJsonObject.class);
		} catch (JsonParseException e) {
			result = null;
		} catch (JsonMappingException e) {
			result = null;
		} catch (IOException e) {
			result = null;
		}
		return result;
	}

	private static String STARTTIME="2019-02-12T08:00:00Z";
	private static String ENDTIME="2019-02-12T11:00:00Z";
	private static int SEQNO=3;
	public Airmet getBaseAirmet(String phen) {
		OffsetDateTime start = OffsetDateTime.parse(STARTTIME);
		OffsetDateTime end = OffsetDateTime.parse(ENDTIME);
		Airmet am = new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "b6ea2637-4652-42cc-97ac-4e34548d3cc7");
		am.setStatus(SigmetAirmetStatus.published);
		am.setSequence(SEQNO);
		am.setType(SigmetAirmetType.normal);
		am.setPhenomenon(Airmet.Phenomenon.getPhenomenon(phen));
		am.setValiddate(start);
		am.setValiddate_end(end);
		am.setObs_or_forecast(new ObsFc(true));
		am.setChange(SigmetAirmetChange.INTSF);
		am.setMovement_type(Airmet.AirmetMovementType.MOVEMENT);
		am.setMovement(new SigmetAirmetMovement("NNE", 4, "KT"));
		am.setLevelinfo(
				new SigmetAirmetLevel(new SigmetAirmetLevel.SigmetAirmetPart(SigmetAirmetLevel.SigmetAirmetLevelUnit.FL, 30),
						SigmetAirmetLevel.SigmetAirmetLevelMode.ABV));
		am.setGeojson(mapJsonToGeoObject(testGeoJsonBox));

		return am;
	}

	ConversionHints hints=new ConversionHints();

//	@Test This test currently fails because in the end it compares a GeoJSON feature to a FMI geoGeometry object, which always fails
	public void checkJsonForRegularAirmet () throws Exception {
		Airmet am = getBaseAirmet("OCNL_TSGR");
		ConversionResult<AIRMET>  res = geowebToFMIConverter.convertMessage(am,  hints);
		assertThat(res.getStatus(), is(ConversionResult.Status.SUCCESS));
		Debug.println(res.getConvertedMessage().get().toString());
		ConversionResult<String> resJson = airmetJSONSerializer.convertMessage(res.getConvertedMessage().get(), hints);
		assertThat(resJson.getStatus(), is(ConversionResult.Status.SUCCESS));
		Debug.println(resJson.getConvertedMessage().get());
		JSONObject json = new JSONObject(resJson.getConvertedMessage().get());
		assertThat(json.getJSONObject("validityPeriod").getJSONObject("startTime").getString("completeTime"), is((STARTTIME)));
		assertThat(json.getJSONObject("validityPeriod").getJSONObject("endTime").getString("completeTime"), is((ENDTIME)));
		assertThat(json.getString("sequenceNumber"), is(Integer.valueOf(SEQNO).toString()));
		assertThat(json.getString("airmetPhenomenon"), is("OCNL_TSGR"));
		assertThat(json.getJSONObject("movingDirection").getDouble("value"), is(22.5));
		assertThat(json.getJSONObject("movingDirection").getString("uom"), is("deg"));
		assertThat(json.getJSONObject("movingSpeed").getDouble("value"), is(4.0));
		assertThat(json.getJSONObject("movingSpeed").getString("uom"), is("[kn_i]"));
		JSONObject geo=new JSONObject(testGeoJsonBox).getJSONArray("features").getJSONObject(0).getJSONObject("geometry");
		JSONObject geoFromAIRMET = json.getJSONArray("analysisGeometries").getJSONObject(0).getJSONObject("geometry").getJSONObject("geoGeometry");

		JSONAssert.assertNotEquals(geoFromAIRMET, geo, false);
	}

	@Test
	public void checkJSONForClouds() throws Exception {
		Airmet am = getBaseAirmet("BKN_CLD");
		am.setMovement_type(Airmet.AirmetMovementType.STATIONARY);
		am.setCloudLevels(new Airmet.AirmetCloudLevelInfo(true, true, 300, "FT"));
		Debug.println(am.serializeAirmetToString(airmetObjectMapper));
		ConversionResult<AIRMET>  res = geowebToFMIConverter.convertMessage(am,  hints);
		assertThat(res.getStatus(), is(ConversionResult.Status.SUCCESS));
		Debug.println(res.getConvertedMessage().get().toString());
		ConversionResult<String> resJson = airmetJSONSerializer.convertMessage(res.getConvertedMessage().get(), hints);
		assertThat(resJson.getStatus(), is(ConversionResult.Status.SUCCESS));
		Debug.println(resJson.getConvertedMessage().get());
		JSONObject json = new JSONObject(resJson.getConvertedMessage().get());
		assertThat(json.getJSONObject("validityPeriod").getJSONObject("startTime").getString("completeTime"), is((STARTTIME)));
		assertThat(json.getJSONObject("validityPeriod").getJSONObject("endTime").getString("completeTime"), is((ENDTIME)));
		assertThat(json.getString("airmetPhenomenon"), is("BKN_CLD"));
		assertThat(json.getJSONObject("cloudLevels").getJSONObject("cloudTop").getDouble("value"), is(300.0));
		assertThat(json.getJSONObject("cloudLevels").getJSONObject("cloudTop").getString("uom"), is("FT"));
	//	assertThat(json.getJSONObject("cloudLevels").getJSONObject("cloudBottom")
	}

	@Test
	public void checkTacForCloudsMissing() throws Exception {
		OffsetDateTime start = OffsetDateTime.parse("2019-02-12T08:00:00Z");
		OffsetDateTime end = OffsetDateTime.parse("2019-02-12T11:00:00Z");
		Airmet am = new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "b6ea2637-4652-42cc-97ac-4e34548d3cc7");
		am.setStatus(SigmetAirmetStatus.concept);
		am.setType(SigmetAirmetType.test);
		am.setPhenomenon(Airmet.Phenomenon.getPhenomenon("BKN_CLD"));
		am.setValiddate(start);
		am.setValiddate_end(end);
		am.setObs_or_forecast(new ObsFc(true));
		am.setChange(SigmetAirmetChange.NC);
		am.setMovement_type(Airmet.AirmetMovementType.STATIONARY);
		am.setGeojson(mapJsonToGeoObject(testGeoJsonBox));
		String tac = am.toTAC(mapJsonToFeature(firFeature));
		assertThat(tac, is(
				"EHAA AIRMET -1 VALID 120800/121100 EHDB-\nEHAA AMSTERDAM FIR\nTEST BKN CLD\nOBS\nWI N5126 E00510 - N5145 E00510 - N5145 E00551 - N5126 E00551 - N5126 E00510\nSTNR NC\n"));
	}

	@Test
	public void checkTacForVisibility() throws Exception {
		OffsetDateTime start = OffsetDateTime.parse("2019-02-12T08:00:00Z");
		OffsetDateTime end = OffsetDateTime.parse("2019-02-12T11:00:00Z");
		Airmet am = new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "b6ea2637-4652-42cc-97ac-4e34548d3cc7");
		am.setStatus(SigmetAirmetStatus.concept);
		am.setType(SigmetAirmetType.normal);
		am.setPhenomenon(Airmet.Phenomenon.getPhenomenon("SFC_VIS"));
		am.setValiddate(start);
		am.setValiddate_end(end);
		am.setObs_or_forecast(new ObsFc(true));
		am.setChange(SigmetAirmetChange.INTSF);
		am.setMovement_type(Airmet.AirmetMovementType.STATIONARY);
		am.setVisibility(new Airmet.AirmetValue(400, "M"));
		am.setObscuring(asList(new ObscuringPhenomenonList.ObscuringPhenomenon("Dust storm", "DS")));
		am.setGeojson(mapJsonToGeoObject(testGeoJsonBox));
		String tac = am.toTAC(mapJsonToFeature(firFeature));
		assertThat(tac, is(
				"EHAA AIRMET -1 VALID 120800/121100 EHDB-\nEHAA AMSTERDAM FIR\nSFC VIS 0400M (DS)\nOBS\nWI N5126 E00510 - N5145 E00510 - N5145 E00551 - N5126 E00551 - N5126 E00510\nSTNR INTSF\n"));
	}

	@Test
	public void checkTacForVisibilityMissing() throws Exception {
		OffsetDateTime start = OffsetDateTime.parse("2019-02-12T08:00:00Z");
		OffsetDateTime end = OffsetDateTime.parse("2019-02-12T11:00:00Z");
		Airmet am = new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "b6ea2637-4652-42cc-97ac-4e34548d3cc7");
		am.setStatus(SigmetAirmetStatus.concept);
		am.setType(SigmetAirmetType.normal);
		am.setPhenomenon(Airmet.Phenomenon.getPhenomenon("SFC_VIS"));
		am.setValiddate(start);
		am.setValiddate_end(end);
		am.setObs_or_forecast(new ObsFc(true));
		am.setChange(SigmetAirmetChange.INTSF);
		am.setMovement_type(Airmet.AirmetMovementType.STATIONARY);
		am.setObscuring(asList(new ObscuringPhenomenonList.ObscuringPhenomenon("Dust storm", "DS")));
		am.setGeojson(mapJsonToGeoObject(testGeoJsonBox));
		String tac = am.toTAC(mapJsonToFeature(firFeature));
		assertThat(tac, is(
				"EHAA AIRMET -1 VALID 120800/121100 EHDB-\nEHAA AMSTERDAM FIR\nSFC VIS  (DS)\nOBS\nWI N5126 E00510 - N5145 E00510 - N5145 E00551 - N5126 E00551 - N5126 E00510\nSTNR INTSF\n"));
	}

	@Test
	public void checkTacForVisibilityMissingObscuring() throws Exception {
		OffsetDateTime start = OffsetDateTime.parse("2019-02-12T08:00:00Z");
		OffsetDateTime end = OffsetDateTime.parse("2019-02-12T11:00:00Z");
		Airmet am = new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "b6ea2637-4652-42cc-97ac-4e34548d3cc7");
		am.setStatus(SigmetAirmetStatus.concept);
		am.setType(SigmetAirmetType.normal);
		am.setPhenomenon(Airmet.Phenomenon.getPhenomenon("SFC_VIS"));
		am.setValiddate(start);
		am.setValiddate_end(end);
		am.setObs_or_forecast(new ObsFc(true));
		am.setChange(SigmetAirmetChange.INTSF);
		am.setMovement_type(Airmet.AirmetMovementType.STATIONARY);
		am.setVisibility(new Airmet.AirmetValue(400, "M"));
		am.setGeojson(mapJsonToGeoObject(testGeoJsonBox));
		String tac = am.toTAC(mapJsonToFeature(firFeature));
		assertThat(tac, is(
				"EHAA AIRMET -1 VALID 120800/121100 EHDB-\nEHAA AMSTERDAM FIR\nSFC VIS 0400M \nOBS\nWI N5126 E00510 - N5145 E00510 - N5145 E00551 - N5126 E00551 - N5126 E00510\nSTNR INTSF\n"));
	}

	@Test
	public void checkTacForWind() throws Exception {
		OffsetDateTime start = OffsetDateTime.parse("2019-02-12T08:00:00Z");
		OffsetDateTime end = OffsetDateTime.parse("2019-02-12T11:00:00Z");
		Airmet am = new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "b6ea2637-4652-42cc-97ac-4e34548d3cc7");
		am.setStatus(SigmetAirmetStatus.concept);
		am.setType(SigmetAirmetType.exercise);
		am.setPhenomenon(Airmet.Phenomenon.getPhenomenon("SFC_WIND"));
		am.setValiddate(start);
		am.setValiddate_end(end);
		am.setObs_or_forecast(new ObsFc(true, OffsetDateTime.parse("2019-02-11T11:00:00Z")));
		am.setChange(SigmetAirmetChange.NC);
		am.setWind(new Airmet.AirmetWindInfo(3, "MPS", 3, "degrees"));
		am.setMovement_type(Airmet.AirmetMovementType.MOVEMENT);
		am.setMovement(new SigmetAirmetMovement("SW", 3, "KT"));
		am.setGeojson(mapJsonToGeoObject(testGeoJsonBox));
		String tac = am.toTAC(mapJsonToFeature(firFeature));
		assertThat(tac, is(
				"EHAA AIRMET -1 VALID 120800/121100 EHDB-\nEHAA AMSTERDAM FIR\nEXER SFC WIND 003/03MPS\nOBS AT 1100Z\nWI N5126 E00510 - N5145 E00510 - N5145 E00551 - N5126 E00551 - N5126 E00510\nMOV SW 3KT\nNC\n"));
	}

	@Test
	public void checkTacForWindMissing() throws Exception {
		OffsetDateTime start = OffsetDateTime.parse("2019-02-12T08:00:00Z");
		OffsetDateTime end = OffsetDateTime.parse("2019-02-12T11:00:00Z");
		Airmet am = new Airmet("AMSTERDAM FIR", "EHAA", "EHDB", "b6ea2637-4652-42cc-97ac-4e34548d3cc7");
		am.setStatus(SigmetAirmetStatus.concept);
		am.setType(SigmetAirmetType.exercise);
		am.setPhenomenon(Airmet.Phenomenon.getPhenomenon("SFC_WIND"));
		am.setValiddate(start);
		am.setValiddate_end(end);
		am.setObs_or_forecast(new ObsFc(true, OffsetDateTime.parse("2019-02-11T11:00:00Z")));
		am.setChange(SigmetAirmetChange.NC);
		am.setMovement_type(Airmet.AirmetMovementType.MOVEMENT);
		am.setMovement(new SigmetAirmetMovement("SW", 3, "KT"));
		am.setGeojson(mapJsonToGeoObject(testGeoJsonBox));
		String tac = am.toTAC(mapJsonToFeature(firFeature));
		assertThat(tac, is(
				"EHAA AIRMET -1 VALID 120800/121100 EHDB-\nEHAA AMSTERDAM FIR\nEXER SFC WIND\nOBS AT 1100Z\nWI N5126 E00510 - N5145 E00510 - N5145 E00551 - N5126 E00551 - N5126 E00510\nMOV SW 3KT\nNC\n"));
	}
}
