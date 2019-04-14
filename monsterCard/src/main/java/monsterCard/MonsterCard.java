package monsterCard;

import java.util.HashMap;
import io.javalin.Javalin;
import io.javalin.core.util.JettyServerUtil;
import org.eclipse.jetty.server.session.SessionHandler;
import org.json.JSONObject;

public class MonsterCard {//TODO add some logging like in timer demo
	
	static final int MAX_MESSAGE_SIZE = 1000000000; //1GB
	static final long MAX_TIMEOUT = 3600000; //1 hour
	
	public static void main(String[] args) {
		Javalin app = Javalin.create().enableStaticFiles("/public");

		app.sessionHandler(() -> {
			SessionHandler handler = JettyServerUtil.defaultSessionHandler();
			handler.setHttpOnly(false);
			
			return handler;
		});
		
		app.wsFactoryConfig(wsFactory -> {
			wsFactory.getPolicy().setMaxTextMessageSize(MAX_MESSAGE_SIZE);
			wsFactory.getPolicy().setIdleTimeout(MAX_TIMEOUT);
		});
		
		//no redirect for root is needed, going to root (/), javalin serves index.html
		
		GameManager manager = new GameManager();
		
		app.get("/games", ctx -> {
			System.out.println("id "+ctx.req.getSession().getId());
			System.out.println("games requested");
			
			ctx.contentType("application/json");
			
			ctx.result(manager.getGamesJson().toString());
		});
		
		app.get("/game/:id", ctx -> {
			
			int id = ctx.pathParam("id", Integer.class).get();
			Game game = manager.getGame(id);
			
			if (game == null) {
				ctx.result("no game with id "+id+" exists");
				return;
			}
			//render template
			
			HashMap<String, Object> model = new HashMap<>();
			
			model.put("id", id);
			model.put("gameName", game.gameName);
			
			ctx.render("game.vtl", model);
		});
		
		app.post("/create", ctx -> {
			System.out.println("create requested");
			
			ctx.contentType("application/json");
			
			JSONObject json = new JSONObject(ctx.body());
			
			String ownerId = json.getString("sessionId");
			String gameName = json.getString("name");
			
			int gameId = manager.createGame(ownerId, gameName);
			
			//return id of newly created game
			ctx.result(new JSONObject().put("gameId", gameId).toString());
		});
		
		//http and websocket api handled by game manager and api handlers within game objects
		app.post("/state/:id", manager);
		app.ws("/game/:id", manager);
		
		app.start(7000);
	}
}
