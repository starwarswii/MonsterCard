package monsterCard;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.javalin.Javalin;
import io.javalin.websocket.WsSession;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;


public class Chat {
    private static Map<WsSession, String> userUsernameMap = new ConcurrentHashMap<>();
    private static int nextUserNumber = 1;





    //TODO: get this working with multiple chatrooms


    public static void main(String[] args) {

        Javalin.create()
                .enableStaticFiles("/public")
                .ws("/chat", ws -> {
                    ws.onConnect(session -> {
                        String username = "User" + nextUserNumber++;
                        userUsernameMap.put(session, username);
                        broadcastMessage("Server", (username + " joined the chat"));
                    });

                    ws.onClose((session, status, message) -> {
                        String username = userUsernameMap.get(session);
                        userUsernameMap.remove(session);
                        broadcastMessage("Server", (username + " left the chat"));
                    });

                    ws.onMessage((session, message) -> {
                        broadcastMessage(userUsernameMap.get(session), message);
                    });
                })
                .start(7070);





    }




    private static void broadcastMessage(String sender, String message) {

        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            session.send(
                    new JSONObject()
                        .put("userMessage", sender + ": " + message)
                        .put("userlist", userUsernameMap.values()).toString());

        });
    }

}








