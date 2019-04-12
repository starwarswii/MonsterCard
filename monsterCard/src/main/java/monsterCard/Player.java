package monsterCard;

public class Player extends User {
	//TODO maybe just a string? but then less extendible? donno
	Card card;
	int score;
	
	public Player(String name, String sessionId) {
		super(name, sessionId);
		//TODO put username in card? probably not needed at this point
		card = new Card();
		score = 0;
	}
	
	public void clearCard() {
		card = new Card();
	}
	
	//Allows user to draw on their canvas and save the results to their card
	public void updateCard(String svgString) {
		card.update(svgString);
	}
	
	public String getCardString() {
		return card.getSvgString();
	}
}
