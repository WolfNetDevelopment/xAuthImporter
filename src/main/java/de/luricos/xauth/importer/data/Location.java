package de.luricos.xauth.importer.data;

public class Location {
	private String uid;
	private double x, y, z;
	private float yaw, pitch;
	private int global;

	public Location (String uid, double x, double y, double z, float yaw, float pitch, int global) {
		this.uid = uid;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.global = global;
	}

	public String getUID() { return uid; }
	public double getX() { return x; }
	public double getY() { return y; }
	public double getZ() { return z; }
	public float getYaw() { return yaw; }
	public float getPitch() { return pitch; }
	public int getGlobal() { return global; }
}