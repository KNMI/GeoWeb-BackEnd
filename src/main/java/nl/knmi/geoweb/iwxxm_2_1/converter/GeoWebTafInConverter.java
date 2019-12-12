package nl.knmi.geoweb.iwxxm_2_1.converter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFForecast;
import lombok.extern.slf4j.Slf4j;
import fi.fmi.avi.model.SurfaceWind;
import nl.knmi.geoweb.backend.product.taf.TAFtoTACMaps;
import nl.knmi.geoweb.backend.product.taf.Taf;

@Slf4j
public class GeoWebTafInConverter extends AbstractGeoWebTafInConverter<TAF> {
    @Override
    public ConversionResult<Taf> convertMessage(TAF input, ConversionHints hints) {
        ConversionResult<Taf> retval = new ConversionResult<>();
        Taf taf = new Taf();
        Taf.Metadata metadata = new Taf.Metadata();
        metadata.setUuid(UUID.randomUUID().toString());
        metadata.setIssueTime(OffsetDateTime.ofInstant(input.getIssueTime().get().getCompleteTime().get().toInstant(), ZoneId.of("Z")));
        metadata.setValidityStart(
                OffsetDateTime.ofInstant(input.getValidityTime().get().getStartTime().get().getCompleteTime().get().toInstant(), ZoneId.of("Z")));
        metadata.setValidityEnd(OffsetDateTime.ofInstant(input.getValidityTime().get().getEndTime().get().getCompleteTime().get().toInstant(), ZoneId.of("Z")));
        metadata.setLocation(input.getAerodrome().getDesignator());
        metadata.setStatus(Taf.TAFReportPublishedConcept.inactive);

        switch (input.getStatus()) {
            case NORMAL:
                metadata.setType(Taf.TAFReportType.normal);
                break;
            case AMENDMENT:
                metadata.setType(Taf.TAFReportType.amendment);
                break;
            case CORRECTION:
                metadata.setType(Taf.TAFReportType.correction);
                break;
            case CANCELLATION:
                metadata.setType(Taf.TAFReportType.canceled);
                break;
            case MISSING:
                metadata.setType(Taf.TAFReportType.missing);
                break;
        }
        taf.setMetadata(metadata);

        Taf.Forecast forecast = new Taf.Forecast();
        input.getBaseForecast().ifPresent(bfc -> {
            if (bfc.isCeilingAndVisibilityOk()) {
                forecast.setCaVOK(true);
            }
            updateForecastVisibility(forecast, bfc, retval);
            updateForecastWind(forecast, bfc, retval);
            updateForecastTemperature(forecast, bfc, retval);
            updateForecastCloud(forecast, bfc, retval);
            updateForecastWeather(forecast, bfc, retval);
        });
        taf.setForecast(forecast);

        List<Taf.ChangeForecast> changeForecasts = new ArrayList<>();
        input.getChangeForecasts().ifPresent(chFct -> {
                    for (TAFChangeForecast changeForecast : chFct) {
                        Taf.ChangeForecast ch = new Taf.ChangeForecast();
                        ch.setChangeStart(OffsetDateTime.ofInstant(changeForecast.getPeriodOfChange().getStartTime().get().getCompleteTime().get().toInstant(), ZoneId.of("Z")));
                        if (changeForecast.getPeriodOfChange().getEndTime().isPresent()) {
                            ch.setChangeEnd(OffsetDateTime.ofInstant(changeForecast.getPeriodOfChange().getEndTime().get().getCompleteTime().get().toInstant(), ZoneId.of("Z")));
                        }
                        Taf.Forecast chFc = new Taf.Forecast();
                        switch (changeForecast.getChangeIndicator()) {
                            case FROM:
                                ch.setChangeType("FM");
                                break;
                            case BECOMING:
                                ch.setChangeType("BECMG");
                                break;
                            case TEMPORARY_FLUCTUATIONS:
                                ch.setChangeType("TEMPO");
                                break;
                            case PROBABILITY_30:
                                ch.setChangeType("PROB30");
                                break;
                            case PROBABILITY_40:
                                ch.setChangeType("PROB40");
                                break;
                            case PROBABILITY_30_TEMPORARY_FLUCTUATIONS:
                                ch.setChangeType("PROB30 TEMPO");
                                break;
                            case PROBABILITY_40_TEMPORARY_FLUCTUATIONS:
                                ch.setChangeType("PROB40 TEMPO");
                                break;
                            default:
                        }

                        if (changeForecast.isCeilingAndVisibilityOk()) {
                            log.debug("SETTING CHANGEFORECAST CAVOK");
                            chFc.setCaVOK(true);
                        }

                        updateForecastVisibility(chFc, changeForecast, retval);
                        updateForecastWind(chFc, changeForecast, retval);
                        updateForecastCloud(chFc, changeForecast, retval);
                        updateForecastWeather(chFc, changeForecast, retval);

                        ch.setForecast(chFc);

                        changeForecasts.add(ch);

                    }
                });
        taf.setChangegroups(changeForecasts);

        retval.setStatus(ConversionResult.Status.SUCCESS);
        retval.setConvertedMessage(taf);
        return retval;
    }

    private String getUomFromUnit(String unit) {
        if (unit.equals("[kn_i]")) {
            return "KT";
        } else if (unit.equals("m/s")) {
            return "MPS";
        } else {
            return "KT";
        }
    }

    private void updateForecastVisibility(Taf.Forecast fc, TAFForecast tafForecast, ConversionResult<Taf> result) {
        if (tafForecast.getPrevailingVisibility().isPresent()) {
            Taf.Forecast.TAFVisibility visibility=new Taf.Forecast.TAFVisibility();
            NumericMeasure visibilityValue=tafForecast.getPrevailingVisibility().get();
            if (visibilityValue.getValue().intValue()>9999) {
                visibility.setValue(9999);
            } else {
                visibility.setValue(visibilityValue.getValue().intValue());
            }
            if (visibilityValue.getUom().equals("m")){
                visibility.setUnit("M");
            } else if (visibilityValue.getUom().equals("[ft_i]")) {
               visibility.setUnit("FT");
            } else {
                visibility.setUnit(visibilityValue.getUom());
            }
            fc.setVisibility(visibility);
        } else {
            fc.setVisibility(null);
        }
    }

    private void updateForecastWind(Taf.Forecast fc, TAFForecast tafForecast, ConversionResult<Taf> result) {
        if (tafForecast.getSurfaceWind().isPresent()) {
            Taf.Forecast.TAFWind wind = new Taf.Forecast.TAFWind();
            SurfaceWind inWind = tafForecast.getSurfaceWind().get();
            wind.setSpeed(inWind.getMeanWindSpeed().getValue().intValue());
            wind.setUnit(getUomFromUnit(inWind.getMeanWindSpeed().getUom()));
            wind.setDirection(inWind.isVariableDirection() ? "VRB" : inWind.getMeanWindDirection().get().getValue().intValue());
            if (inWind.getWindGust().isPresent()) {
                wind.setGusts(inWind.getWindGust().get().getValue().intValue());
            }
            fc.setWind(wind);
        }
    }

    private void updateForecastCloud(Taf.Forecast fc, TAFForecast tafForecast, ConversionResult<Taf> result) {
        if (tafForecast.getCloud().isPresent()) {
            CloudForecast cf = tafForecast.getCloud().get();
            if (cf.isNoSignificantCloud()) {
                Taf.Forecast.TAFCloudType ct = new Taf.Forecast.TAFCloudType();
                ct.setIsNSC(true);

                fc.setClouds(Arrays.asList(ct));
            } else {
                if (cf.getVerticalVisibility().isPresent()) {
                    fc.setVertical_visibility(cf.getVerticalVisibility().get().getValue().intValue()/100);
                } else {
                    List<Taf.Forecast.TAFCloudType> clouds = new ArrayList<>();
                    for (CloudLayer cloudLayer : cf.getLayers().get()) {
                        Taf.Forecast.TAFCloudType ct = new Taf.Forecast.TAFCloudType();
                        if (cloudLayer.getAmount().isPresent()) {
                            switch (cloudLayer.getAmount().get()) {
                                case FEW:
                                    ct.setAmount("FEW");
                                    break;
                                case BKN:
                                    ct.setAmount("BKN");
                                    break;
                                case SCT:
                                    ct.setAmount("SCT");
                                    break;
                                case OVC:
                                    ct.setAmount("OVC");
                                    break;
                                default:
                                    break;
                            }
                        }
                        if (cloudLayer.getBase().isPresent()) {
                            ct.setHeight(cloudLayer.getBase().get().getValue().intValue()/100);
                        }
                        if (cloudLayer.getCloudType().isPresent()) {
                            ct.setMod(cloudLayer.getCloudType().get().toString());
                        }
                        clouds.add(ct);
                    }
                    fc.setClouds(clouds);
                }
            }
        }
     }

    private void updateForecastWeather(Taf.Forecast fc, TAFForecast tafForecast, ConversionResult<Taf> result) {
        if (tafForecast.isNoSignificantWeather()) {
            List<Taf.Forecast.TAFWeather> weatherElems = new ArrayList<>();
            Taf.Forecast.TAFWeather weather = new Taf.Forecast.TAFWeather();
            weather.setIsNSW(true);
            weatherElems.add(weather);
            fc.setWeather(weatherElems);
        } else {
            if (tafForecast.getForecastWeather().isPresent()) {
                List<Taf.Forecast.TAFWeather> weatherElems = new ArrayList<>();

                List<Weather> weather = tafForecast.getForecastWeather().get();
                for (Weather w : weather) {
                    Taf.Forecast.TAFWeather tafWeather = new Taf.Forecast.TAFWeather();
                    try {
                        tafWeather=TAFtoTACMaps.fromTacString(w.getCode());
                        weatherElems.add(tafWeather);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
                fc.setWeather(weatherElems);
            }
        }
    }

    private void updateForecastTemperature(Taf.Forecast fc, TAFBaseForecast tafBaseForecast, ConversionResult<Taf> result) {
    }

}
