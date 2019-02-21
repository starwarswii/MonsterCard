let id = id => document.getElementById(id);

console.log("Running on line 3\n");
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
    console.log("printing from sendAndClear\n");
    if (message !== "") {
        ws.send(message);
        id("message").value = "";
    }
}



function updateChat(msg) {
    console.log("printing from updateChat\n");
    let data = JSON.parse(msg.data);
    id("chat").insertAdjacentHTML("afterbegin", data.userMessage);
    id("userlist").innerHTML = data.userlist.map(user => "<li>" + user + "</li>").join("");
}