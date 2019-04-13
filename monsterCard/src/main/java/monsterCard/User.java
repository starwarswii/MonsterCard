package monsterCard;

//TODO should this be abstract?
//could add a isPlayer abstract function, might be useful
public class User {
	
	String name;
	String sessionId; //TODO may not need this, depends on how we build the system
	
	//-1 means haven't voted
	//1 means voted for card 1
	//2 means voted for card 2
	Integer currentVote;
	
	public User(String name, String sessionId) {
		this.name = name;
		this.sessionId = sessionId;
		
		currentVote = -1;
	}
}
