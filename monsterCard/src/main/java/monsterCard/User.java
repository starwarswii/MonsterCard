package monsterCard;

public class User {
	String username, sessionID;
	
	//Default constructor, takes in a user provided username and saves it
	public User(String name, String ID) {
		username = name;
		sessionID = ID;
	}
	
	//Should read input from a user's button press to increment a corresponding vote count (vote1 or vote2) in the Game class
	public void vote() {
		
	}
	
	//Causes the User to exit the Game
	public void quit() {
		
	}
}
