package monsterCard;

//represents an actual player in the game. holds a card
public class Player extends User {
	Card card;
	int score; //current overall game score
	
	public Player(String name, String sessionId) {
		super(name, sessionId);
		card = new Card(); //give the player a new blank card
		score = 0;
	}
	
	//reset the card
	public void clearCard() {
		card = new Card();
	}
	
	//update the user's card with a new card image
	public void updateCard(String svgString) {
		card.update(svgString);
	}
	
	//get the current image on the card as an svg string
	public String getCardString() {
		return card.getSvgString();
	}
}
