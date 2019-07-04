package nl.knmi.geoweb.backend.security.models;

import java.util.Arrays;
import java.util.List;

import lombok.Data;

/**
 * RoleToPrivilegesMapper
 */
@Data
public class RoleToPrivilegesMapper {
    private Privilege[] privileges;
    private String roleName;

    public List<Privilege> getPrivilegesAsList() {
        return Arrays.asList(privileges);
    }
}
