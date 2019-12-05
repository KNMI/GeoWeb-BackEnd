package nl.knmi.geoweb.backend.usermanagement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserLogin {
	/**
	 * Return null if not signed in, otherwise returns the username.
	 * 
	 * @return
	 */
	public static String getUserName() {
		if (SecurityContextHolder.getContext().getAuthentication() == null
				|| !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
				|| (SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
			return null;
		} else {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			return auth.getName();
		}
	}

	/**
	 * Returns list of roles, returns list with length 0 if not signed in.
	 * 
	 * @return
	 */
	public static String[] getUserPrivileges() {
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
