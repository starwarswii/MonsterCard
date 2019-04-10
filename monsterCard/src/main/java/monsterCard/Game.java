package monsterCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import org.json.JSONObject;
import io.javalin.websocket.WsSession;


public class Game {
	
	enum State {
		
		BEFORE_GAME("before"),
		DRAWING("drawing"),
		VOTING("voting"),
		END_GAME("end");
			
		String name;
			
		State(String name) {
			this.name = name;
		}
	}
	
	
	static final int TIMER_LENGTH = 20;
	
	//TODO make websocket manager thing
	//eh maybe not now, depends on need?
	//on join, adds to list
	//on leave, removes from list
	//could also handle "this is who i am" messages
	//and could even use it to define a custom "command" language
	//person says thing, we do this.
	
	String ownerId; //session id of owner
	String name; //name of this game
	
	//TODO volatile needed? on one? on both?
	volatile int timerValue;
	volatile boolean timerRunning;
	
	//used to count down the timer in another thread
	Timer timer;
	TimerTask timerTask;
	
	Map<WsSession, String> websocketToSessionId;
	Map<String, User> sessionIdToUser;
	
	String player1; //session id of active player 1
	int votes1; // votes they have received
	
	String player2;
	int votes2;
	
	int currentRound; //the current round
	List<String> wentThisRound; //list of player session ids that already went this round
	
	State currentState;
	
	
	
	public Game(String ownerId, String name) {
		this.ownerId = ownerId;
		this.name = name;
		
		timerValue = TIMER_LENGTH;
		timerRunning = false;
		
		timer = new Timer();
		
		websocketToSessionId = new HashMap<>();
		sessionIdToUser = new HashMap<>();
		
		player1 = null;
		votes1 = 0;
		
		player2 = null;
		votes2 = 0;
		
		currentRound = 0;
		wentThisRound = new ArrayList<>();
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
		
		timerRunning = false;
		timerValue = TIMER_LENGTH;
	}
	
	public void sendToAll(String message) {
		for (WsSession session : websocketToSessionId.keySet()) {
			if (session.isOpen()) {
				session.send(message);	
			}
		}
	}
	
	//helper function that lets you write stuff like
	//sendToAll(x -> x.put("key", "value"));
	//it is essentially just calling the constructor and toString for you
	//TODO i'd say convert things to use this method, or remove it
	//it has small overhead
	public void sendToAll(Function<JSONObject, JSONObject> x) {
		sendToAll(x.apply(new JSONObject()).toString());
	}
	
	//for whatever reason the JSESSIONID cookie is "xxxxxxxxx.yyyy"
	//and the actual session id is "xxxxxxxxx"
	private String parseSessionId(String raw) {
		return raw.split("\\.")[0];
	}
	
	private List<Player> getPlayers() {
		ArrayList<Player> players = new ArrayList<>();
		
		for (User user : sessionIdToUser.values()) {
			
			//TODO maybe better data structure than having to do this
			//TODO could probably improve with u.isPlayer() or something
			
			if (user instanceof Player) {
				players.add((Player)user);
			}
		}
		
		return players;
	}
	
	private List<String> getPlayerIds() {
		ArrayList<String> sessionIds = new ArrayList<>();
		
		for (Entry<String, User> entry : sessionIdToUser.entrySet()) {
			
			if (entry.getValue() instanceof Player) {
				sessionIds.add(entry.getKey());
			}
		}
		
		return sessionIds;
	}
	
	private List<Spectator> getSpectators() {
		ArrayList<Spectator> spectators = new ArrayList<>();
		
		for (User user : sessionIdToUser.values()) {
			
			if (user instanceof Spectator) {
				spectators.add((Spectator)user);
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
		
		//when socket closes, we still have user, but they're "away" or something
		//add button to leave game, this removes player from game
		
		System.out.println(response);
		
		JSONObject map = new JSONObject(response);
		
		String type = map.getString("type");
		
		if (type.equals("whoiam")) {
			String sessionId = parseSessionId(map.getString("sessionId"));
			
			String username = map.getString("username");
			
			//TODO let everyone else know a user joined?
			//with chat message
			//or maybe that behavior should be somewhere else
			
			//TODO could specify if player or spectator,
			//websocket -> (sessionId, isSpectator)
			//then sessionid -> player or sessionid -> spectator
			
			//TODO need to add support for users leaving the game
			//could be done with certain message, or timeout?
			//what should happen when owner leaves game?
			//could close game, or assign another owner, or something
			
			//TODO if problems arise, try using websocket session id instead as key
			websocketToSessionId.put(session, sessionId);
			
			if (!sessionIdToUser.containsKey(sessionId)) {
				sessionIdToUser.put(sessionId, new Player(username, sessionId));
			}
			//TODO handle different user types (player vs spectator)
			//maybe dont set user type immediately and let them send a message indicating what
			//they want to change to? would suggest combining Player and Spectator classes
			
			return;
		}
		
		
		if (!websocketToSessionId.containsKey(session)) {
			System.out.println("got message from unrecognized session "+session+", ignoring");
			return;
		}
		
		switch (type) {
		
			case "chat":
				
				String message = map.getString("message");
				String sender = map.getString("sender");
				//TODO could look up websocket id to get username, instead of trusting the given sender
				
				
				//TODO handle senders
				//could either send along sender
				//or figure out sender
				//or pass id along with message
				
				//TODO add support for user list
				//could be type:chat, event:join
				//and type:chat event:message for what this is now
				
				sendToAll(new JSONObject()
					.put("type", "chat")
					.put("sender", sender)
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

			case "image":

				String imageSVG = map.getString("img");

				//TODO handle card creation adding to dealer deck

				sendToAll(new JSONObject()
					.put("type", "image")
					.put("img", imageSVG)
				.toString());

				break;
				
			case "change state":
				//Update the internal game state
				getNextState();
				//Send the new game state to all clients
				sendToAll(new JSONObject()
						.put("type", "change state")
						.put("value", currentState.name())
					.toString());
				
				break;
				
			case "vote":
				//Get the value representing the card the user intended to vote for, and increment its vote count
				int vote = map.getInt("value");
				if(vote==1) {
					votes1++;
				}else {
					votes2++;
				}
				//Send the updated vote counts to all clients
				sendToAll(new JSONObject()
						.put("type", "vote")
						.put("card", 1)
						.put("count", votes1)
					.toString());
				
				sendToAll(new JSONObject()
						.put("type", "vote")
						.put("card", 2)
						.put("count", votes2)
					.toString());
				break;
				
			default:
				System.out.println("unrecognized message type "+type);
			
		}
	}
	
	
	//Randomly chooses two new Players to play the next round
	//Sets player1 and player2 to be equal to those players' IDs
	//Each player plays once per round, assuming an even number of players. Currently caps at 3 rounds
	//One player will play twice in a round (specifically, in the last two face offs) if there are
	//an odd number of players
	//TODO there's probably a better way to do this
	//could maybe figure out all matchings at once and keep a list?
	public void changeActivePlayers() {
		int maxRounds = 3;
		
		List<String> players = getPlayerIds();

		if (wentThisRound.size() == players.size()) {
			if (currentRound == maxRounds) {
				//TODO end game
				//might be good for games to have a pointer to the GameManager that holds it
				//so they can tell it to remove themselves
			} else {
				currentRound++;
				wentThisRound.clear();
			}
		}
		
		Random random = new Random();
		
		while (true) {
			
			int i = random.nextInt(players.size());
			
			if (!wentThisRound.contains(players.get(i))) {
				wentThisRound.add(players.get(i));
				player1 = players.get(i);
				break;
			}
		}
		
		while (true) {
			
			if (wentThisRound.size() == players.size()) {
				break;
			}
			
			int i = random.nextInt(players.size());
			
			if (!wentThisRound.contains(players.get(i))) {
				
				wentThisRound.add(players.get(i));
				player2 = players.get(i);
				break;
			}
		}
	}
	
	//decides who won the round based on vote counts
	public String decideRoundWinner() {
		return votes1 > votes2 ? player1 : player2;
	}
	
	//After round winner is decided, the consequences of the round are put into effect.
	//TODO: code method to take the losing card and give it to the winner to edit
	public void RoundConsequences() {
		String winner = decideRoundWinner();
	}
	
	public void getNextState() {
		//Switch from the current game state to the next. The game follows a set order of states, so we can just
		//proceed with a given order
		//Done when a change state message is sent
		switch(currentState) {
			case BEFORE_GAME:
				currentState = State.DRAWING;
			case DRAWING:
				currentState = State.VOTING;
			case VOTING:
				currentState = State.END_GAME;
			case END_GAME:
				break;
		}
	}
	
	//TODO did not include:
	//addPlayer()
	//addSpectator()
	//removePlayer()
	//removeSpecator()
	//quit()
	//main()
	//at the moment it's not totally clear how players/spectators will be added to
	//games, but it will probably be through websockets. if there are functions to add them,
	//(which would end up being very simple anyway), they would be private
	//we're also not sure how stopping/ending/quitting a game will work. the game
	//might notify the manager, which will do it
	//main() may come back, or it may be spread out across various functions/stuff in handleMessage()
	//also we need to figure out how cards will work. will we have List<Card>? instance of Dealer?
}
