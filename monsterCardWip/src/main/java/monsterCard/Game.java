package monsterCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

import org.json.JSONObject;

import io.javalin.json.JavalinJson;
import io.javalin.websocket.WsSession;

public class Game {
	static final int TIMER_LENGTH = 20;
	
	//TODO make websocket manager thing
	//on join, adds to list
	//on leave, removes from lisr
	//could also handle "this is who i am" messages
	//and could even use it to definbe a custom "command" language
	//person says thing, we do this.
	
	ArrayList<WsSession> websockets; //list of sessions
	String ownerId; //session id of owner
	
	//TODO?
	//ArrayList<WsSession> chatWebsockets;
	//Map<WsSession, String> websocketToSessionId;
	
	//String gameName;
	
	int id; //TODO maybe not needed
	
	//TODO volatile needed? on one? on both?
	volatile int timerValue;
	volatile boolean timerRunning;
	
	Timer timer;
	TimerTask timerTask;
	
	public Game(int id, String ownerId/*, String gameName*/) {
		this.id = id;
		this.ownerId = ownerId;
		//this.gameName = gameName;
		
		websockets = new ArrayList<>();
		
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
		for (WsSession s : websockets) {
			s.send(message);
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
	
	public void addUser(WsSession user) {
		websockets.add(user);
	}
	
	public void removeUser(WsSession user) {
		websockets.remove(user); //commented out as it should allow rejoining
		
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
		//TODO org.json seems to be better than Jackson. see if we can tell javalin to use it
		HashMap<String, String> map = JavalinJson.fromJson(response, HashMap.class);
		
		String type = map.get("type");
		
		//System.out.println("handling message from "+user.getId()+" with given id "+id+": "+message);
		
		switch (type) {
		
			case "chat":
				
				//send to everyone
				
				String message = map.get("message");
				
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
				
				//for whatever reason the JSESSIONID cookie is "xxxxxxxxx.yyyy" and the actual session id is "xxxxxxxxx"
				String id = map.get("id").split("\\.")[0];
				//TODO probably don't need to have id always? could only be there some of the time
				//like a password
				
				String command = map.get("command");
				
				
				if (command.equals("start") && isOwner(id)) {
					System.out.println("starting!");
					
					//also notifies everyone
					startTimer();	
				}
				
				break;
				
			default:
				System.out.println("unrecognized message type "+type);
			
		}
	}
}
