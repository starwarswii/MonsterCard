package monsterCard;

import java.util.HashMap;
import java.util.Set;

public class GameManager {
	
	int nextGameId;
	//TODO make all maps/ list the generic type, e.g. Map
	HashMap<Integer, Game> games;
	
	public GameManager() {
		nextGameId = 1;
		games = new HashMap<>();
	}
	
	public int createGame(String owner) {
		games.put(nextGameId, new Game(nextGameId, owner));
		return nextGameId++; //increment, but return the old value
	}	
	
	public Game getGame(int id) {
		return games.get(id);
	}
	
	public boolean gameExists(int id) {
		return games.containsKey(id);
	}
	
	public Integer[] getGameIds() {
		Set<Integer> set = games.keySet();
		return set.toArray(new Integer[set.size()]);
	}
}