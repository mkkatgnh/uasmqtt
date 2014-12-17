import de.fhpotsdam.unfolding.*;
import de.fhpotsdam.unfolding.geo.*;
import de.fhpotsdam.unfolding.utils.*;
import de.fhpotsdam.unfolding.providers.*;

import se.goransson.qatja.messages.*;
import se.goransson.qatja.*;
import com.google.gson.Gson;

import java.net.*;

UnfoldingMap map;
Qatja client;
Gson gson;
final static String MQTT_BROKER_IP = "127.0.0.1";
String hostId;

AttitudeAndPosition uasPos;

Location currentLocation = new Location(54.5f, 8.0f);

public void setup() {
  size(1300, 880, P2D);
  noStroke();
  hostId = System.getenv("computername");
  if (hostId == null || hostId.length() == 0) {
    hostId = System.getenv("hostname");
  } else if (hostId == null || hostId.length() == 0) {
    hostId = "hostId";
  }

  gson = new Gson();
  uasPos = new AttitudeAndPosition();

  //  AttitudeAndPosition obj = new AttitudeAndPosition();
  //  String json = gson.toJson(obj);
  //  System.out.println(json);

  map = new UnfoldingMap(this, new Microsoft.AerialProvider());
  map.setTweening(true);
  map.zoomToLevel(3);
  map.panTo(currentLocation);
  MapUtils.createDefaultEventDispatcher(this, map);

  client = new Qatja( this );
  client.DEBUG = true;

  client.connect( MQTT_BROKER_IP, 1883, "uasConGround" + hostId );
}

void keyPressed() {
  byte qos = 0;
  if (key == 'c' || key == 'c') {
    client.subscribe( "uascon/+/position", qos );
  } else if (key == ' ') {
    client.publishAtLeastOnce("uascon/"+hostId+"/event", "{\"cam\":\"takepicture\"}");
  }
}

public void draw() {
  background(0);
  map.draw();

  // Draws locations on screen positions according to their geo-locations.

  //  currentLocation = new Location(52.5f - (my / 30.0f), 8.0f + (mx / 30.0f));
  currentLocation = new Location(uasPos.getLat(), uasPos.getLon());

  // Fixed-size marker
  ScreenPosition posScreen = map.getScreenPosition(currentLocation);
  fill(200, 200, 200, 250);
  text(String.format("%.1f", uasPos.getAlt()), posScreen.x, posScreen.y);
  fill(200, 0, 0, 250);
  translate(posScreen.x, posScreen.y);

  rotate((float)Math.toRadians(uasPos.getYaw()));
  triangle(0, 0, 7, 20, -7, 20);
}

void mqttCallback(MQTTPublish msg) {
  String payload = new String(msg.getPayload());
  uasPos = gson.fromJson(payload, AttitudeAndPosition.class);
}

