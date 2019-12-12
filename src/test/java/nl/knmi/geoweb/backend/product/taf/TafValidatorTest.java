package nl.knmi.geoweb.backend.product.taf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.text.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import org.json.JSONException;
import org.json.JSONObject;
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

// FIXME: [entire file] TafSchemaStore and TafValidator are re-instantiated for each test. This is not desirable (performance, breaks dependency injection by Spring).
// It needs to be refactored in a way that there will be only a single instance used multiple times.

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class })
public class TafValidatorTest {

	@Value(value = "${geoweb.products.storeLocation}")
	String productstorelocation;

	@Autowired
	@Qualifier("tafObjectMapper")
	private ObjectMapper tafObjectMapper;

	@Test
	public void testValidateOK() throws Exception {
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);

		String taf = Tools.readResource("Taf_valid.json");

		JSONObject tafAsJSON = new JSONObject(taf);
		TafValidationResult report = tafValidator.validate(tafAsJSON.toString());
		assertThat(report.isSucceeded(), is(true));
	}

	@Test
	public void testValidateFails() throws IOException, JSONException, ProcessingException, ParseException {
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);

		String taf = Tools.readResource("./Taf_invalid.json");
		JSONObject tafAsJSON = new JSONObject(taf);
		TafValidationResult report = tafValidator.validate(tafAsJSON.toString());
		assertThat(report.isSucceeded(), is(false));
	}

	/* Tests if prob30 change in wind difference is more than 5 knots */
	@Test
	public void testValidate_test_taf_change_in_wind_enough_difference() throws Exception {
		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":120,\"speed\":40,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T12:00:00Z\",\"changeEnd\":\"2018-06-18T14:00:00Z\",\"changeType\":\"PROB30\",\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":120,\"speed\":40,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		log.error(report.getErrors().asText());
		assertThat(report.getErrors().toString(), is("{\"/changegroups/0/forecast/wind/windEnoughDifference\":"
				+ "[\"Change in wind must be at least 30 degrees in direction or 5 knots (2 MPS) in speed or gust\"]}"));
		assertThat(report.isSucceeded(), is(false));
	}

	/* Tests speedOperator and gustsOperator */
	@Test
	public void testValidate_test_taf_speedOperator_and_gustOperator() throws Exception {

		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":100,\"speed\":20,\"gusts\":31,\"gustsOperator\":\"above\",\"speedOperator\":\"above\",\"unit\":\"MPS\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/*
	 * Tests Changegroups: changegroup with gust and previous group without gust
	 */
	@Test
	public void testValidate_test_taf_changeGroup_with_gust_should_validateOK() throws Exception {

		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":200,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T12:00:00Z\",\"changeEnd\":\"2018-06-18T15:00:00Z\",\"changeType\":\"BECMG\",\"forecast\":{\"wind\":{\"direction\":200,\"speed\":22,\"gusts\":36,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/*
	 * Tests Changegroups: changegroup without gust and previous group with gust
	 */
	@Test
	public void testValidate_test_taf_changeGroup_without_gust_should_validateOK() throws Exception {

		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":200,\"speed\":20,\"gusts\":36,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T12:00:00Z\",\"changeEnd\":\"2018-06-18T15:00:00Z\",\"changeType\":\"BECMG\",\"forecast\":{\"wind\":{\"direction\":200,\"speed\":22,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/* Tests validation should simply not crash on this one: One */
	@Test
	public void testValidate_test_taf_validation_should_not_crash_1() throws Exception {
		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":200,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T16:00:00Z\",\"changeEnd\":\"2018-06-18T20:00:00Z\",\"changeType\":\"BECMG\",\"forecast\":{\"wind\":{\"direction\":200,\"speed\":22,\"gusts\":30,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(false));
	}

	/* Tests validation should simply not crash on this one: Two */
	@Test
	public void testValidate_test_taf_validation_should_not_crash_2() throws Exception {
		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":200,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T16:00:00Z\",\"changeEnd\":\"2018-06-18T20:00:00Z\",\"changeType\":\"BECMG\",\"forecast\":{\"wind\":{\"direction\":200,\"speed\":22,\"gusts\":37,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/*
	 * Tests Changegroups: changegroup and previous group with gust and less then 5 knots of gust difference.
	 */
	@Test
	public void testValidate_test_taf_groups_with_gust_difference_less_then_5_should_validateOK() throws Exception {

		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":200,\"speed\":20,\"gusts\":30,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T12:00:00Z\",\"changeEnd\":\"2018-06-18T15:00:00Z\",\"changeType\":\"BECMG\",\"forecast\":{\"wind\":{\"direction\":200,\"speed\":20,\"gusts\":33,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		log.error(report.getErrors().asText());
		assertThat(report.getErrors().toString(), is("{\"/changegroups/0/forecast/wind/windEnoughDifference\":"
				+ "[\"Change in wind must be at least 30 degrees in direction or 5 knots (2 MPS) in speed or gust\"]}"));
		assertThat(report.isSucceeded(), is(false));
	}

	/*
	 * Tests Changegroups: changegroup and previous group with gust and more then 5 knots of gust difference.
	 */
	@Test
	public void testValidate_test_taf_groups_with_gust_difference_more_then_5_should_validateOK() throws Exception {

		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":200,\"speed\":20,\"gusts\":30,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T12:00:00Z\",\"changeEnd\":\"2018-06-18T15:00:00Z\",\"changeType\":\"BECMG\",\"forecast\":{\"wind\":{\"direction\":200,\"speed\":20,\"gusts\":37,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/*
	 * Tests Changegroups: changegroup and previous group with gust and less then 5 knots of speed difference.
	 */
	@Test
	public void testValidate_test_taf_groups_with_speed_difference_less_then_5_should_validateOK() throws Exception {

		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":200,\"speed\":20,\"gusts\":35,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T12:00:00Z\",\"changeEnd\":\"2018-06-18T15:00:00Z\",\"changeType\":\"BECMG\",\"forecast\":{\"wind\":{\"direction\":200,\"speed\":22,\"gusts\":35,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		log.error(report.getErrors().asText());
		assertThat(report.getErrors().toString(), is("{\"/changegroups/0/forecast/wind/windEnoughDifference\":"
				+ "[\"Change in wind must be at least 30 degrees in direction or 5 knots (2 MPS) in speed or gust\"]}"));
		assertThat(report.isSucceeded(), is(false));
	}

	/*
	 * Tests Changegroups: changegroup and previous group with gust and more then 5 knots of speed difference.
	 */
	@Test
	public void testValidate_test_taf_groups_with_speed_difference_more_then_5_should_validateOK() throws Exception {

		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":200,\"speed\":20,\"gusts\":35,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T12:00:00Z\",\"changeEnd\":\"2018-06-18T15:00:00Z\",\"changeType\":\"BECMG\",\"forecast\":{\"wind\":{\"direction\":200,\"speed\":25,\"gusts\":35,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/* Clouds NOT ascending in height should give valid pointer */
	@Test
	public void testValidate_test_taf_clouds_not_ascending_in_height_should_give_valid_pointer() throws Exception {
		String tafString = "{\"forecast\":{\"clouds\":[{\"amount\":\"OVC\",\"height\":20,\"mod\":\"CB\"},{\"amount\":\"OVC\",\"height\":15}],\"visibility\":{\"unit\":\"M\",\"value\":6000},\"weather\":[{\"qualifier\":\"moderate\",\"descriptor\":\"showers\",\"phenomena\":[\"rain\"]}],\"wind\":{\"direction\":200,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T14:00:00Z\",\"changeEnd\":\"2018-06-18T16:00:00Z\",\"changeType\":\"PROB30\",\"forecast\":{\"visibility\":{\"unit\":\"M\",\"value\":7000},\"wind\":{\"direction\":200,\"speed\":25,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		log.error(report.getErrors().asText());
		assertThat(report.getErrors().toString(),
				is("{\"/forecast/clouds/1/cloudsHeightAscending\":[\"Cloud groups must be ascending in height\"]}"));
		assertThat(report.isSucceeded(), is(false));
	}

	/* Clouds with the same height if CB should give valid pointer */
	@Test
	public void testValidate_test_taf_clouds_with_same_height_if_CB_should_give_valid_pointer() throws Exception {
		String tafString = "{\"forecast\":{\"clouds\":[{\"amount\":\"OVC\",\"height\":15},{\"amount\":\"OVC\",\"height\":15,\"mod\":\"CB\"}],\"visibility\":{\"unit\":\"M\",\"value\":6000},\"weather\":[{\"qualifier\":\"moderate\",\"descriptor\":\"showers\",\"phenomena\":[\"rain\"]}],\"wind\":{\"direction\":200,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T14:00:00Z\",\"changeEnd\":\"2018-06-18T16:00:00Z\",\"changeType\":\"PROB30\",\"forecast\":{\"visibility\":{\"unit\":\"M\",\"value\":7000},\"wind\":{\"direction\":200,\"speed\":25,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/* Multiple CB in Clouds group should npt validate */
	@Test
	public void testValidate_test_taf_clouds_multiple_CB_should_not_validate() throws Exception {
		String tafString = "{\"forecast\":{\"clouds\":[{\"amount\":\"OVC\",\"height\":20,\"mod\":\"CB\"},{\"amount\":\"OVC\",\"height\":25,\"mod\":\"CB\"}],\"visibility\":{\"unit\":\"M\",\"value\":6000},\"weather\":[{\"qualifier\":\"moderate\",\"descriptor\":\"showers\",\"phenomena\":[\"rain\"]}],\"wind\":{\"direction\":200,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T14:00:00Z\",\"changeEnd\":\"2018-06-18T16:00:00Z\",\"changeType\":\"PROB30\",\"forecast\":{\"visibility\":{\"unit\":\"M\",\"value\":7000},\"wind\":{\"direction\":200,\"speed\":25,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(false));
	}

	/* Clouds ascending in height should validate */
	@Test
	public void testValidate_test_taf_clouds_ascending_in_height_should_validate() throws Exception {
		String tafString = "{\"forecast\":{\"clouds\":[{\"amount\":\"OVC\",\"height\":20,\"mod\":\"CB\"},{\"amount\":\"OVC\",\"height\":25}],\"visibility\":{\"unit\":\"M\",\"value\":6000},\"weather\":[{\"qualifier\":\"moderate\",\"descriptor\":\"showers\",\"phenomena\":[\"rain\"]}],\"wind\":{\"direction\":200,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T14:00:00Z\",\"changeEnd\":\"2018-06-18T16:00:00Z\",\"changeType\":\"PROB30\",\"forecast\":{\"visibility\":{\"unit\":\"M\",\"value\":7000},\"wind\":{\"direction\":200,\"speed\":25,\"unit\":\"KT\"}}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/* FZFG (Freezing Fog) alleen bij <= 1000 toestaan */
	@Test
	public void testValidate_test_taf_FZFG_only_below_1000m_visibility() throws Exception {
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);

		/* TESTING 1000 SHOULD VALIDATE */
		String tafString = "{\"forecast\":{\"clouds\":[{\"amount\":\"OVC\",\"height\":20,\"mod\":\"CB\"}],\"visibility\":{\"unit\":\"M\",\"value\":900},\"weather\":[{\"qualifier\":\"moderate\",\"descriptor\":\"freezing\",\"phenomena\":[\"fog\"]},{\"qualifier\":\"moderate\",\"descriptor\":\"showers\",\"phenomena\":[\"rain\"]}],\"wind\":{\"direction\":200,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[]}";
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));

		/* TESTING 2000 SHOULD NOT VALIDATE */
		tafString = "{\"forecast\":{\"clouds\":[{\"amount\":\"OVC\",\"height\":20,\"mod\":\"CB\"}],\"visibility\":{\"unit\":\"M\",\"value\":2000},\"weather\":[{\"qualifier\":\"moderate\",\"descriptor\":\"freezing\",\"phenomena\":[\"fog\"]},{\"qualifier\":\"moderate\",\"descriptor\":\"showers\",\"phenomena\":[\"rain\"]}],\"wind\":{\"direction\":200,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[]}";
		report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(false));
	}

	/* Test metadata properties */
	@Test
	public void testValidate_test_metadataProperties() throws Exception {
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);

		/* TESTING 1000 SHOULD VALIDATE, lowercase concept */
		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":100,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHTW\",\"status\":\"concept\",\"type\":\"normal\",\"validityStart\":\"2018-06-20T06:00:00Z\",\"validityEnd\":\"2018-06-21T12:00:00Z\"},\"changegroups\":[]}";
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));

		/* TESTING 2000 SHOULD NOT VALIDATE, uppercase CONCEPt */
		tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":100,\"speed\":20,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHTW\",\"status\":\"CONCEPT\",\"type\":\"normal\",\"validityStart\":\"2018-06-20T06:00:00Z\",\"validityEnd\":\"2018-06-21T12:00:00Z\"},\"changegroups\":[]}";
		report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(false));
	}

	/* Test MIFG Moderate Shallow Fog */
	@Test
	public void testValidate_test_taf_MIFG_moderate_shallow_fog() throws Exception {
		String tafString = "{\"forecast\":{\"clouds\":[{\"amount\":\"FEW\",\"height\":20}],\"visibility\":{\"unit\":\"M\",\"value\":6000},\"weather\":[{\"qualifier\":\"moderate\",\"descriptor\":\"shallow\",\"phenomena\":[\"fog\"]}],\"wind\":{\"direction\":120,\"speed\":40,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"type\":\"normal\",\"validityStart\":\"2018-08-15T06:00:00Z\",\"validityEnd\":\"2018-08-16T12:00:00Z\"},\"changegroups\":[]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/* Tests if FG / FOG with 1100 meters gives proper feedback message */
	@Test
	public void testValidate_test_taf_FOG_1100_meters_proper_feedback() throws Exception {
		String tafString = "{\"forecast\":{\"clouds\":\"NSC\",\"visibility\":{\"unit\":\"M\",\"value\":1100},\"weather\":[{\"qualifier\":\"moderate\",\"phenomena\":[\"fog\"]}],\"wind\":{\"direction\":120,\"speed\":40,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"type\":\"normal\",\"validityStart\":\"2018-08-15T12:00:00Z\",\"validityEnd\":\"2018-08-16T18:00:00Z\"},\"changegroups\":[]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		log.error(report.getErrors().asText());
		assertThat(report.getErrors().toString(), is(
				"{\"/forecast/visibilityAndFogWithoutDescriptorWithinLimit\":[\"Fog requires a visibility of less than 1000 meters\"]}"));
		assertThat(report.isSucceeded(), is(false));
	}

	/* Tests if heavy fog (+FG) gives proper feedback message */
	@Test
	public void testValidate_test_taf_FOG_heavy_proper_feedback() throws Exception {
		String tafString = "{\"forecast\":{\"clouds\":\"NSC\",\"visibility\":{\"unit\":\"M\",\"value\":1000},\"weather\":[{\"qualifier\":\"heavy\",\"phenomena\":[\"widespread dust\"]}],\"wind\":{\"direction\":120,\"speed\":40,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"type\":\"normal\",\"validityStart\":\"2018-08-15T12:00:00Z\",\"validityEnd\":\"2018-08-16T18:00:00Z\"},\"changegroups\":[]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		log.error(report.getErrors().asText());
		assertThat(report.getErrors().toString(),
				is("{\"/forecast/weather/0/qualifier\":[\"Qualifier of intensity (-,+,VC) can only be used in combination with a precipitation type\"]}"));
		assertThat(report.isSucceeded(), is(false));
	}

	/* Tests with CB or TCU without weather group should validate */
	@Test
	public void testValidate_test_taf_changeGroup_without_weather_group_and_with_CB_or_TCU_should_validateOK() throws Exception {
		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":120,\"speed\":40,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T14:00:00Z\",\"changeEnd\":\"2018-06-18T16:00:00Z\",\"changeType\":\"BECMG\",\"forecast\":{\"clouds\":[{\"amount\":\"BKN\",\"height\":4},{\"amount\":\"FEW\",\"height\":35,\"mod\":\"CB\"}]}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.isSucceeded(), is(true));
	}

	/* Tests: FM changegroup with NSW should give proper feedback message */
	@Test
	public void testValidate_test_taf_FM_changeGroup_with_NSW_proper_feedback() throws Exception {
		String tafString = "{\"forecast\":{\"caVOK\":true,\"wind\":{\"direction\":120,\"speed\":40,\"unit\":\"KT\"}},\"metadata\":{\"location\":\"EHAM\",\"validityStart\":\"2018-06-18T12:00:00Z\",\"validityEnd\":\"2018-06-19T18:00:00Z\"},\"changegroups\":[{\"changeStart\":\"2018-06-18T14:00:00Z\",\"changeEnd\":\"2018-06-19T18:00:00Z\",\"changeType\":\"FM\",\"forecast\":{\"weather\":\"NSW\"}}]}";
		TafSchemaStore tafSchemaStore = new TafSchemaStore(productstorelocation);
		TafValidator tafValidator = new TafValidator(tafSchemaStore, tafObjectMapper);
		TafValidationResult report = tafValidator.validate(tafString);
		assertThat(report.getErrors().toString(),
				is("{\"/forecast/forecastGivenWithFM\":[\"FM changegroups needs entire forecast and cannot contain NSW\"]}"));
		assertThat(report.isSucceeded(), is(false));
	}
}
