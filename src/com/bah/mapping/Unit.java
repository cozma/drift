package com.bah.mapping;

public class Unit {

	private String ID;
	private String bearing;
	private String lat;
	private String lng;
	private String type;
	private String teamName;
	private String missionID;
	
	public Unit(String ID, String bearing, String lat, String lng, String type, String teamName, String missionID){
		this.ID = ID;
		this.bearing = bearing;
		this.lat = lat;
		this.lng = lng;
		this.type = type;
		this.teamName = teamName;
		this.missionID = missionID;
	}
	
	public String getID() {
		return ID;
	}
	public void setID(String ID){
		this.ID = ID;
	}
	public double getBearing() {
		return Double.parseDouble(bearing);
	}
	public void setBearing(String bearing){
		this.bearing = bearing;
	}
	public void setLat(String lat){
		this.lat = lat;
	}
	public double getLat(){
		return Double.parseDouble(lat);
	}
	public void setLng(String lng){
		this.lng = lng;
	}
	public double getLng(){
		return Double.parseDouble(lng);
	}
	public void setType(String type){
		this.type = type;
	}
	public String getType(){
		return type;
	}
	
	public void setTeamName(String teamName){
		this.teamName = teamName;
	}
	
	public String getTeamName(){
		return teamName;
	}
	
	public void setMissionID(String missionID){
		this.missionID = missionID;
	}
	
	public String getMissionID(){
		return missionID;
	}
	
	public String toString(){
		return ID + " " + type + " " + bearing + " @ " + lat + " , " + lng;
	}
}
