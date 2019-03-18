$(function() {
	//we run this code on page load
	
	console.log("ready");
	
	var roomId = $("#roomId").text();
	
	//the server session id within Jetty. we need this as the id
	//of Javalin WebSocket sessions doesnt match this (it's a totally different id format)
	//so we will send this id with each message we send to the server
	//so the server knows who we are
	var sessionId = Cookies.get("JSESSIONID");
	
	var $start = $("#start");
	var $timer = $("#timer");
	
	//TODO could send id with parameters, instead of using path params
	//might be better? i donno
	$.get("/state/"+roomId, function(data) {
		console.log("did GET to /state, got back:", data);
		
		var isOwner = data.isOwner;
		var isRunning = data.isRunning;
		
		if (!isOwner) {
			$start.hide();
		}
		
		if (isRunning) {
			var timerValue = data.value;
			$start.prop("disabled", true);
			$timer.text(timerValue);
		}
	});
	
	var socket = new WebSocket("ws://"+location.hostname+":"+location.port+"/room/"+roomId);
	
	function sendMessage(message) {
		socket.send(JSON.stringify({id: sessionId, message: message}));
	}
	
	socket.onopen = function(event) {
		console.log("Websocket opened");
		console.log(event);
	};
	
	socket.onclose = function(event) {
		console.log("WebSocket closed");
		console.log(event);
	};
	
	socket.onmessage = function(response) {
		console.log("message!");
		
		var message = response.data;
		
		console.log("got message:", message);
		console.log(response);
		
		if (message == "start") {
			$start.prop("disabled", true);
			
		} else if (message == "stop") {
			$start.prop("disabled", false);
			$timer.text("not running");
		
		} else {
			$timer.text(message);
		}
	};
	
	$start.click(function(event) {
		console.log("clicked");
		console.log(event);
		//when the button is clicked, we send "start" to the server
		sendMessage("start");
	});
});
