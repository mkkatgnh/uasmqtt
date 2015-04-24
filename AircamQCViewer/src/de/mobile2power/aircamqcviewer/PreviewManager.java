package de.mobile2power.aircamqcviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

public class PreviewManager {
	
	private int upperLeftX, upperLeftY, middleX;
	private int width = 178 * 2;
	private int height = 144 * 2;
	
	public void set(int middleX, int topY) {
		this.middleX = middleX;
		this.upperLeftY = topY;
		width = middleX * 3 / 2; //(middleX * 4) / 5;
		height = (width * 3) / 4;
	}
	
	public boolean isWithin(float x, float y, float border) {
		boolean within = x > (upperLeftX - border) &&
				x < (upperLeftX + width + border) &&
				y > (upperLeftY - border) &&
				y < (upperLeftY + height + border);
		return within;
	}

	public void draw(Canvas canvas, Preview preview) {
		byte[] jpegData = preview.getJpegImage();
//		width = preview.getWidth() * 2;
//		height = preview.getHeight() * 2;
		upperLeftX = middleX - (width / 2);
		if (jpegData != null && preview.getWidth() > 0 && preview.getHeight() > 0) {
			Bitmap previewFrame = BitmapFactory.decodeByteArray(
					jpegData, 0, jpegData.length);
			Bitmap scaledBitmap = Bitmap.createScaledBitmap(
					previewFrame, width,
					height, false);
			canvas.drawBitmap(scaledBitmap,
					upperLeftX, upperLeftY, null);
		}
	}
}
