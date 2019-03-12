$(function() {

    var canvas = new fabric.Canvas('c', {
        isDrawingMode: true
    });

    var undo = [];
    var redo = [];
    var state;
    var drawing = true;

    canvas.on("object:added", function () {
        if(drawing) {
            redo = []; // clears all redo states
            undo.push(state); // adds the last state before drawing to the stack
            state = JSON.stringify(canvas); // updates the state for undomanager
        }
    });

    canvas.on("object:modified", function () {
        if(drawing) {
            redo = [];
            undo.push(state);
            state = JSON.stringify(canvas);
        }
    });

    function replay(playStack, saveStack) { // shifts the state over
        saveStack.push(state);
        state = playStack.pop();
        canvas.clear();
        drawing = false; // makes sure the canvas doesn't see replay as drawing so it doesn't update the undo/redo stacks
        canvas.loadFromJSON(state, function() {
            canvas.renderAll();
        });
        drawing = true;
    }

    document.addEventListener('keydown', function(event) {
        if(event.ctrlKey) {
            if (event.keyCode == 90) { // undo
                if(undo.length > 0) { // won't undo if there is no undo state left
                    replay(undo, redo);
                }
            } else if (event.keyCode == 89) { // redo
                if(redo.length > 0) { // won't redo if there is no redo state left
                    replay(redo, undo);
                }
            }
        }
    });

    fabric.Object.prototype.transparentCorners = false;

    var drawingClear = document.getElementById("clearcanvas");
    var drawingColor = document.getElementById("drawing_color");
    var drawingLineWidth = document.getElementById("drawing_linewidth");
    var $save = $("#save");
    var $req = $("#req");

    drawingClear.onclick = function() { // clear canvas button
        canvas.clear();
    };

    drawingColor.onchange = function() { // edits the brush color
        canvas.freeDrawingBrush.color = this.value;
    };

    drawingLineWidth.onchange = function() { // edits the brush width
        canvas.freeDrawingBrush.width = parseInt(this.value, 10) || 1;
        this.previousSibling.innerHTML = this.value;
    };

    if (canvas.freeDrawingBrush) {
        canvas.freeDrawingBrush.color = drawingColor.value;
        canvas.freeDrawingBrush.width = parseInt(drawingLineWidth.value, 10) || 1;
    }


    var socket = new WebSocket("ws://"+location.hostname+":"+location.port+"/draw");
    socket.onopen = function() {
        console.log("Websocket opened");
    };
    socket.onclose = function() {
        console.log("WebSocket closed");
    };

    $save.click(function() { // converts the canvas to SVG and sends it to the server
        // console.log(canvas.toSVG());
        socket.send(canvas.toSVG());
    });

    $req.click(function() {
        // console.log("Request|" + document.getElementById("cardID").value);
        socket.send("Request:" + document.getElementById("cardID").value); // sends a request for the specific card the client wants
    });

    socket.onmessage = function(msg) {
        // console.log("got message", msg, msg.data);
        fabric.loadSVGFromString(msg.data, function(objects, options) { // parses in data into a callback function and
            var obj = fabric.util.groupSVGElements(objects, options); // creates the canvas object from the SVG input
            canvas.clear(); // clears the canvas so it can render the new SVG
            canvas.add(obj).renderAll();
        });
    };

});