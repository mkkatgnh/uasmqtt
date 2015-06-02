package de.mobile2power.tcpmavlink2uasmqtt;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SerialConnector extends Thread implements SerialPortEventListener {
	private int port;
	private ServerSocket server = null;
	private Socket client = null;
	private boolean running = false;
	OutputStream serialOutputstream = null;
	InputStream serialInputstream = null;

	public SerialConnector(final int port) throws IOException {
		this.port = port;
	}

	private void handleSocketConnection(Socket client) throws IOException {
		InputStream inputstream = client.getInputStream();
		copyStream(inputstream, serialOutputstream);
	}

	private void copyStream(InputStream inputstream, OutputStream outputstream)
			throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		while ((len = inputstream.read(buffer)) != -1) {
			outputstream.write(buffer, 0, len);
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

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public void addSerialPort(SerialPort serialPort) throws IOException {
		serialOutputstream = serialPort.getOutputStream();
		serialInputstream = serialPort.getInputStream();
	}

	@Override
	public void serialEvent(SerialPortEvent arg0) {
		OutputStream out;
		try {
			out = client.getOutputStream();
			copyStream(serialInputstream, out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
