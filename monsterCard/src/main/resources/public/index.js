$(function() {
	//we run this code on page load
	
	console.log("ready");
	
	var sessionId = util.getSessionId;
	
	var $games = $("#games");
	var $create = $("#create");
	var $refresh = $("#refresh");
	var $gameIDForm = $("#gameIDForm");
	
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
				
				//worth noting that clicking on these game links will not keep url parameters
				//so for testing, with fake url-session ids, just visit games directly
				$games.append("<a href=\"/game/"+id+"\">Join Game "+id+" ("+name+")</a><br>");
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
			var gameId = response.gameId;
			util.redirect("/game/"+gameId);
		});
	});

	$(document).ready(function() {
		$gameIDForm.on("submit", function (e) {
			e.preventDefault();
			var data = $("input[name=gameID]").val();
			util.redirect("/game/"+data);
		});
	});


});
