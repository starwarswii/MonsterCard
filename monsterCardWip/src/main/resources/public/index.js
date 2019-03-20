$(function() {
	//we run this code on page load
	
	console.log("ready");
	
	var $games = $("#games");
	var $create = $("#create");
	var $refresh = $("#refresh");
	
	function loadGameList() {
		$games.empty()
		//TODO auto refresh on new server creation
		//have server send refresh request via websocket
		$.get("/games", function(data) {
			console.log("did GET to /games, got back:", data);
			
			if (data.length == 0) {
				$games.append("no games");
				return;
			}
			
			for (var i = 0; i < data.length; i++) {
				//var x = $("<b>TEXT</b>");
				//$games.append("<b>TEXT</b>");
				$games.append("<a href=\"/game/"+data[i]+"\">Join Game "+data[i]+"</a><br>");
			}
			
		});
	}
	
	loadGameList();
	
	$refresh.click(loadGameList);
	
	$create.click(function() {
		$.get("/create", function(data) {
			window.location.href = "/game/"+data;
		});
	});
	
	
	


});
