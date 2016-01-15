//  Author: Taras Shemberko

var busyIndicator;
var request;
var user = "";
var collectionName = 'rememberMe';

function wlCommonInit(){
			
	$("#mainPage").hide();
	$("#customReason").hide();
	
	callJSONStore();	
	
	//WL.App.setServerUrl("http://192.168.229.164:10080/V2SoftSync",null,null);
	busyIndicator = new WL.BusyIndicator( null, {text : 'Loading...'});
	busyIndicator.show();
	
	//check Internet connection
	WL.Device.getNetworkInfo(
			function (networkInfo) {
				busyIndicator.hide();
				if (networkInfo.isNetworkConnected == "false") {
		        	$("#internetInfo").text("No Internet Connectivity!");
				}
			}
		);
	
	
	request = new WLResourceRequest("/adapters/sendNotification/Employee/getSubject", WLResourceRequest.GET);
	request.send().then(function(response){
		//Load subjects from DB
	    setSubject(response);		
	//	WL.Logger.debug("Received response!");
	});
	
	WL.Client.setHeartBeatInterval(5);
	
	//check connection to the server
	document.addEventListener(WL.Events.WORKLIGHT_IS_CONNECTED, connectDetected, false);
	document.addEventListener(WL.Events.WORKLIGHT_IS_DISCONNECTED, disconnectDetected, false);

	
}
 	
var challengeHandler = WL.Client.createWLChallengeHandler("V2SoftRealm");
challengeHandler.logger = WL.Logger.create({pkg:"challengeHandler"});

challengeHandler.handleChallenge = function(challenge){
    var authStatus = challenge.authStatus;
	//this.logger.info("handleChallenge :: authStatus :: " + authStatus)
 
    if (authStatus == "credentialsRequired"){
        $("#mainPage").hide();
        $("#divLogin").show();
        $("#AuthUsername").empty();
        $("#AuthPassword").empty();
    } 
	
	if (challenge.errorMessage != "Please provide your credentials!"){
		showMessage("Error", challenge.errorMessage);
	}
	
}

function loginButtonClick(){	 
    challengeHandler.submitChallengeAnswer({
    	username: $("#AuthUsername").val(),
    	password: $("#AuthPassword").val()
    });
}

challengeHandler.processSuccess = function (data){
	busyIndicator = new WL.BusyIndicator( null, {text : 'Loading...'});
	busyIndicator.show();
	//this.logger.info("processSuccess ::", data);
	
	
	//adding username to a JSONStore
    if ($("#rememberMe").is(':checked')){
    	if (user == "" || user == null){
    		addToJSONStore(true, $("#AuthUsername").val());
    	} else if (user != $("#AuthUsername").val()){
    		replaceInJSONStore($("#AuthUsername").val());
    	} 	
    } else {
    	deleteDataInJSONStore();
    }
    
	var now = new Date();
	$("#fromDate,#toDate").val(now.getFullYear()+"-"+("0" + (now.getMonth() + 1)).slice(-2)+"-"+
			 ("0" + now.getDate()).slice(-2));
	 
	$("#fromTime,#toTime").val(now.getHours() + ":" + now.getMinutes());
	
    $("#divLogin").hide();
    $("#mainPage").show();   
    
    request.setQueryParameter("username", $("#AuthUsername").val());
    busyIndicator.hide();
}

challengeHandler.handleFailure = function (data){
//	this.logger.info("handleFailure ::", data);
	//showMessage("Error", "We are temporarily unable to connect to Server. Please try again later.");
    $("#divLogin").show();
    $("#mainPage").hide();
}

function senNotification(){
	var option = $("#subject option:selected").text();	
	var req = new WLResourceRequest("/adapters/sendNotification/Employee/createNotif", WLResourceRequest.POST, 3000);
	
	//validate subject
	if ($("#subject").val() == "" || $("#subject").val() == null){
		showMessage("Error","Please choose a reason!");
		return;
	} 
	
	//validate note
	if ($("#note").val() == "" || $("#note").val() == null){
		showMessage("Error","Please enter detals!");
		return;
	}
	
	//validate custom reason and take data
	if ($("#subject").val() == "other"){
		if ($("#customReason").val() == "" || $("#customReason").val() == null){
			showMessage("Error","Please enter subject.")
			return;
		} else {
			option = $("#customReason").val();
		}
		
	}
	
	//validate dates
	if (new Date($("#toDate").val()) < new Date($("#fromDate").val())){
		showMessage("Error","Invalid date range.");
		return;
	}
	
	//validate time
	if (Date.parse($("#toDate").val() + ' ' + $("#toTime").val()) <
			Date.parse($("#fromDate").val() + ' ' + $("#fromTime").val())){
		showMessage("Error","Invalid time range.");
		return;
	}
	
	req.sendFormParameters({
		username: $("#AuthUsername").val(),
	    fromDate: $("#fromDate").val(),
	    toDate:	$("#toDate").val(),
	    fromTime: $("#fromTime").val(),
	    toTime:	$("#toTime").val(),
	    reason: option,
	    note: $("#note").val()
	}).then(function(response){
		if (response.responseJSON.status == "Ok"){
			showMessage("Alert","We received your information.");
		} else {
			showMessage("Error","We didn't receieve your information.\nPlease call your manager.");
		}
	},
	function(error){
		showMessage("Error", "We are temporarily unable to connect to Server. Please try again later.");
	});
}

$("#subject").change(function(){
	if ($("#subject").val() == "other"){
		$("#customReason").show();	
	} else {
		$("#customReason").empty();
		$("#customReason").hide();
	}
});

function setSubject(response){
	
		var res = response.responseJSON;	
		
		for (var i = 0; i < res.length; i++){
			$("#subject").append($("<option></option>").val(res[i].subjectID).html(res[i].description));
		};
		$("#subject").append($("<option></option>").val("other").html("Other"));
	
	busyIndicator.hide();
}

function logOut(){
	WL.SimpleDialog.show(
			"Logout", 
			'You are about to be logged out. Tap "Cancel" to continue your session ' +
			'or tap "Logout" to end your serrion now',
			[{
				text: "Logout", handler: function() {WL.Client.logout('V2SoftRealm', {onSuccess:
					WL.Client.reloadApp}); }
				
			},
			{
				text: "Cancel"
			}]);
	
}

function showMessage(title, text){
	WL.SimpleDialog.show(
		    title,
		     text, 
		     [{
		         text: "OK"
		     }]
		);
}

function connectDetected(){
	WL.Client.setHeartBeatInterval(10);
	request.send();
	$("#internetInfo").hide();
}

function disconnectDetected(){
	WL.Client.setHeartBeatInterval(1);
	$("#internetInfo").show();
	showMessage("Error", "We are temporarily unable to connect to Server. Please try again later.");
	$("#internetInfo").text("No Internet Connectivity!");
}

function callJSONStore(){
	//Object that defines all the collections
	var collections = {};
	//Object that defines the 'rememberMe' collection
	collections[collectionName] = {};
	//Object that defines the Search Fields for the 'rememberMe' collection
	collections[collectionName].searchFields = {rememberme: 'boolean', username: 'string'}
	WL.JSONStore.init(collections)
	.then(function () {
		//lookup for a record
		findInJSONStrore();	
	})
	.fail(function (errorObject) {
		return;
	});	
}

function addToJSONStore(checked, username){
	var data = {rememberme: checked, username: username};
	var options = {}; //default
	WL.JSONStore.get(collectionName)
	.add(data, options)
	.then(function () {
		return;
	})
	.fail(function (errorObject) {
		return;
	});
}

function findInJSONStrore(){
	WL.JSONStore.get(collectionName)
	.findById(1)
	.then(function (result) {
	//result = [{_id: 1, json: {rememberme: 'boolean', username: 'string'}}]
		for (var i = 0; i < result.length; i++){
			//alert(result[i].json.username);
			$("#AuthUsername").val(result[i].json.username);
			$("#rememberMe").prop('checked', result[i].json.rememberme);
			user = result[i].json.username;
		}		
	})
	.fail(function (errorObject) {
		return;
	});
}

function replaceInJSONStore(username){
	var document = {_id: 1, json: {rememberme: true, username: username}};
	var options = {}; //default
	WL.JSONStore.get(collectionName)
	.replace(document, options)
	.then(function () {
		return;
	})
	.fail(function (errorObject) {
		return;
	});
}

function deleteDataInJSONStore(){
	WL.JSONStore.get(collectionName)
	.removeCollection()
	.then(function () {
		return;
	})
	.fail(function (errorObject) {
		return;
	});
}