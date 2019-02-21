$(function() {
	//we run this code on page load
	
	console.log("ready");
	
	//set up jquery variables for the two elements we need
	//based on id, using css selectors
	var $start = $("#start");
	var $timer = $("#timer");
	
	//we start with a get request to /state to see if the timer is running when the page finishes loading
	$.get("/state", function(data) {
		console.log("did GET to /state, got back:", data);
		
		//if we don't get back "stopped", then we got back a timer value
		if (data !== "stopped") {
			//so then the timer is running, so we disable the button and
			//put the current time we got in the timer
			$start.prop("disabled", true);
			$timer.text(data);
		}
	});
	
	//set up a websocket to root/timer
	var socket = new WebSocket("ws://"+location.hostname+":"+location.port+"/timer");
	
	//when we open or close, we just log it in the console
	socket.onopen = function(event) {
		console.log("Websocket opened");
		console.log(event);
	};
	
	socket.onclose = function(event) {
		console.log("WebSocket closed");
		console.log(event);
	};
	
	//an error is unlikely to occur, but this is here
	//just to note that it exists
	socket.onerror = function(error) {
		console.log("Websocket error:", error);
	};
	
	//when we get a message
	socket.onmessage = function(msg) {
		console.log("got message:", msg.data);
		console.log(msg);
		
		//if the server tells us the timer started,
		//we disable our button
		if (msg.data === "start") {
			$start.prop("disabled", true);
			
		//if it told us the timer stopped, we enable
		//the button and update the timer text
		} else if (msg.data === "stop") {
			$start.prop("disabled", false);
			$timer.text("not running");
		
		//otherwise, if the message doesn't say "start" or "stop",
		//then it must be a number representing the current
		//timer value, so we update our display
		} else {
			$timer.text(msg.data);
		}
	};
	
	//set up a click event handler on the button
	//note than in javascript, functions can be called with more arguments than they have
	//this means that you can give these kinds of jquery handlers functions like function() {...}
	//even though it's going to try to call it with an event parameter
	//this is totally fine, and lets you use function() if you don't need to
	//use the event, which is pretty common
	//i'm receiving and logging the event here just so we can look at what it's made of
	//in this case it doesn't actually have any info we need
	$start.click(function(event) {
		console.log("clicked");
		console.log(event);
		//when the button is clicked, we send "start" to the server
		socket.send("start");
	});
});
