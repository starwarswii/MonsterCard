package monsterCard;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.ArrayList;

public class Dealer {
	List<Card> store;
	List<Integer> dealt;
	
	//Default constructor, populates the Dealer with blank Card objects
	public Dealer() {
		int blankCardCount = 10;
		
		store = new ArrayList<>();
		dealt = new ArrayList<>();
		
		for (int i = 0; i < blankCardCount; i++) {
			store.add(new Card());
		}
	}
	
	//TODO May want another constructor that populates the Dealer with Cards taken in from the database?
	
	//Adds all cards from a list of cards to storage
	public void addCards(List<Card> newCards) {
		store.addAll(newCards);
	}
	
	//Adds new blank Cards to 'store'
	public void addCards(int count) {
		for (int i = 0; i < count; i++) {
			store.add(new Card());
		}
	}
	
	//Provides a List containing random Card objects of length 'num'
	//When cards are dealt to players, their index in 'store' is added to 'dealt'
	//so that we know that Card is taken
	//Stops dealing cards if all Cards have been dealt, and deals the rest as blank cards
	public List<Card> getCards(int num) {
		
		List<Card> temp = new ArrayList<>();
		Random random = new Random();
		
		for (int i = 0; i < num; i++) {
			
			//Adds 10 new blank Cards to deal out
			if (dealt.size() == store.size()) {
				addCards(10);
			}
			
			int index = random.nextInt(store.size());
			
			//TODO something looks fishy here. we don't add a card if the random index
			//is already in dealt? shouldn't we pick another number?
			if (!dealt.contains(index)) {
				
				temp.add(store.get(index));
				dealt.add(index);
				
			} else {
				i--;
			}
		}
		
		return temp;
	}
	
	//Returns Cards that were previously dealt out by getCards()
	public void returnCard(List<Card> hand) throws NoSuchElementException {
		
		for (int i = 0; i < hand.size(); i++) {

			//First get the index of the card in the store
			int index = store.indexOf(hand.get(i));

			if (index == -1)  {
				throw new NoSuchElementException("Trying to return card that wasn't originally in deck");
			} else {
				dealt.remove(index);
			}
		}
	}
}
