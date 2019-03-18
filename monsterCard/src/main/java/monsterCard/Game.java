package monsterCard;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Game {
	String gameName;
	Map<String,Player> players;
	List<Spectator> spectators;
	List<Card> deck;
	int votes1, votes2, gameID;
	String activeplayer1, activeplayer2;
	List<String> alreadygone;
	
	//Default constructor, requires a user provided name for the game and an internally generated game ID
	public Game(String name, int ID) {
		gameID = ID;
	    gameName = name;
	    players = new HashMap<String,Player>();
	    spectators = new ArrayList<Spectator>();
	    deck = new ArrayList<Card>();
	    votes1 = votes2 = 0;
	    activeplayer1 = null;
	    activeplayer2 = null;
	    alreadygone = new ArrayList<String>();
	}
	
	//Adds a new Player with a user provided username
	public void addPlayer(String ID, String name) {
		Player newPlayer = new Player(name,ID);
		players.put(ID, newPlayer);
	}
	
	//Adds a new Spectator with a user provided username
	public void addSpectator(String name, String ID) {
		Spectator newSpectator = new Spectator(name,ID);
		spectators.add(newSpectator);
	}
	
	public void changeActivePlayers() {
		if(alreadygone.size()==players.size()) {
			this.quit();
		}
		Random rand = new Random();
		int i = 100;
		String[] ids = (String[]) players.keySet().toArray();
		do {
			i = rand.nextInt(players.size());
			if(!alreadygone.contains(ids[i])) {
				alreadygone.add(ids[i]);
				activeplayer1 = ids[i];
				break;
			}
		}while(true);
		do {
			i = rand.nextInt(players.size());
			if(!alreadygone.contains(ids[i])) {
				alreadygone.add(ids[i]);
				activeplayer2 = ids[i];
			}
		}while(true);
	}
	
	//End the game, and return the game ID to GameManager for it to use to remove this game from the list of games
	public int quit() {
		
		return gameID;
	}
	
}
