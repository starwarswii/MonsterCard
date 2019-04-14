package monsterCard;

//represents a spectator. has no additional functionality from a user,
//but the concrete class itself is used to differentiate it
public class Spectator extends User {

	public Spectator(String name, String sessionId) {
		super(name, sessionId);
	}
}
