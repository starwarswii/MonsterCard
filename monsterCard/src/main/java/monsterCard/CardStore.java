package monsterCard;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

public class CardStore {
	List<Card> store;
	List<Integer> dealt;
	
	//Default constructor, populates the CardStore with blank Card objects
	public CardStore() {
		store = new ArrayList<Card>();
		for(int i=0;i<9;i++) {
			store.add(new Card());
		}
		dealt = new ArrayList<Integer>();
	}
	
	//Adds all cards from a list of cards to storage
	public void addCards(List<Card> new_cards) {
		for(int i = 0; i < new_cards.size(); i++) {
			store.add(new_cards.get(i));
		}
	}
	
	public void addCards(int new_cards) {
		for(int i = 0; i < new_cards; i++) {
			store.add(new Card());
		}
	}
	
	//Provides a List containing random Card objects of length 'num'
	//Should add a tracker for which Cards have already been 'dealt' to players
	//Stops dealing cards if all Cards have been dealt, and deals the rest as blank cards
	public List<Card> getCards(int num) {
		List<Card> tmp = new ArrayList<Card>();
		Random rand = new Random();
		int index = 0;
		for(int i = 0; i < num; i++) {
			//Adds 10 new blank Cards to deal out
			if(dealt.size()==store.size()) {
				this.addCards(10);
			}
			index = rand.nextInt(store.size());
			if(!dealt.contains(index)) {
				tmp.add(store.get(index));
				dealt.add(index);
			}else {
				i--;
			}
		}
		return tmp;
	}
	
	//Returns Cards that were previously dealt out by getCards()
	public void returnCard(List<Card> hand) {
		
	}
}
