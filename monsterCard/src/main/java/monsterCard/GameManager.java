package monsterCard;

import java.util.Map;
import java.util.HashMap;

public class GameManager {
	Map<String,Integer> name_to_id;
	Map<Integer,Game> games;
	int num_games;
	
	//Default constructor
	public GameManager() {
		games = new HashMap<Integer,Game>();
		name_to_id = new HashMap<String,Integer>();
		num_games = 0;
	}
	
	//Takes a user provided String, creates a new game with that String as its name, and adds it to the HashMap 'games'
	//Then, increments num_games
	public void addGame(String name) {
		num_games++;
		games.put(num_games, new Game(name,num_games));
		name_to_id.put(name,num_games);
	}
	
	//Takes in input of the player's name, their session ID, and the name of the game they want to join
	//Passes information to the correct game for the player to be added to that game
	public void addPlayertoGame(String name, String ID, String game_name) {
		games.get(name_to_id.get(game_name)).addPlayer(name, ID);
		//TODO: Change the user's web page such that they load into the game on their end
		//Should maybe done via HTML in a separate file, unsure how it links to this function
	}
	
	//Removes a terminated game specified by 'ID' from 'games', and decrements num_games
	public void endGame(int ID) {
		games.remove(ID-1);
		num_games--;
	}
}

