package com.v2soft;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.worklight.server.auth.api.MissingConfigurationOptionException;
import com.worklight.server.auth.api.UserIdentity;
import com.worklight.server.auth.api.WorkLightAuthLoginModule;
import com.worklight.server.auth.api.WorkLightLoginModuleBase;

public class V2SoftLoginModule implements WorkLightAuthLoginModule{


	private static final long serialVersionUID = -4520393464219450146L;
	private static final Logger logger = Logger.getLogger(V2SoftLoginModule.class.getName());
	
	private static DataSource ds = null;
    private static Context ctx = null;

    private String receivedUsername;
    
    @Override
	public void init(Map<String, String> options) throws MissingConfigurationOptionException {
    	logger.info("V2SoftLoginModule :: Initializing. options :: " + options.toString());	
	}
    
	@Override
	public boolean login(Map<String, Object> authenticationData) {
		logger.info("V2SoftLoginModule :: login. authenticationData :: " + authenticationData.toString());
		 
		try {
			ctx = new InitialContext();
			ds = (DataSource)ctx.lookup("jdbc/V2SoftSync");
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			logger.info("V2SoftLoginModule :: NamingException :: " + e);
		}
      	    
		receivedUsername = (String) authenticationData.get(V2SoftAuthenticator.USERNAME_KEY);
		String receivedPassword = (String) authenticationData.get(V2SoftAuthenticator.PASSWORD_KEY);
        
		Connection con;
		try {
			con = ds.getConnection();
			PreparedStatement getUser = con.prepareStatement("SELECT * FROM empinfo");
			ResultSet data = getUser.executeQuery();
			
			while(data.next()){
				if ((data.getString("username").equals(receivedUsername) && data.getString("password").equals(receivedPassword))){
					getUser.close();
					con.close();
					return true;
				} 
			}
		} catch (SQLException e) {
			logger.info("V2SoftLoginModule :: SQLException :: " + e);
		}
		
		return false;
	}
	
	@Override
	public UserIdentity createIdentity(String loginModule) {
		logger.info("V2SoftLoginModule :: createIdentity. realm :: " + loginModule);
		
		HashMap<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("customAttrName", "customAttrValue");
		
		UserIdentity userIdentity = new UserIdentity(loginModule, receivedUsername, receivedUsername, null, attributes, null);
		return userIdentity;
	}


	@Override
	public void logout() {
		logger.info("V2SoftLoginModule :: logout");
		receivedUsername = null;
		
	}

	@Override
	public void abort() {
		// TODO Auto-generated method stub
		
	}

	

	@Override
	public WorkLightLoginModuleBase clone() throws CloneNotSupportedException {
		return (WorkLightLoginModuleBase) super.clone(); 
	}

	
}
