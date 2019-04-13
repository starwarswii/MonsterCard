var util = {}

util.getSessionId = function() {
	//the server session id within Jetty. we need this as the id
	//of Javalin WebSocket sessions doesn't match this (it's a totally different id format)
	//so we will send this id to let the server know who we are
	
	//we allow setting a custom session id using an id parameter in the url
	//note this will not work correctly if you attempt to be the owner using a custom session id
	var sessionId = new URL(window.location.href).searchParams.get("id");
	
	if (sessionId === null) {
		
		sessionId = Cookies.get("JSESSIONID");
		
		if (sessionId === undefined) {
			//best we can do for now
			//usually, it gets defined by the time we need it
			return "undefined";
		}
		
		//for whatever reason the JSESSIONID cookie is "xxxxxxxxx.yyyy"
		//and the actual session id is "xxxxxxxxx", so we split it off before sending
		//this actually also works if for some reason the cookie doesn't contain a period
		sessionId = sessionId.split(".")[0];
	}
	
	return sessionId;
};

//helper function that posts json to a url, and gets json back
util.postJson = function(url, data, success) {
	return $.ajax({
		url: url,
		type: "POST",
		data: JSON.stringify(data),
		contentType: "application/json;charset=utf-8", //what you send
		dataType: "json", //what you receive
		success: success,
		error: function(jqXHR, textStatus, errorThrown) {
			console.log("an ajax error occured");
			console.log("jqXHR", jqXHR);
			console.log("textStatus", textStatus);
			console.log("errorThrown", errorThrown);
		}
	});
};

//redirects to the given url, keeping url parameters
//this allows us to keep our fake session id if it's present
util.redirect = function(url) {
	window.location.href = url + document.location.search
};
