package monsterCard;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.json.JSONObject;
import io.javalin.websocket.WsSession;

//holds a series of mappings from type to function for handling requests
//constructed and used by Game
public class ApiHandler {
	
	//mapping of message type to function handles that type
	//given a websocket and the data
	Map<String, BiConsumer<WsSession, JSONObject>> websocketMap;
	
	//mapping of message type to function that handles that type and returns the response
	//given the data
	Map<String, Function<JSONObject, JSONObject>> httpMap;
	
	public ApiHandler() {
		websocketMap = new HashMap<>();
		httpMap = new HashMap<>();
	}
	
	//these two functions just put the given (type, function) binding into one of the maps
	public void registerWebsocketHandler(String type, BiConsumer<WsSession, JSONObject> function) {
		websocketMap.put(type, function);
	}
	
	public void registerHttpHandler(String type, Function<JSONObject, JSONObject> function) {
		httpMap.put(type, function);
	}
	
	//these two functions do a lookup into the map and call the corresponding function
	public void handleWebsocketMessage(WsSession websocket, JSONObject map) {
		if (!isValidMessage(map)) {
			return;
		}
		
		String type = map.getString("type");
		
		if (!websocketMap.containsKey(type)) {
			System.out.println("websocket message has invalid type "+type+". ignoring");
			return;
		}
		
		System.out.println("handling websocket message of type "+type);
		websocketMap.get(type).accept(websocket, map);
	}
	
	public JSONObject handleHttpMessage(JSONObject map) {
		if (!isValidMessage(map)) {
			return new JSONObject().put("error", "no type");
		}
		
		String type = map.getString("type");
		
		if (!httpMap.containsKey(type)) {
			System.out.println("http message has invalid type "+type+". ignoring");
			return new JSONObject().put("error", "invalid type");
		}
		
		System.out.println("handling http message of type "+type);
		return httpMap.get(type).apply(map);
	}
	
	//checks whether the given message is valid, aka has a type
	static boolean isValidMessage(JSONObject map) {
		if (!map.has("type")) {
			System.out.println("message does not have type. ignoring");
			return false;
		}
		
		return true;
	}
}
