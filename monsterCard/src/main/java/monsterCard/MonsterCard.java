package monsterCard;

import io.javalin.Javalin;

public class MonsterCard {
	
	public static void main(String[] args) {
		Javalin app = Javalin.create().enableStaticFiles("/public");
		
		//no redirect for root is needed, going to root (/), javalin serves index.html
		
		app.start(7000);
	}
}
