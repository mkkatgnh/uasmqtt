package de.mobile2power.aircamqcviewer;

import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class ControlPadActivity extends Activity  {

	private Operator operator;

	byte[] takePictureEventData;

	int screenHeight;
	int screenWidth;

	private UavPosition uavPosition = new UavPosition();
	
	private TakePictureState pictureTakeState;

	private BluetoothSocket btSocket = null;

	private ControlView mySurfaceView;
	private PreviewManager previewManager = new PreviewManager();

	private Preview preview = null;
	final Activity parentActivity = this;

	private WakeLock mWakeLock;
	private PowerManager mPowerManager;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mobile_rc);

		mySurfaceView = new ControlView(this);
		setContentView(mySurfaceView);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		operator = new Operator();
		Intent myIntent = getIntent(); // gets the previously created intent
		String brokerUrl = myIntent.getStringExtra("brokerUrl"); 
		operator.setup(brokerUrl);
		operator.connectToBroker();

		screenHeight = metrics.heightPixels;
		screenWidth = metrics.widthPixels;

//		int padSize = (int) (screenHeight * 0.25);
//		int distanceFromBorder = padSize + (padSize / 5);

		previewManager.set(screenWidth / 2, 0);

		preview = new Preview();
		operator.setPreview(preview);

		pictureTakeState = new TakePictureState();
		pictureTakeState.reset();
		operator.setTakePictureState(pictureTakeState);
		operator.setUavPosition(uavPosition);
		
		operator.run();

		// Get an instance of the PowerManager
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		// Create a bright wake lock
		mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

	}

	@Override
	protected void onResume() {
		super.onResume();
		mySurfaceView.onResumeMySurfaceView();
		if (this.isFinishing()) {
			freeResources();
		}
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
		mySurfaceView.onPauseMySurfaceView();
		// and release our wake-lock
		mWakeLock.release();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.isFinishing()) {
			freeResources();
			System.runFinalizersOnExit(true);
		}
	}

	private void freeResources() {
		mySurfaceView.onDestroy();
		if (btSocket != null) {
			try {
				btSocket.close();
			} catch (IOException e) {
				Toast.makeText(this, "cannot close bluetooth connection",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	class ControlView extends SurfaceView implements Runnable {

		final int CONCURRENT_TOUCH = 2;

		private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		float[] x = new float[CONCURRENT_TOUCH];
		float[] y = new float[CONCURRENT_TOUCH];
		boolean[] isTouch = new boolean[CONCURRENT_TOUCH];

		float[] x_last = new float[CONCURRENT_TOUCH];
		float[] y_last = new float[CONCURRENT_TOUCH];
		boolean[] isTouch_last = new boolean[CONCURRENT_TOUCH];

		Thread thread = null;
		SurfaceHolder surfaceHolder;
		volatile boolean running = false;

		volatile boolean touched = false;
		volatile float touched_x, touched_y;

		public ControlView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
			surfaceHolder = getHolder();
			paint.setTextAlign(Align.CENTER);
			paint.setTextSize(15);
		}

		public void onResumeMySurfaceView() {
			running = true;
			thread = new Thread(this);
			thread.start();
		}

		public void onDestroy() {
			running = false;
		}

		public void onPauseMySurfaceView() {
			boolean retry = true;
			running = false;
			while (retry) {
				try {
					thread.join();
					retry = false;
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}

		@Override
		public void run() {
			while (running) {
				if (surfaceHolder.getSurface().isValid()) {
					Canvas canvas = surfaceHolder.lockCanvas();
					previewManager.draw(canvas, preview);
					showAltitude(canvas, uavPosition);

					for (int id = 0; id <= 1; id++) {
//						if (isWithinPreviewButNotWithinPads(id)) {
							if (pictureTakeState.isNone()
									&& isTouch[id] == true) {
								pictureTakeState.next();
							}
							if (pictureTakeState.isTrigger()
									&& isTouch[id] == false) {
								pictureTakeState.next();
//								Log.d("pic", "trigger take pic");
							}
//						}
					}
					try {
						Thread.sleep(25); // 1000/25 = 40 Control inputs/minute
											// at max
					} catch (InterruptedException e) {
						// do nothing
					}
					surfaceHolder.unlockCanvasAndPost(canvas);
				}

			}
		}

		public void showAltitude(Canvas canvas, UavPosition uavPosition) {
			paint.setColor(Color.GRAY);
			paint.setStrokeWidth(5);
			paint.setTextSize(80);
			String altitude = String
					.format("%d", (int) uavPosition.getAltitude());

			canvas.drawText(altitude, screenWidth / 2, screenHeight - 70, paint);
		}

		private boolean isWithinPreviewButNotWithinPads(int id) {
			// Log.d("pad", "test within preview");
			return previewManager.isWithin(x[id], y[id], 5);
		}

		@Override
		public boolean onTouchEvent(MotionEvent motionEvent) {
			int pointerIndex = ((motionEvent.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			int pointerId = motionEvent.getPointerId(pointerIndex);
			int action = (motionEvent.getAction() & MotionEvent.ACTION_MASK);
			int pointCnt = motionEvent.getPointerCount();

			if (pointCnt <= CONCURRENT_TOUCH) {
				if (pointerIndex <= CONCURRENT_TOUCH - 1) {

					for (int i = 0; i < pointCnt; i++) {
						int id = motionEvent.getPointerId(i);
						isTouch_last[id] = isTouch[id];
						x[id] = motionEvent.getX(i);
						y[id] = motionEvent.getY(i);
					}

					switch (action) {
					case MotionEvent.ACTION_DOWN:
						isTouch[pointerId] = true;
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						isTouch[pointerId] = true;
						break;
					case MotionEvent.ACTION_MOVE:
						isTouch[pointerId] = true;
						break;
					case MotionEvent.ACTION_UP:
						isTouch[pointerId] = false;
						isTouch_last[pointerId] = false;
						break;
					case MotionEvent.ACTION_POINTER_UP:
						isTouch[pointerId] = false;
						isTouch_last[pointerId] = false;
						break;
					case MotionEvent.ACTION_CANCEL:
						isTouch[pointerId] = false;
						isTouch_last[pointerId] = false;
						break;
					default:
						isTouch[pointerId] = false;
						isTouch_last[pointerId] = false;
					}
				}
			}
			return true;
		}
	}
}
