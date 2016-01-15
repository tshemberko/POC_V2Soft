package com.v2soft;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.worklight.core.auth.ext.WorklightProtocolAuthenticator;
import com.worklight.server.auth.api.AuthenticationResult;
import com.worklight.server.auth.api.AuthenticationStatus;
import com.worklight.server.auth.api.MissingConfigurationOptionException;
import com.worklight.server.auth.api.UserIdentity;
import com.worklight.server.auth.api.WorkLightAuthenticator;

public class V2SoftAuthenticator extends WorklightProtocolAuthenticator{

	private static final long serialVersionUID = -3689591128305954547L;

	private static Logger logger = Logger.getLogger(V2SoftAuthenticator.class.getName());
	
	private static final String GREETING_MESSAGE_PROPERTY = "greetingMessage";
	protected static final String USERNAME_KEY = "auth.username";
	protected static final String PASSWORD_KEY = "auth.password";
	
	private String greetingMessage;
	private String username;
	private String password;

	@Override
	public void init(Map<String, String> option) throws MissingConfigurationOptionException {	
		logger.info("V2SoftAuthenticator :: Initializing. options :: " + option.toString());
		
		super.init(option);
		try {
			greetingMessage = option.remove(GREETING_MESSAGE_PROPERTY).toString();
		} catch (Exception e){
			throw new MissingConfigurationOptionException(GREETING_MESSAGE_PROPERTY);
		}
	}

	@Override
	public AuthenticationResult processRequest(HttpServletRequest request, HttpServletResponse response,
			boolean isAccessToProtectedResource) throws IOException, ServletException {

		logger.info("V2SoftAuthenticator :: processRequest :: " + request.getRequestURI() + ", isAccessToProtectedResource :: " + isAccessToProtectedResource);
		
		if (!isAccessToProtectedResource){
			return AuthenticationResult.createFrom(AuthenticationStatus.REQUEST_NOT_RECOGNIZED);
		}
		
		JSONObject challengeResponse = (JSONObject) getChallengeResponse(request);
		
		if (null == challengeResponse){
			return generateChallenge(greetingMessage);
		} 

		username = (String) challengeResponse.get("username");
		password = (String) challengeResponse.get("password");
		
		if (null == username || null == password || username.length() == 0 || password.length() == 0){
			return generateChallenge("Username and password cannot be blank");
		} else {
			return AuthenticationResult.createFrom(AuthenticationStatus.SUCCESS);
			
		}
	}
	
	private AuthenticationResult generateChallenge(String errorMessage){
		AuthenticationResult authenticationResult = AuthenticationResult.createFrom(AuthenticationStatus.CLIENT_INTERACTION_REQUIRED);
		JSONObject challengeObj = new JSONObject();
		challengeObj.put("authStatus", "credentialsRequired");
		challengeObj.put("errorMessage", errorMessage);
		authenticationResult.setJson(challengeObj);
		return authenticationResult;
	}

	
	@Override
	public AuthenticationResult processRequestAlreadyAuthenticated(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		logger.info("V2SoftAuthenticator :: processRequestAlreadyAuthenticated :: " + request.getRequestURI());
		return AuthenticationResult.createFrom(AuthenticationStatus.REQUEST_NOT_RECOGNIZED);
	}

	
	@Override
	public Map<String, Object> getAuthenticationData() {
		logger.info("V2SoftAuthenticator :: getAuthenticationData");
		Map<String, Object> authData = new HashMap<String, Object>();
		authData.put(USERNAME_KEY, username);
		authData.put(PASSWORD_KEY, password);
		return authData;
	}

	@Override
	public AuthenticationResult processAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			String errorMessage) throws IOException, ServletException {
		logger.info("V2SoftAuthenticator :: processAuthenticationFailure :: " + request.getRequestURI() + ", errorMessage :: " + errorMessage);
		
		if (null == errorMessage){
			return generateChallenge("Invalid credentials. Try again.");
		} else {
			AuthenticationResult result = AuthenticationResult.createFrom(AuthenticationStatus.FAILURE);
			JSONObject resultJson = new JSONObject();
			resultJson.put("failureReason", errorMessage);
			result.setJson(resultJson);
			return result;
		}
	}

}
