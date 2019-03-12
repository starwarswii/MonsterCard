package monsterCard;

import java.util.ArrayList;
import java.util.List;

public class Player extends User {
	List<Card> hand;
	Card activeCard;
	
	//Default constructor, takes in username from user
	public Player(String name) {
		super(name);
		hand = new ArrayList<Card>();
	}
}
