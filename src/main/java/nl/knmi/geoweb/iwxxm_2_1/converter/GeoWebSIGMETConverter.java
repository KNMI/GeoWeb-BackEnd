package nl.knmi.geoweb.iwxxm_2_1.converter;

import static nl.knmi.geoweb.backend.product.sigmet.Sigmet.Phenomenon.VA_CLD;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.geojson.Feature;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.Airspace;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.UnitPropertyGroup;
import fi.fmi.avi.model.immutable.AirspaceImpl;
import fi.fmi.avi.model.immutable.GeoPositionImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.TacOrGeoGeometryImpl;
import fi.fmi.avi.model.immutable.UnitPropertyGroupImpl;
import fi.fmi.avi.model.immutable.VolcanoDescriptionImpl;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import fi.fmi.avi.model.sigmet.SigmetIntensityChange;
import fi.fmi.avi.model.sigmet.immutable.PhenomenonGeometryImpl;
import fi.fmi.avi.model.sigmet.immutable.PhenomenonGeometryWithHeightImpl;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import fi.fmi.avi.model.sigmet.immutable.SigmetReferenceImpl;
import fi.fmi.avi.model.sigmet.immutable.VAInfoImpl;
import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetMovementType;
import nl.knmi.geoweb.backend.product.sigmet.geo.GeoUtils;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetType;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetUtils;

@Slf4j
public class GeoWebSIGMETConverter extends AbstractGeoWebSigmetConverter<SIGMET> {

    @Override
    public ConversionResult<SIGMET> convertMessage(Sigmet input, ConversionHints hints) {
        log.trace("convertMessage: " + this.getClass().getName());
        ConversionResult<SIGMET> retval = new ConversionResult<>();
        SIGMETImpl.Builder sigmet = new SIGMETImpl.Builder();

        sigmet.setIssuingAirTrafficServicesUnit(getFicInfo(input.getFirname(), input.getLocation_indicator_icao()));
        sigmet.setMeteorologicalWatchOffice(getMWOInfo(input.getLocation_indicator_mwo(), input.getLocation_indicator_mwo()));

        AirspaceImpl.Builder airspaceBuilder=new AirspaceImpl.Builder()
                .setDesignator(input.getLocation_indicator_icao())
                .setType(Airspace.AirspaceType.FIR)
                .setName(input.getFirname());
        sigmet.setAirspace(airspaceBuilder.build());

        if (input.getIssuedate() == null) {
            sigmet.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.now()));
        } else {
            sigmet.setIssueTime(PartialOrCompleteTimeInstant.of(input.getIssuedate().atZoneSameInstant(ZoneId.of("UTC"))));
        }
        //Phenomenon
        switch (input.getPhenomenon()) {
            case VA_CLD:
                sigmet.setSigmetPhenomenon(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA);
                break;
            case TROPICAL_CYCLONE:
                sigmet.setSigmetPhenomenon(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.TC);
                break;
            default:
                sigmet.setSigmetPhenomenon(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.valueOf(input.getPhenomenon().toString()));
        }
        sigmet.setSequenceNumber(Integer.toString(input.getSequence())); //This should be a String, so int sequence is stringified

        PartialOrCompleteTimePeriod.Builder validPeriod = new PartialOrCompleteTimePeriod.Builder();
        validPeriod.setStartTime(PartialOrCompleteTimeInstant.of(input.getValiddate().atZoneSameInstant(ZoneId.of("UTC"))));
        validPeriod.setEndTime(PartialOrCompleteTimeInstant.of(input.getValiddate_end().atZoneSameInstant(ZoneId.of("UTC"))));
        sigmet.setValidityPeriod(validPeriod.build());

//        SigmetAnalysisImpl.Builder sa = new SigmetAnalysisImpl.Builder();
        if (input.getObs_or_forecast() != null) {
            log.debug("obs_or_fcst found "+input.getObs_or_forecast().isObs());
            if (input.getObs_or_forecast().isObs()) {
                sigmet.setAnalysisType(SigmetAnalysisType.OBSERVATION);
            } else {
                sigmet.setAnalysisType(SigmetAnalysisType.FORECAST);
            }
        } else {
            log.error("obs_or_fcst NOT found");
        }

        switch (input.getChange()) {
            case WKN:
                sigmet.setIntensityChange(SigmetIntensityChange.WEAKENING);
                break;
            case NC:
                sigmet.setIntensityChange(SigmetIntensityChange.NO_CHANGE);
                break;
            case INTSF:
                sigmet.setIntensityChange(SigmetIntensityChange.INTENSIFYING);
                break;
            default:
                sigmet.setIntensityChange(SigmetIntensityChange.NO_CHANGE);
        }

        boolean fpaRequired = true;

        if (input.getMovement_type() == null) {
            input.setMovement_type(SigmetMovementType.STATIONARY);
        }
        switch (input.getMovement_type()) {
            case STATIONARY:
                sigmet.setMovingDirection(Optional.empty());
                sigmet.setMovingSpeed(Optional.empty());
                fpaRequired = false;
                break;
            case MOVEMENT:
                if ((input.getMovement().getDir() != null) && (input.getMovement().getSpeed() != null)) {
                    NumericMeasure numDir = NumericMeasureImpl.of(input.getMovement().getDir().getDir(), "deg");
                    sigmet.setMovingDirection(numDir);
                    String uom = input.getMovement().getSpeeduom();
                    if ("KMH".equals(uom)) {
                        uom = "km/h";
                    }
                    if ("KT".equals(uom)) {
                        uom = "[kn_i]";
                    }
                    NumericMeasure numSpd = NumericMeasureImpl.of(input.getMovement().getSpeed(), uom);
                    sigmet.setMovingSpeed(numSpd);
                    fpaRequired = false;
                }
                break;
            case FORECAST_POSITION:
                break;
        }

        // Hack in case of no_va_expected: do not generate a forecastPositionAnalysis
        if (input.getPhenomenon()==VA_CLD) {
            if ((input.getVa_extra_fields()!=null)&&input.getVa_extra_fields().isNo_va_expected()) {
                fpaRequired=false;  //no forecastpositionanalysis in case of NO_VA_EXP
            }
        }

        log.debug("levelinfo: " + input.getLevelinfo());

        PhenomenonGeometryWithHeightImpl.Builder phenBuilder = new PhenomenonGeometryWithHeightImpl.Builder();
        if (input.getLevelinfo() != null) {
            log.debug("setLevelInfo(" + input.getLevelinfo().getMode() + ")");
            switch (input.getLevelinfo().getMode()) {
                case BETW:
                    NumericMeasure nmLower = NumericMeasureImpl.of((double) input.getLevelinfo().getLevels()[0].getValue(),
                            input.getLevelinfo().getLevels()[0].getUnit().toString());
                    phenBuilder.setLowerLimit(nmLower);
                    NumericMeasure nmUpper = NumericMeasureImpl.of((double) input.getLevelinfo().getLevels()[1].getValue(),
                            input.getLevelinfo().getLevels()[1].getUnit().toString());
                    phenBuilder.setUpperLimit(nmUpper);
                    break;
                case BETW_SFC:
                    nmLower = NumericMeasureImpl.of(0.0, "FT"); //Special case for SFC: 0FT
                    phenBuilder.setLowerLimit(nmLower);
                    nmUpper = NumericMeasureImpl.of((double) input.getLevelinfo().getLevels()[1].getValue(),
                            input.getLevelinfo().getLevels()[1].getUnit().toString());
                    phenBuilder.setUpperLimit(nmUpper);
                    break;
                case AT:
                    nmLower = NumericMeasureImpl.of((double) input.getLevelinfo().getLevels()[0].getValue(),
                            input.getLevelinfo().getLevels()[0].getUnit().toString());
                    phenBuilder.setLowerLimit(nmLower);
                    phenBuilder.setUpperLimit(nmLower);
                    break;
                case ABV:
                    nmLower = NumericMeasureImpl.of((double) input.getLevelinfo().getLevels()[0].getValue(),
                            input.getLevelinfo().getLevels()[0].getUnit().toString());
                    phenBuilder.setLowerLimit(nmLower);
                    phenBuilder.setUpperLimit(nmLower);
                    phenBuilder.setUpperLimitOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                    break;
                case TOPS:
                    nmUpper = NumericMeasureImpl.of((double) input.getLevelinfo().getLevels()[0].getValue(),
                            input.getLevelinfo().getLevels()[0].getUnit().toString());
                    phenBuilder.setUpperLimit(nmUpper);
                    break;
                case TOPS_ABV:
                    nmUpper = NumericMeasureImpl.of((double) input.getLevelinfo().getLevels()[0].getValue(),
                            input.getLevelinfo().getLevels()[0].getUnit().toString());
                    phenBuilder.setUpperLimit(nmUpper);
                    phenBuilder.setUpperLimitOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                    break;
                case TOPS_BLW:
                    nmLower = NumericMeasureImpl.of((double) input.getLevelinfo().getLevels()[0].getValue(),
                            input.getLevelinfo().getLevels()[0].getUnit().toString());
                    phenBuilder.setLowerLimit(nmLower);
                    phenBuilder.setLowerLimitOperator(AviationCodeListUser.RelationalOperator.BELOW);
                    break;
            }
        }

        if (input.getStatus().equals(SigmetAirmetStatus.published)) {
            if (input.getCancels() == null) {
                phenBuilder.setGeometry(TacOrGeoGeometryImpl.of(GeoUtils.jsonFeature2FmiAviGeometry((Feature) SigmetAirmetUtils.extractSingleStartGeometry(input.getGeojson()))));
                if ((input.getObs_or_forecast() != null)&& (input.getObs_or_forecast().getObsFcTime() != null)){
                    phenBuilder.setTime(PartialOrCompleteTimeInstant.of(input.getObs_or_forecast().getObsFcTime().atZoneSameInstant(ZoneId.of("UTC"))));
                }
                phenBuilder.setApproximateLocation(false);
                log.debug("FPA required: " + fpaRequired);
                if (fpaRequired) {
                    PhenomenonGeometryImpl.Builder fpaPhenBuilder=new PhenomenonGeometryImpl.Builder();
                    fpaPhenBuilder.setTime(PartialOrCompleteTimeInstant.of(input.getValiddate_end().atZoneSameInstant(ZoneId.of("UTC"))));
                    fpaPhenBuilder.setGeometry(TacOrGeoGeometryImpl.of(GeoUtils.jsonFeature2FmiAviGeometry((Feature) input.extractSingleEndGeometry())));
                    fpaPhenBuilder.setApproximateLocation(false);
                    sigmet.setForecastGeometries(Arrays.asList(fpaPhenBuilder.build()));
                }
            }else {
                phenBuilder.setGeometry(Optional.empty());
            }
        }
        sigmet.setAnalysisGeometries(Arrays.asList(phenBuilder.build()));

         //Not translated
        sigmet.setTranslated(false);
        if (input.getStatus().equals(SigmetAirmetStatus.published)) {
            if (input.getCancels() != null) {
                sigmet.setStatus(AviationCodeListUser.SigmetAirmetReportStatus.CANCELLATION);
                SigmetReferenceImpl.Builder sigmetReferenceBuilder = new SigmetReferenceImpl.Builder();
                sigmetReferenceBuilder.setIssuingAirTrafficServicesUnit(sigmet.getIssuingAirTrafficServicesUnit());
                sigmetReferenceBuilder.setMeteorologicalWatchOffice(sigmet.getMeteorologicalWatchOffice());
                sigmetReferenceBuilder.setPhenomenon(sigmet.getSigmetPhenomenon());
                PartialOrCompleteTimePeriod.Builder cancelledPeriod = new PartialOrCompleteTimePeriod.Builder();
                cancelledPeriod.setStartTime(PartialOrCompleteTimeInstant.of(input.getCancelsStart().atZoneSameInstant(ZoneId.of("UTC"))));
                cancelledPeriod.setEndTime(PartialOrCompleteTimeInstant.of(input.getValiddate_end().atZoneSameInstant(ZoneId.of("UTC"))));
                sigmetReferenceBuilder.setValidityPeriod(cancelledPeriod.build());
                sigmetReferenceBuilder.setSequenceNumber(input.getCancels().toString());
                sigmet.setCancelledReference(sigmetReferenceBuilder.build());
            } else {
                sigmet.setStatus(AviationCodeListUser.SigmetAirmetReportStatus.NORMAL);
            }
        } else {
            sigmet.setStatus(AviationCodeListUser.SigmetAirmetReportStatus.NORMAL);
        }

        if (input.getType().equals(SigmetAirmetType.normal)) {
            sigmet.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.OPERATIONAL);
            sigmet.setPermissibleUsageReason(Optional.empty());
        } else {
            if (input.getType().equals(SigmetAirmetType.exercise)) {
                sigmet.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL);
                sigmet.setPermissibleUsageReason(AviationCodeListUser.PermissibleUsageReason.EXERCISE);
            } else {
                sigmet.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL);
                sigmet.setPermissibleUsageReason(AviationCodeListUser.PermissibleUsageReason.TEST);
            }
        }

        if (input.getPhenomenon().equals(VA_CLD)) {
            if (input.getVa_extra_fields() != null) {
                VolcanoDescriptionImpl.Builder volcanoBuilder = new VolcanoDescriptionImpl.Builder();
                if (input.getVa_extra_fields().getVolcano() != null) {
                    if (input.getVa_extra_fields().getVolcano().getName() != null) {
                        volcanoBuilder.setVolcanoName("MT " + input.getVa_extra_fields().getVolcano().getName());
                    }
                    if (input.getVa_extra_fields().getVolcano().getPosition() != null) {
                        GeoPositionImpl.Builder geoPositionBuilder = GeoPositionImpl.builder().setCoordinateReferenceSystemId(
                                AviationCodeListUser.CODELIST_VALUE_EPSG_4326);
                        double[] pos = new double[2];
                        pos[0] = input.getVa_extra_fields().getVolcano().getPosition().get(0).doubleValue();
                        pos[1] = input.getVa_extra_fields().getVolcano().getPosition().get(1).doubleValue();
                        geoPositionBuilder.addCoordinates(pos);

                        volcanoBuilder.setVolcanoPosition(geoPositionBuilder.build());
                    }
                }
                VAInfoImpl.Builder vaInfoBuilder = new VAInfoImpl.Builder();
                vaInfoBuilder.setVolcano(volcanoBuilder.build());
                if ((input.getVa_extra_fields().getMove_to() != null) && (input.getVa_extra_fields().getMove_to().get(0) != null)) {
                    vaInfoBuilder.setVolcanicAshMovedToFIR(
                            getFirInfo(input.getVa_extra_fields().getMove_to().get(0) + " FIR", input.getVa_extra_fields().getMove_to().get(0)));
                }
                sigmet.setVAInfo(vaInfoBuilder.build());
            }
        }
        retval.setConvertedMessage(sigmet.build());

        return retval;
    }

    private String getFirType(String firName) {
        String firType=null;
        if (firName.endsWith("FIR")) {
            firType = "FIR";
        } else if (firName.endsWith("UIR")) {
            firType = "UIR";
        } else if (firName.endsWith("CTA")) {
            firType = "CTA";
        } else if (firName.endsWith("FIR/UIR")) {
            firType = "OTHER:FIR_UIR";
        } else {
            return "OTHER:UNKNOWN";
        }
        return firType;
    }

    String getFirName(String firFullName){
        return firFullName.trim().replaceFirst("(\\w+)\\s((FIR|UIR|CTA|UIR/FIR)$)", "$1");
    }

    private UnitPropertyGroup getFicInfo(String firFullName, String icao) {
        String firName=firFullName.trim().replaceFirst("(\\w+)\\s((FIR|UIR|CTA|UIR/FIR)$)", "$1");
        UnitPropertyGroupImpl.Builder unit = new UnitPropertyGroupImpl.Builder();
        unit.setPropertyGroup(firName, icao, "FIC");
        return unit.build();
    }

    private UnitPropertyGroup getFirInfo(String firFullName, String icao) {
        String firName=getFirName(firFullName);
        UnitPropertyGroupImpl.Builder unit = new UnitPropertyGroupImpl.Builder();
        unit.setPropertyGroup(firName, icao, getFirType(firFullName));
        return unit.build();
    }

    private UnitPropertyGroup getMWOInfo(String mwoFullName, String locationIndicator) {
        String mwoName=mwoFullName.trim().replace("(\\w+)\\s(MWO$)", "$1");
        UnitPropertyGroupImpl.Builder mwo = new UnitPropertyGroupImpl.Builder();
        mwo.setPropertyGroup(mwoName, locationIndicator, "MWO");
        return mwo.build();
    }
}

