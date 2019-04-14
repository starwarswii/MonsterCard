$(function() {
	//we run this code on page load
	
	console.log("ready");
	
	var sessionId = util.getSessionId;
	
	var $games = $("#games");
	var $create = $("#create");
	var $refresh = $("#refresh");
	
	function loadGameList() {
		$games.empty();
		
		//TODO auto refresh on new server creation
		//have server send refresh request via websocket
		$.get("/games", function(data) {
			console.log("did GET to /games, got back:", data);
			
			if (data.length == 0) {
				$games.append("no games");
				return;
			}
			
			for (var i = 0; i < data.length; i++) {
				var game = data[i];
				var id = game.id;
				var name = game.name;
				
				var joinButton = $("<button>Join</button>").click(function() {
					util.redirect("/game/"+id);
				});
				
				var spectateButton = $("<button>Spectate</button>").click(function() {
					util.redirect("/game/"+id, true);
				});
				
				$games.append("Game "+id+" ("+name+")&nbsp;", joinButton, "&nbsp;", spectateButton, "<br>");
			}
			
		});
	}
	
	loadGameList();
	
	$refresh.click(loadGameList);
	
	$create.click(function() {
		
		var gameName = null;
		while (gameName === null) {
			gameName = prompt("enter a game name", "MonsterCard");
		}
		
		util.postJson("/create", {sessionId: sessionId(), name: gameName}, function(response) {
			util.redirect("/game/"+response.gameId);
		});
	});

	$("#joinButton").click(function() {
		var id = $("#gameId").val();
		
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
