package de.mobile2power.aircamqcviewer;


public class CamPosAndView {

	private double lat;
	private double lon;
	private double alt;
	
	private double yaw;
	private double pit;
	private double rol;
	
	private double angh;
	private double angv;
	
	
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLon() {
		return lon;
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	public double getAlt() {
		return alt;
	}
	public void setAlt(double alt) {
		this.alt = alt;
	}
	public double getYaw() {
		return yaw;
	}
	public void setYaw(double yaw) {
		this.yaw = yaw;
	}
	public double getPit() {
		return pit;
	}
	public void setPit(double pit) {
		this.pit = pit;
	}
	public double getRol() {
		return rol;
	}
	public void setRol(double rol) {
		this.rol = rol;
	}
	public double getAngh() {
		return angh;
	}
	public void setAngh(double angh) {
		this.angh = angh;
	}
	public double getAngv() {
		return angv;
	}
	public void setAngv(double angv) {
		this.angv = angv;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(alt);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(angh);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(angv);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(pit);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(rol);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(yaw);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		CamPosAndView other = (CamPosAndView) obj;
		if (Double.doubleToLongBits(alt) != Double.doubleToLongBits(other.alt))
			return false;
		if (Double.doubleToLongBits(angh) != Double
				.doubleToLongBits(other.angh))
			return false;
		if (Double.doubleToLongBits(angv) != Double
				.doubleToLongBits(other.angv))
			return false;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
			return false;
		if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon))
			return false;
		if (Double.doubleToLongBits(pit) != Double.doubleToLongBits(other.pit))
			return false;
		if (Double.doubleToLongBits(rol) != Double.doubleToLongBits(other.rol))
			return false;
		if (Double.doubleToLongBits(yaw) != Double.doubleToLongBits(other.yaw))
			return false;
		return true;
	}
	
	
}