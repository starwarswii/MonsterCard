package monsterCard;

import java.util.HashMap;
import io.javalin.Javalin;
import io.javalin.core.util.JettyServerUtil;
import org.eclipse.jetty.server.session.SessionHandler;
import org.json.JSONObject;

//the main class. starts up the javalin server, and sets up handlers
public class MonsterCard {
	
	static final int MAX_MESSAGE_SIZE = 1000000000; //1GB
	static final long MAX_TIMEOUT = 3600000; //1 hour
	
	public static void main(String[] args) {
		Javalin app = Javalin.create().enableStaticFiles("/public");

		app.sessionHandler(() -> {
			SessionHandler handler = JettyServerUtil.defaultSessionHandler();
			
			//we need httOnly to be false so we can access the JSESSIONID cookie on the client
			//side using javascript. in this application, that is essentially the user identifier
			handler.setHttpOnly(false);
			
			return handler;
		});
		
		//we increase some max values to allow more data and longer delays
		app.wsFactoryConfig(wsFactory -> {
			wsFactory.getPolicy().setMaxTextMessageSize(MAX_MESSAGE_SIZE);
			wsFactory.getPolicy().setIdleTimeout(MAX_TIMEOUT);
		});
		
		
		//construct the game manager. it will handle most requests
		GameManager manager = new GameManager();
		
		//note no redirect for root is needed. when going to root (/), javalin serves index.html
		
		//returns game list
		app.get("/games", ctx -> {
			System.out.println("id "+ctx.req.getSession().getId());
			System.out.println("games requested");
			
			ctx.contentType("application/json");
			
			//get game list from game manager
			ctx.result(manager.getGamesJson().toString());
		});
		
		//renders the game template
		app.get("/game/:id", ctx -> {
			
			int id = ctx.pathParam("id", Integer.class).get();
			Game game = manager.getGame(id);
			
			if (game == null) {
				ctx.result("no game with id "+id+" exists");
				return;
			}
			
			//render template
			
			//we load the model with game id and name so it
			//can be rendered into the html
			HashMap<String, Object> model = new HashMap<>();
			model.put("id", id);
			model.put("gameName", game.gameName);
			
			ctx.render("game.vtl", model);
		});
		
		//request to create a game
		app.post("/create", ctx -> {
			System.out.println("create requested");
			
			ctx.contentType("application/json");
			
			JSONObject json = new JSONObject(ctx.body());
			
			String ownerId = json.getString("sessionId");
			String gameName = json.getString("name");
			
			//ask manager to create game, will give back game id
			int gameId = manager.createGame(ownerId, gameName);
			
			//return id of newly created game
			ctx.result(new JSONObject().put("gameId", gameId).toString());
		});
		
		//http and websocket api handled by game manager and api handlers within game objects
		//so we just point javalin at the manager, and it will handle the rest
		app.post("/state/:id", manager);
		app.ws("/game/:id", manager);
		
		//start the javalin server on port 7000
		app.start(7000);
	}
}
