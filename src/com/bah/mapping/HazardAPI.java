package com.bah.mapping;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
public interface HazardAPI {

	@GET("/hazards" + PathActivity.MISSION_ID) // without mission_id returns all
	public void getHazards(Callback<List<Hazard>> response);
	
	@POST("/hazards")
			public void sendHazard(@Body Hazard hazard, Callback<Hazard> cb);

	
}
