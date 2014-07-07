package com.bah.mapping;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
public interface UnitAPI {

	@GET("/units")
	public void getUnits(Callback<List<Unit>> response);
	
	@POST("/units")
	void sendPos(@Body Unit unit, Callback<Unit> cb);
	
}
