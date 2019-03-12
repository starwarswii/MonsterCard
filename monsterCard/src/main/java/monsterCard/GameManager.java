package monsterCard;

import java.util.List;
import java.util.ArrayList;

public class GameManager {
	List<Game> games;
	int num_games;
	
	public GameManager() {
		games = new ArrayList<Game>();
		num_games = 0;
	}
}
