package de.mobile2power.aircamqcviewer;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;

public class Operator implements MqttCallback {

	private long currentTimeMqtt = 0;
	private static final long TIME_10_FPS = 1000 / 10; // 10 per seconds
	private ConnectorMQTT connectorMQTT = new ConnectorMQTT();
	private CamPosAndView attitudePosition;
	private Sender sender;
	private Gson gson = new Gson();
	private String json;
	private Preview previewDTO = null;
	private TakePictureState takePictureState = null;

	public void setup(String brokerUrl) {
		connectorMQTT.setupConnection(brokerUrl, this);
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
			json = gson.toJson(attitudePosition);
			if (connectorMQTT.connectionEstablished()) {
//				connectorMQTT.sendMessage(json.getBytes(), "position");
				if (takePictureState.isRelease()) {
					connectorMQTT.sendMessage(
							"{\"cam\":\"takepicture\"}".getBytes(), "event");
					takePictureState.reset();
				}
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

	@Override
	public void connectionLost(Throwable arg0) {
		if (connectorMQTT.connectionEstablished()) {
			connectorMQTT.teardownConnection();
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		if (topic.endsWith("campreview")) {
			previewDTO.setJpegImage(message.getPayload());
			previewDTO.setWidth(640);
			previewDTO.setHeight(480);
		}
//		if (topic.endsWith("position")) {
//			String payload = new String(message.getPayload());
//			CamPosAndView uasPos = new CamPosAndView();
//			uasPos = gson.fromJson(payload, CamPosAndView.class);
//			attitudePosition.setAlt(uasPos.getAlt());
//			attitudePosition.setLat(uasPos.getLat());
//			attitudePosition.setLon(uasPos.getLon());
//		}
	}

	public void setPreview(Preview preview) {
		this.previewDTO = preview;
	}

	public void setTakePictureState(TakePictureState takePictureState) {
		this.takePictureState = takePictureState;
	}
}