package monsterCard;

import java.util.List;
import java.util.ArrayList;

public class CardStore {
	List<Card> store;
	
	public CardStore() {
		store = new ArrayList<Card>();
		for(int i=0;i<9;i++) {
			store.add(new Card());
		}
	}
}
