var $wrapper = $("#wrapper");
var $voteButtons = $("#voteButtons");
var $canvasControl = $("#canvasControls");
var $chat = $("#chat");

function clearChat($chat) {
    $chat.empty();
}

function initializeStartGame($chat, $wrapper, $canvasControl, $voteButtons) {
    clearChat($chat);
    $wrapper.hide();
    $canvasControl.hide();
    $voteButtons.hide();
}

function initializeDrawing($chat, $wrapper, $canvasControl, $voteButtons, c1) {
    clearChat($chat);
    c1.isDrawingMode = true;
    $wrapper.show();
    $canvasControl.show();
    $voteButtons.hide();
}

function initializeVoting($chat, $wrapper, $canvasControl, $voteButtons, c1) {
    clearChat($chat);
    c1.isDrawingMode = false;
    $wrapper.show();
    $canvasControl.hide();
    $voteButtons.show();
}

function initializeEnd($chat, $wrapper, $canvasControl, $voteButtons) {
    clearChat($chat);
    $wrapper.hide();
    $canvasControl.hide();
    $voteButtons.hide();
}