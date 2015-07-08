package de.mobile2power.aircamqc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;

import com.google.gson.Gson;

public class Operator implements MqttCallback {

	private static final byte MAVLINK_START = (byte) 0xFE;

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
	private BluetoothReceiverThread btReceiver;
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

	private OutputStream bluetoothOutStream = null;
	private InputStream bluetoothInStream = null;

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
		btReceiver = new BluetoothReceiverThread();
		btReceiver.start();

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
		if (topic.contains(connectorMQTT.clientId)) {
			return;
		}
		if (topic.endsWith("event")) {
			String payload = new String(content.getPayload());
//			
//			Event event = new Event();
//			event = gson.fromJson(payload, Event.class);
//			if ("cam".equals(event.getType())
//					&& "takepicture".equals(event.getAction())) {
			if ("{\"cam\":\"takepicture\"}".equals(payload)) {
				takePicture();
			}
		}
		if (bluetoothOutStream != null && topic.endsWith("mavlink/gc")) {
			byte[] payload = content.getPayload();
			bluetoothOutStream.write(payload, 0, payload.length);
			bluetoothOutStream.flush();
		}
	}

	class BluetoothReceiverThread extends Thread {

		@Override
		public void run() {
			while (true) {
				try {
					if (bluetoothInStream != null
							&& bluetoothInStream.available() > 0) {
						byte[] txBuffer = null;
						txBuffer = copyStream(bluetoothInStream);
						connectorMQTT.sendMessage(txBuffer, "mavlink/uav");
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

	}

	protected byte[] copyStream(InputStream inputstream) throws IOException {
		byte[] buffer = new byte[9];
		byte[] txBuffer = null;
		int len;
		int payloadLength = 0;
		len = inputstream.read(buffer, 0, buffer.length);
		if (len == buffer.length) {
			int i = 0;
			// Looking for the start of the mavlink package
			while ((i < buffer.length) && (buffer[i] != MAVLINK_START)) {
				i++;
			}
			if (i >= buffer.length) {
				i = 0;
			}
			if (buffer.length > (i+1)) {
				payloadLength = buffer[i + 1] & 0xFF;
				txBuffer = new byte[payloadLength + 8];
				// copy start and rest of mavlink package into txBuffer
				if ((payloadLength + 8) >= len) {
					System.arraycopy(buffer, i, txBuffer, 0, len - i);
					// try to read the rest of the mavlink package
					int secondPayloadPart = (payloadLength + 8) - (len - i);
					if (secondPayloadPart > 0) {
						inputstream.read(txBuffer, len - i, secondPayloadPart);
					}
				}
			}

		} else {
			txBuffer = new byte[len];
			System.arraycopy(buffer, 0, txBuffer, 0, len);
		}
		return txBuffer;
	}

	public void previewTransmit(boolean checked) {
		this.previewTransmit = checked;
	}

	public void setPressureSensorAvailable(boolean airPressureSensorAvailable) {
		this.pressureSensorAvailable = airPressureSensorAvailable;
	}

	public void setOutputStream(OutputStream bluetoothOutStream) {
		this.bluetoothOutStream = bluetoothOutStream;
	}

	public void setInputStream(InputStream bluetoothInStream) {
		this.bluetoothInStream = bluetoothInStream;
	}
}
