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

	private long currentTimeMqtt = 0;
	private long currentTimePreview = 0;
	private long currentTimeControlQC = 0;
	private static final long TIME_5_FPS = 1000 / 5; // 10 per seconds
	private static final long TIME_10_FPS = 1000 / 10; // 10 per seconds
	private static final long TIME_50_FPS = 1000 / 50;
	private AttitudeAndPositionManager posManager = new AttitudeAndPositionManager();
	private ConnectorMQTT connectorMQTT = new ConnectorMQTT();
	private CamPosAndView attitudePosition;
	private Sender sender;
	private Gson gson = new Gson();
	private String json;
	private CamPreview camPreview;
	private boolean previewHasToRestart = false;
	private String pictureFolder = Environment.getExternalStorageDirectory()
			.getPath() + File.separator + "AircamQC";
	private Location location;
	private Preview previewDTO = new Preview();
	private boolean previewTransmit;
	private boolean pressureSensorAvailable = false;

	public Operator(LocationManager locationManager) {
		posManager.setup(locationManager, pressureSensorAvailable);
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

		if (currentTime > (currentTimeMqtt + TIME_10_FPS)) {
			currentTimeMqtt = currentTime;
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
		if (currentTime > (currentTimePreview + TIME_5_FPS)) {
			currentTimePreview = currentTime;
			// send periodly
			if (connectorMQTT.connectionEstablished()) {
				if (previewTransmit) {
					connectorMQTT.sendMessage(previewDTO.getJpegImage(),
							"campreview");
				}
			}
		}
	}

	public void controlQC(long currentTime) {
		if (currentTime > (currentTimeControlQC + TIME_50_FPS)) {
			currentTimeControlQC = currentTime;
			// send periodly
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
					// parent.controlQC(currentTime);
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
		this.camPreview.setPreviewDTO(previewDTO);
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
	public void messageArrived(String topic, MqttMessage content)
			throws Exception {
		takePicture();
	}

	public void previewTransmit(boolean checked) {
		this.previewTransmit = checked;
	}

	public void setPressureSensorAvailable(boolean airPressureSensorAvailable) {
		this.pressureSensorAvailable = airPressureSensorAvailable;
	}
}
