package nl.knmi.geoweb.backend.product.sigmetairmet;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ObsFc {
    private boolean obs=true ;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime obsFcTime;
    public ObsFc(){};
    public ObsFc(boolean obs){
        this.obs=obs;
        this.obsFcTime=null;
    }
    public ObsFc(boolean obs, OffsetDateTime obsTime) {
        this.obs=obs;
        this.obsFcTime=obsTime;
    }
    public String toTAC () {
        StringBuilder sb = new StringBuilder();

        if (this.obs) {
            sb.append("OBS");
        } else {
            sb.append("FCST");
        }

        if (this.obsFcTime != null) {
            sb.append(" AT ").append(String.format("%02d", this.obsFcTime.getHour())).append(String.format("%02d", this.obsFcTime.getMinute())).append("Z");
        }

        return sb.toString();
    }
}