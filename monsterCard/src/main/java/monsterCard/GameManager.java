package monsterCard;

import java.util.Map;
import java.util.HashMap;

public class GameManager {
	Map<Integer,Game> games;
	int num_games;
	
	public GameManager() {
		games = new HashMap<Integer,Game>();
		num_games = 0;
	}
	
	public void addGame(String name) {
		games.put(num_games+1, new Game(name,num_games+1));
		num_games++;
	}
	
	public void endGame(int ID) {
		games.remove(ID-1);
	}
}

