//this file/object serves as a series of utility functions used by other files
//we add them to this object in the global scope
var util = {}


//returns the server session id within Jetty. we need this as the id
//of Javalin WebSocket sessions doesn't match this (they're a totally different id format)
//so we will send this id to let the server know who we are
//to prevent issues, this value should not be cached, and this function
//should be called every time the value is needed
util.getSessionId = function() {
	
	//we allow setting a custom session id using an id parameter in the url
	//e.g. site.com?id=1 gives a custom id of 1
	var sessionId = new URL(window.location.href).searchParams.get("id");
	
	//if the "fake" session id is null, we attempt to get the real session id
	//stored in the cookie
	if (sessionId === null) {
		
		sessionId = Cookies.get("JSESSIONID");
		
		//if we can't get it, it means jetty hasn't given us the id yet
		if (sessionId === undefined) {
			//best we can do for now is just return some placeholder value
			//usually, it gets defined by the time we need it
			//this is the reason we don't cache this value and call the function every time
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
//calls the given success callback on success
util.postJson = function(url, data, success) {
	return $.ajax({
		url: url,
		type: "POST",
		data: JSON.stringify(data),
		contentType: "application/json;charset=utf-8", //the type you send
		dataType: "json", //the type you receive
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
//if spec is unset (undefined) nothing more happens
//if spec is true, spec=1 is added to the url parameters
//if spec is false, it is removed
util.redirect = function(url, spec) {
	
	if (spec === undefined) {
		//perform a normal redirect, appending the current url params
		window.location.href = url + window.location.search
		return;
	}
	
	//otherwise we need to do a bit more work
	
	//create a url object from the current url
	var obj = new URL(window.location.href);
	
	//add or remove the spec parameter according to spec
	if (spec) {
		obj.searchParams.set("spec", 1);
	} else {
		obj.searchParams.delete("spec");
	}
	
	//now we redirect, using the newly updated params
	//obj.search contains the question mark, obj.searchParams.toString() does not
	window.location.href = url + obj.search;
};
