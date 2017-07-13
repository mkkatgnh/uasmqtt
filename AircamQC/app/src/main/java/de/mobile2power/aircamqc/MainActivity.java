package de.mobile2power.aircamqc;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Intent;
import android.provider.Settings.Secure;

public class MainActivity extends Activity {

	private Operator operator;
	private SensorManager mSensorManager;
	private WakeLock mWakeLock;
	private PowerManager mPowerManager;
	private Sensor mRotationVectorSensor;
	private CamPreview camPreview; // <1>
	
	private Spinner bluetoothDeviceSpinner;
	private BluetoothManager bluetoothManager = new BluetoothManager();
	private OutputStream bluetoothOutStream = null;
	private InputStream bluetoothInStream = null;
	private boolean bluetoothConnectionEstablished = false;
	private String bluetoothNameAndAddress;
	private String clientId = "id";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get an instance of the PowerManager
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		// Create a bright wake lock
		mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		
		operator = new Operator((LocationManager) this.getSystemService(Context.LOCATION_SERVICE));
		operator.setPressureSensorAvailable(airPressureSensorAvailable());

		mRotationVectorSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		
		fillBluetoothSpiner();

		clientId = Secure.getString(getApplicationContext().getContentResolver(),
				Secure.ANDROID_ID);

		initGUIElements();
		initCamPreview();
		
		operator.run();
	}

	private boolean airPressureSensorAvailable() {
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
			List<Sensor> pressureSensors = mSensorManager
					.getSensorList(Sensor.TYPE_PRESSURE);
			for (int i = 0; i < pressureSensors.size(); i++) {
				if ((pressureSensors.get(i).getVendor().contains("Google Inc."))
						&& (pressureSensors.get(i).getVersion() == 3)) {
					return true;
				}
			}
		}
		return false;
	}

	private void initCamPreview() {
		camPreview = new CamPreview(this); // <3>
		((FrameLayout) findViewById(R.id.preview)).addView(camPreview); // <4>
		operator.setCamPreview(camPreview);
	}

	public void shareControlUrl() {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, "http://www.mobile2power.de/aircam/control.php?aid=" + clientId);
		sendIntent.setType("text/plain");
		startActivity(sendIntent);
	}

	private void initGUIElements() {
		final Button buttonStart = (Button) findViewById(R.id.connectMQTTBrokerButton);
		buttonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startPreview();
				operator.setup("tcp://www.mobile2power.de:1883", clientId);
				operator.connectToBroker(clientId);
			}

			private void startPreview() {
				if (camPreview.isTakePictureCallbackInactive()) {
					// Log.d("pic", "start preview");
					camPreview.preparePreviewCallbackOnCam();
					camPreview.startPreview();
				}
			}
		});

		final Button buttonDisconnect = (Button) findViewById(R.id.disconnectMQTTBrokerButton);
		buttonDisconnect.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				operator.disconnect();
			}
		});

		final Button buttonAltToZero = (Button) findViewById(R.id.setAltitudeLevelToZeroButton);
		buttonAltToZero.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				operator.altitudeSetZero();
			}
		});
		
		final CheckBox checkboxTransmitPreview = (CheckBox) findViewById(R.id.setTransmitPreviewCheckBox);
		checkboxTransmitPreview.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				operator.previewTransmit(checkboxTransmitPreview.isChecked());
			}
		});

		final Button buttonConnectBT = (Button) findViewById(R.id.connectBluetoothDeviceButton);
		buttonConnectBT.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getValuesFromInput();
				connectBTDevice();
			}
		});

		final Button shareControlUrl = (Button) findViewById(R.id.shareControlUrlButton);
		shareControlUrl.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				shareControlUrl();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void connectBTDevice() {
		bluetoothConnectionEstablished = bluetoothManager
				.connectToDevice(bluetoothNameAndAddress);
		if (bluetoothConnectionEstablished) {
			bluetoothOutStream = bluetoothManager
					.getOutputStream();
			operator.setOutputStream(bluetoothOutStream);
			bluetoothInStream = bluetoothManager.getInputStream();
			operator.setInputStream(bluetoothInStream);
		} else {
			Toast.makeText(this, "Cannot connect to bluetooth device",
					Toast.LENGTH_LONG).show();
		}
	}

	private void fillBluetoothSpiner() {
		List<String> spinnerArray = new ArrayList<String>();
		String[] boundedDevices = bluetoothManager
				.getBoundedDevices();
		int selectedDevice = -1;
		int i = 0;
		if (boundedDevices != null) {
			for (String device : boundedDevices) {
				spinnerArray.add(device.split(",")[0]);
				i++;
			}
		} else {
			Toast.makeText(this,
					"There are no bounded bluetooth devices available",
					Toast.LENGTH_LONG).show();
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spinnerArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		bluetoothDeviceSpinner = (Spinner) findViewById(R.id.bluetoothSpinner);
		bluetoothDeviceSpinner.setAdapter(adapter);
		bluetoothDeviceSpinner.setSelection(selectedDevice);
	}

	private void getValuesFromInput() {
		String[] boundedDevices = bluetoothManager
				.getBoundedDevices();
		if (bluetoothDeviceSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION) {
			bluetoothNameAndAddress = boundedDevices[bluetoothDeviceSpinner
							.getSelectedItemPosition()];
		}
	}

//	private void transferDataToBluetooth(byte[] data) {
//		if (bluetoothConnectionEstablished && bluetoothOutStream != null) {
//			try {
//				bluetoothOutStream.write(data);
//				bluetoothOutStream.flush();
//			} catch (IOException e) {
//				// What do we do in case of connection lost to QC communication?
//			}
//		}
//	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(operator.getSensorEventListener(), mRotationVectorSensor, 10000);
		mSensorManager.registerListener(operator.getSensorEventListener(), mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
		/*
		 * when the activity is resumed, we acquire a wake-lock so that the
		 * screen stays on, since the user will likely not be fiddling with the
		 * screen or buttons.
		 */
		mWakeLock.acquire();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(operator.getSensorEventListener());
		// and release our wake-lock
		mWakeLock.release();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
}
