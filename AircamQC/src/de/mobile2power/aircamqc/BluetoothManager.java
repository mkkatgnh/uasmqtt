package de.mobile2power.aircamqc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class BluetoothManager {

	public static final String BLUETOOTH_MODULE_UUID = "00001101-0000-1000-8000-00805F9B34FB";

	private BluetoothSocket btSocket = null;

	public BluetoothManager() {
	}
	
	public String[] getBoundedDevices() {
		String[] resultDeviceArray = null;
		BluetoothAdapter mBluetoothAdapter = null;

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter != null) {
			// Get a set of currently paired devices
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
					.getBondedDevices();

			// If there are paired devices, add each one to the ArrayAdapter
			if (pairedDevices.size() > 0)
				resultDeviceArray = new String[pairedDevices.size()];
			int i = 0;
			for (BluetoothDevice device : pairedDevices) {
				resultDeviceArray[i] = device.getName() + ","
						+ device.getAddress();
				i++;
			}
		}
		return resultDeviceArray;
	}
	
	public boolean connectToDevice(String deviceNameAndAddress) {
		BluetoothDevice mbtDevice = null;
		BluetoothAdapter mBluetoothAdapter = null;

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0)
			for (BluetoothDevice device : pairedDevices) {
				if (device.getAddress().equals(deviceNameAndAddress.split(",")[1])) {
					mbtDevice = device;
				}
			}
		if (mbtDevice == null) {
			return false;
		}
		try {
			btSocket = mbtDevice.createRfcommSocketToServiceRecord(UUID
					.fromString(BLUETOOTH_MODULE_UUID));
			btSocket.connect();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public OutputStream getOutputStream() {
		try {
			return btSocket.getOutputStream();
		} catch (IOException e) {
			return null;
		}
	}
	
	public InputStream getInputStream() {
		try {
			return btSocket.getInputStream();
		} catch (IOException e) {
			return null;
		}
	}
}