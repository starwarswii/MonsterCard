$(function() {
	//we run this code on page load
	
	console.log("ready");
	
	var gameId = $("#gameId").text();
	
	//the server session id within Jetty. we need this as the id
	//of Javalin WebSocket sessions doesnt match this (it's a totally different id format)
	//so we will send this id with each message we send to the server
	//so the server knows who we are
	var sessionId = Cookies.get("JSESSIONID");
	
	var $start = $("#start");
	var $timer = $("#timer");
	
	var $chat = $("#chat");
	
	var $messageBox = $("#messageBox");
	var $send = $("#send");
	
	//TODO could send id with parameters, instead of using path params
	//might be better? i donno
	$.get("/state/"+gameId, function(data) {
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
	
	var socket = new WebSocket("ws://"+location.hostname+":"+location.port+"/game/"+gameId);
	
	function sendMessage(obj) {
		socket.send(JSON.stringify(obj));
	}
	
	socket.onopen = function(event) {
		console.log("Websocket opened");
		console.log(event);
		
		console.log("sending whoiam message");
		sendMessage({type: "whoiam", sessionId: sessionId});
	};
	
	socket.onclose = function(event) {
		console.log("WebSocket closed");
		console.log(event);
	};
	
	socket.onmessage = function(response) {
		console.log("message!");
		
		//TODO rename?
		var map = JSON.parse(response.data);
		
		console.log("got message:", map);
		console.log(response);
		
		var type = map.type;
		
		//TODO there's definitely a nicer way to do this
		switch (type) {
		
			case "chat":
				
				var sender = map.sender;
				var message = map.message;
				
				//quick hack to escape html, so we dont allows rendering whatever html the user types
				//https://stackoverflow.com/a/6234808/3249197
				//TODO is this good enough? better way?
				var escapedMessage = $("<div />").text(message).html();
				
				$chat.append(sender+": "+escapedMessage+"<br>");
				
				break;
				
			case "timer":
				
				var event = map.event;
				
				switch (event) {
				
					case "start":
						$start.prop("disabled", true);
						break;
						
					case "stop":
						$start.prop("disabled", false);
						$timer.text("not running");
						break;
					
					case "value":
						var value = map.value;
						$timer.text(value);
						break;
					
					default:
						console.log("unknown event type ", event);
				}
				
				break;
				
			default:
				console.log("unknown message type ", type);	
		}
	};
	
	$send.click(function() {
		var message = $messageBox.val();
		
		if (message !== "") {
			sendMessage({
				type: "chat",
				//sender: "someone",
				message: message
			});
			
			$messageBox.val("");
		}
	});
	
	$messageBox.keypress(function(event) {
		if (event.keyCode === 13) {//13 is enter
			$send.click();
		}
	});
	
	
	$start.click(function(event) {
		console.log("clicked");
		console.log(event);
		//when the button is clicked, we send "start" to the server
		sendMessage({
			type: "timer",
			command: "start",
			sessionId: sessionId
		});
	});
	
});
