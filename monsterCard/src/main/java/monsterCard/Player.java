package monsterCard;

import java.util.ArrayList;
import java.util.List;

public class Player extends User {
	List<Card> hand;
	
	public Player(String name, String sessionId) {
		super(name, sessionId);
		hand = new ArrayList<>();
	}
	
	private Card activeCard() {
		return hand.get(0);
	}
	
	//At the start of the game, either fetch a hand of cards from the database,
	//or give the user a hand of blank cards to draw on
	//For now, only gives blank cards
	//TODO this functionallity might be better if done in Dealer or Game
	public void getCards() {
		for (int i = 0; i < 3; i++) {
			hand.add(new Card());
		}
	}
	
	//Allows user to draw on their canvas and save the results to their 'activeCard'
	//TODO: Add code to allow users to draw cards and save drawings
	public void drawOnCard(String svgString) {
		activeCard().update(svgString);
	}
	
	public String displayCard() {
		return activeCard().getSvgString();
	}
	
	//Allows user to choose which card they want to be active for a duel
	public void chooseCard() {
		//TODO chooseCard implementation
	}
}
