package monsterCard;

import java.util.Map;
import java.util.HashMap;

public class GameManager {
	Map<Integer,Game> games;
	int num_games;
	
	//Default constructor
	public GameManager() {
		games = new HashMap<Integer,Game>();
		num_games = 0;
	}
	
	//Takes a user provided String, creates a new game with that String as its name, and adds it to the HashMap 'games'
	//Then, increments num_games
	public void addGame(String name) {
		games.put(num_games+1, new Game(name,num_games+1));
		num_games++;
	}
	
	//Removes a terminated game specified by 'ID' from 'games', and decrements num_games
	public void endGame(int ID) {
		games.remove(ID-1);
		num_games--;
	}
}

