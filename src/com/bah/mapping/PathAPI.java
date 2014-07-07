package com.bah.mapping;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
public interface PathAPI {

	@GET("/path" + PathActivity.UNIQUE_TEAM_ID)
	public void getPaths(Callback<List<Path>> response);
	
}
