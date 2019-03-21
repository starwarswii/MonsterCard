package monsterCard;

import java.util.ArrayList;
import java.util.List;

public class CardState {
	
	String svgString;
	
	List<String> descriptors;
	
	public CardState() {
		svgString = null;
		descriptors = null;
	}
	
	public CardState(CardState state, String newSvgString, List<String> newDescriptors) {
		svgString = newSvgString;
		
		//add the given state's descriptors plus the new descriptors
		descriptors = new ArrayList<>(state.descriptors);
		
		if (newDescriptors != null) {
			descriptors.addAll(newDescriptors);
		}
	}
	
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
