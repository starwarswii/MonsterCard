package monsterCard;

import java.util.ArrayList;
import java.util.List;

//holds a single history element of a card
public class CardState {
	
	String svgString;
	
	List<String> descriptors;
	
	//creates empty card state
	public CardState() {
		svgString = null;
		descriptors = null;
	}
	
	//creates card state from a previous card state, and some additional changes as parameters
	//additional changes take priority over the old state
	public CardState(CardState state, String newSvgString, List<String> newDescriptors) {
		svgString = newSvgString;
		
		//will add the given state's descriptors plus the new descriptors
		descriptors = null;
		
		//descriptors of the state may be null
		//if it is, then the arraylist constructor call with null causes NullPointerException
		//so we need to test for it here
		if (state.descriptors != null) {
			
			descriptors = new ArrayList<>(state.descriptors);
		}
		
		if (newDescriptors != null) {
			descriptors.addAll(newDescriptors);
		}
	}
	
	//copy constructor
	public CardState(CardState state) {
		this(state.svgString, state.descriptors);
	}
	
	public CardState(String svgString, List<String> descriptors) {
		this.svgString = svgString;
		this.descriptors = descriptors;
	}
	
	public String getSvgString() {
		return svgString;
	}
	
	public List<String> getDescriptors() {
		return descriptors;
	}
}
