package com.bah.mapping;

public class Path {

	
	private String point;
	private String name;
	private String lat;
	private String lng;
	
	public String getPoint(){
		return point;
	}
	
	public void setPoint(String point){
		this.point = point;
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public double getLat(){
		return Double.parseDouble(lat);
	}
	
	public void setLat(String lat){
		this.lat = lat;
	}
	
	public double getLng(){
		return Double.parseDouble(lng);
	}
	
	public void setLng(String lng){
		this.lng = lng;
	}
	
	public String toString(){
		return point + " " + name + " @ " + lat + "," + lng;
	}
	
}
