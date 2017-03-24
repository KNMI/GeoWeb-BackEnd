package nl.knmi.geoweb.backend.usermanagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

public class UserStore {

	@Getter
	public class GeoWebUser {
		private String username;
		private String password;
		private List<String>roles=new ArrayList<String>();
		public GeoWebUser(String nm, String pw, String[]roles) {
			username=nm;
			password=pw;
			for (String role: roles) {
				this.roles.add(role);
			}
		}
		public GeoWebUser(String nm, String pw, List<String>roles) {
			username=nm;
			password=pw;
			for (String role: roles) {
				this.roles.add(role);
			}
		}
	}
	
	private static UserStore instance;

	public boolean userHasRole(String user, String role) {
		if (store.containsKey(user)){
          for (String r: store.get(user).getRoles()) {
        	  if (r.equals(role)) return true;
          }
		}
		return false;
	}
	
	public String[] getUserRoles(String user) {
		if (store.containsKey(user)){
			return store.get(user).getRoles().toArray(new String[0]);
		}
		return null;
	}
	
	public static UserStore getInstance() {
		if (instance==null) {
			synchronized (UserStore.class) {
				instance=new UserStore();
				instance.generateUserStore();
			}
		}
		return instance;
	}

	private Map<String, GeoWebUser> store;

	public UserStore() {
	}

	private void addUserToStore(String name, String pw, String[]roles){
		store.put(name, new GeoWebUser(name, pw, roles));
	}

	private void generateUserStore() {
		store=new HashMap<String, GeoWebUser>();
		addUserToStore("ernst", "ernst",new String[]{"USER"});
		addUserToStore("guest", "guest",new String[]{"USER"});
		addUserToStore("met1", "met1",new String[]{"USER","MET"});
		addUserToStore("met2", "met1",new String[]{"USER", "MET"});
		addUserToStore("admin", "admin",new String[]{"USER","ADMIN"});
	}

	public GeoWebUser getUser(String nm) {
		if (store==null) {
			generateUserStore();
		}
		for (String k: store.keySet()){
			if (k.equals(nm)) {
				GeoWebUser found=store.get(k);
				GeoWebUser user=new GeoWebUser(found.getUsername(), "###", found.getRoles());
				return user;
			}
		}
		return null;
	}
	
	public GeoWebUser checkUser(String nm, String passwd) {
		if (store==null) {
			generateUserStore();
		}
		for (String k: store.keySet()){
			if (k.equals(nm) && store.get(k).getPassword().equals(passwd)) {
				GeoWebUser found=store.get(k);
				GeoWebUser user=new GeoWebUser(found.getUsername(), "###", found.getRoles());
				return user;
			}
		}
		return null;
	}
}
