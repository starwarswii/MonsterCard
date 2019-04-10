package monsterCard;

import java.util.HashMap;
import io.javalin.Javalin;
import io.javalin.core.util.JettyServerUtil;
import org.eclipse.jetty.server.session.SessionHandler;


public class MonsterCard {//TODO add some logging like in timer demo
	
	//static final int MAX_MESSAGE_SIZE = 1000000000; //1GB
	static final int MAX_MESSAGE_SIZE = Integer.MAX_VALUE;
	
	public static void main(String[] args) {
		Javalin app = Javalin.create().enableStaticFiles("/public");

		app.sessionHandler(() -> {
			SessionHandler  handler = JettyServerUtil.defaultSessionHandler();
			//SessionHandler handler = new SessionHandler();
			
			handler.setHttpOnly(false);
			
			return handler;
		});
		
		app.wsFactoryConfig(wsFactory -> {
			wsFactory.getPolicy().setMaxTextMessageSize(MAX_MESSAGE_SIZE);
			
			//TODO probably only need the first one. we don't send binary messages
			//wsFactory.getPolicy().setMaxBinaryMessageSize(MAX_MESSAGE_SIZE);
		});
		
		//no redirect for root is needed, going to root (/), javalin serves index.html
		
		GameManager manager = new GameManager();
		
		app.get("/games", ctx -> {
			System.out.println("id "+ctx.req.getSession().getId());
			
			System.out.println("games requested");
			ctx.json(manager.getGameIds());
			//TODO i think you might be able to configure the json mapper
			//this means its possible to define how an object is converted to json
			//so for example you could make a GameManager convert to json of a list of ids
			//probably not needed, but could be cool to look into
			
		});
		
		
		//TODO add support for providing game name
		app.get("/create", ctx -> {
			System.out.println("create requested");
			String ownerId = ctx.req.getSession().getId();
			int gameId = manager.createGame(ownerId);
			
			//return id of newly created game
			ctx.json(gameId);
			
		});
		
		
		app.get("/game/:id", ctx -> {
			int id = Integer.parseInt(ctx.pathParam("id")); //for now assume id is always a valid int
			if (manager.gameExists(id)) {
				//render template
				
				HashMap<String, Object> model = new HashMap<>();
				model.put("id", id);
				
				ctx.render("game.vtl", model);
				
				
			} else {
				ctx.result("no game with id "+id+" exists");
			}
		});
		
		app.get("/state/:id", ctx -> {
			//TODO all this stuff should probably live in Game
			//and then moved to the websocket handling class
			//that class might be better handling arbitrary messages
			//both websocket and http
			//and then this could be a single endpoint, but could post json
			//like post to /api with data {type:status, game:1}
			
			int id = Integer.parseInt(ctx.pathParam("id"));
			if (manager.gameExists(id)) {
				//build map and return
				
				String user = ctx.req.getSession().getId();
				//String user = ctx.req.getSession()
				
				Game game = manager.getGame(id);
				
				HashMap<String, Object> map = new HashMap<>();
				//map.put("id", id);
				map.put("user", user);
				map.put("isOwner", game.isOwner(user));
				
				//TODO possible race condition, timerRunning could change
				//between this call and the next
				//unlikely though
				map.put("isRunning", game.timerRunning);
				if (game.timerRunning) {
					map.put("value", game.timerValue);
				}
				
				ctx.json(map);
				
			} else {
				ctx.json("invalid");
			}
		});
		
		app.ws("/game/:id", ws -> {
			
			ws.onConnect(session -> {
				int id = Integer.parseInt(session.pathParam("id"));
				System.out.println("websocket connection made from "+session.host()+" with id "+session.getId());
				
				//assume the id is a valid game
				//TODO we might be ok assuming this, as naturally you will be served a "game not found" page
				//if you type an invalid id, and that wont spawn a websocket
				
				manager.getGame(id).handleConnect(session);
			});

			ws.onClose((session, statusCode, reason) -> {
				int id = Integer.parseInt(session.pathParam("id"));
				System.out.println("websocket connection closed from "+session.host()+" with id "+session.getId()+". "+statusCode+": "+reason);
				
				manager.getGame(id).handleClose(session);
			});

			ws.onMessage((session, response) -> {
				int id = Integer.parseInt(session.pathParam("id"));
				System.out.println("got message from "+session.host()+" with id "+session.getId()+": "+response);
				
				manager.getGame(id).handleMessage(session, response);
			});
		});
		
		
		
		app.start(80);
	}
}
