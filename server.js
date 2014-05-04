var express = require('express');
var app = express();

app.get('/', function(req, res) {
    res.sendfile('public/index.html');
});

app.use(express.static(__dirname + '/public'));

var port = 1234;
app.listen(port);
console.log('listening on port',port);
