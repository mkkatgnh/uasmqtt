package de.mobile2power.aircamqc;

import java.io.File;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;

import com.google.gson.Gson;

public class Operator implements MqttCallback {

	private long time1fps = 0;
	private long timeFps = 100; // 10 per seconds
	private AttitudeAndPositionManager posManager = new AttitudeAndPositionManager();
	private ConnectorMQTT connectorMQTT = new ConnectorMQTT();
	private CamPosAndView attitudePosition;
	private Sender sender;
	private Gson gson = new Gson();
	private String json;
	private CamPreview camPreview;
	private boolean previewHasToRestart = false;
	private String pictureFolder = Environment.getExternalStorageDirectory().getPath() + File.separator + "AircamQC";
	private Location location;


	public Operator(LocationManager locationManager) {
		posManager.setup(locationManager);
		File folder = new File(pictureFolder);
		if (!folder.exists()) {
			folder.mkdir();
		}
	}

	public void setup(String brokerUrl) {
		connectorMQTT.setupConnection(brokerUrl, this);
	}

	public SensorEventListener getSensorEventListener() {
		return posManager;
	}

	public void run() {
		sender = new Sender();
		sender.setParent(this);
		sender.start();
	}

	public void communicateTaskCaller(long currentTime) throws IOException {

		if (currentTime > (time1fps + timeFps)) {
			time1fps = currentTime;
			// send periodly
			attitudePosition = posManager.getAttitudePosition();
			attitudePosition.setAngh(camPreview.getHorizontalViewAngle());
			attitudePosition.setAngv(camPreview.getVerticalViewAngle());
			location = posManager.getLocation();
			json = gson.toJson(attitudePosition);
			camPreview.setAttitudePositionJson(json);
			if (connectorMQTT.connectionEstablished()) {
				connectorMQTT.sendMessage(json.getBytes(), "position");
			}
		}
	}

	class Sender extends Thread {

		private long currentTime;
		private Operator parent = null;

		public void setParent(Operator parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			while (true) {
				try {
					parent.communicateTaskCaller(currentTime);
					parent.restartPreview();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				currentTime = System.currentTimeMillis();
			}

		}

	}

	public void connectToBroker() {
		connectorMQTT.connect();
	}

	public void disconnect() {
		connectorMQTT.teardownConnection();
	}

	public void altitudeSetZero() {
		posManager.altitudeSetZero();
	}

	public void setCamPreview(CamPreview camPreview) {
		this.camPreview = camPreview;
		this.camPreview.setPictureFolder(pictureFolder);
	}
	
	private void takePicture() {
		if (previewHasToRestart == false) {
			camPreview.takePicture(location);
			previewHasToRestart = true;
		}
	}

	private void restartPreview() {
		if (previewHasToRestart && camPreview.isTakePictureCallbackInactive()) {
			previewHasToRestart = false;
			camPreview.preparePreviewCallbackOnCam();
			camPreview.startPreview();
		}

	}

	@Override
	public void connectionLost(Throwable arg0) {
		connectorMQTT.teardownConnection();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage content) throws Exception {
		takePicture();
	}
}
