package com.example.mapping;

public class Coordinate {
	
	private int point;
	private String name;
	private double latitude;
	private double longitude;
	
	public Coordinate(int point, String name, double latitude, double longitude){
		this.point = point;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public int getPoint(){
		return this.point;
	}
	
	public String getName(){
		return this.name;
	}
	
	public double getLat(){
		return this.latitude;
	}
	
	public double getLong(){
		return this.longitude;
	}
	
	public String toString(){
		return this.point + ":" + this.name + " - (" + this.latitude + "," + this.longitude + ")";
	}
}
