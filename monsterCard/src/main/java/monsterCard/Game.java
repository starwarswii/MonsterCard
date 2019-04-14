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
	
	//Player player1; //session id of active player 1
	int votes1; // votes they have received
	
	//Player player2;
	int votes2;
	
	int currentRound; //the current round
	static final int MAX_ROUNDS = 3;
	List<String> wentThisRound; //list of player session ids that already went this round
	
	State currentState;
	
	//store matchups
	//matchups[matchupIndex] and matchups[matchupIndex+1] are the current matchup
	List<Player> matchups;
	int matchupIndex;
	
	Random random;
	
	ApiHandler handler;
	
	public Game(String ownerId, String gameName) {
		this.ownerId = ownerId;
		this.gameName = gameName;
		
		timerValue = TIMER_LENGTH;
		timerRunning = false;
		
		timer = new Timer();
		
		websocketToSessionId = new HashMap<>();
		sessionIdToUser = new HashMap<>();
		
		votes1 = 0;
		votes2 = 0;
		
		currentRound = 0;
		wentThisRound = new ArrayList<>();
		
		currentState = State.BEFORE_GAME;
		
		random = new Random();
		
		handler = new ApiHandler();
		registerWebsocketHandlers();
		registerHttpHandlers();
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
		);
		
		timerRunning = true;
		
		timerTask = new TimerTask() {
			public void run() {
				
				System.out.println("timer is "+timerValue);
				
				if (timerValue == 0) {
					System.out.println("stopping timer");
					
					
					sendToAll(new JSONObject()
						.put("type", "timer")
						.put("event", "stop")
					);
					
					
					stopTimer();
					cancel(); //this.cancel() cancels the timer task we're in
				} else {
					
					sendToAll(new JSONObject()
						.put("type", "timer")
						.put("event", "value")
						.put("value", timerValue)
					);
					
					
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
	
	private void sendToAll(JSONObject json) {
		sendTo(websocketToSessionId.keySet(), json);
	}
	
	private void sendTo(Iterable<WsSession> websockets, JSONObject json) {
		for (WsSession session : websockets) {
			if (session.isOpen()) {
				session.send(json.toString());	
			}//TODO remove session if not open anymore? seems like a nice spot to do so
		}
	}
	
	private void sendGameMessage(String message) {
		sendToAll(new JSONObject()
			.put("type", "chat")
			.put("sender", ":")
			.put("message", message)
		);
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
	
	public void registerWebsocketHandlers() {
		
		//TODO we could likely get away with passing in session id instead of websocket
		//we don't seem to use actual websocket, and could just require the websocket to be linked
		//before calling any of these api functions
		
		handler.registerWebsocketHandler("chat", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			User user = sessionIdToUser.get(sessionId);
			
			String message = map.getString("message");
			String sender = user.name;
			
			//TODO add support for user list
			//could be type:chat, event:join
			//and type:chat event:message for what this is now
			
			sendToAll(new JSONObject()
				.put("type", "chat")
				.put("sender", sender)
				.put("message", message)
			);
		});
		
		handler.registerWebsocketHandler("timer", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			
			String command = map.getString("command");
			
			//the only command for now is start
			if (!command.equals("start")) {
				System.out.println("unrecognized timer command "+command);
				return;
			}
			
			if (isOwner(sessionId)) {
				System.out.println("starting!");
				//also notifies everyone
				startTimer();
				
			} else {
				System.out.println("someone who wasn't the owner attempted to start the timer. ignoring");
			}
		});
		
		handler.registerWebsocketHandler("card", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			User user = sessionIdToUser.get(sessionId);
			
			if (!(user instanceof Player)) {
				System.out.println("someone who wasn't a player tried to update their card. ignoring");
				return;
			}
			
			String svgString = map.getString("value");
			((Player)user).updateCard(svgString);

		});
		
		handler.registerWebsocketHandler("changeState", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			
			if (!isOwner(sessionId)) {
				System.out.println("someone who wasn't the owner attempted to change the state. ignoring");
				return;
			}
			
			//Switch from the current game state to the next. The game follows a set order of states, so we can just
			//proceed with a given order
			
			//TODO could put these rules inside the enum itself maybe
			switch (currentState) {
				case BEFORE_GAME:
					
					transitionToState(State.DRAWING);
					break;
					
				case DRAWING:
					
					createMatchups(); //also increments round
					loadMatchup();
					
					transitionToState(State.VOTING);
					break;
					
				case VOTING:

					determineWinner();
					
					if (loadMatchup()) {
						//in middle of round, go to voting
						transitionToState(State.VOTING);
						
					} else if (currentRound < MAX_ROUNDS){
						//end of round, but not end of game, shuffle and go to drawing
						shuffleCards();
						displayScores(false);
						transitionToState(State.DRAWING);
						
					} else {
						//end of round and end of game, go to endgame
						displayScores(true);
						resetScores();
						transitionToState(State.END_GAME);
					}
					
					break;
					
				case END_GAME:
					
					//for now, we transition back to starting state, and reset
					//TODO remove probably
					
					currentRound = 0;
					
					sendToAll(new JSONObject()
						.put("type", "card")
						.put("clear", true)
					);
					
					transitionToState(State.BEFORE_GAME);
			}
		});
		
		handler.registerWebsocketHandler("vote", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			User user = sessionIdToUser.get(sessionId);
			
			int vote = map.getInt("value");
			
			incrementVote(user, vote);
			
			//Send the updated vote counts to all clients
			sendToAll(new JSONObject()
				.put("type", "vote")
				.put("votes1", votes1)
				.put("votes2", votes2)
			);
		});
		
		handler.registerWebsocketHandler("leaveGame", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			User user = sessionIdToUser.get(sessionId);
			
			if (!isOwner(sessionId)) {
				sessionIdToUser.remove(sessionId);
				
				sendGameMessage(user.name+" left the game");
				
			} else {
				//TODO how to handle owner leaving?
				//could like close the game and kick everyone or something
				System.out.println("owner tried to leave game, ignoring");
			}
		});
	}
	
	public void registerHttpHandlers() {
		
		handler.registerHttpHandler("amINew", map -> {
			JSONObject response = new JSONObject();
			
			String sessionId = map.getString("sessionId");
			
			boolean newUser = !sessionIdToUser.containsKey(sessionId);
			
			response.put("newUser", newUser);
			
			if (!newUser) {
				response.put("username", sessionIdToUser.get(sessionId).name);
			}
			
			return response;
		});
		
		handler.registerHttpHandler("createUser", map -> {
			JSONObject response = new JSONObject();
			
			String sessionId = map.getString("sessionId");
			
			String username = map.getString("username");
			boolean isSpectator = map.getBoolean("isSpectator");
			
			if (isSpectator) {
				sessionIdToUser.put(sessionId, new Spectator(username, sessionId));
			} else {
				sessionIdToUser.put(sessionId, new Player(username, sessionId));
			}
			
			return response;
		});
		
		handler.registerHttpHandler("state", map -> {
			JSONObject response = new JSONObject();
			
			String sessionId = map.getString("sessionId");
			
			response.put("isOwner", isOwner(sessionId));
			response.put("timerRunning", timerRunning);
			
			if (timerRunning) {
				response.put("timerValue", timerValue);
			}
			
			boolean isSpectator = sessionIdToUser.get(sessionId) instanceof Spectator;
			
			response.put("isSpectator", isSpectator);
			
			response.put("currentState", currentState.name());
			
			if (currentState == State.VOTING) {
				Player player1 = getPlayer1();
				Player player2 = getPlayer2();
				
				response.put("player1", player1.name);
				response.put("card1", player1.getCardString());
				response.put("votes1", votes1);
				
				response.put("player2", player2.name);
				response.put("card2", player2.getCardString());
				response.put("votes2", votes2);
			}
			
			return response;
		});
	}
	
	public void handleConnect(WsSession websocket) {
		//this is probably no-op
	}
	
	public void handleClose(WsSession websocket) {
		websocketToSessionId.remove(websocket);
	}
	
	public void handleMessage(WsSession websocket, JSONObject map) {	
		if (!ApiHandler.isValidMessage(map)) {
			return;
		}
		
		//we do most handling using the ApiHandler, but we have a special case for the
		//linkWebsocket message type, as that needs to occur first
		String type = map.getString("type");
		
		if (type.equals("linkWebsocket")) {
			String sessionId = map.getString("sessionId");
			
			boolean newUser = !websocketToSessionId.containsValue(sessionId);
			
			websocketToSessionId.put(websocket, sessionId);
			
			if (newUser) {
				//by the time this is called, the user should already be set up from the http calls
				//but just in case, we check first
				String name = "an unidentified user";
				String spectatorString = "";
				
				if (sessionIdToUser.containsKey(sessionId)) {
					User user = sessionIdToUser.get(sessionId);
					
					name = user.name;
					if (user instanceof Spectator) {
						spectatorString = " as a spectator";
					}
				}
				
				sendGameMessage(name+" joined the game"+spectatorString);
			}
			
			return;
		}
		
		if (!websocketToSessionId.containsKey(websocket)) {
			System.out.println("got message from unauthenticated (unlinked) websocket "+websocket.getId()+", ignoring");
			return;
		}
		
		//at this point, this method ensures that this websocket is bound to a session id
		
		//the order of calls on the client side should also ensure that at this point we also have a user object
		//connected to the given session id. this is because the http api "createUser" request should be performed
		//before opening the websocket, and reaching this point
		
		//User user = sessionIdToUser.get(sessionId);
		
		//handle any other type of message
		handler.handleWebsocketMessage(websocket, map);
	}
	
	public JSONObject handleHttpMessage(JSONObject map) {
		return handler.handleHttpMessage(map);
		
	}
	
	//constructs the matchups list
	//matchups will be an even list of players such that adjacent pairs
	//are matched to each other
	//e.g. (0,1), (2,3) etc
	//matchups may contain duplicate players, but it will
	//be shallow copies. we aren't duplicating player instances
	//calling this function also increments currentRound
	public void createMatchups() {
		
		//TODO might be clearer if this was just inlined in drawing
		currentRound++;

		matchups = getPlayers();
		matchupIndex = -2; //we start two above, as loadMatchup increments first
		
		//special case for one player
		//just match them with themselves
		if (matchups.size() == 1) {
			matchups.add(matchups.get(0));
			return;
		}
		
		//we pass in random just so it doesn't create another
		//source of randomness. really unnecessary, but since
		//we need a Random for nextInt() below, might as well
		Collections.shuffle(matchups, random);
		
		//if odd number of players
		if (matchups.size() % 2 != 0) {
			
			//pick a random player from 0 to second to last
			//and append them, matching with the last player
			//we don't pick last so we don't match the last player
			//with themselves
			matchups.add(matchups.get(random.nextInt(matchups.size()-1)));
		}
	}
	
	//returns false if at end of voting
	public boolean loadMatchup() {
		matchupIndex += 2;
		
		votes1 = 0;
		votes2 = 0;
		
		return matchupIndex < matchups.size();
	}
	
	public Player getPlayer1() {
		return matchups.get(matchupIndex);
	}
	
	public Player getPlayer2() {
		return matchups.get(matchupIndex+1);
	}
	
	public void transitionToState(State state) {
		JSONObject json = new JSONObject();
		
		json.put("type", "changeState");
		json.put("currentState", state.name());
		
		//if we're transitioning to voting
		if (state == State.VOTING) {
			clearVotes();
			
			Player player1 = getPlayer1();
			Player player2 = getPlayer2();
			
			json.put("player1", player1.name);
			json.put("card1", player1.getCardString());
			json.put("votes1", votes1);
			
			json.put("player2", player2.name);
			json.put("card2", player2.getCardString());
			json.put("votes2", votes2);
		}
		
		//perform transition
		currentState = state;
		System.out.println("transitioned to state "+currentState.name());
		
		sendToAll(json);
	}
	
	//picks winner with highest score,
	//gives them a point, and sends a message
	//announcing the winner
	public void determineWinner() {
		Player winner;
		Player loser;
		
		if (votes1 > votes2) {
			winner = getPlayer1();
			loser = getPlayer2();
			
		} else if (votes1 < votes2) {
			winner = getPlayer2();
			loser = getPlayer1();
			
		} else {
			
			//do random tiebreaking
			if (random.nextBoolean()) {
				winner = getPlayer1();
				loser = getPlayer2();
				
			} else {
				winner = getPlayer2();
				loser = getPlayer1();
			}
		}
		
		winner.score++;
		
		sendGameMessage(winner.name+" beats "+loser.name);
	}
	
	//shuffles cards among players and tells them their new cards
	public void shuffleCards() {
		
		//create a reverse map. has all websockets belonging to each player
		//we do this in order to contact all open websockets (if there happen to be more than one)
		//to let them know of the card change
		Map<Player, Set<WsSession>> playerToWebsockets = getPlayersToWebsockets();
		
		//create list of all card strings
		List<String> cards = new ArrayList<>();
		
		for (Player player : playerToWebsockets.keySet()) {
			cards.add(player.getCardString());
		}
		
		Collections.shuffle(cards, random);
		
		//convert that reverse map into a list, so we can associate card i in the shuffled list with player i
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
			);
		}
	}
	
	public void displayScores(boolean showWinner) {
		List<Player> players = getPlayers();
		
		//sort by score
		//note we use reverse order of p1, p2 in the compare, so we sort high to low
		Collections.sort(players, (p1, p2) -> Integer.compare(p2.score, p1.score));
		
		sendGameMessage("current scores:");
		for (Player player : players) {
			sendGameMessage(player.name+": "+player.score);
		}
		
		if (showWinner) {
			sendGameMessage(players.get(0).name+" wins!");
		}
	}
	
	public void resetScores() {
		for (Player player : getPlayers()) {
			player.score = 0;
		}
	}
	
	public void incrementVote(User user, int newVote) {
		
		int oldVote = user.currentVote;
		
		//if they didn't vote before, add vote normally
		if (oldVote == -1) {
			
			if (newVote == 1) {
				votes1++;
			} else {
				votes2++;
			}
			
		//else if the're changing their vote, remove from old one
		} else if (oldVote != newVote) {
			if (newVote == 1) {
				votes2--;
				votes1++;
			} else {
				votes1--;
				votes2++;
			}
		}
		
		//else the old and new vote were the same, so we do nothing
		
		//and in all cases, update the new old vote to the new vote
		user.currentVote = newVote;
	}
	
	public void clearVotes() {
		for (User user : sessionIdToUser.values()) {
			user.currentVote = -1;
		}
	}
}
