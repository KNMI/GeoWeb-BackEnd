package nl.knmi.geoweb.backend.security.models;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.security.core.GrantedAuthority;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Privilege implements GrantedAuthority {
    TEST("test"),
    TAF_EDIT("TAF_edit"),
    TAF_READ("TAF_read"),
    TAF_SETTINGS_EDIT("TAF_settings_edit"),
    TAF_SETTINGS_READ("TAF_settings_read"),
    SIGMET_EDIT("SIGMET_edit"),
    SIGMET_READ("SIGMET_read"),
    SIGMET_SETTINGS_EDIT("SIGMET_settings_edit"),
    SIGMET_SETTINGS_READ("SIGMET_settings_read"),
    AIRMET_EDIT("AIRMET_edit"),
    AIRMET_READ("AIRMET_read"),
    AIRMET_SETTINGS_EDIT("AIRMET_settings_edit"),
    AIRMET_SETTINGS_READ("AIRMET_settings_read");

    private final String authority;

    private Privilege(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @JsonCreator
    static Privilege findValue(@JsonProperty("authority") String authority) {
        return Arrays.stream(Privilege.values())
            .filter(privilege -> privilege.authority.equals(authority))
            .findFirst()
            .get();
    }
}
