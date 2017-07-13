package de.mobile2power.aircamqc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class CamPreview extends SurfaceView implements SurfaceHolder.Callback {

	private static final int FACTOR_TO_GET_ENOUGH_EVEN_FOR_P880 = 9;

	private static final int PREVIEW_WIDTH = 640;

	private static final int WAIT_MILLIS_TILL_NEXTPIC = 100;
	SurfaceHolder mHolder; // <2>
	public Camera camera = null; // <3>
	private boolean takePictureCallbackInactive = true;
	private long takePictureCallbackInactiveTimestamp = 0l;
	private String pictureFolder = null;
	private Activity parentActivity;
	// private CamPosAndView attitudePosition;
	private String attitudePositionJson = "";

	private int compressionValue = 8;
	private int previewWidth = 0;
	private int previewHeight = 0;
	private int[] previewFPS = { 0, 0 };
	private Rect rect;
	private Preview preview;

	CamPreview(Activity context) {
		super(context);

		parentActivity = context;
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder(); // <4>
		mHolder.addCallback(this); // <5>
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // <6>
	}

	// Called once the holder is ready
	public void surfaceCreated(SurfaceHolder holder) { // <7>
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		if (camera == null) {
			camera = Camera.open(); // <8>
			chooseCamPreviewSize();
			chooseCamSize();
			chooseCamPreviewFPS();
			installCamPreviewBuffer();
			try {
				camera.setPreviewDisplay(holder); // <9>

				preparePreviewCallbackOnCam();
			} catch (IOException e) { // <13>
				camera.release();
				camera = null;
			}
		}
	}

	public void preparePreviewCallbackOnCam() {

		camera.setPreviewCallbackWithBuffer(new PreviewCallback() { // <10>
			// Called for each frame previewed
			public void onPreviewFrame(byte[] data, Camera camera) { // <11>
				Camera.Parameters parameters = camera.getParameters();

				int width = parameters.getPreviewSize().width;
				int height = parameters.getPreviewSize().height;

				ByteArrayOutputStream outstr = new ByteArrayOutputStream();
				YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width,
						height, null);
				yuvimage.compressToJpeg(rect, compressionValue, outstr);

				preview.setJpegImage(outstr.toByteArray());
				camera.addCallbackBuffer(data);
				// CamPreview.this.invalidate(); // <12>
			}
		});
		// }
	}

	private void chooseCamPreviewSize() {
		if (camera != null && camera.getParameters() != null) {
			List<Size> sizes = camera.getParameters()
					.getSupportedPreviewSizes();
			Iterator<Size> sizesIter = sizes.iterator();
			Size size = null;
			do {
				size = sizesIter.next();
			} while (sizesIter.hasNext() && size.width < PREVIEW_WIDTH);
			previewWidth = size.width;
			previewHeight = size.height;
		}
		camera.getParameters().setPreviewSize(previewWidth, previewHeight);
		rect = new Rect(0, 0, previewWidth, previewHeight);
	}

	
	private void chooseCamPreviewFPS() {
		Camera.Parameters parameters = camera.getParameters();
		List<int[]> fpss = new ArrayList<int[]>();
		fpss.addAll(parameters.getSupportedPreviewFpsRange());
		if (fpss != null) {
			previewFPS = fpss.get(fpss.size() - 1);
		}
	}

	private void installCamPreviewBuffer() {
		final int BITS_PER_BYTE = 8;
		final int bitsPerPixel = ImageFormat.getBitsPerPixel(camera
				.getParameters().getPreviewFormat());
		// XXX: According to the documentation the buffer size can be
		// calculated by width * height * bytesPerPixel. However, this
		// returned an error saying it was too small. It always needed
		// to be exactly 1.5 times larger. But the P880 need more than 1.5 times. 9 times seems ok.
		int mPreviewBufferSize = previewWidth * previewHeight * bitsPerPixel / BITS_PER_BYTE
				* FACTOR_TO_GET_ENOUGH_EVEN_FOR_P880 + 1;
		camera.addCallbackBuffer(new byte[mPreviewBufferSize]);
	}

	private void chooseCamSize() {
		if (camera != null && camera.getParameters() != null) {
			Parameters parameters = camera.getParameters();
			List<Size> sizes = parameters.getSupportedPictureSizes();
			int maxWidth = 0;
			int maxHeight = 0;
			for (Size size : sizes) {
				if (size.width > maxWidth) {
					maxWidth = size.width;
					maxHeight = size.height;
				}
			}
			parameters.setPictureSize(maxWidth, maxHeight);
			camera.setParameters(parameters);
		}
	}

	// Called when the holder is destroyed
	public void surfaceDestroyed(SurfaceHolder holder) { // <14>
		if (camera != null) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
	}

	public void startPreview() {
		camera.startPreview();
	}

	// Called when holder has changed
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) { // <15>
		if (camera != null) {
			camera.stopPreview();
			setCameraDisplayOrientation(parentActivity, camera);
		}
		startPreview();
	}

	private void setCameraDisplayOrientation(Activity activity,
			android.hardware.Camera camera) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(0, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
	}

	public void takePicture(Location location) {
		takePictureCallbackInactive = false;
		if (location != null) {
			Parameters parameters = camera.getParameters();
			parameters.setGpsLatitude(location.getLatitude());
			parameters.setGpsLongitude(location.getLongitude());
			parameters.setGpsAltitude(location.getAltitude());
			parameters.setGpsTimestamp(location.getTime());
			parameters.setGpsProcessingMethod("Android Location");
			camera.setParameters(parameters);
		}
		camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		Log.d("AircamQC", "takePicture");
	}

	// Called when shutter is opened
	ShutterCallback shutterCallback = new ShutterCallback() { // <6>
		public void onShutter() {
		}
	};

	// Handles data for raw picture
	PictureCallback rawCallback = new PictureCallback() { // <7>
		public void onPictureTaken(byte[] data, Camera camera) {
		}
	};

	// Handles data for jpeg picture
	PictureCallback jpegCallback = new PictureCallback() { // <8>
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d("AircamQC", "onPictureTaken");
			FileOutputStream outStream = null;
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
					.format(new Date());

			try {
				// Write to SD Card
				String pictureNameAbsolute = pictureFolder + File.separator
						+ timeStamp + ".jpg";
				outStream = new FileOutputStream(pictureNameAbsolute); // <9>
				Log.d("AircamQC", pictureNameAbsolute);
				outStream.write(data);
				outStream.close();

				PrintWriter out = new PrintWriter(pictureFolder
						+ File.separator + timeStamp + ".json");
				out.println(attitudePositionJson);
				out.close();
			} catch (FileNotFoundException e) { // <10>
				Log.e("AircamQC", e.getMessage());
			} catch (IOException e) {
				Log.e("AircamQC", e.getMessage());
			} finally {
				setTakePictureCallbackInactive(true);
			}
		}

	};

	private void setTakePictureCallbackInactive(boolean inactive) {
		takePictureCallbackInactive = inactive;
		takePictureCallbackInactiveTimestamp = System.currentTimeMillis();
	}

	public boolean isTakePictureCallbackInactive() {
		return isTakePictureDelayOk() && takePictureCallbackInactive;
	}

	private boolean isTakePictureDelayOk() {
		return (System.currentTimeMillis() - takePictureCallbackInactiveTimestamp) > WAIT_MILLIS_TILL_NEXTPIC;
	}

	public void setPictureFolder(String pictureFolder) {
		this.pictureFolder = pictureFolder;
	}

	double getVerticalViewAngle() {
		return camera != null ? camera.getParameters().getVerticalViewAngle()
				: 0.0d;
	}

	double getHorizontalViewAngle() {
		return camera != null ? camera.getParameters().getHorizontalViewAngle()
				: 0.0d;
	}

	public void setAttitudePositionJson(String json) {
		attitudePositionJson = json;
	}

	public void setPreviewDTO(Preview previewDTO) {
		this.preview = previewDTO;
	}

}
