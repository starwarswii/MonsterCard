$(function() {
	//we run this code on page load
	
	console.log("ready");
	
	//we alias the util function
	var sessionId = util.getSessionId;
	
	var $games = $("#games");
	var $create = $("#create");
	var $refresh = $("#refresh");
	
	//loads the game list from the server's json
	function loadGameList() {
		$games.empty();
		
		//do a get request to /games
		$.get("/games", function(data) {
			console.log("did GET to /games, got back:", data);
			
			//if the array returned is empty, just put "no games"
			if (data.length == 0) {
				$games.append("no games");
				return;
			}
			
			//otherwise, go through each entry in the list
			for (var i = 0; i < data.length; i++) {
				var game = data[i];
				var id = game.id;
				var name = game.name;
				
				//we create two buttons using jquery that connect to that game
				var joinButton = $("<button>Join</button>").click(function() {
					util.redirect("/game/"+id);
				});
				
				//the spectator button adds the spec=1 url parameter to tell the game we're spectating
				var spectateButton = $("<button>Spectate</button>").click(function() {
					util.redirect("/game/"+id, true);
				});
				
				//put the game name and both buttons in the list
				$games.append("Game "+id+" ("+name+")&nbsp;", joinButton, "&nbsp;", spectateButton, "<br>");
			}
			
		});
	}
	
	//we load the list on page load
	loadGameList();
	
	//the refresh button just calls loadGameList()
	//TODO could add auto refresh on new server creation
	//have server send refresh request via websocket
	$refresh.click(loadGameList);
	
	//create contacts the server to create a new game,
	//then redirects the client to it
	$create.click(function() {
		
		//we keep asking the user for a name till they give us one
		var gameName = null;
		while (gameName === null) {
			gameName = prompt("enter a game name", "MonsterCard");
		}
		
		//and now we do a post to /create and give the server the game name
		//(along with the session id so it knows who the owner is)
		util.postJson("/create", {sessionId: sessionId(), name: gameName}, function(response) {
			//the server will give us back the game id, so we redirect to that page
			util.redirect("/game/"+response.gameId);
		});
	});

	//these two buttons just read the game id from the
	//input box, and redirect just like the buttons in the game list do
	$("#joinButton").click(function() {
		var id = $("#gameId").val();
		
		//we only redirect if the box isn't empty
		if (id !== "") {
			util.redirect("/game/"+id);
		}
	});
	
	$("#spectateButton").click(function() {
		var id = $("#gameId").val();
		
		if (id !== "") {
			util.redirect("/game/"+id, true);
		}
	});
});
