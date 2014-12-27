import de.fhpotsdam.unfolding.*;
import de.fhpotsdam.unfolding.geo.*;
import de.fhpotsdam.unfolding.utils.*;
import de.fhpotsdam.unfolding.providers.*;

import se.goransson.qatja.messages.*;
import se.goransson.qatja.*;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import java.net.*;

UnfoldingMap map;
Qatja client;
Gson gson;
final static String MQTT_BROKER_IP = "127.0.0.1";
String hostId;

CamPosAndView uasPos;
List<CamPosAndView> picturePositions = new ArrayList<CamPosAndView>();

Location currentLocation = new Location(54.5f, 8.0f);

public void setup() {
  size(1300, 880, P2D);
  noStroke();

  PFont mono;
  mono = loadFont("AndaleMono-18.vlw");
  textFont(mono);

  hostId = System.getenv("computername");
  if (hostId == null || hostId.length() == 0) {
    hostId = System.getenv("hostname");
  } else if (hostId == null || hostId.length() == 0) {
    hostId = "hostId";
  }

  gson = new Gson();
  uasPos = new CamPosAndView();

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
    picturePositions.add(uasPos);
  } else if (key == 'm' || key == 'M') {
    map.zoomAndPanTo(currentLocation, 17);
  }
}

public void draw() {
  background(0);
  map.draw();

  // Draws locations on screen positions according to their geo-locations.

  currentLocation = new Location(uasPos.getLat(), uasPos.getLon());

  ScreenPosition posScreen = map.getScreenPosition(currentLocation);
  ImageSize size = calculateImageGroundSize(uasPos.getAngh(), uasPos.getAngv(), 20.0); // uasPos.getAlt());

  fill(230, 230, 230, 255);
  text(String.format("%.1f", uasPos.getAlt()), posScreen.x, posScreen.y);
  
  drawTakenPhotoAreaSize();
  drawArrowAndPhotoAreaSize(posScreen, size);
}

void drawArrowAndPhotoAreaSize(ScreenPosition posScreen, ImageSize size) {
  pushMatrix();
  translate(posScreen.x, posScreen.y);

  rotate((float)Math.toRadians(uasPos.getYaw()));
  fill(200, 100, 0, 80);
  rect(-(size.getWith()/2), -(size.getHeight()/2), size.getWith(), size.getHeight());
  fill(200, 0, 0, 250);
  triangle(0, 0, 7, 20, -7, 20);
  popMatrix();
}

void drawTakenPhotoAreaSize() {
  for (CamPosAndView photoPos : picturePositions) {
    Location photoLocation = new Location(photoPos.getLat(), photoPos.getLon());

    ScreenPosition photoPosScreen = map.getScreenPosition(photoLocation);

    ImageSize photoSize = calculateImageGroundSize(photoPos.getAngh(), photoPos.getAngv(), 20.0); // photoPos.getAlt());

    pushMatrix();
    translate(photoPosScreen.x, photoPosScreen.y);


    rotate((float)Math.toRadians(photoPos.getYaw()));
    fill(250, 180, 0, 80);
    rect(-(photoSize.getWith()/2), -(photoSize.getHeight()/2), photoSize.getWith(), photoSize.getHeight());
    popMatrix();
  }
}

void mqttCallback(MQTTPublish msg) {
  String payload = new String(msg.getPayload());
  uasPos = new CamPosAndView();
  uasPos = gson.fromJson(payload, CamPosAndView.class);
}

ImageSize calculateImageGroundSize(double angleHorizontal, double angleVertical, double altitude) {
  float halfHorizontalMeter = (float)altitude * (float)Math.tan(Math.toRadians(angleHorizontal) / 2.0);
  float halfVerticalMeter   = (float)altitude * (float)Math.tan(Math.toRadians(angleVertical) / 2.0);

  Location movedHor = GeoUtils.getDestinationLocation(currentLocation, 0.0, halfHorizontalMeter / 1000.0);
  Location corner = GeoUtils.getDestinationLocation(movedHor, 90.0, halfVerticalMeter / 1000.0);
  ScreenPosition cornerPos = map.getScreenPosition(corner);
  ScreenPosition posScreen = map.getScreenPosition(currentLocation);
  ImageSize size = new ImageSize();
  size.setWith((cornerPos.x - posScreen.x)*2);
  size.setHeight((cornerPos.y - posScreen.y)*2);
  return size;
}

