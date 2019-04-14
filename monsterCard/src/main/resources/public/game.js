$(function() {
	//we run this code on page load
	console.log("ready");

	var gameId = $("#gameId").text();

	var sessionId = util.getSessionId;

	// drawing canvas controls
	var $drawingClear = $("#clearcanvas");
	var $drawingColor = $("#drawing_color");
	var $drawingLineWidth = $("#drawing_linewidth");

	// image sender
	//TODO rename?
	var $save = $("#save");

	// timer items
	var $start = $("#start");
	var $timer = $("#timer");

	// chat items
	var $chat = $("#chat");
	var $messageBox = $("#messageBox");
	var $send = $("#send");

	// vote items
	var $vote1 = $("#vote1");
	var $vote2 = $("#vote2");
	var $score1 = $("#score1");
	var $score2 = $("#score2");
	var voted = 0;

	// change state
	var $next = $("#next");

	// used to hide and show items
	var $wrapper = $("#wrapper");
	var $canvasControl = $("#canvasControls");

	var $currentState = $("#currentState");

	var $leave = $("#leave");

	// CANVAS CODE =====================================================================================================
	
	// initialize drawing canvas, allows drawing
	var drawC = new fabric.Canvas('drawC', {isDrawingMode: true});
	
	// initialize display canvas, unable to edit only displays image of what was retrieved from server
	var c1 = new fabric.StaticCanvas('c1', {selectable: false});
	var c2 = new fabric.StaticCanvas('c2', {selectable: false});

	var undo = []; // undo stack
	var redo = []; // redo stack
	var state; // the last state of the drawingCanvas
	var drawing = true;

	var username = null;
	var isSpectator;
	
	function sendCard() {
		sendMessage({
			type: "card",
			value: drawC.toSVG()
		});
	}

	// STATES ==========================================================

	//TODO maybe put these functions in an object or something to keep them more separate maybe
	//or not, this could be fine

	function switchToState(map) {

		var state = map.currentState;

		console.log("switching to state", state);

		$currentState.text(state);

		//TODO could be replaced by an object with functions, or something
		switch (state) {
			case "BEFORE_GAME":
				initializeStartGame();
				break;

			case "VOTING":
				initializeVoting();

				$("#results-c1").text("Card 1: by "+map.player1);
				loadImg(c1, map.card1);
				$score1.text("Votes: "+map.votes1);
				
				$("#results-c2").text("Card 2: by "+map.player2);
				loadImg(c2, map.card2);
				$score2.text("Votes: "+map.votes2);

				break;

			case "DRAWING":
				initializeDrawing();
				break;

			case "END_GAME":
				initializeEnd();
				break;

			default:
				console.log("unknown state", state);
		}
	}

	function initializeStartGame() {
		$wrapper.hide();
		$canvasControl.hide();
		$("#drawCanvas").hide();
		$("#displayCanvas").hide();
	}

	function initializeDrawing() {
		if (!isSpectator) {
			$wrapper.show();
			$canvasControl.show();
			$("#results-draw").show();
			$("#drawCanvas").show();
		}

		$("#results-c1").hide();
		$("#results-c2").hide();		
		$("#displayCanvas").hide();
	}

	function initializeVoting() {
		voted = 0;
		$wrapper.show();
		$canvasControl.hide();
		$score1.text("Votes: 0");
		$score2.text("Votes: 0");
		$("#results-draw").hide();
		$("#results-c1").show();
		$("#results-c2").show();
		$("#drawCanvas").hide();
		$("#displayCanvas").show();
	}

	function initializeEnd() {
		$wrapper.hide();
		$canvasControl.hide();
		$("#drawCanvas").hide();
		$("#displayCanvas").hide();
	}

	// when canvas is modified, record any changes to the undo stack and clear redo stack
	drawC.on("object:added", function() {
		if (drawing) {
			redo = []; // clears all redo states
			undo.push(state); // adds the last state before drawing to the stack
			state = JSON.stringify(drawC); // updates the state for undomanager

			//"autosave" every time you add a stroke
			sendCard();
		}
	});
	drawC.on("object:modified", function() {
		if (drawing) {
			redo = [];
			undo.push(state);
			state = JSON.stringify(drawC);

			sendCard();
		}
	});

	// shifts the state over depending on which stack is given
	function replay(playStack, saveStack) {
		saveStack.push(state);
		state = playStack.pop();
		drawC.clear();
		drawing = false;
		drawC.loadFromJSON(state, function() {
			drawC.renderAll();
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
		if (event.ctrlKey) {
			if (event.keyCode === 90) { // undo
				if (undo.length > 0) { // won't undo if there is no undo state left
					replay(undo, redo);
					sendCard();
				}
			} else if (event.keyCode === 89) { // redo
				if (redo.length > 0) { // won't redo if there is no redo state left
					replay(redo, undo);
					sendCard();
				}
			}
		}
	});

	// clear canvas button
	$drawingClear.click(function() {
		drawC.clear();
		sendCard();
	});

	// edits the brush color
	$drawingColor.on("change", function() {
		drawC.freeDrawingBrush.color = $drawingColor.val();
	});

	// edits the brush width
	$drawingLineWidth.on("change", function() {
		drawC.freeDrawingBrush.width = parseInt($drawingLineWidth.val(), 10) || 1;
		this.previousSibling.innerHTML = $drawingLineWidth.val();
	});

	// updates the brush when free drawing is turned on
	if (drawC.freeDrawingBrush) {
		drawC.freeDrawingBrush.color = $drawingColor.val();
		drawC.freeDrawingBrush.width = parseInt($drawingLineWidth.val(), 10) || 1;
	}
	// END OF CANVAS CODE ==============================================================================================

	//TODO rename "state" url to something else
	util.postJson("/state/"+gameId, {type: "amINew", sessionId: sessionId()}, function(response) {
		console.log("sent amINew, got back:", response);

		if (response.newUser) {

			//prompt returns null if they exit or click cancel
			//so we loop till they give an answer
			while (username === null) {
				username = prompt("enter a username", "someone");
			}

			//note we only check for spectator if a new player
			//we are assuming normal players won't rejoin as spectators without leaving first
			isSpectator = new URLSearchParams(window.location.search).has("spec");
			console.log("isSpectator from url is", isSpectator);
			
			//we don't really care about response from this one, but after it we want to set up the websocket
			util.postJson("/state/"+gameId,
				{
					type: "createUser",
					sessionId: sessionId(),
					username: username,
					isSpectator: isSpectator
				},
				
				loadHttpState
			);
			
			

		} else {
			username = response.username;
			loadHttpState()
		}
	});

	
	//sets up all variables for local state, then calls setUpWebsocket()
	function loadHttpState() {
		//TODO could send id with parameters, instead of using path params
		//might be better? i donno
		util.postJson("/state/"+gameId, {type: "state", sessionId: sessionId()}, function(response) {
			console.log("did POST to /state, got back:", response);

			var isOwner = response.isOwner;
			var timerRunning = response.timerRunning;
			
			isSpectator = response.isSpectator;
			console.log("isSpectator from state is", isSpectator);
			
			if (!isOwner) {
				$start.hide();
				$next.hide();
			} else {
				$leave.hide();
			}

			if (timerRunning) {
				var timerValue = data.value;
				$start.prop("disabled", true);
				$timer.text(timerValue);
			}

			switchToState(response);
			
			setUpWebsocket();
		});
	}
	
	

	//is initialized in setUpWebsocket(), which will be called in the "amINew" callback above
	var socket;

	function sendMessage(obj) {
		socket.send(JSON.stringify(obj));
	}

	function setUpWebsocket() {

		socket = new WebSocket("ws://"+location.hostname+":"+location.port+"/game/"+gameId);

		socket.onopen = function(event) {
			console.log("Websocket opened");
			console.log(event);

			console.log("sending link message");
			sendMessage({
				type: "linkWebsocket",
				sessionId: sessionId()
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

					//quick hack to escape html, so we dont allow rendering whatever html the user types
					//https://stackoverflow.com/a/6234808/3249197
					//TODO is this good enough? better way?
					var escapedMessage = $("<div />").text(message).html();

					//TODO make chat scroll and not just get longer and longer
					//can probably be done through html
					$chat.append(sender+": "+escapedMessage+"<br>");

					//scrolls to the bottom of the chat
					$chat.scrollTop($chat.prop("scrollHeight"));

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

				case "card":

					if (map.clear) {
						drawC.clear();

					} else {
						loadImg(drawC, map.value);
					}

					break;

				case "changeState":

					switchToState(map);

					break;

				case "vote":
					var votes1 = map.votes1;
					var votes2 = map.votes2;

					$score1.text("Votes: "+votes1);
					$score2.text("Votes: "+votes2);

					break;

				default:
					console.log("unknown message type ", type);
			}
		};
	}



	$send.click(function() {
		var message = $messageBox.val();

		if (message !== "") {
			sendMessage({
				type: "chat",
				message: message
			});

			$messageBox.val("");
		}
	});

	$vote1.click(function() {
		sendMessage({
			type: "vote",
			value: "1"
		});
	});

	$vote2.click(function() {
		sendMessage({
			type: "vote",
			value: "2"
		});
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
		});
	});


	$save.click(function() { // converts the canvas to SVG and sends it to the server
		console.log("image sent:")
		// console.log(drawingCanvas.toSVG());
		// when button is clicked, we send the image SVG to the server to be stored
		sendCard();
	});

	$next.click(function() {
		sendMessage({type: "changeState"});
	});

	$leave.click(function() {
		sendMessage({type: "leaveGame"});
		util.redirect("/", false);
	});

});
