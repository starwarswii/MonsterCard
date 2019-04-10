$(function() {
	//we run this code on page load
	console.log("ready");
	
	var gameId = $("#gameId").text();

	//the server session id within Jetty. we need this as the id
	//of Javalin WebSocket sessions doesnt match this (it's a totally different id format)
	//so we will send this id with each message we send to the server
	//so the server knows who we are
	//TODO check if this is undefined/null/whatever, and show an error, and refresh
	var sessionId = Cookies.get("JSESSIONID");

	// drawing canvas controls
	var $drawingClear = $("#clearcanvas");
	var $drawingColor = $("#drawing_color");
	var $drawingLineWidth = $("#drawing_linewidth");

	// image sender
	var $save = $("#save");

	// timer items
	var $start = $("#start");
	var $timer = $("#timer");

	// chat items
	var $chat = $("#chat");
	var $messageBox = $("#messageBox");
	var $send = $("#send");
	var username = prompt("Please select a username", "someone");

	// change state
	var $next = $("#next");

	// used to hide and show items
	var $wrapper = $("#wrapper");
	var $voteButtons = $("#voteButtons");
	var $canvasControl = $("#canvasControls");


	// CANVAS CODE =====================================================================================================
	// intilize drawing canvas, allows drawing
	var c1 = new fabric.Canvas('c1', {
		isDrawingMode: true
	});
	// intilize display canvas, unable to edit only displays image of what was retrieved from server
	var c2 = new fabric.StaticCanvas('c2', {
		selectable: false
	});

	var undo = []; // undo stack
	var redo = []; // redo stack
	var state; // the last state of the drawingCanvas
	var drawing = true;

	// when canvas is modified, record any changes to the undo stack and clear redo stack
	c1.on("object:added", function () {
		if(drawing) {
			redo = []; // clears all redo states
			undo.push(state); // adds the last state before drawing to the stack
			state = JSON.stringify(c1); // updates the state for undomanager
		}
	});
	c1.on("object:modified", function () {
		if(drawing) {
			redo = [];
			undo.push(state);
			state = JSON.stringify(c1);
		}
	});

	// shifts the state over depending on which stack is given
	function replay(playStack, saveStack) {
		saveStack.push(state);
		state = playStack.pop();
		c1.clear();
		drawing = false;
		c1.loadFromJSON(state, function() {
			c1.renderAll();
		});
		drawing = true;
	}

	// helper function to load any img SVG to the given canvas
	function loadImg(canvas, SVG) {
		fabric.loadSVGFromString(SVG, function(objects, options) { // parses in data into a callback function and
			var obj = fabric.util.groupSVGElements(objects, options); // creates the canvas object from the SVG input
			canvas.clear(); // clears the canvas so it can render the new SVG
			canvas.add(obj).renderAll();
		});
	}

	document.addEventListener('keydown', function(event) {
		if(event.ctrlKey) {
			if (event.keyCode === 90) { // undo
				if(undo.length > 0) { // won't undo if there is no undo state left
					replay(undo, redo);
				}
			} else if (event.keyCode === 89) { // redo
				if(redo.length > 0) { // won't redo if there is no redo state left
					replay(redo, undo);
				}
			}
		}
	});

	// clear canvas button
	$drawingClear.click(function() {
		c1.clear();
	});

	// edits the brush color
	$drawingColor.on("change", function() {
		c1.freeDrawingBrush.color = $drawingColor.val();
	});

	// edits the brush width
	$drawingLineWidth.on("change", function() {
		c1.freeDrawingBrush.width = parseInt($drawingLineWidth.val(), 10) || 1;
		this.previousSibling.innerHTML = $drawingLineWidth.val();
	});

	// updates the brush when free drawing is turned on
	if (c1.freeDrawingBrush) {
		c1.freeDrawingBrush.color = $drawingColor.val();
		c1.freeDrawingBrush.width = parseInt($drawingLineWidth.val(), 10) || 1;
	}
	// END OF CANVAS CODE ==============================================================================================

	
	//TODO could send id with parameters, instead of using path params
	//might be better? i donno
	$.get("/state/"+gameId, function(data) {
		console.log("did GET to /state, got back:", data);
		
		var isOwner = data.isOwner;
		var isRunning = data.isRunning;
		
		if (!isOwner) {
			$start.hide();
			$next.hide();
		}
		
		if (isRunning) {
			var timerValue = data.value;
			$start.prop("disabled", true);
			$timer.text(timerValue);
		}

		initializeStartGame($chat, $wrapper, $canvasControl, $voteButtons);	
	});
	
	var socket = new WebSocket("ws://"+location.hostname+":"+location.port+"/game/"+gameId);
	
	function sendMessage(obj) {
		socket.send(JSON.stringify(obj));
	}
	
	socket.onopen = function(event) {
		console.log("Websocket opened");
		console.log(event);
		
		console.log("sending whoiam message");
		sendMessage({
			type: "whoiam",
			username: username,
			sessionId: sessionId
		});
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

			// TODO: add number to which canvas to paint
			case "image":

				var displayImage = map.img;
				// var canvasNum = map.num;
				loadImg(c2, displayImage);

				break;

			case "changeState":
				var value = map.value;
				console.log(value);

				switch (value) {
					case "voting":
						initializeVoting($chat, $wrapper, $canvasControl, $voteButtons, c1);
						break;
					case "drawing":
						initializeDrawing($chat, $wrapper, $canvasControl, $voteButtons, c1);
						break;
					case "end":
						initializeEnd($chat, $wrapper, $canvasControl, $voteButtons);
						break;
					case "before":
						initializeStartGame($chat, $wrapper, $canvasControl, $voteButtons);
						break;
					default:
						console.log("unknown, defaulting to end game")
						initializeEnd($chat, $wrapper, $canvasControl, $voteButtons);
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
				sender: username,
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

	// TODO fix max message size
	// message size too large for 536
	// https://stackoverflow.com/questions/17497173/jetty-9-websocket-server-max-message-size-on-session
	$save.click(function() { // converts the canvas to SVG and sends it to the server
		console.log("image sent:")
		// console.log(drawingCanvas.toSVG());
		// when button is clicked, we send the image SVG to the server to be stored
		sendMessage({
			type: "image",
			img: c1.toSVG()
		});
	});

	$next.click(function() {
		sendMessage({
			type: "changeState"
		});
	});

});
