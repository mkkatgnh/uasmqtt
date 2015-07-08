package de.mobile2power.tcpmavlink2uasmqtt;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.util.TooManyListenersException;

public class GateServer {
	private static int tcpServerPort = 55555;
	private static String serialDeviceName = "/dev/tty30";

	private static SerialPort serialPort;
	private static SerialConnector mavlinkServer;
	private static MQTTConnector mqttLink;
	private static String mqttHostname;

	public static void main(String args[]) throws Exception {
		if (args.length == 3) {
			serialDeviceName = args[1];
			mqttHostname = args[1];
			tcpServerPort = Integer.parseInt(args[2]);
			GateServer server = new GateServer();
			if ("mqtt".equals(args[0])) {
				mqttLink = new MQTTConnector(tcpServerPort);
				mqttLink.setup(mqttHostname);
				mqttLink.start();
			} else {
				mavlinkServer = new SerialConnector(tcpServerPort);
				server.initSerial();
				mavlinkServer.addSerialPort(serialPort);
				mavlinkServer.start();
			}
		} else {
			System.out
					.println("GateServer [serial|mqtt][<device>|<mqtthost>] <TCP port>");
		}
	}

	private void initSerial() throws NoSuchPortException, PortInUseException,
			UnsupportedCommOperationException, IOException,
			TooManyListenersException {
		serialPort = connectSerial(serialDeviceName);
		serialPort.addEventListener(mavlinkServer);
		serialPort.notifyOnDataAvailable(true);
	}

	private SerialPort connectSerial(String portName)
			throws NoSuchPortException, PortInUseException,
			UnsupportedCommOperationException {
		SerialPort serialPort = null;

		CommPortIdentifier portIdentifier = CommPortIdentifier
				.getPortIdentifier(portName);

		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else {
			CommPort commPort = portIdentifier.open(this.getClass().getName(),
					2000);
			if (commPort instanceof SerialPort) {
				serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(57600, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
//				serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8,
//						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			} else {
				System.out
						.println("Error: Only serial ports are handled by this example.");
			}
		}
		return serialPort;
	}

}
