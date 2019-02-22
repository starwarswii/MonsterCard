package drawDemo;

import io.javalin.Javalin;
import io.javalin.websocket.WsSession;
import java.util.ArrayList;

public class drawDemo {
    public static void main(String[] args) {
        Javalin app = Javalin.create().enableStaticFiles("/public");

        ArrayList<WsSession> sessions = new ArrayList<>();
        ArrayList<ArrayList<Card>> cards = new ArrayList<ArrayList<Card>>();

        //no redirect for root is needed, going to room (/), javalin serves index.html

        app.ws("/draw", ws -> {

            ws.onConnect(session -> {
                System.out.println("connection made "+session);
                sessions.add(session);
            });

            ws.onClose((session, statusCode, reason) -> {
                System.out.println("connection closed "+session+" "+statusCode+" "+reason);
                sessions.remove(session);
            });

            ws.onMessage((session, msg) -> {
                String [] msgSplit = msg.split(":");
                if(msgSplit.length != 0 && msgSplit[0].equals("Request")) { // Takes in a request for a card and outputs the SVG
                    int index = Integer.parseInt(msgSplit[1]);
                    if(index <= cards.size()) {
                        session.send(cards.get(index-1).get(0).getImage());
                    } else {
                        System.out.println("invalid card");
                    }
                } else { // Takes the SVG string, creates a new card object and puts in a 2d ArrayList
                    ArrayList<Card> newCardStart = new ArrayList<Card>();
                    newCardStart.add(new Card(msg, "SampleText"));
                    cards.add(newCardStart);
                }
            });
        });

        app.start(7000);
    }
}
