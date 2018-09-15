package nl.knmi.geoweb.backend.usermanagement;

import java.util.HashMap;
import java.util.Map;

import nl.knmi.geoweb.backend.usermanagement.model.GeoWebUser;
import nl.knmi.geoweb.backend.usermanagement.model.RoleType;

public class UserStore {

	private static UserStore instance;

	public boolean userHasRole(String user, String role) {
		if (store.containsKey(user)){
          for (RoleType r: store.get(user).getRoles()) {
        	  if (r.toString().equals(role)) return true;
          }
		}
		return false;
	}
	
	public String[] getUserRoles(String user) {
		if (store.containsKey(user)){
			return store.get(user).getRoleNames().toArray(new String[0]);
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

	private void addUserToStore(String name, String pw, RoleType[]roles){
		store.put(name, new GeoWebUser(name, pw, roles));
	}

	private void generateUserStore() {
		store=new HashMap<String, GeoWebUser>();
		addUserToStore("ernst", "ernst",new RoleType[]{RoleType.USER});
		addUserToStore("guest", "guest",new RoleType[]{RoleType.USER});
		addUserToStore("met1", "met1",new RoleType[]{RoleType.USER,RoleType.MET});
		addUserToStore("beheerder1", "beheerder1",new RoleType[]{RoleType.USER,RoleType.ADMIN});
		addUserToStore("met2", "met1",new RoleType[]{RoleType.USER, RoleType.MET});
		addUserToStore("admin", "admin",new RoleType[]{RoleType.USER,RoleType.ADMIN});
		addUserToStore("admin", "admin",new RoleType[]{RoleType.USER, RoleType.ADMIN});
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
