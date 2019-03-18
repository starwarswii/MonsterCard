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
}
