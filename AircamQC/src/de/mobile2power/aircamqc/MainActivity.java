package de.mobile2power.aircamqc;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

	private Operator operator;
	private SensorManager mSensorManager;
	private WakeLock mWakeLock;
	private PowerManager mPowerManager;
	private Sensor mRotationVectorSensor;
	private CamPreview camPreview; // <1>
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get an instance of the PowerManager
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		// Create a bright wake lock
		mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		operator = new Operator((LocationManager) this.getSystemService(Context.LOCATION_SERVICE));

		mRotationVectorSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		
		initGUIElements();
//		operator.setCamPreview(camPreview);
		initCamPreview();
		
		operator.run();
	}

	private void initCamPreview() {
		camPreview = new CamPreview(this); // <3>
		((FrameLayout) findViewById(R.id.preview)).addView(camPreview); // <4>
		operator.setCamPreview(camPreview);
	}

	private void initGUIElements() {
		final Button buttonStart = (Button) findViewById(R.id.connectMQTTBrokerButton);
		buttonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				EditText brokerUrl = (EditText) findViewById(R.id.mqttBrokerUrl);
				operator.setup(brokerUrl.getText().toString());
				operator.connectToBroker();
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
