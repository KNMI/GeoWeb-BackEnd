package nl.knmi.geoweb.backend.usermanagement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Value;
import nl.knmi.geoweb.backend.security.services.SecurityServices;

@RestController
public class UserLogin {
    private static String activeProfiles;

	@Value("${spring.profiles.active:}")
	public void setActiveProfiles(String actProfile){
		activeProfiles = actProfile;
	}

	/**
	 * Return null if not signed in, otherwise returns the username.
	 * If profile is generic - return MET1 
	 * @return
	 */
	public static String getUserName() {
		if(SecurityServices.isProfileActive(activeProfiles, "generic")){
			return "MET1";
        }else{
			if (SecurityContextHolder.getContext().getAuthentication() == null
					|| !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
					|| (SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
				return null;
			} else {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				return auth.getName();
			}
		}
	}

	/**
	 * Returns list of roles, returns list with length 0 if not signed in.
	 * 
	 * @return
	 */
	public static String[] getUserPrivileges() {
		if(SecurityServices.isProfileActive(activeProfiles, "generic")){
			String[] privileges = {"AIRMET_edit", "AIRMET_read", "AIRMET_settings_read", "SIGMET_edit", "SIGMET_read", 
			                       "SIGMET_settings_read", "TAF_edit", "TAF_read", "TAF_settings_read", "WMSSERVICES_FORECASTER"};
			return privileges;
        }else{
			if (SecurityContextHolder.getContext().getAuthentication() == null
					|| !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
					|| (SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
				return "ANONYMOUS".split(",");
			} else {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				// return auth.getAuthorities().stream().toArray(String[]::new);
				// List<GrantedAuthority> grantedAuthorities = new
				// ArrayList<GrantedAuthority>(); for (Authority auth : auths)
				// grantedAuthorities.add(new GrantedAuthorityImpl(auth.getName())); return
				// grantedAuthorities; }
				List<String> va = auth.getAuthorities().stream().map(authority -> authority.getAuthority())
						.collect(Collectors.toList());
				return va.toArray(new String[0]);
					// return (String[]) auth.getAuthorities().stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()).toArray();
			}
		}
	}
}
