package monsterCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import org.json.JSONObject;
import io.javalin.websocket.WsSession;

public class Game {
	
	enum State {
		BEFORE_GAME,
		DRAWING,
		VOTING,
		END_GAME
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
	String gameName; //name of this game
	
	//TODO volatile needed? on one? on both?
	volatile int timerValue;
	volatile boolean timerRunning;
	
	//used to count down the timer in another thread
	Timer timer;
	TimerTask timerTask;
	
	Map<WsSession, String> websocketToSessionId;
	Map<String, User> sessionIdToUser;
	Map<String, Integer> winCounts;
	
	String player1; //session id of active player 1
	int votes1; // votes they have received
	
	String player2;
	int votes2;
	
	int currentRound; //the current round
	static final int MAX_ROUNDS = 3;
	List<String> wentThisRound; //list of player session ids that already went this round
	
	State currentState;
	
	
	
	public Game(String ownerId, String gameName) {
		this.ownerId = ownerId;
		this.gameName = gameName;
		
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
		
		currentState = State.BEFORE_GAME;
	}
	
	public boolean isOwner(String sessionId) {
		return sessionId.equals(ownerId);
	}
	
	//TODO remove timer at some point?
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
	
	private void sendToAll(String message) {
		sendTo(websocketToSessionId.keySet(), message);
	}
	
	private void sendTo(Iterable<WsSession> websockets, String message) {
		for (WsSession session : websockets) {
			if (session.isOpen()) {
				session.send(message);	
			}//TODO remove session if not open anymore? seems like a nice spot to do so
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
	
	private Map<Player, Set<WsSession>> getPlayersToWebsockets() {
		HashMap<Player, Set<WsSession>> playerToWebsockets = new HashMap<>();
		
		for (Entry<WsSession, String> entry : websocketToSessionId.entrySet()) {
			WsSession websocket = entry.getKey();
			String sessionId = entry.getValue();
			
			//redundant check, as the sending method checks if it's open,
			//but this keeps us from adding many websockets that are closed
			//ideally closed websockets should be removed from websocketToSessionId
			if (websocket.isOpen()) {
				User user = sessionIdToUser.get(sessionId);
				
				if (user instanceof Player) {
					Player player = (Player)user;
					
					if (!playerToWebsockets.containsKey(player)) {
						playerToWebsockets.put(player, new HashSet<>());
					}
					
					playerToWebsockets.get(player).add(websocket);
				}
			}
			
		}
		
		return playerToWebsockets;
 
	}
	
	public void handleConnect(WsSession websocket) {
		//this is probably no-op
	}
	
	public void handleClose(WsSession websocket) {
		websocketToSessionId.remove(websocket);
	}
	
	public void handleMessage(WsSession websocket, JSONObject map) {
		
		//TODO have sockets tell you their session id on connection, use that to figure out who it is
		//need a distinction between player/user and socket connection
		
		//when socket closes, we still have user, but they're "away" or something
		//add button to leave game, this removes player from game
		
		System.out.println(map);
		
		String type = map.getString("type");
		
		if (type.equals("linkWebsocket")) {
			String sessionId = map.getString("sessionId");
			
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
			
			boolean newUser = !websocketToSessionId.containsValue(sessionId);
			
			websocketToSessionId.put(websocket, sessionId);
			
			if (newUser) {
				//by the time this is called, the user should already be set up from the http calls
				//but just in case, we check first
				String name = "an unidentified user";
				
				if (sessionIdToUser.containsKey(sessionId)) {
					name = sessionIdToUser.get(sessionId).name;
				}
				
				sendToAll(new JSONObject()
					.put("type", "chat")
					.put("sender", "Game")
					.put("message", name+" joined the game")
				.toString());
			}
			
			//TODO handle different user types (player vs spectator)
			//maybe dont set user type immediately and let them send a message indicating what
			//they want to change to? would suggest combining Player and Spectator classes
			
			return;
		}
		
		if (!websocketToSessionId.containsKey(websocket)) {
			System.out.println("got message from unrecognized session "+websocket+", ignoring");
			return;
		}
		
		//at this point, this method ensures that this websocket is bound to a session id
		String sessionId = websocketToSessionId.get(websocket);
		
		//the order of calls on the client side should ensure that at this point we also have a user object
		//connected to the given session id. this is because the http api "createUser" request should be performed
		//before opening the websocket, and reaching this point
		User user = sessionIdToUser.get(sessionId);
		
		switch (type) {
		
			case "chat":
				
				String message = map.getString("message");
				String sender = user.name;
				
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
				String svgString = map.getString("img");
				
				((Player)user).updateCard(svgString);
				
				//TODO this will be removed. no response to this request
				//just updates the card on the server side
				sendToAll(new JSONObject()
					.put("type", "image")
					.put("img", svgString)
				.toString());

				break;
				
			case "changeState":
				
				if (!isOwner(sessionId)) {
					System.out.println("someone who wasn't the owner attempted to change the state. ignoring");
					return;
				}
				
				//Update the internal game state
				
				//Switch from the current game state to the next. The game follows a set order of states, so we can just
				//proceed with a given order
				switch (currentState) {
					case BEFORE_GAME:
						
						currentState = State.DRAWING;
						
						sendToAll(new JSONObject()
							.put("type", "changeState")
							.put("value", currentState.name())
						.toString());
						
						break;
					case DRAWING:
						changeActivePlayers();
						//maybe not needed, but here for now
						votes1 = 0;
						votes2 = 0;
						
						currentState = State.VOTING;
						
						//TODO this is a mess, must be nicer way, might require redesign
						String card1 = ((Player)sessionIdToUser.get(player1)).getCardString();
						String card2 = ((Player)sessionIdToUser.get(player2)).getCardString();
						
						sendToAll(new JSONObject()
							.put("type", "changeState")
							.put("value", currentState.name())
							.put("card1", card1)
							.put("card2", card2)
						.toString());
						
						break;
						
					case VOTING:

						//TODO this is gross, need a nicer way to figure out how far
						//into a given round we are
						//boolean must be made before calling changeActivePlayers()
						boolean endOfRound = wentThisRound.size() == getPlayerIds().size();
						
						changeActivePlayers();
						
						if (endOfRound) {
							//transition to drawing, or endgame if final round
							
							currentState = currentRound <= MAX_ROUNDS ? State.DRAWING : State.END_GAME;
							
							
							if (currentState == State.DRAWING) {
								//shuffle cards and tell players their new cards
								
								List<String> cards = new ArrayList<>();
								
								Map<Player, Set<WsSession>> playerToWebsockets = getPlayersToWebsockets();
								
								for (Player player : playerToWebsockets.keySet()) {
									cards.add(player.getCardString());
								}
								
								Collections.shuffle(cards);
								
								List<Entry<Player, Set<WsSession>>> entryList = new ArrayList<>(playerToWebsockets.entrySet());
								
								for (int i = 0; i < entryList.size(); i++) {
									
									Entry<Player, Set<WsSession>> entry = entryList.get(i);
									
									Player player = entry.getKey();
									Set<WsSession> websockets = entry.getValue();
									
									String newCard = cards.get(i);
									
									player.updateCard(newCard);
									
									sendTo(websockets, new JSONObject()
										.put("type", "card")
										.put("value", newCard)
									.toString());
									
								}
							}
							
							sendToAll(new JSONObject()
								.put("type", "changeState")
								.put("value", currentState.name())
							.toString());
							
						} else {
							//transition to voting. in middle of round
							String winnerId;
							String loserId;
							if (votes1 > votes2) {
								winnerId = player1;
								loserId = player2;
							} else {
								winnerId = player2;
								loserId = player1;
							}
							
							votes1 = 0;
							votes2 = 0;
							
							Player winner = (Player)sessionIdToUser.get(winnerId);
							Player loser = (Player)sessionIdToUser.get(loserId);
							
							winner.score++;
							
							//TODO duplicate variable. maybe we /should/ compartmentalize this huge function
							//but how? probably can just pass in session id and response to handle it
							//or maybe don't even need response?
							message = winner.name+" beats "+loser.name;
							
							sendToAll(new JSONObject()
								.put("type", "chat")
								.put("sender", "Game")
								.put("message", message)
							.toString());
							
							currentState = State.END_GAME;
							
							sendToAll(new JSONObject()
								.put("type", "changeState")
								.put("value", currentState.name())
							.toString());
						}

						break;
					case END_GAME:
						
						//for now, TODO remove
						currentState = State.BEFORE_GAME;
						currentRound = 0;
						
						sendToAll(new JSONObject()
							.put("type", "changeState")
							.put("value", currentState.name())
						.toString());
						break;
				}
				
				System.out.println(currentState.name());

				
				break;
				
			case "vote":
				//TODO move vote if previously voted
				//Get the value representing the card the user intended to vote for, and increment its vote count
				int vote = map.getInt("value");
				if (vote == 1) {
					votes1++;
				} else {
					votes2++;
				}
				//Send the updated vote counts to all clients
				sendToAll(new JSONObject()
					.put("type", "vote")
					.put("votes1", votes1)
					.put("votes2", votes2)
				.toString());
				
				break;
				
				
			case "leaveGame":
				if (!isOwner(sessionId)) {
					sessionIdToUser.remove(sessionId);
					
					sendToAll(new JSONObject()
						.put("type", "chat")
						.put("sender", "Game")
						.put("message", user.name+" left the game")
					.toString());
				} else {
					//TODO how to handle owner leaving?
					System.out.println("owner tried to leave game, ignoring");
				}
				break;
				
			default:
				System.out.println("unrecognized message type "+type);
			
		}
	}
	
	public JSONObject handleHttp(JSONObject map) {
		
		String type = map.getString("type");
		String sessionId = map.getString("sessionId");
		
		JSONObject response = new JSONObject();
		
		switch (type) {
		
			case "amINew":
				
				boolean newUser = !sessionIdToUser.containsKey(sessionId);
				
				response.put("newUser", newUser);
				
				if (!newUser) {
					response.put("username", sessionIdToUser.get(sessionId).name);
				}
				
				return response;
				
			case "createUser":
				
				String username = map.getString("username");
				
				sessionIdToUser.put(sessionId, new Player(username, sessionId));
				
				return response;
				
			case "state":
				
				response.put("isOwner", isOwner(sessionId));
				response.put("timerRunning", timerRunning);
				
				if (timerRunning) {
					response.put("timerValue", timerValue);
				}
				
				response.put("currentState", currentState.name());
				
				return response;
				
			default:
				System.out.println("unrecognised http api type "+type+", ignoring");
				response.put("error", "invalid type");
				return response;
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
		
		List<String> players = getPlayerIds();

		if (wentThisRound.size() == players.size()) {
			if (currentRound > MAX_ROUNDS) {
				//TODO end game
				//might be good for games to have a pointer to the GameManager that holds it
				//so they can tell it to remove themselves
				//TODO temporary, to keep game going (i think)
				wentThisRound.clear();
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
	//probably unnecessary, can inline
	public String decideRoundWinner() {
		return votes1 > votes2 ? player1 : player2;
	}
	
	//decides round winner, and puts consequences of round into effect.
	//TODO: code method to take the losing card and give it to the winner to edit
	public void RoundConsequences() {
		String winner = decideRoundWinner();
	}
	
	public void beforeToDrawing() {
		//assign players? 
	}
	
	public void drawingToVoting() {
		//choose cards to battle
	}
	
	public void votingToEndRound() {
		//declare winner
		//add winValue
		RoundConsequences();
		//clear vote count for next round
		votes1 = 0;
		votes2 = 0;
	}
	
	public void endRoundtoDrawing() {
		//assign users to cards for next round
	}
	
	public void endRoundToEndGame() {
		//assign winner
	}
	
	public void endGameToStartGame() {
		//clear variables for next game
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
