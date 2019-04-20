$(function() {
	//we run this code on page load
	console.log("ready");

	//embedded in the html is the id of the current game
	//it is put there by the server when rendering the template
	//in order to tell the javascript which game we're in
	//another way to do with would be to read the url and parse
	//the number on the end
	var gameId = $("#gameId").text();

	//we alias the util function
	var sessionId = util.getSessionId;

	// drawing canvas controls
	var $drawingClear = $("#clearcanvas");
	var $drawingColor = $("#drawing_color");
	var $drawingLineWidth = $("#drawing_linewidth");

	//sends image to server
	//somewhat unnecessary with autosaving,
	//but still good to have
	var $save = $("#save");

	//timer items
	var $start = $("#start");
	var $timer = $("#timer");

	//chat items
	var $chat = $("#chat");
	var $messageBox = $("#messageBox");
	var $send = $("#send");

	//vote items
	var $vote1 = $("#vote1");
	var $vote2 = $("#vote2");
	var $score1 = $("#score1");
	var $score2 = $("#score2");

	//button to change the state
	var $next = $("#next");

	//used to hide and show items
	var $wrapper = $("#wrapper");
	var $canvasControl = $("#canvasControls");

	var $currentState = $("#currentState");

	var $leave = $("#leave");
	
	//initialize drawing canvas, allows drawing
	var drawC = new fabric.Canvas('drawC', {isDrawingMode: true});
	
	//initialize display canvas, unable to edit only displays image of what was retrieved from server
	var c1 = new fabric.StaticCanvas('c1', {selectable: false});
	var c2 = new fabric.StaticCanvas('c2', {selectable: false});

	var undo = []; //undo stack
	var redo = []; //redo stack
	var state; //the last state of the drawingCanvas
	var drawing = true;

	//username starts as null as that
	//is what prompt() returns when no answer is given
	//both of these will be set by some initialization calls to the server
	var username = null;
	var isSpectator;
	
	//sends the current card to the server
	//this function is performed in many different places
	//so we create it to reduce duplicated code
	function sendCard() {
		sendMessage({
			type: "card",
			value: drawC.toSVG()
		});
	}

	

	//switches to a given state given the json map returned by the server
	//the map is needed as certain states may provide extra information
	//in the map (like voting)
	function switchToState(map) {

		var state = map.currentState;

		console.log("switching to state", state);

		$currentState.text(state);

		//based on the state, call various functions or
		//perform extra work
		//TODO could maybe be replaced by an object with functions
		switch (state) {
			case "BEFORE_GAME":
				initializeStartGame();
				break;

			case "VOTING":
				initializeVoting();

				//we also put the cards, creators, and vote counts
				//in the correct places
				//note we don't need to explicitly escape player names here,
				//as jquery's text() function does it for us
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

	//these functions below hide, show, and initialize various elements
	//for different states
	
	function initializeStartGame() {
		$wrapper.hide();
		$canvasControl.hide();
		$("#drawCanvas").hide();
		$("#displayCanvas").hide();
	}

	function initializeDrawing() {
		//if the user is a spectator, we don't show them the canvas,
		//as they won't draw a card
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

	//when canvas is modified, record any changes to the undo stack and clear redo stack
	drawC.on("object:added", function() {
		if (drawing) {
			redo = []; //clears all redo states
			undo.push(state); //adds the last state before drawing to the stack
			state = JSON.stringify(drawC); //updates the state for undomanager

			//"autosave" every time you add a stroke
			sendCard();
		}
	});
	
	//same as above, but for modification instead of addition
	drawC.on("object:modified", function() {
		if (drawing) {
			redo = [];
			undo.push(state);
			state = JSON.stringify(drawC);

			sendCard();
		}
	});

	//shifts the state over depending on which stack is given
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

	//helper function to load any img SVG to the given canvas
	function loadImg(canvas, SVG) {
		fabric.loadSVGFromString(SVG, function(objects, options) {
			//parses in data into a callback function and creates the canvas object from the SVG input
			var obj = fabric.util.groupSVGElements(objects, options);
			canvas.clear(); //clears the canvas so it can render the new SVG
			canvas.add(obj).renderAll();
		});
	}

	//set up ctrl+z and ctrl+y for undo and redo
	$(document).keydown(function(event) {
		if (event.ctrlKey) {
			if (event.keyCode === 90) {//Z: undo
				if (undo.length > 0) {//won't undo if there is no undo state left
					replay(undo, redo);
					//we also tell the server our new card state
					//on undo and redo
					sendCard();
				}
			} else if (event.keyCode === 89) { //Y: redo
				if (redo.length > 0) {//won't redo if there is no redo state left
					replay(redo, undo);
					sendCard();
				}
			}
		}
	});

	//the clear button clears the canvas
	$drawingClear.click(function() {
		drawC.clear();
		sendCard();
	});

	//updates the brush color
	$drawingColor.change(function() {
		drawC.freeDrawingBrush.color = $drawingColor.val();
	});

	//edits the brush width
	$drawingLineWidth.change(function() {
		drawC.freeDrawingBrush.width = parseInt($drawingLineWidth.val(), 10) || 1;
		$("#lineWidth").text(drawC.freeDrawingBrush.width); //update the number showing the width
	});

	//updates the brush when free drawing is turned on
	if (drawC.freeDrawingBrush) {
		drawC.freeDrawingBrush.color = $drawingColor.val();
		drawC.freeDrawingBrush.width = parseInt($drawingLineWidth.val(), 10) || 1;
	}

	//now we send the server a series of requests to figure out the current state
	//first we ask the server if we're a new or returning user
	util.postJson("/state/"+gameId, {type: "amINew", sessionId: sessionId()}, function(response) {
		console.log("sent amINew, got back:", response);

		if (response.newUser) {
			//if we're new, we ask the user for a username
			
			//prompt returns null if they exit or click cancel
			//so we loop till they give an answer
			while (username === null) {
				username = prompt("enter a username", "someone");
			}

			//now we check the url bar to see if we should be a spectator (spec is set)
			//note we only check for spectator if a new player
			//we are assuming normal players won't rejoin as spectators without leaving properly first
			isSpectator = new URLSearchParams(window.location.search).has("spec");
			
			//now we ask the server to create a new user with the user-specified username
			//we don't really care about response from this one, but after it we want to set up state and websocket,
			//so we hook on the success and perform those
			util.postJson("/state/"+gameId,
				{
					type: "createUser",
					sessionId: sessionId(),
					username: username,
					isSpectator: isSpectator
				},
				
				loadHttpState
			);
			
		//otherwise if we're not a new user,
		} else {
			//then the server will provide us with our old (current) username,
			//so we just set that, then set up the rest of the state
			username = response.username;
			loadHttpState()
		}
	});

	
	//sets up all variables for local state, then calls setUpWebsocket()
	function loadHttpState() {
		//we ask the server for the current game state
		//TODO could send id with parameters, instead of using path params. maybe is better
		util.postJson("/state/"+gameId, {type: "state", sessionId: sessionId()}, function(response) {
			console.log("did POST to /state, got back:", response);

			//from the response, we set many local variables according to the given values
			var isOwner = response.isOwner;
			var timerRunning = response.timerRunning;
			
			isSpectator = response.isSpectator;
			
			//hide and show owner and non-owner specific controls
			if (!isOwner) {
				$start.hide();
				$next.hide();
			} else {
				$leave.hide();
			}

			//set up timer if applicable
			if (timerRunning) {
				var timerValue = data.value;
				$start.prop("disabled", true);
				$timer.text(timerValue);
			}

			//switch to the current room state
			switchToState(response);
			
			//and now we call this to set up the websocket,
			//which should happen last
			setUpWebsocket();
		});
	}
	
	//is initialized in setUpWebsocket(), which will be called in the "amINew" callback above
	var socket;

	//helper function that sends the given javascript object
	//as a json string through the websocket
	function sendMessage(obj) {
		socket.send(JSON.stringify(obj));
	}

	//set up the websocket. will be invoked last, after other setup functions
	function setUpWebsocket() {

		//the websocket points at this current url, but with "ws" protocol
		//calling this constructor also starts the process of opening the websocket
		socket = new WebSocket("ws://"+location.hostname+":"+System.getenv("PORT")+"/game/"+gameId);

		//when the websocket opens,
		socket.onopen = function(event) {
			console.log("Websocket opened");
			console.log(event);

			//we send the websocket link message to associate
			//this websocket with this session id
			console.log("sending link message");
			sendMessage({
				type: "linkWebsocket",
				sessionId: sessionId()
			});
		};

		//we don't do much on websocket close, just some logging
		socket.onclose = function(event) {
			console.log("WebSocket closed");
			console.log(event);
		};

		//when we receive a websocket message
		socket.onmessage = function(response) {
			console.log("message!");

			//we parse it into json object
			var map = JSON.parse(response.data);

			console.log("got message:", map);
			console.log(response);

			var type = map.type;

			//based on the type, we perform different operations
			//TODO there might be a nicer way to do this
			switch (type) {

				//chat message
				case "chat":

					//read the sender and message from the json
					//we escape so we don't allow rendering
					//whatever html the user types
					var sender = util.escape(map.sender);
					var message = util.escape(map.message);
					
					//then we append that message, along with the sender, to the chat box
					$chat.append(sender+": "+message+"<br>");

					//when we get a new message, we also scroll the chat box to the bottom
					$chat.scrollTop($chat.prop("scrollHeight"));

					break;

				//timer related message event
				case "timer":

					var event = map.event;

					//depending on the type of timer event,
					//perform different actions
					switch (event) {

						case "start":
							//the timer has started, so
							//disable the "start timer" button
							$start.prop("disabled", true);
							break;

						case "stop":
							//the timer has stopped, so enable the button
							//and note the timer is no longer running
							$start.prop("disabled", false);
							$timer.text("not running");
							break;

						case "value":
							//the timer's current value is provided,
							//so we update the timer text
							var value = map.value;
							$timer.text(value);
							break;

						default:
							console.log("unknown event type ", event);
					}

					break;

				//card update message
				case "card":

					//if the server indicates we should
					//clear the card, we do so
					if (map.clear) {
						drawC.clear();

					} else {
						//otherwise, we load the given svg image into
						//the card
						loadImg(drawC, map.value);
					}

					break;

				//change state request
				case "changeState":

					//we switch to the given state
					//we pass in the whole map, as this
					//function handles special cases where the
					//server provides more information on a
					//given state (like voting)
					switchToState(map);

					break;

				//vote counts update message
				case "vote":
					var votes1 = map.votes1;
					var votes2 = map.votes2;

					//update the displays with the new counts
					$score1.text("Votes: "+votes1);
					$score2.text("Votes: "+votes2);

					break;

				default:
					console.log("unknown message type ", type);
			}
		};
	}

	//clicking the send button for the chat
	$send.click(function() {
		var message = $messageBox.val();

		//if the message isn't blank
		if (message !== "") {
			//we send it to the server
			//we don't need to tell the server who
			//we are or what our username is, as the
			//server already knows that based on the websocket
			//we're using to send this message
			sendMessage({
				type: "chat",
				message: message
			});

			$messageBox.val("");
		}
	});
	
	//if you press enter in the chat message box,
	//we click the send button
	$messageBox.keypress(function(event) {
		if (event.keyCode === 13) {//13 is enter
			$send.click();
		}
	});

	//clicking the two voting buttons just sends
	//two different vote messages to the server
	//the server handles everything regarding
	//preventing multiple voting and changing votes
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

	//clicking the timer start button
	$start.click(function(event) {
		console.log("clicked");
		console.log(event);
		
		//we just send the timer start message to the server
		sendMessage({
			type: "timer",
			command: "start",
		});
	});

	//the force save button
	$save.click(function() {
		console.log("image sent:")
		
		//just like autosaving, we again
		//just call this function
		sendCard();
	});

	//the next state button just
	//send the request to the server
	$next.click(function() {
		sendMessage({type: "changeState"});
	});

	//the leave button tells the server we're leaving,
	//then redirects to the homepage, removing
	//the spectator flag if it's present
	$leave.click(function() {
		sendMessage({type: "leaveGame"});
		util.redirect("/", false);
	});
});
