package monsterCard;

//represents a user. may actually be a player or spectator (the concrete subclasses)
//this just encapsulates some basic data and (somewhat) voting functionality
public abstract class User {
	
	String name;
	String sessionId; //the corresponding session id of the user
	
	//number representing state of current vote
	//-1 means haven't voted
	//1 means voted for card 1
	//2 means voted for card 2
	int currentVote;
	
	//makes the user with the given name and id
	public User(String name, String sessionId) {
		this.name = name;
		this.sessionId = sessionId;
		
		currentVote = -1;
	}
}
