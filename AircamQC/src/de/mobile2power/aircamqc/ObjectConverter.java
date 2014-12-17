package de.mobile2power.aircamqc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import android.os.Parcel;
import android.os.Parcelable;

public class ObjectConverter {

	public static byte[] serializeObjectToByteArray(Object obj)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);
		oos.flush();
		byte[] buf = baos.toByteArray();
		return buf;
	}

	public static Object deserializeByteArrayToObject(byte[] array)
			throws StreamCorruptedException, IOException,
			ClassNotFoundException {
		Object obj = null;
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
				array));
		obj = in.readObject();
		in.close();
		return obj;
	}
	
    public static byte[] marshall(Parcelable parceable) {
        Parcel parcel = Parcel.obtain();
        parceable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle(); // not sure if needed or a good idea
        return bytes;
    }

    public static Parcel unmarshall(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0); // this is extremely important!
        return parcel;
    }
}
