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
			
			if (manager.gameExists(id)) {
				//render template
				
				Game game = manager.getGame(id);
				
				HashMap<String, Object> model = new HashMap<>();
				
				model.put("id", id);
				model.put("gameName", game.gameName);
				
				ctx.render("game.vtl", model);
				
			} else {
				ctx.result("no game with id "+id+" exists");
			}
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
		
		app.post("/state/:id", ctx -> {
			System.out.println("got http post request");
			//TODO all this stuff should probably live in Game
			//and then moved to the websocket handling class
			//that class might be better handling arbitrary messages
			//both websocket and http
			//and then this could be a single endpoint, but could post json
			//like post to /api with data {type:status, game:1}
			
			//we could use ctx.json(), but that is calling JavalinJson.toJson()
			//which is overkill, as we're just using org.json
			//so equivalently, we can just set the content type, and use ctx.result(json.toString())
			ctx.contentType("application/json");
			
			int id = ctx.pathParam("id", Integer.class).get();
			if (manager.gameExists(id)) {
				
				Game game = manager.getGame(id);
				
				ctx.result(game.handleHttp(new JSONObject(ctx.body())).toString());
			} else {
				ctx.result(new JSONObject().put("error", "invalid room id").toString());
			}
		});
		
		app.ws("/game/:id", ws -> {
			
			//TODO move this stuff into a websocket handler class
			ws.onConnect(session -> {
				System.out.println("websocket connection made from "+session.host()+" with id "+session.getId());
				
				int id = Integer.parseInt(session.pathParam("id"));
				
				if (manager.gameExists(id)) {	
					manager.getGame(id).handleConnect(session);
				}
			});

			ws.onClose((session, statusCode, reason) -> {
				System.out.println("websocket connection closed from "+session.host()+" with id "+session.getId()+". "+statusCode+": "+reason);
				
				int id = Integer.parseInt(session.pathParam("id"));
				
				if (manager.gameExists(id)) {
					manager.getGame(id).handleClose(session);
				}
			});

			ws.onMessage((session, response) -> {
				System.out.println("got message from "+session.host()+" with id "+session.getId());
				
				int id = Integer.parseInt(session.pathParam("id"));
				
				if (manager.gameExists(id)) {
					manager.getGame(id).handleMessage(session, new JSONObject(response));
				}
			});
		});
		
		app.start(7000);
	}
}
