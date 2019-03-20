package monsterCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import org.json.JSONObject;
import io.javalin.websocket.WsSession;

public class Game {
	static final int TIMER_LENGTH = 20;
	
	//TODO make websocket manager thing
	//eh maybe not now, depeneds on need?
	//on join, adds to list
	//on leave, removes from lisr
	//could also handle "this is who i am" messages
	//and could even use it to definbe a custom "command" language
	//person says thing, we do this.
	
	//ArrayList<WsSession> websockets; //list of sessions
	String ownerId; //session id of owner
	String name; //name of this game
	
	//TODO volatile needed? on one? on both?
	volatile int timerValue;
	volatile boolean timerRunning;
	
	Timer timer;
	TimerTask timerTask;
	
	HashMap<WsSession, String> websocketToSessionId;
	HashMap<String, User> sessionIdToUser;
	
	//for now, assume a message will tell you what player sent it
	//so we don't need any maps
	//ArrayList<Player> players;
	//ArrayList<Spectator> spectators;
	
	public Game(String ownerId, String name) {
		this.ownerId = ownerId;
		this.name = name;
		
		//websockets = new ArrayList<>();
		
		timerValue = TIMER_LENGTH;
		timerRunning = false;
		
		timer = new Timer();
		
		websocketToSessionId = new HashMap<>();
		sessionIdToUser = new HashMap<>();
		
		//players = new ArrayList<>();
		//spectators = new ArrayList<>();
		
	}
	
	public boolean isOwner(String userId) {
		return userId.equals(ownerId);
	}
	
	public void startTimer() {
		//notify everyone
		//at the moment, this only serves to notify
		//the owner, so the start button can be disabled
		
		sendToAll(new JSONObject()
			.put("type", "timer")
			.put("event", "start")
		.toString());
		
		timerRunning = true;
		
		timerTask = new TimerTask() {
			public void run() {
				
				System.out.println("timer is "+timerValue);
				
				if (timerValue == 0) {
					System.out.println("stopping timer");
					
					
					sendToAll(new JSONObject()
						.put("type", "timer")
						.put("event", "stop")
					.toString());
					
					
					stopTimer();
					cancel(); //this.cancel() cancels the timer task we're in
				} else {
					
					sendToAll(new JSONObject()
						.put("type", "timer")
						.put("event", "value")
						.put("value", timerValue)
					.toString());
					
					
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
		for (WsSession s : websocketToSessionId.keySet()) {
			if (s.isOpen()) {
				s.send(message);	
			}
		}
	}
	
	//helper function that lets you write stuff like
	//sendToAll(x -> x.put("key", "value"));
	//it is essentally just calling the constructor and toString for you
	//TODO i'd say convert things to use this method, or remove it
	//it has small overhead
	public void sendToAll(Function<JSONObject, JSONObject> x) {
		sendToAll(x.apply(new JSONObject()).toString());
	}
	
//	public void addUser(WsSession user) {
//		websockets.add(user);
//	}
//	
//	public void removeUser(WsSession user) {
//		websockets.remove(user);
//		
//		//TODO what should happen when removing owner?
//		//could close room, or assign another owner, or somthing
//	}
	
	//for whatever reason the JSESSIONID cookie is "xxxxxxxxx.yyyy"
	//and the actual session id is "xxxxxxxxx"
	private String parseSessionId(String raw) {
		return raw.split("\\.")[0];
	}
	
	private List<Player> getPlayers() {
		ArrayList<Player> players = new ArrayList<>();
		for (User u: sessionIdToUser.values()) {
			//TODO maybe better data structure than having to do this
			//TODO could probably improve with u.isPlayer() or somthing
			if (u instanceof Player) {
				players.add((Player)u);
			}
		}
		return players;
	}
	
	private List<Spectator> getSpectators() {
		ArrayList<Spectator> spectators = new ArrayList<>();
		for (User u: sessionIdToUser.values()) {
			if (u instanceof Spectator) {
				spectators.add((Spectator)u);
			}
		}
		return spectators;
	}
	
	public void handleConnect(WsSession session) {
		//this is probably no-op
	}
	
	public void handleClose(WsSession session) {
		websocketToSessionId.remove(session);
	}
	
	public void handleMessage(WsSession session, String response) {
		
		//TODO have sockets tell you their session id on connection, use that to figure out who it is
		//need a distinction between player/user and socket connection
		
		//when socket closes, we still have user, but they're "away" or somthing
		//add button to leave room, this removes player from room
		
		System.out.println(response);
		
		JSONObject map = new JSONObject(response);
		
		String type = map.getString("type");
		
		if (type.equals("whoiam")) {
			String sessionId = parseSessionId(map.getString("sessionId"));
			
			//TODO let everyone else know a user joined?
			//with chat message
			//or maybe that behavior should be somewhere else
			
			//TODO if problems arise, try using websocket session id instead as key
			websocketToSessionId.put(session, sessionId);
			
			if (!sessionIdToUser.containsKey(sessionId)) {
				sessionIdToUser.put(sessionId, new Player("temp name", sessionId));
			}
			//TODO handle different user types (player vs spectator)
			//maybe dont set user type immediately and let them send a message indicating what
			//they want to change to? would suggest coming Player and Spectator classes
			
			return;
		}
		
		
		if (!websocketToSessionId.containsKey(session)) {
			System.out.println("got message from unrecognized session "+session+", ignoring");
			return;
		}
		
		switch (type) {
		
			case "chat":
				
				//send to everyone
				
				String message = map.getString("message");
				
				//TODO handle senders
				//could either send along sender
				//or figure out sender
				//or pass id along with message
				
				sendToAll(new JSONObject()
					.put("type", "chat")
					.put("sender", "someone")
					.put("message", message)
				.toString());
				
				break;
				
			case "timer":
				
				String command = map.getString("command");
				
				
				switch (command) {
					case "start":
						
						String sessionId = websocketToSessionId.get(session);
						
						if (isOwner(sessionId)) {
							System.out.println("starting!");
							//also notifies everyone
							startTimer();
							
						} else {
							System.out.println("someone who wasn't the owner attempted to start the timer. ignoring");
						}
						
						
						
						break;
						
					default:
						System.out.println("unrecognized timer command "+command);
				}
				
				
				break;
				
			default:
				System.out.println("unrecognized message type "+type);
			
		}
	}
}
