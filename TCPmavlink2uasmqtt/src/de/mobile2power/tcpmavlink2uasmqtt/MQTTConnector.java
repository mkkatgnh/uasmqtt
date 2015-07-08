package de.mobile2power.tcpmavlink2uasmqtt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTConnector extends Thread implements MqttCallback {
	private static final byte MAVLINK_START = (byte) 0xFE;
	final String topicDomain = "uascon";
	int qos = 2;
	String broker = "tcp://192.168.178.21:1883";
	String clientId = "apmproxy";
	MemoryPersistence persistence = new MemoryPersistence();
	MqttClient sampleClient = null;
	private boolean running;
	private ServerSocket server = null;
	private int port;
	private Socket client = null;
	MqttConnectOptions connOpts;

	MQTTConnector(int port) {
		this.port = port;
	}

	public void setup(String mqttHostname) {

		broker = "tcp://" + mqttHostname + ":1883";
		try {
			sampleClient = new MqttClient(broker, clientId, persistence);
			connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			connOpts.setWill("pahodemo/clienterrors", "crashed".getBytes(), 2,
					true);
			connOpts.setKeepAliveInterval(30);
			sampleClient.connect(connOpts);
			sampleClient.setCallback(this);
			sampleClient.setTimeToWait(1000); // 1 second
			subscribeTopic();
			// sampleClient.disconnect();

		} catch (MqttException me) {
			me.printStackTrace();
		}
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void messageArrived(String topic, MqttMessage mqttMessage)
			throws Exception {
		if (topic.contains(clientId)) {
			return;
		}
		OutputStream out;
		try {
			out = client.getOutputStream();
			if (out != null) {
				out.write(mqttMessage.getPayload(), 0,
						mqttMessage.getPayload().length);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		running = true;
		try {
			server = new ServerSocket(port);
			client = server.accept();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Thread should run as long as parent class
		while (running) {

			try {
				handleSocketConnection(client);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
		}
		if (client != null)
			try {
				client.close();
			} catch (IOException e) {
			}
	}

	private void handleSocketConnection(Socket client) throws IOException {
		InputStream inputstream = client.getInputStream();
		byte[] txBuffer;
		try {
			txBuffer = copyStream(inputstream);
			MqttMessage message = new MqttMessage(txBuffer);
			message.setQos(qos);
			sampleClient.publish(topicDomain + "/" + clientId + "/mavlink/gc",
					message);
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	public void subscribeTopic() {
		try {
			int[] subQoS = { 0, 0 };
			String[] topics = { topicDomain + "/+/event",
					topicDomain + "/+/mavlink/uav" };
			sampleClient.subscribe(topics, subQoS);
		} catch (MqttSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public String getBroker() {
		return broker;
	}

	public void setBroker(String broker) {
		this.broker = broker;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
}
