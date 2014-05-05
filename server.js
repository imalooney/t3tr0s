var express = require('express');
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io').listen(server);
var fs = require('fs');

app.get('/', function(req, res) {
    res.sendfile('public/index.html');
});

app.use(express.static(__dirname + '/public'));

var port = 1234;
server.listen(port);
console.log('listening on port',port);

io.set('log level', 1);
io.sockets.on('connection', function(socket) {
  fs.watch('public/t3tr0s.js', function() {
    socket.emit('refresh');
  });
});
