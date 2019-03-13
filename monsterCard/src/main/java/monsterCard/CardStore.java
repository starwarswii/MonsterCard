package monsterCard;

import java.util.List;
import java.util.ArrayList;

public class CardStore {
	List<Card> store;
	
	//Default constructor, populates the CardStore with blank Card objects
	public CardStore() {
		store = new ArrayList<Card>();
		for(int i=0;i<9;i++) {
			store.add(new Card());
		}
	}
}
