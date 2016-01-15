/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
*/

package com.sendNotification;

import java.util.Properties;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.worklight.adapters.rest.api.WLServerAPI;
import com.worklight.adapters.rest.api.WLServerAPIProvider;
import com.worklight.core.auth.OAuthSecurity;

@Path("/Employee")
public class SendNotificationResource {
	/*
	 * For more info on JAX-RS see https://jsr311.java.net/nonav/releases/1.1/index.html
	 */
		
	//Define logger (Standard java.util.Logger)
	static Logger logger = Logger.getLogger(SendNotificationResource.class.getName());

    //Define the server api to be able to perform server operations
    WLServerAPI api = WLServerAPIProvider.getWLServerAPI();

	static DataSource ds;
	static Context ctx;
	
	private String fullName = "";
	private String timePeriod = "";
	private String subject = "";
	private String body = "";
	private String senderEmail = "";
	private String adminEmail = "";
	
	 public static void init() throws NamingException{
	    	ctx = new InitialContext();
	    	
	    	ds = (DataSource)ctx.lookup("jdbc/V2SoftSync");
	 } 
	
//	@GET
//	@Produces("application/json")
//	@Path("/getManager")
//	@OAuthSecurity(scope="V2SoftRealm")
	public String getManager(String username) throws SQLException{
	
		Connection con = ds.getConnection(); 
		
		PreparedStatement getManager = con.prepareStatement("SELECT * FROM empinfo WHERE empID = " + 
															"(SELECT managerID FROM empinfo " + 
															"WHERE username = ?)");
		
		try{
			String result = "";
			
			getManager.setString(1, username);
			ResultSet data = getManager.executeQuery();
			
			if(data.first()){		
				result = data.getString("email");
				return result;						
			} else{
				return "Manager not found.";
			}
		}
		finally{
			//Close resources in all cases
			getManager.close();
			con.close();
		}
	} 
	
	@GET
	@Produces("application/json")
	@Path("/getSubject")
	@OAuthSecurity(scope="V2SoftRealm")
	public Response getSubject() throws SQLException{
	
		Connection con = ds.getConnection(); 
		
		PreparedStatement getSubject = con.prepareStatement("SELECT * FROM Subject");
		
		JSONArray result = new JSONArray();
			
		ResultSet data = getSubject.executeQuery();
			
		while(data.next()){
			JSONObject item = new JSONObject();	
			item.put("subjectID", data.getString("subjectId"));
			item.put("description", data.getString("description"));
			result.add(item);				
		} 		
		
		//Close resources in all cases
		getSubject.close();
		con.close();
		
		return Response.ok(result).build();
		
	} 
	
	
	@POST
	@Path("/createNotif")
	@Produces("application/json")
	@OAuthSecurity(scope="V2SoftRealm")
	public Response createNotif(@FormParam("username") String username, 
							   @FormParam("fromDate") String fromDate, 
							   @FormParam("toDate") String toDate, 
							   @FormParam("fromTime") String fromTime, 
							   @FormParam("toTime") String toTime,
						       @FormParam("reason") String reason, 
							   @FormParam("note") String note) 
										throws SQLException{
		Connection con = ds.getConnection();
		PreparedStatement getEmployee = con.prepareStatement("SELECT * FROM empinfo ei " + 
															"INNER JOIN admin ad ON " + 
															"ei.adminID = ad.adminID " + 
															"WHERE username = ?");
		
		try{
			getEmployee.setString(1, username);
			ResultSet data = getEmployee.executeQuery();
			
			JSONObject result = new JSONObject();
			
			if(data.first()){
				
				fullName = data.getString("firstName") + " " + data.getString("lastName");
				timePeriod = convertTime(fromTime) + " " + convertDate(fromDate) + " to " + convertTime(toTime) + " " + convertDate(toDate);
				senderEmail = data.getString("email");
				adminEmail = data.getString("adminEmail");
				subject = reason;
				body = note;
				
				PreparedStatement createNotif = con.prepareStatement("INSERT INTO empnotif (empinfoid, fromdate, todate, fromTime, toTime, reason, note) VALUES (?,?,?,?,?,?,?)");
				
				createNotif.setString(1, data.getString("empinfoid"));
				createNotif.setString(2, fromDate);
				createNotif.setString(3, toDate);
				createNotif.setString(4, fromTime);
				createNotif.setString(5, toTime);
				createNotif.setString(6, reason);
				createNotif.setString(7, note);
				createNotif.executeUpdate();
				
				createNotif.close();
				
				result.put("status", "Ok");
				
				return Response.ok(result).build();

							
			} else{
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Record was not created...").build();
			}
		}
		finally{
			//Close resources in all cases
			getEmployee.close();
			con.close();
			sendEmail(username);
		}
		
	}
	
	private void sendEmail(String username){
		final String sender = "someEmail@v2soft.com";
		final String senderPassword = "password";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		//props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "mail2.v2soft.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(sender, senderPassword);
				}
		  });

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(sender));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse("tshemberko@v2soft.com, tshemberko@v2soft.com"));
			//getManager(username) + ", " + adminEmail
			
			message.addRecipient(Message.RecipientType.CC, new InternetAddress(
		            senderEmail));
			message.setSubject("[" + fullName + "]" + ": " + subject);
			message.setText("Hello,"
				+ "\n\nI will be absent from "	+ timePeriod
				+ "\n\n" + body
				+ "\n\nThank you," + "\n" + fullName);

			Transport.send(message);

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static String convertTime(String time){
		SimpleDateFormat toTime = new SimpleDateFormat("hh:mm a");
		SimpleDateFormat fromTime = new SimpleDateFormat("hh:mm");
		String result = "";
		try {
			result = toTime.format(fromTime.parse(time));
		} catch (ParseException e) {		
			e.printStackTrace();
		}
		return result;
	}
	   
	private static String convertDate(String date){
		SimpleDateFormat toDate = new SimpleDateFormat("mm/dd/yyyy");
		SimpleDateFormat fromDate = new SimpleDateFormat("yyyy-mm-dd");
		String result = "";
		try {
			result = toDate.format(fromDate.parse(date));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		 return result;
	}
	
}


