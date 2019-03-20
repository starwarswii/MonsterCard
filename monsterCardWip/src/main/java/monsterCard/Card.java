package monsterCard;

import java.util.ArrayList;
import java.util.List;

public class Card {
	
	String name;
	String creator;
	
	List<CardState> cardStates;

	//creates a blank card
	public Card() {
		name = null;
		creator = null;
		cardStates = new ArrayList<>();
		cardStates.add(new CardState());
	}
	
	public Card(String name, String creator, String svgString, List<String> descriptors) {
		this.name = name;
		this.creator = creator;
		
		cardStates = new ArrayList<>();
		cardStates.add(new CardState(svgString, descriptors));
	}
	
	private CardState currentState() {
		return cardStates.get(cardStates.size()-1);
	}
	
	//TODO might need setter for name and creator
	//depends on how blank cards work
	
	public String getName() {
		return name;
	}
	
	public String getCreator() {
		return creator;
	}
	
	public String getSvgString() {
		return currentState().getSvgString();
	}
	
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
