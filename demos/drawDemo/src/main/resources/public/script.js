$(function() {

    var canvas = new fabric.Canvas('c', {
        isDrawingMode: true
    });

    fabric.Object.prototype.transparentCorners = false;

    var drawingClear = document.getElementById("clearcanvas");
    var drawingColor = document.getElementById("drawing_color");
    var drawingLineWidth = document.getElementById("drawing_linewidth");
    var $save = $("#save");
    var $req = $("#req");

    drawingClear.onclick = function() {
        canvas.clear();
    };

    drawingColor.onchange = function() {
        canvas.freeDrawingBrush.color = this.value;
    };

    drawingLineWidth.onchange = function() {
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

    $save.click(function() {
        // console.log(canvas.toSVG());
        socket.send(canvas.toSVG());
    });

    $req.click(function() {
        // console.log("Request|" + document.getElementById("cardID").value);
        socket.send("Request:" + document.getElementById("cardID").value);
    });

    socket.onmessage = function(msg) {
        // console.log("got message", msg, msg.data);
        fabric.loadSVGFromString(msg.data, function(objects, options) {
            var obj = fabric.util.groupSVGElements(objects, options);
            canvas.clear();
            canvas.add(obj).renderAll();
        });
    };

});