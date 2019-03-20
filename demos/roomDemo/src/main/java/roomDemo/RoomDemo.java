package roomDemo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import io.javalin.Javalin;
import io.javalin.json.JavalinJson;
import io.javalin.websocket.WsSession;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;

public class RoomDemo {//TODO add some logging like in timer demo
	
	public static class Game {
		static final int TIMER_LENGTH = 20;
		
		ArrayList<WsSession> users; //list of sessions TODO rename probably
		String ownerId; //session id of owner
		
		int id; //TODO maybe not needed
		
		//TODO volatile needed? on one? on both?
		volatile int timerValue;
		volatile boolean timerRunning;
		
		Timer timer;
		TimerTask timerTask;
		
		public Game(int id, String ownerId) {
			this.id = id;
			this.ownerId = ownerId;
			
			users = new ArrayList<>();
			
			timerValue = TIMER_LENGTH;
			timerRunning = false;
			
			timer = new Timer();
			
			//TODO may need to be defined each time we want to time
			//aka within startTimer()
		}
		
		public boolean isOwner(String userId) {
			return userId.equals(ownerId);
		}
		
		public void startTimer() {
			//notify everyone
			//at the moment, this only serves to notify
			//the owner, so the start button can be disabled
			sendToAll("start");
			
			timerRunning = true;
			
			timerTask = new TimerTask() {
				public void run() {
					
					System.out.println("timer is "+timerValue);
					
					if (timerValue == 0) {
						System.out.println("stopping timer");
						sendToAll("stop");
						stopTimer();
						cancel(); //this.cancel() cancels the timer task we're in
					} else {
						sendToAll(Integer.toString(timerValue));
						//could also instead send json strings using JavalinJson.toJson(object)
						//something like {"running": true, "value": 30}
						
						timerValue--;
					}
				}
			};
			
			timer.schedule(timerTask, 0, 1000);
			//fixed rate mode is also an option
			//timer.scheduleAtFixedRate(...);
		}
		
		public void stopTimer() {
			timerTask.cancel();
			//timer.purge(); //TODO really not necessary, see javadoc
			
			timerRunning = false;
			timerValue = TIMER_LENGTH;
		}
		
		public void sendToAll(String message) {
			for (WsSession s : users) {
				s.send(message);
			}
		}
		
		public void addUser(WsSession user) {
			users.add(user);
		}
		
		public void removeUser(WsSession user) {
			users.remove(user); //commented out as it should allow rejoining
			
			//TODO what should happen when removing owner?
			//could close room, or assign another owner, or somthing
		}
		
		public void handleMessage(WsSession user, String response) {
			
			//TODO have sockets tell you their session id on connection, use that to figure out who it is
			//need a distinction between player/user and socket connection
			
			//when socket closes, we still have user, but they're "away" or somthing
			//add button to leave room, this removes player from room
			
			System.out.println(response);
			
			@SuppressWarnings("unchecked")
			HashMap<String, String> map = JavalinJson.fromJson(response, HashMap.class);
			
			//for whatever reason the JSESSIONID cookie is "xxxxxxxxx.yyyy" and the actual session id is "xxxxxxxxx"
			String id = map.get("id").split("\\.")[0];
			String message = map.get("message");
			
			System.out.println("handling message from "+user.getId()+" with given id "+id+": "+message);
			
			//System.out.printf("owner is (%s), user is (%s)\n", ownerId, id);
			//System.out.printf("message (%s)\n", message);
			
			if (message.equals("start") && isOwner(id)) {
				System.out.println("starting!");
				startTimer();	
			}
		}
	}
	
	
	public static class GameManager {
		
		int nextGameId;
		HashMap<Integer, Game> games;
		
		public GameManager() {
			nextGameId = 1;
			games = new HashMap<>();
		}
		
		public int createGame(String owner) {
			games.put(nextGameId, new Game(nextGameId, owner));
			return nextGameId++; //increment, but return the old value
		}	
		
		public Game getGame(int id) {
			return games.get(id);
		}
		
		public boolean gameExists(int id) {
			return games.containsKey(id);
		}
		
		public Integer[] getGameIds() {
			Set<Integer> set = games.keySet();
			return set.toArray(new Integer[set.size()]);
		}
	}
	
	
	public static void main(String[] args) {
		Javalin app = Javalin.create().enableStaticFiles("/public");

		app.sessionHandler(() -> {
			
			SessionHandler handler = new SessionHandler();
			
			handler.setHttpOnly(false);
			
			SessionCache cache = new DefaultSessionCache(handler);
			cache.setSessionDataStore(new NullSessionDataStore());
			handler.setSessionCache(cache);
			
			return handler;
		});
		
		//no redirect for root is needed, going to root (/), javalin serves index.html
		
		GameManager manager = new GameManager();
		
		app.get("/rooms", ctx -> {
			System.out.println("id "+ctx.req.getSession().getId());
			
			System.out.println("rooms requested");
			ctx.json(manager.getGameIds());
			//TODO i think you might be able to configure the json mapper
			//this means its possible to define how an object is converted to json
			//so for example you could make a GameManager convert to json of a list of ids
			//probably not needed, but could be cool to look into
			
		});
		
		
		app.get("/create", ctx -> {
			System.out.println("create requested");
			String ownerId = ctx.req.getSession().getId();
			int gameId = manager.createGame(ownerId);
			
			//return id of newly created game
			ctx.json(gameId);
			
		});
		
		
		app.get("/room/:id", ctx -> {
			int id = Integer.parseInt(ctx.pathParam("id")); //for now assume id is always a valid int
			if (manager.gameExists(id)) {
				//render template
				
				HashMap<String, Object> model = new HashMap<>();
				model.put("id", id);
				
				ctx.render("room.vtl", model);
				
				
			} else {
				ctx.result("no room with id "+id+" exists");
			}
		});
		
		app.get("/state/:id", ctx -> {
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
				map.put("isRunning", game.timerRunning);
				if (game.timerRunning) {
					map.put("value", game.timerValue);
				}
				
				ctx.json(map);
				
			} else {
				ctx.json("invalid");
			}
		});
		
		app.ws("/room/:id", ws -> {
			
			ws.onConnect(session -> {
				int id = Integer.parseInt(session.pathParam("id"));
				System.out.println("websocket connection made from "+session.host()+" with id "+session.getId());
				
				//assume the id is a valid game
				
				manager.getGame(id).addUser(session);
			});

			ws.onClose((session, statusCode, reason) -> {
				int id = Integer.parseInt(session.pathParam("id"));
				System.out.println("websocket connection closed from "+session.host()+" with id "+session.getId()+". "+statusCode+": "+reason);
				
				manager.getGame(id).removeUser(session);
			});

			ws.onMessage((session, response) -> {
				int id = Integer.parseInt(session.pathParam("id"));
				System.out.println("got message from "+session.host()+" with id "+session.getId()+": "+response);
				
				manager.getGame(id).handleMessage(session, response);
			});
		});
		
		
		
		app.start(7000);
	}
}
