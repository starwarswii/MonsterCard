package monsterCard;

import java.util.ArrayList;
import java.util.List;

//Add CardManager class

public class Card {
	List<String> descriptors;
	String card_name, creator_name, currentversion;
	
	//Default constructor
	public Card() {
		descriptors = new ArrayList<String>();
		card_name = creator_name = currentversion = null;
	}
	
	public void updatecard(String version) {
		currentversion = version;
	}
	
	public String displayCard() {
		return currentversion;
	}
	
	public void addDescriptor(String new_descriptor) {
		descriptors.add(new_descriptor);
	}
}
