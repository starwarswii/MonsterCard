package monsterCard;

import java.util.ArrayList;
import java.util.List;

//holds a card and its full history
public class Card {
	
	String name;
	String creator;
	
	//list of previous states of this card
	List<CardState> cardStates;

	//creates a blank card
	public Card() {
		name = null;
		creator = null;
		cardStates = new ArrayList<>();
		//we ensure there's always at least one state
		cardStates.add(new CardState());
	}
	
	//creates a card given all the parameters
	public Card(String name, String creator, String svgString, List<String> descriptors) {
		this.name = name;
		this.creator = creator;
		
		cardStates = new ArrayList<>();
		cardStates.add(new CardState(svgString, descriptors));
	}
	
	//gets the current state of the card,
	//which is just the last state in the list
	//as we ensure there's always at least one state, this
	//function won't run into any problems
	private CardState currentState() {
		return cardStates.get(cardStates.size()-1);
	}
	
	public String getName() {
		return name;
	}
	
	public String getCreator() {
		return creator;
	}
	
	//get svg string of current state
	public String getSvgString() {
		return currentState().getSvgString();
	}
	
	//get descriptors of current state
	public List<String> getDescriptors() {
		return currentState().getDescriptors();
	}
	
	//TODO these two alternative update() methods might not be needed
	public void update(String newSvgString) {
		update(newSvgString, null);
	}
	
	public void update(List<String> newDescriptors) {
		update(currentState().getSvgString(), newDescriptors);
	}
	
	//create a new card state with specified parameters, based on previous card state
	public void update(String newSvgString, List<String> newDescriptors) {
		cardStates.add(new CardState(currentState(), newSvgString, newDescriptors));
	}
}
