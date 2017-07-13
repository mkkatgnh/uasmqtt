package de.mobile2power.aircamqc;

import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class ConnectorMQTT {
	private String topicDomain = "uascon";
	private int qos = 0;
	private String broker = "tcp://www.mobile2power.de:1883";
	public String clientId = "androidtx";
	private MqttClient sampleClient;
	MemoryPersistence persistence = new MemoryPersistence();
	MqttConnectOptions connOpts;

	public void setupConnection(String brokerUrl, MqttCallback callback, String clientId) {
		try {
			if (brokerUrl != null && brokerUrl.length() > 0) {
				broker = brokerUrl;
			}
			sampleClient = new MqttClient(broker, "android" + clientId, persistence);
			connOpts = new MqttConnectOptions();
			connOpts.setWill("pahodemo/clienterrors", "crashed".getBytes(), 2,
					true);
			connOpts.setCleanSession(true);
			connOpts.setKeepAliveInterval(30);
			sampleClient.setCallback(callback);
			sampleClient.setTimeToWait(1000); // 1 second

			this.clientId = clientId;

		} catch (MqttException e) {
			e.printStackTrace();
		}

	}

	public void teardownConnection() {
		try {
			sampleClient.disconnect();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendMessage(byte[] payload, String sensorPart) {
		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		try {
			if (sampleClient != null) {
				sampleClient.publish(topicDomain + "/" + clientId + "/"
						+ sensorPart, message);
			}
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void connect(String clientId) {
		try {
			sampleClient.connect(connOpts);
			int[] subQoS = { 0, 0 };
			String[] topics = { topicDomain + "/" + clientId + "/event",
					topicDomain + "/" + clientId + "/mavlink/gc" };
			sampleClient.subscribe(topics, subQoS);
		} catch (MqttSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean connectionEstablished() {
		if (sampleClient == null) {
			return false;
		}
		return sampleClient.isConnected();
	}
}
