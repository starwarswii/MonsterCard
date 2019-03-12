package monsterCard;

import java.util.List;
import java.util.ArrayList;

public class Game {
	String gameName;
	List<Player> players;
	List<Spectator> spectators;
	List<Card> deck;
	int votes1, votes2, gameID;
	Player activeplayer1, activeplayer2;
	
	//Default constructor, requires a user provided name for the game and an internally generated game ID
	public Game(String name, int ID) {
		gameID = ID;
	    gameName = name;
	    players = new ArrayList<Player>();
	    spectators = new ArrayList<Spectator>();
	    deck = new ArrayList<Card>();
	    votes1 = votes2 = 0;
	    activeplayer1 = null;
	    activeplayer2 = null;
	}
	
	//Adds a new Player with a user provided username
	public void addPlayer(String name) {
		Player newPlayer = new Player(name);
		players.add(newPlayer);
	}
	
	//Adds a new Spectator with a user provided username
	public void addSpectator(String name) {
		Spectator newSpectator = new Spectator(name);
		spectators.add(newSpectator);
	}
	
	public int quit() {
		
		return gameID;
	}
	
}
