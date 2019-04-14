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

//the main game class. holds all game information, and is
//forwarded all request handling by the game manager
public class Game {
	
	//possible states the game can be in
	enum State {
		BEFORE_GAME,
		DRAWING,
		VOTING,
		END_GAME
	}
	
	static final int TIMER_LENGTH = 20;
	
	String ownerId; //session id of owner
	String gameName; //name of this game
	
	//variables that handle the timer
	//TODO is volatile needed? on one? on both?
	volatile int timerValue;
	volatile boolean timerRunning;
	
	//used to count down the timer in another thread
	Timer timer;
	TimerTask timerTask;
	
	//these two maps hold the basic information about players
	//we keep track of websocket sessions linked to session ids
	//and then user ids to player
	//this allows players to rejoin on different websockets
	Map<WsSession, String> websocketToSessionId;
	Map<String, User> sessionIdToUser;
	
	//keep track of votes during voting
	int votes1;
	int votes2;
	
	int currentRound;
	static final int MAX_ROUNDS = 3;
	
	State currentState;
	
	//store matchups for voting
	//matchups[matchupIndex] and matchups[matchupIndex+1] are the current matchup
	List<Player> matchups;
	int matchupIndex;
	
	Random random;
	
	//handles most api related requests
	ApiHandler handler;
	
	//creates a game with given owner and name
	//also sets up the api handler
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
		
		currentState = State.BEFORE_GAME;
		
		random = new Random();
		
		//load the handler with (type, function) bindings
		handler = new ApiHandler();
		registerWebsocketHandlers();
		registerHttpHandlers();
	}
	
	//returns true if the given session id corresponds
	//to the owner of this game
	public boolean isOwner(String sessionId) {
		return sessionId.equals(ownerId);
	}
	
	//starts the timer using another thread
	//TODO mayberemove timer at some point?
	public void startTimer() {
		//notify everyone
		//at the moment, this only serves to notify
		//the owner, so the start button can be disabled
		
		//tell everyone the timer is starting
		sendToAll(new JSONObject()
			.put("type", "timer")
			.put("event", "start")
		);
		
		timerRunning = true;
		
		//this timer class will tick the timer value once a second
		timerTask = new TimerTask() {
			public void run() {
				
				System.out.println("timer is "+timerValue);
				
				//if timer is done
				if (timerValue == 0) {
					System.out.println("stopping timer");
					
					//inform everyone
					sendToAll(new JSONObject()
						.put("type", "timer")
						.put("event", "stop")
					);
					
					
					stopTimer();
					cancel(); //this.cancel() cancels the timer task we're in
				} else {
					
					//the timer's still running
					//so tell everyone the value
					sendToAll(new JSONObject()
						.put("type", "timer")
						.put("event", "value")
						.put("value", timerValue)
					);
					
					timerValue--;
				}
			}
		};
		
		//here, schedule it every second
		timer.schedule(timerTask, 0, 1000);
		//fixed rate mode is also an option
		//timer.scheduleAtFixedRate(...);
	}
	
	//stop the timer, and reset it
	public void stopTimer() {
		timerTask.cancel();
		
		timerRunning = false;
		timerValue = TIMER_LENGTH;
	}
	
	//send the given json message to every currently connected websocket
	private void sendToAll(JSONObject json) {
		sendTo(websocketToSessionId.keySet(), json);
	}
	
	//send a message to a given list of websockets
	private void sendTo(Iterable<WsSession> websockets, JSONObject json) {
		for (WsSession session : websockets) {
			
			//although websockets should be removed when closed,
			//we check here just in case, as an exception
			//is thrown if trying to send a message on a closed websocket
			if (session.isOpen()) {
				session.send(json.toString());	
			}
			//TODO remove session if not open anymore? seems like a nice spot to do so
		}
	}
	
	//send a game message to all connected websockets
	//a game message is really just a chat message
	private void sendGameMessage(String message) {
		sendToAll(new JSONObject()
			.put("type", "chat")
			//we use colon so the name part is unobtrusive ":: "
			.put("sender", ":")
			.put("message", message)
		);
	}
	
	//filters through the currently connected users
	//and returns a list of specifically Players
	private List<Player> getPlayers() {
		ArrayList<Player> players = new ArrayList<>();
		
		for (User user : sessionIdToUser.values()) {
			
			//TODO maybe better data structure than having to do this
			//could probably improve with u.isPlayer() or something
			//then again, this is fast enough and works fine
			if (user instanceof Player) {
				players.add((Player)user);
			}
		}
		
		return players;
	}
	
	//builds a sort-of "reverse map" of player->set of websockets
	private Map<Player, Set<WsSession>> getPlayersToWebsockets() {
		HashMap<Player, Set<WsSession>> playerToWebsockets = new HashMap<>();
		
		for (Entry<WsSession, String> entry : websocketToSessionId.entrySet()) {
			WsSession websocket = entry.getKey();
			String sessionId = entry.getValue();
			
			//redundant check, as the sending method checks if it's open,
			//but this keeps us from adding many websockets that are closed
			//ideally closed websockets should be removed from websocketToSessionId,
			//but it doesn't hurt to check
			if (websocket.isOpen()) {
				User user = sessionIdToUser.get(sessionId);
				
				if (user instanceof Player) {
					Player player = (Player)user;
					
					//if we haven't seen this player before, initialize the set
					if (!playerToWebsockets.containsKey(player)) {
						playerToWebsockets.put(player, new HashSet<>());
					}
					
					//add the websocket to the set
					playerToWebsockets.get(player).add(websocket);
				}
			}
		}
		
		return playerToWebsockets;
	}
	
	//sets up all the handlers for websocket messages on the static handler field
	public void registerWebsocketHandlers() {
		
		//TODO we could likely get away with passing in session id instead of websocket
		//we don't seem to use actual websocket, and could just require the websocket to be linked
		//before calling any of these api functions
		
		//chat message
		handler.registerWebsocketHandler("chat", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			User user = sessionIdToUser.get(sessionId);
			
			String message = map.getString("message");
			//we use the websocket to determine sender's name
			String sender = user.name;
			
			//TODO could add support for a user list
			//could be type:chat, event:join
			//and type:chat event:message for what this is now
			
			//send everyone the message
			sendToAll(new JSONObject()
				.put("type", "chat")
				.put("sender", sender)
				.put("message", message)
			);
		});
		
		//timer request
		handler.registerWebsocketHandler("timer", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			
			String command = map.getString("command");
			
			//the only command for now is start
			if (!command.equals("start")) {
				System.out.println("unrecognized timer command "+command);
				return;
			}
			
			//ensure only the owner can start the timer
			if (isOwner(sessionId)) {
				System.out.println("starting!");
				//also notifies everyone
				startTimer();
				
			} else {
				System.out.println("someone who wasn't the owner attempted to start the timer. ignoring");
			}
		});
		
		//card image update
		handler.registerWebsocketHandler("card", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			User user = sessionIdToUser.get(sessionId);
			
			if (!(user instanceof Player)) {
				System.out.println("someone who wasn't a player tried to update their card. ignoring");
				return;
			}
			
			String svgString = map.getString("value");
			
			//update the server-side card for that player
			((Player)user).updateCard(svgString);
		});
		
		//change game state request
		handler.registerWebsocketHandler("changeState", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			
			if (!isOwner(sessionId)) {
				System.out.println("someone who wasn't the owner attempted to change the state. ignoring");
				return;
			}
			
			//Switch from the current game state to the next
			
			//as the client doesn't tell us what state to transition to,
			//this switch statement tells us, given the current state, what state to transition to
			//TODO could put these rules inside the enum itself maybe
			switch (currentState) {
				case BEFORE_GAME:
					
					//beforegame always goes to drawing
					transitionToState(State.DRAWING);
					break;
					
				case DRAWING:
					
					//always goes to voting, so we need to set up for that
					
					//generate the matchups for the voting round, and load the first one
					createMatchups(); //also increments round
					loadMatchup();
					
					transitionToState(State.VOTING);
					break;
					
				case VOTING:

					//determines the round winner
					determineWinner();
					
					//load matchup will return false if we're out of matches
					//so we attempt to load
					if (loadMatchup()) {
						//on success, it means we're in the middle of round,
						//so we go (back) to voting
						transitionToState(State.VOTING);
						
					//on failure to load a match, if we still have rounds to go,
					} else if (currentRound < MAX_ROUNDS){
						//then it means we're at the end of a round, but not
						//the end of the game, so we shuffle the cards and
						//go back to drawing
						shuffleCards();
						displayScores(false); //we also display scores so far
						transitionToState(State.DRAWING);
					
					//otherwise,
					} else {
						//we're both at the end of a round and the end of the game,
						//so we display the winner, reset, and go to endgame
						displayScores(true);
						resetScores();
						transitionToState(State.END_GAME);
					}
					
					break;
					
				case END_GAME:
					
					//for now, we transition back to starting state, and reset
					//allowing games to keep playing in a loop
					
					currentRound = 0;
					
					//tell all players to clear their cards
					sendToAll(new JSONObject()
						.put("type", "card")
						.put("clear", true)
					);
					
					transitionToState(State.BEFORE_GAME);
			}
		});
		
		//vote request
		handler.registerWebsocketHandler("vote", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			User user = sessionIdToUser.get(sessionId);
			
			int vote = map.getInt("value");
			
			//increment the vote based on who they voted for
			//keeping in mind who they voted for previously
			incrementVote(user, vote);
			
			//Send the updated vote counts to all clients
			sendToAll(new JSONObject()
				.put("type", "vote")
				.put("votes1", votes1)
				.put("votes2", votes2)
			);
		});
		
		//leaving game
		handler.registerWebsocketHandler("leaveGame", (websocket, map) -> {
			String sessionId = websocketToSessionId.get(websocket);
			User user = sessionIdToUser.get(sessionId);
			
			//for now, to avoid complexity, we do not give game owners
			//the option to leave a game
			if (!isOwner(sessionId)) {
				//we just remove the user from the map
				sessionIdToUser.remove(sessionId);
				
				sendGameMessage(user.name+" left the game");
				
			} else {
				//TODO how to handle owner leaving?
				//could like close the game and kick everyone or something
				System.out.println("owner tried to leave game, ignoring");
			}
		});
	}
	
	//sets up all the handlers for http messages on the static handler field
	public void registerHttpHandlers() {
		
		//request to see if new user or returning user
		handler.registerHttpHandler("amINew", map -> {
			JSONObject response = new JSONObject();
			
			String sessionId = map.getString("sessionId");
			
			boolean newUser = !sessionIdToUser.containsKey(sessionId);
			
			response.put("newUser", newUser);
			
			//if the're an existing user, we tell them their current server-side username
			if (!newUser) {
				response.put("username", sessionIdToUser.get(sessionId).name);
			}
			
			return response;
		});
		
		//request to create a new user
		handler.registerHttpHandler("createUser", map -> {
			JSONObject response = new JSONObject();
			
			String sessionId = map.getString("sessionId");
			
			String username = map.getString("username");
			boolean isSpectator = map.getBoolean("isSpectator");
			
			//we add the given type into the map, using the client provided
			//username and session id
			if (isSpectator) {
				sessionIdToUser.put(sessionId, new Spectator(username, sessionId));
			} else {
				sessionIdToUser.put(sessionId, new Player(username, sessionId));
			}
			
			return response;
		});
		
		//request to get current game state
		handler.registerHttpHandler("state", map -> {
			JSONObject response = new JSONObject();
			
			String sessionId = map.getString("sessionId");
			
			//we just load in various properties of the current game
			response.put("isOwner", isOwner(sessionId));
			response.put("timerRunning", timerRunning);
			
			if (timerRunning) {
				response.put("timerValue", timerValue);
			}
			
			boolean isSpectator = sessionIdToUser.get(sessionId) instanceof Spectator;
			
			response.put("isSpectator", isSpectator);
			
			response.put("currentState", currentState.name());
			
			//we add a bit more information if we're currently in the voting state
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
	
	//handle a websocket connection. this is called by GameManager
	public void handleConnect(WsSession websocket) {
		//this is probably no-op, as we do all work after getting some
		//inital setup messages
	}
	
	//handle a websocket closing. this is called by GameManager
	public void handleClose(WsSession websocket) {
		//we just remove the websocket from our map
		websocketToSessionId.remove(websocket);
	}
	
	//handle a websocket message. this is called by GameManager
	public void handleMessage(WsSession websocket, JSONObject map) {	
		//we check for message validity first
		if (!ApiHandler.isValidMessage(map)) {
			return;
		}
		
		//we do most handling using the ApiHandler, but we have a special case for the
		//linkWebsocket message type, as that needs to occur first in any websocket
		//before other message types are permitted
		String type = map.getString("type");
		
		if (type.equals("linkWebsocket")) {
			//connect the websocket to the given session id
			
			String sessionId = map.getString("sessionId");
			
			//we also keep track of if we've seen this session id before
			//this way, if a user joins via multiple tabs (so different websockets),
			//we don't get the "join game" message below multiple times
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
				
				//inform everyone of the new user joining
				sendGameMessage(name+" joined the game"+spectatorString);
			}
			
			return;
		}
		
		//if we haven't gotten a linkWebsocket message from this websocket, then we don't allow other message types
		if (!websocketToSessionId.containsKey(websocket)) {
			System.out.println("got message from unauthenticated (unlinked) websocket "+websocket.getId()+", ignoring");
			return;
		}
		
		//at this point, this method ensures that this websocket is bound to a session id
		
		//the order of calls on the client side should also ensure that at this point we also have a user object
		//connected to the given session id. this is because the http api "createUser" request should be performed
		//before opening the websocket, and reaching this point
		
		//so now, we can handle any other type of message using the api handler
		
		handler.handleWebsocketMessage(websocket, map);
	}
	
	//handle a http message. this is called by GameManager
	public JSONObject handleHttpMessage(JSONObject map) {
		//we just forward all handling to the api handler, as there isn't
		//any special case like there in in the websocket messages
		return handler.handleHttpMessage(map);
		
	}
	
	//constructs the matchups list
	//matchups will be an even list of players such that adjacent pairs
	//are matched to each other
	//e.g. (0,1), (2,3) etc
	//matchups may contain duplicate players, but it will
	//be shallow copies. we aren't duplicating player instances.
	//calling this function also increments currentRound
	public void createMatchups() {
		
		//increment the round
		currentRound++;

		matchups = getPlayers();
		matchupIndex = -2; //we start two above, as loadMatchup() increments first
		
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
			//we don't pick last so we don't match the last player with themselves
			matchups.add(matchups.get(random.nextInt(matchups.size()-1)));
		}
	}
	
	//moves the matchup index pointer to the next matchup
	//returns false if at end of voting
	//after calling this method, you can use
	//getPlayer1/2() below to get the matchup-ed players
	//also resets the vote counts
	public boolean loadMatchup() {
		matchupIndex += 2;
		
		votes1 = 0;
		votes2 = 0;
		
		return matchupIndex < matchups.size();
	}
	
	//returns the current first player in the matchup
	public Player getPlayer1() {
		return matchups.get(matchupIndex);
	}
	
	//returns the current second player in the matchup
	public Player getPlayer2() {
		return matchups.get(matchupIndex+1);
	}
	
	//changes to the given state, informing players,
	//and handling any extra needed logic
	public void transitionToState(State state) {
		JSONObject json = new JSONObject();
		
		json.put("type", "changeState");
		json.put("currentState", state.name());
		
		//if we're transitioning to voting,
		//we add extra information about the current matchup
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
		
		//inform players of change
		sendToAll(json);
	}
	
	//picks a current matchup winner with highest score,
	//gives them a point, and sends a message announcing
	//the winner to all players
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
			
			//update the new card server-side
			player.updateCard(newCard);
			
			//tell them their new card
			sendTo(websockets, new JSONObject()
				.put("type", "card")
				.put("value", newCard)
			);
		}
	}
	
	//prints the current scores in the chat
	//if showWinner is true, also declares the overall winner
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
			//the first player in the sorted list will have the highest score
			sendGameMessage(players.get(0).name+" wins!");
		}
	}
	
	//reset all the player's scores to 0
	public void resetScores() {
		for (Player player : getPlayers()) {
			player.score = 0;
		}
	}
	
	//updates a player's vote keeping in mind who was voted for previously
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
		
		//and in all cases, update the new oldVote to the newVote
		user.currentVote = newVote;
	}
	
	//resets all users vote to "no vote"
	public void clearVotes() {
		for (User user : sessionIdToUser.values()) {
			user.currentVote = -1;
		}
	}
}
