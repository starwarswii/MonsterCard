let id = id => document.getElementById(id);


let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/chat");
ws.onmessage = msg => updateChat(msg);
ws.onclose = () => alert("WebSocket connection closed");

id("send").addEventListener("click", () => sendAndClear(id("message").value));
id("message").addEventListener("keypress", function (e) {
    if (e.keycode === 13) {
        console.log("Heard enter key get pressed\n");
        sendAndClear(e.target.value);
    }
});


function sendAndClear(message) {
    console.log("sending \"" + message + "\" from websocket\n");
    if (message !== "") {
        ws.send(message);
        id("message").value = "";
    }
}



function updateChat(msg) {
    let data = JSON.parse(msg.data);
    console.log("updating the chat with \"" + data.userMessage + "\"\n");
    id("chat").insertAdjacentHTML("afterbegin", "<br>" + data.userMessage);
}