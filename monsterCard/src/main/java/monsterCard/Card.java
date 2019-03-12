package monsterCard;

import java.util.ArrayList;
import java.util.List;

//Add CardManager class

public class Card {
	List<String> descriptors;
	String card_name, creator_name, currentversion;
	
	public Card() {
		descriptors = new ArrayList<String>();
		card_name = creator_name = currentversion = null;
	}
	
}
