package de.mobile2power.aircamqcviewer;

public class TakePictureState {
	private static final int PIC_EVENT_NONE = 0;
	private static final int PIC_EVENT_TRIGGER = 1;
	private static final int PIC_EVENT_RELEASE = 2;

	private int currentState = PIC_EVENT_NONE;
	
	public void next() {
		if (currentState < 3) {
			currentState += 1;
		} else {
			currentState = 0;
		}
	}
	
	public boolean isNone() {
		return currentState == PIC_EVENT_NONE;
	}

	public boolean isTrigger() {
		return currentState == PIC_EVENT_TRIGGER;
	}

	public boolean isRelease() {
		return currentState == PIC_EVENT_RELEASE;
	}
	
	public void reset() {
		currentState = PIC_EVENT_NONE;
	}
	
	@Override
	public String toString() {
		return "TakePictureState [currentState=" + currentState + "]";
	}
}
