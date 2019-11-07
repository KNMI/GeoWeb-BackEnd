package nl.knmi.geoweb.backend.product.sigmetairmet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;

@JsonInclude(Include.NON_NULL)
@Getter
public class SigmetAirmetMovement {
    private Integer speed;
    private String speeduom;
    private SigmetAirmetDirection dir;

    public SigmetAirmetMovement() {
    }

    public SigmetAirmetMovement(String dir, int speed, String uoM) {
        this.speed = speed;
        this.speeduom = uoM;
        this.dir = SigmetAirmetDirection.getDirection(dir);
    }

    public String getSpeeduom() {
        if (this.speeduom == null) {
            return "KT";
        } else {
            return speeduom;
        }
    }

    public String toTAC() {
        if ((this.dir != null) && (this.speed != null)) {
            if (this.speeduom == null) {
                return "MOV " + this.dir.toString() + " " + this.speed + "KT";
            } else {
                return "MOV " + this.dir.toString() + " " + this.speed + this.speeduom;
            }
        }
        return "";
    }
}
