package de.mobile2power.aircamqcviewer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initGUIElements();
	}

	private void initGUIElements() {
		final Intent serverIntentConfigurationActivity = new Intent(this, ControlPadActivity.class);
		final int OPEN_SETTINGS_ACTIVITY = 2;

        final Button buttonStart = (Button) findViewById(R.id.connectMQTTBrokerButton);
		buttonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				EditText brokerUrl = (EditText) findViewById(R.id.mqttBrokerUrl);
				serverIntentConfigurationActivity.putExtra("brokerUrl", brokerUrl.getText().toString());
    			startActivityForResult(serverIntentConfigurationActivity,
    					OPEN_SETTINGS_ACTIVITY);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
}
