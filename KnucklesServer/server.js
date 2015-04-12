var express = require('express');
var app = express();
var bodyParser = require('body-parser');
var f = require('./f');

app.use(bodyParser.json());

var mraa = require('mraa');

var OUT_PIN = 3;
var out = new mraa.Gpio(OUT_PIN);
out.dir(mraa.DIR_OUT);
var is_on = false;
out.write(0);

var settings = {"min_temp":23.0 ,"manual":true}

app.get('/', function (req, res) {
  res.send('Hello World!');
});

var SENSOR_COUNT = 5;

var sensors = [];
for(var i = 0; i < SENSOR_COUNT; i++){
	sensors[i] = new mraa.Aio(i);
}

function getState() {
  return {
    "sensors":readings,
    "temps":temps,
    "is_heating": is_on
  };
}

function manageTemp(t) {
	if(!settings.manual){
		if(t <= settings.min_temp && !is_on){
			is_on = true;
			out.write(1);
			console.log("(auto) turned on");
		} else if(t >= settings.min_temp && is_on){
			is_on = false;
			out.write(0);
			console.log("(auto) turned off");	
		}
	}
}

app.get('/heat', function (req, res) {
  var data = getState();
  res.send(data);
});

app.post('/heat', function (req, res) {
  var body = req.body;
  var on = 0;

  if('settings' in body){
    if('min_temp' in body.settings){
      settings.min_temp = body.settings.min_temp;
    }
    if('manual' in body.settings){
      settings.manual = body.settings.manual;
    }
  }

  if(settings.manual){
    if(body.on){
      is_on = true;
      on = 1;
    } else {
      is_on = false;
      on = 0;
    }
  }

  out.write(on);
  var resp = {"request": body, "state": getState(), "settings": settings};
  console.log(resp);
  res.send(resp);
});

var readings = [];
var temps = [];
setInterval(function(){
	for(var i = 0; i < SENSOR_COUNT; i++){
		readings[i] = sensors[i].read();
		temps[i] = f.btot(readings[i]).t_c;
	}
}, 100);

var server = app.listen(8080, function () {

  var host = server.address().address;
  var port = server.address().port;

  console.log('Example app listening at http://%s:%s', host, port);

});
