package monsterCard;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;

public class GameManager {
	
	int nextGameId;
	Map<Integer, Game> games;
	
	public GameManager() {
		nextGameId = 1;
		games = new HashMap<>();
	}
	
	public int createGame(String owner) {
		return createGame(owner, null);
	}
	
	public int createGame(String owner, String name) {
		if (name == null) {
			name = "Game "+nextGameId;
		}
		
		games.put(nextGameId, new Game(owner, name));
		return nextGameId++; //increment, but return the old value
	}	
	
	public Game getGame(int id) {
		return games.get(id);
	}
	
	public boolean gameExists(int id) {
		return games.containsKey(id);
	}
	
	public JSONArray getGamesJson() {
		JSONArray array = new JSONArray();
		
		for (Entry<Integer, Game> entry : games.entrySet()) {
			int id = entry.getKey();
			Game game = entry.getValue();
			
			JSONObject object = new JSONObject();
			object.put("id", id);
			object.put("name", game.gameName);
			
			array.put(object);
		}
		
		return array;
	}
	
	//TODO did not include:
	//addPlayerToGame()
	//endGame()
	//it might make more sense to have these operations on games
	//and use manager.getGame(x).addPlayer() or whatever
	//then again, its the "game" manager, so maybe it should have some proxy methods
}
