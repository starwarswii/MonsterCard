package monsterCard;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;
import io.javalin.Context;
import io.javalin.Handler;
import io.javalin.websocket.CloseHandler;
import io.javalin.websocket.ConnectHandler;
import io.javalin.websocket.MessageHandler;
import io.javalin.websocket.WsHandler;
import io.javalin.websocket.WsSession;

//handles list of games, and is responsible for forwarding websocket and http requests
//to the corresponding game
public class GameManager implements Consumer<WsHandler>, ConnectHandler, CloseHandler, MessageHandler, Handler {
	
	//the next game id to be used
	int nextGameId;
	
	Map<Integer, Game> games;
	
	public GameManager() {
		nextGameId = 1;
		games = new HashMap<>();
	}
	
	//we implement the handlers themselves to handle messages, but we also
	//implement a WsHandler consumer
	//it essentially "configures" a given handler, so we implement it
	//and configure the given handler to use our own handlers
	public void accept(WsHandler handler) {
		handler.onConnect(this);
		handler.onClose(this);
		handler.onMessage(this);
	}
	
	//creates a new game with given owner and name
	//returns the id of the created game
	public int createGame(String owner, String name) {
		if (name == null) {
			name = "Game "+nextGameId;
		}
		
		games.put(nextGameId, new Game(owner, name));
		return nextGameId++; //increment, but return the old value
	}	
	
	//returns a json array of games in the format
	//[{id:1, name:"game"}, etc]
	//this is used by the frontend to render the game list
	public JSONArray getGamesJson() {
		JSONArray array = new JSONArray();
		
		for (Entry<Integer, Game> entry : games.entrySet()) {
			int id = entry.getKey();
			Game game = entry.getValue();
			
			JSONObject object = new JSONObject();
			object.put("id", id);
			object.put("name", game.gameName);
			
			//append game to the array
			array.put(object);
		}
		
		return array;
	}

	//returns a game by id if one exists, else null
	public Game getGame(int id) {
		return games.getOrDefault(id, null);
	}
	
	//parses the url param, and gets the game if it exists
	//otherwise returns null
	private Game getValidGame(WsSession session) {
		int id;
		
		try {
			id = Integer.parseInt(session.pathParam("id"));
			
		} catch (NumberFormatException e) {
			System.out.println("invalid id: "+e.getMessage());
			return null;
		}
		
		if (!games.containsKey(id)) {
			System.out.println("no game with id "+id);
			return null;
		}
		
		return games.get(id);
	}
	
	//parses the url param, and gets the game if it exists
	//otherwise returns null
	private Game getValidGame(Context ctx) {
		
		int id = ctx.pathParam("id", Integer.class).get();
		
		if (!games.containsKey(id)) {
			System.out.println("no game with id "+id);
			return null;
		}
		
		return games.get(id);
	}
	
	//websocket connect handler. called by javalin
	//we get the corresponding game id and forward handling to that game
	public void handle(WsSession session) throws Exception {
		System.out.println("websocket connection made from "+session.host()+" with id "+session.getId());
		
		Game game = getValidGame(session);
		
		if (game != null) {
			game.handleConnect(session);
		}
	}

	//websocket close handler. called by javalin
	//we get the corresponding game id and forward handling to that game
	public void handle(WsSession session, int statusCode, String reason) throws Exception {
		System.out.println("websocket connection closed from "+session.host()+" with id "+session.getId()+". "+statusCode+": "+reason);
		
		Game game = getValidGame(session);
		
		if (game != null) {
			game.handleClose(session);
		}
	}
	
	//websocket message handler. called by javalin
	//we get the corresponding game id and forward handling to that game
	public void handle(WsSession session, String msg) throws Exception {
		System.out.println("got message from "+session.host()+" with id "+session.getId());
		
		Game game = getValidGame(session);
		
		if (game != null) {
			//we convert the string to a json object before giving to the game
			game.handleMessage(session, new JSONObject(msg));
		}
	}

	//http handler. called by javalin
	//we get the corresponding game id and forward handling to that game
	public void handle(Context ctx) throws Exception {
		System.out.println("got http post request");
		
		//we could use ctx.json(), but that is calling JavalinJson.toJson()
		//which is overkill, as we're just using org.json
		//so equivalently, we can just set the content type, and use ctx.result(json.toString())
		ctx.contentType("application/json");
		
		Game game = getValidGame(ctx);
		
		if (game == null) {
			ctx.result(new JSONObject().put("error", "invalid game id").toString());
			return;
		}
		
		//again, get body of message as string and convert to json before giving to game
		ctx.result(game.handleHttpMessage(new JSONObject(ctx.body())).toString());
	}
}
