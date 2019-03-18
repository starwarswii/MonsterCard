package monsterCard;

import java.util.ArrayList;
import java.util.List;

public class Player extends User {
	List<Card> hand;
	Card activeCard;
	
	//Default constructor, takes in username from user
	public Player(String name, String ID) {
		super(name, ID);
		hand = new ArrayList<Card>();
	}
	
	//At the start of the game, either fetch a hand of cards from the database, or give the user a hand of blank cards to draw on
	//For now, only gives blank cards
	public void getCards() {
		for(int i = 0; i < 3; i++) {
			Card tmp = new Card();
			hand.add(tmp);
		}
		activeCard = hand.get(0);
	}
	
	//Allows user to draw on their canvas and save the results to their 'activeCard'
	//TODO: Add code to allow users to draw cards and save drawings
	public void drawCard() {
		String tmp = null;
		activeCard.updatecard(tmp);
	}
}
