package nl.knmi.geoweb.iwxxm_2_1.converter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.TacOrGeoGeometryImpl;
import fi.fmi.avi.model.immutable.UnitPropertyGroupImpl;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import fi.fmi.avi.model.sigmet.SigmetIntensityChange;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;
import fi.fmi.avi.model.sigmet.immutable.AirmetCloudLevelsImpl;
import fi.fmi.avi.model.sigmet.immutable.AirmetReferenceImpl;
import fi.fmi.avi.model.sigmet.immutable.AirmetWindImpl;
import fi.fmi.avi.model.sigmet.immutable.PhenomenonGeometryWithHeightImpl;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.backend.product.airmet.Airmet;
import nl.knmi.geoweb.backend.product.airmet.ObscuringPhenomenonList;
import nl.knmi.geoweb.backend.product.sigmet.geo.GeoUtils;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetType;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetUtils;

public class GeoWebAIRMETConverter extends AbstractGeoWebAirmetConverter<AIRMET> {

    @Override
    public ConversionResult<AIRMET> convertMessage(Airmet input, ConversionHints hints) {
        Debug.println("convertMessage: " + this.getClass().getName());
        ConversionResult<AIRMET> retval = new ConversionResult<>();
        AIRMETImpl.Builder airmet = new AIRMETImpl.Builder();

        airmet.setIssuingAirTrafficServicesUnit(getFicInfo(input.getFirname(), input.getLocation_indicator_icao()));
        airmet.setMeteorologicalWatchOffice(getMWOInfo(input.getLocation_indicator_mwo(), input.getLocation_indicator_mwo()));

        AirspaceImpl.Builder airspaceBuilder=new AirspaceImpl.Builder()
                .setDesignator(input.getLocation_indicator_icao())
                .setType(Airspace.AirspaceType.FIR)
                .setName(input.getFirname());
        airmet.setAirspace(airspaceBuilder.build());

        if (input.getIssuedate() == null) {
            airmet.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.now()));
        } else {
            airmet.setIssueTime(PartialOrCompleteTimeInstant.of(input.getIssuedate().atZoneSameInstant(ZoneId.of("UTC"))));
        }
        airmet.setAirmetPhenomenon(AviationCodeListUser.AeronauticalAirmetWeatherPhenomenon.valueOf(input.getPhenomenon().toString()));
        airmet.setSequenceNumber(Integer.toString(input.getSequence())); //This should be a String, so int sequence is stringified

        PartialOrCompleteTimePeriod.Builder validPeriod = new PartialOrCompleteTimePeriod.Builder();
        validPeriod.setStartTime(PartialOrCompleteTimeInstant.of(input.getValiddate().atZoneSameInstant(ZoneId.of("UTC"))));
        validPeriod.setEndTime(PartialOrCompleteTimeInstant.of(input.getValiddate_end().atZoneSameInstant(ZoneId.of("UTC"))));
        airmet.setValidityPeriod(validPeriod.build());

        if (input.getObs_or_forecast() != null) {
            if (input.getObs_or_forecast().isObs()) {
                airmet.setAnalysisType(SigmetAnalysisType.OBSERVATION);
            } else {
                airmet.setAnalysisType(SigmetAnalysisType.FORECAST);
            }
        } else {
            Debug.errprintln("obs_or_fcst NOT found");
        }

        switch (input.getChange()) {
            case WKN:
                airmet.setIntensityChange(SigmetIntensityChange.WEAKENING);
                break;
            case INTSF:
                airmet.setIntensityChange(SigmetIntensityChange.INTENSIFYING);
                break;
            case NC:
            default:
                airmet.setIntensityChange(SigmetIntensityChange.NO_CHANGE);
        }

        if (input.getMovement_type() == null) {
            input.setMovement_type(Airmet.AirmetMovementType.STATIONARY);
        }
        switch (input.getMovement_type()) {
            case STATIONARY:
                airmet.setMovingDirection(Optional.empty());
                airmet.setMovingSpeed(Optional.empty());
                break;
            case MOVEMENT:
                if ((input.getMovement().getDir() != null) && (input.getMovement().getSpeed() != null)) {
                    NumericMeasure numDir = NumericMeasureImpl.of(input.getMovement().getDir().getDir(), "deg");
                    airmet.setMovingDirection(numDir);
                    String uom = input.getMovement().getSpeeduom();
                    if ("KMH".equals(uom)) {
                        uom = "km/h";
                    }
                    if ("KT".equals(uom)) {
                        uom = "[kn_i]";
                    }
                    NumericMeasure numSpd = NumericMeasureImpl.of(input.getMovement().getSpeed(), uom);
                    airmet.setMovingSpeed(numSpd);
                }
                break;
        }

        Debug.println("levelinfo: " + input.getLevelinfo());

        PhenomenonGeometryWithHeightImpl.Builder phenBuilder = new PhenomenonGeometryWithHeightImpl.Builder();

        switch (input.getPhenomenon()) {
            case BKN_CLD:
            case OVC_CLD:
                //Cloudlevels should be set, copy info to ...
                AirmetCloudLevelsImpl.Builder cloudLevelsBuilder = new AirmetCloudLevelsImpl.Builder();
                Airmet.AirmetCloudLevelInfo cloudLevels = input.getCloudLevels();
                if (cloudLevels != null) {
                    Airmet.LowerCloudLevel lowerCloudLevel = cloudLevels.getLower();
                    if (lowerCloudLevel == null) {
                        Debug.errprintln("lowerCloudLevel is null");
                    } else {
                        if (lowerCloudLevel.getSurface()) {
                            Debug.errprintln("cloudBottom isSurface = true");
                            phenBuilder.setLowerLimit(NumericMeasureImpl.of(0.0, "FT")); //Special case for SFC: 0FT)
                            cloudLevelsBuilder.setCloudBase(NumericMeasureImpl.of(0, "FT"));
                        } else {
                            Debug.errprintln("cloudBottom isSurface = false");
                            phenBuilder.setLowerLimit(NumericMeasureImpl.of(lowerCloudLevel.getVal(), lowerCloudLevel.getUnit()));
                            cloudLevelsBuilder.setCloudBase(NumericMeasureImpl.of(lowerCloudLevel.getVal(), lowerCloudLevel.getUnit()));
                        }
                    }
                    Airmet.UpperCloudLevel upperCloudLevel = cloudLevels.getUpper();
                    if (upperCloudLevel == null) {
                        Debug.errprintln("upperCloudLevel is null");
                    } else {
                        phenBuilder.setUpperLimit(NumericMeasureImpl.of(upperCloudLevel.getVal(), upperCloudLevel.getUnit()));
                        cloudLevelsBuilder.setCloudTop(NumericMeasureImpl.of(upperCloudLevel.getVal(), upperCloudLevel.getUnit()));
                        if ((upperCloudLevel.getAbove() != null) && (upperCloudLevel.getAbove())) {
                            cloudLevelsBuilder.setTopAbove(true);
                        }
                    }
                    airmet.setCloudLevels(cloudLevelsBuilder.build());
                }
                break;
            case SFC_VIS:
                Airmet.AirmetValue vis = input.getVisibility();
                String unit=vis.getUnit();
                if (unit.equals("M")) unit="m";
                NumericMeasure airmetVisibility=NumericMeasureImpl.of(vis.getVal(), unit);
                airmet.setVisibility(airmetVisibility);
                List<ObscuringPhenomenonList.ObscuringPhenomenon> obscuring=input.getObscuring();
                List<AviationCodeListUser.WeatherCausingVisibilityReduction> weatherCausingVisibilityReductionList=new ArrayList<>();
                for (ObscuringPhenomenonList.ObscuringPhenomenon phen:obscuring){
                    AviationCodeListUser.WeatherCausingVisibilityReduction weatherCausingVisibilityReduction=
                        AviationCodeListUser.WeatherCausingVisibilityReduction.fromString(input.getObscuring().get(0).getCode());
                    weatherCausingVisibilityReductionList.add(weatherCausingVisibilityReduction);

                }
                airmet.setObscuration(weatherCausingVisibilityReductionList);
                break;
            case SFC_WIND:
                Airmet.AirmetWindInfo windInfo = input.getWind();
                AirmetWindImpl.Builder airmetWindBuilder = new AirmetWindImpl.Builder();
                String dirUnit=windInfo.getDirection().getUnit();
                if ("degrees".equalsIgnoreCase(dirUnit)) dirUnit="deg";
                airmetWindBuilder.setDirection(NumericMeasureImpl.of(windInfo.getDirection().getVal(), dirUnit));
                String speedUnit=windInfo.getSpeed().getUnit();
                if ("KT".equals(speedUnit)) speedUnit="[kn_i]";
                if ("MPS".equalsIgnoreCase((speedUnit))) speedUnit="m/s";
                airmetWindBuilder.setSpeed(NumericMeasureImpl.of(windInfo.getSpeed().getVal(), speedUnit));
                airmet.setWind(airmetWindBuilder.build());
                break;
            default:
                //Levelinfo might/has to be set
                if (input.getLevelinfo() != null) {
                    Debug.println("setLevelInfo(" + input.getLevelinfo().getMode() + ")");
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
        }

        if (input.getStatus().equals(SigmetAirmetStatus.published)) {
            if (input.getCancels() == null) {
                phenBuilder.setGeometry(TacOrGeoGeometryImpl.of(GeoUtils.jsonFeature2jtsGeometry((Feature) SigmetAirmetUtils.extractSingleStartGeometry(input.getGeojson()))));
                if ((input.getObs_or_forecast() != null)&& (input.getObs_or_forecast().getObsFcTime() != null)){
                    phenBuilder.setTime(PartialOrCompleteTimeInstant.of(input.getObs_or_forecast().getObsFcTime().atZoneSameInstant(ZoneId.of("UTC"))));
                }
                phenBuilder.setApproximateLocation(false);
            }else {
                phenBuilder.setGeometry(Optional.empty());
            }
        }
        airmet.setAnalysisGeometries(Arrays.asList(phenBuilder.build()));

         //Not translated
        airmet.setTranslated(false);
        if (input.getStatus().equals(SigmetAirmetStatus.published)) {
            if (input.getCancels() != null) {
                airmet.setStatus(AviationCodeListUser.SigmetAirmetReportStatus.CANCELLATION);
                AirmetReferenceImpl.Builder airmetReferenceBuilder = new AirmetReferenceImpl.Builder();
                airmetReferenceBuilder.setIssuingAirTrafficServicesUnit(airmet.getIssuingAirTrafficServicesUnit());
                airmetReferenceBuilder.setMeteorologicalWatchOffice(airmet.getMeteorologicalWatchOffice());
                airmetReferenceBuilder.setPhenomenon(airmet.getAirmetPhenomenon());
                PartialOrCompleteTimePeriod.Builder cancelledPeriod = new PartialOrCompleteTimePeriod.Builder();
                cancelledPeriod.setStartTime(PartialOrCompleteTimeInstant.of(input.getCancelsStart().atZoneSameInstant(ZoneId.of("UTC"))));
                cancelledPeriod.setEndTime(PartialOrCompleteTimeInstant.of(input.getValiddate_end().atZoneSameInstant(ZoneId.of("UTC"))));
                airmetReferenceBuilder.setValidityPeriod(cancelledPeriod.build());
                airmetReferenceBuilder.setSequenceNumber(input.getCancels().toString());
                airmet.setCancelledReference(airmetReferenceBuilder.build());
            } else {
                airmet.setStatus(AviationCodeListUser.SigmetAirmetReportStatus.NORMAL);
            }
        } else {
            airmet.setStatus(AviationCodeListUser.SigmetAirmetReportStatus.NORMAL);
        }

        if (input.getType().equals(SigmetAirmetType.normal)) {
            airmet.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.OPERATIONAL);
            airmet.setPermissibleUsageReason(Optional.empty());
        } else {
            if (input.getType().equals(SigmetAirmetType.exercise)) {
                airmet.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL);
                airmet.setPermissibleUsageReason(AviationCodeListUser.PermissibleUsageReason.EXERCISE);
            } else {
                airmet.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL);
                airmet.setPermissibleUsageReason(AviationCodeListUser.PermissibleUsageReason.TEST);
            }
        }

        retval.setConvertedMessage(airmet.build());

        return retval;
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

    private UnitPropertyGroup getMWOInfo(String mwoFullName, String locationIndicator) {
        String mwoName=mwoFullName.trim().replace("(\\w+)\\s(MWO$)", "$1");
        UnitPropertyGroupImpl.Builder mwo = new UnitPropertyGroupImpl.Builder();
        mwo.setPropertyGroup(mwoName, locationIndicator, "MWO");
        return mwo.build();
    }
}

