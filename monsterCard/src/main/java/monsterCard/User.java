package monsterCard;

//TODO should this be abstract?
//could add a isPlayer abstract function, might be useful
public class User {
	
	Integer hasVoted;
	String name;
	String sessionId; //TODO may not need this, depends on how we build the system
	
	public User(String name, String sessionId) {
		hasVoted = -1;
		this.name = name;
		this.sessionId = sessionId;
	}
	
	//Should read input from a user's button press to increment a
	//corresponding vote count (vote1 or vote2) in the Game class
	//TODO then maybe should have pointer to game it's in? donno
	public void vote() {
		
	}
	
	//Causes the User to exit the Game
	public void quit() {
		
	}
}
