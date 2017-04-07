package nl.knmi.geoweb.backend.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.io.File;

public class Config {
	static String SYS_CONFIG_NAME="system.properties";
	static String ROLE_CONFIG_NAME="role_%s.properties";
	static String USER_CONFIG_NAME="user_%s_properties"; 

	String systemDir;
	String rolesDir;
	String userDir;

	public Config(String systemDir, String rolesDir, String userDir) {
		this.systemDir=systemDir;
		this.rolesDir=rolesDir;
		this.userDir=userDir;
	}


	public void addToSystemProperties(Properties newProps) {
		Properties props=this.getSystemConfig();
		props.putAll(newProps);
		File f=new File(systemDir+"/"+SYS_CONFIG_NAME);
		try {
			props.store(new FileOutputStream(f),null);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addToRoleProperties(String role, Properties newProps) {
		Properties props=this.getRoleConfig(role);
		props.putAll(newProps);
		File f=new File(rolesDir+"/"+String.format(ROLE_CONFIG_NAME, role));
		try {
			props.store(new FileOutputStream(f),null);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addToUserProperties(String user, Properties newProps) {
		Properties props=this.getUserConfig(user);
		props.putAll(newProps);
		File f=new File(userDir+"/"+String.format(USER_CONFIG_NAME, user));
		try {
			props.store(new FileOutputStream(f),null);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Properties getSystemConfig() {
		Properties props=new Properties();
		File f=new File(systemDir+"/"+SYS_CONFIG_NAME);
		try {
			props.load(new FileInputStream(f));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return props;
	}	

	private Properties getRoleConfig(String role) {
		Properties props=new Properties();
		File f=new File(rolesDir+"/"+String.format(ROLE_CONFIG_NAME, role));
		try {
			props.load(new FileInputStream(f));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return props;
	}

	private Properties getUserConfig(String user) {
		Properties props=new Properties();
		File f=new File(userDir+"/"+String.format(USER_CONFIG_NAME, user));
		try {
			props.load(new FileInputStream(f));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return props;
	}

	public Properties getConfig(String user, String[]roles) {
		Properties props=this.getSystemConfig();
		for (String role: roles) {
			props.putAll(this.getRoleConfig(role));
		}
		props.putAll(this.getUserConfig(user));
		return props;
	}

	public static void main(String[]args){
		Config cfg=new Config("/tmp", "/tmp", "/tmp");
		Properties ps=new Properties();
		ps.put("a.b", "system");
		cfg.addToSystemProperties(ps);
		
		Properties pr1=new Properties();
		pr1.put("a.b.c.role", "role1");
		cfg.addToRoleProperties("role1", pr1);
		
		Properties pr2=new Properties();
		pr2.put("a.b.c.role", "role2");
		pr2.put("a.b",  "for role2");
		cfg.addToRoleProperties("role2", pr2);
		
		Properties pu1=new Properties();
		pu1.put("d.e.f.username", "user1");
		cfg.addToUserProperties("user1",  pu1);
		Properties pu2=new Properties();
		pu2.put("d.e.f.username", "user2");
		cfg.addToUserProperties("user2",  pu2);
		
		System.err.println("end: "+cfg.getConfig("user1", new String[]{"role1", "role2"}));
	}
}
