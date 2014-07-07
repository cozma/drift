package com.bah.mapping;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
public interface HazardAPI {

	@GET("/hazards")
	public void getHazards(Callback<List<Hazard>> response);
	
	@POST("/hazards")
			public void sendHazard(@Body Hazard hazard, Callback<Hazard> cb);

	
}
