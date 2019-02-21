package timerDemo;

import java.util.ArrayList;
import io.javalin.Javalin;
import io.javalin.JavalinEvent;
import io.javalin.websocket.WsSession;

public class TimerDemo {
	
	static boolean running = false; //if the timer is running
	static int timer; //the remaining time on the timer

	//starts the timer by setting up the variables
	public static void startTimer() {
		timer = 10;
		running = true;
	}
	
	//stops the timer
	public static void stopTimer() {
		running = false;
	}
	
	//sends the given message to all given websocket sessions
	public static void sendToAll(ArrayList<WsSession> sessions, String message) {
		for (WsSession s : sessions) {
			s.send(message);
		}
	}
	
	public static void main(String[] args) {
		//we set up static file serving, meaning all files in src/main/resources/public will be served under root
		//trying to visit http://localhost:7000/foo.txt will attempt to serve src/main/resources/public/foo.txt if it exists
		Javalin app = Javalin.create().enableStaticFiles("/public");
		
		//set up some handlers to log all routes
		app.before(ctx -> System.out.println("Started "+ctx.method()+" \""+ctx.path()+"\" from "+ctx.ip()));
		app.after(ctx -> System.out.println("Finished "+ctx.method()+" \""+ctx.path()+"\" from "+ctx.ip()));

		//set up our list of active websocket sessions
		//it's possible concurrency issues could arise with this
		//it might make sense to switch this structure out with a concurrent one
		ArrayList<WsSession> sessions = new ArrayList<>();
		
		//we don't need some kind of app.get("/", ctx -> {serve index.html});
		//because javalin (or the actual server (jetty) that runs underneath it?) seems to by default serve index.html at root
		
		//set up a route. going to /state will either give "stopped" or the current timer value (as a string)
		app.get("/state", ctx -> ctx.result(running ? Integer.toString(timer) : "stopped"));
		
		//set up our websocket connections under /timer
		app.ws("/timer", ws -> {
			
			ws.onConnect(session -> {
				//when a client connects, add their session to the list we're keeping
				System.out.println("websocket connection made from "+session.host()+" with id "+session.getId());
				sessions.add(session);
			});

			ws.onClose((session, statusCode, reason) -> {
				//when they disconnect, remove them from the list
				System.out.println("websocket connection closed from "+session.host()+" with id "+session.getId()+". "+statusCode+": "+reason);
				sessions.remove(session);
			});

			ws.onMessage((session, msg) -> {
				//when we get a message
				System.out.println("got message from "+session.host()+" with id "+session.getId()+": "+msg);
				if (msg.equals("start")) {
					//if it's "start", we send tell all connected clients and start the timer
					sendToAll(sessions, "start");
					startTimer();
				}
			});
		});
		
		//here we hook in on the "failed to start server" event so that we can stop this code below
		//from running if the server never started. the primary way the server couldn't start would be
		//with a BindException, which occurs when trying to run the server again if it's already running (on that port)
		//this is definitely not necessary (and/or also maybe it would be better to simply hook the code we want to run to run
		//when the server /successfully/ starts up, as unsafely stopping the program with exit() could maybe cause issues),
		//but it's just an extra thing to prevent the infinite loop from running if the server didn't start because we
		//accidentally ran the server twice
		app.event(JavalinEvent.SERVER_START_FAILED, () -> {
			System.out.println("server failed to start, exiting");
			System.exit(1);
		});
		
		//this starts the actual server on port 7000
		//it actually doesn't block, so it's probably starting the server on another thread
		app.start(7000);
		
		//because it doesn't block, we can continue running code here
		//we run our main loop here, using waits to keep the timer's time
		//this works for now, but this method won't be good when handling multiple servers
		//either multiple threads or some sort of timestamp-based method could work
		
		System.out.println("beginning loop");

		while (true) {
			
			//we sleep for some unit of time less than one second
			//less than one second as the timer ticks once a second
			//sleeping is better than not sleeping as not sleeping will have the
			//server spin in this loop as fast as possible, which isn't a great idea
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			//if the timer's running
			if (running) {
				
				//if the timer ticks to the end
				if (timer == -1) {
					//we stop the timer and tell all the clients we did so
					System.out.println("stopping timer");
					stopTimer();
					sendToAll(sessions, "stop");
					
				} else {
					//otherwise tell everyone the current timer value
					//again we need to make it a string because that's what websockets can send
					System.out.println("sending timer value "+timer);
					sendToAll(sessions, Integer.toString(timer));
					
					//here's our one second sleep that makes the timer go at the speed it does
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					//decrement the current timer value
					timer--;
					
				}

			}
			
		}
		
	}
}
