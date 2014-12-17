package de.mobile2power.aircamqc;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import de.mobile2power.aircamqc.gps.GPSCallback;
import de.mobile2power.aircamqc.gps.GPSManager;

public class AttitudeAndPositionManager implements GPSCallback, SensorEventListener {

	private GPSManager gpsManager;
	private AttitudeAndPosition attitudePosition = new AttitudeAndPosition();
	private Location location;
    private float pressure_value = 0.0f;
    private float height = 0.0f;
    private float groundHeight = 0.0f;

	private final float[] mRotationMatrix = new float[16];

	public void setup(final LocationManager locationManager) {
		gpsManager = new GPSManager();
		gpsManager.startListening(locationManager);
		gpsManager.setGPSCallback(this);

	}
	
	@Override
	public void onGPSUpdate(Location location) {
		attitudePosition.setLat(location.getLatitude());
		attitudePosition.setLon(location.getLongitude());
//		attitudePosition.setAlt(location.getAltitude());
		this.location = location;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// we received a sensor event. it is a good practice to check
		// that we received the proper event
		if (Sensor.TYPE_PRESSURE == event.sensor.getType()) {
			pressure_value = event.values[0];
			height = SensorManager.getAltitude(
					SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure_value);
			attitudePosition.setAlt(height-groundHeight);
		}
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			// convert the rotation-vector to a 4x4 matrix. the matrix
			// is interpreted by Open GL as the inverse of the
			// rotation-vector, which is what we want.
			// SensorManager.getRotationMatrixFromVector(
			// mRotationMatrix , event.values);
			// / R[ 0] R[ 1] R[ 2] 0 \
			// | R[ 4] R[ 5] R[ 6] 0 |
			// | R[ 8] R[ 9] R[10] 0 |
			// \ 0 0 0 1 /
	        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
	        float[] remapCoords = new float[16];
	        // Landscape
	        //SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X, remapCoords);
	        
	        // Portait
	        SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Y, remapCoords);
	        float[] orientationVals = new float[3];
			SensorManager.getOrientation(remapCoords, orientationVals );
			
			attitudePosition.setYaw(Math.toDegrees(orientationVals[0] + Math.PI));
			attitudePosition.setPit(Math.toDegrees(orientationVals[1]));
			attitudePosition.setRol(Math.toDegrees(orientationVals[2]));
		}
	}

	public AttitudeAndPosition getAttitudePosition() {
		return attitudePosition;
	}

	public void altitudeSetZero() {
		groundHeight = height;
		height = 0.0f;
	}

	public Location getLocation() {
		return this.location;
	}
}
