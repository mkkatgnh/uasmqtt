package de.mobile2power.aircamqcviewer;

import java.io.Serializable;
import java.util.Arrays;

public class Preview implements Serializable {
	private static final long serialVersionUID = 8807674979651105792L;

	private int width = 0;
	private int height = 0;
	
	private byte[] jpegImage = {};
	
	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public byte[] getJpegImage() {
		return jpegImage;
	}

	public void setJpegImage(byte[] jpegImage) {
		this.jpegImage = jpegImage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + height;
		result = prime * result + Arrays.hashCode(jpegImage);
		result = prime * result + width;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Preview other = (Preview) obj;
		if (height != other.height)
			return false;
		if (!Arrays.equals(jpegImage, other.jpegImage))
			return false;
		if (width != other.width)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Preview [width=" + width + ", height=" + height
				+ ", jpegImage=" + Arrays.toString(jpegImage) + "]";
	}
}
