package com.nflscheduling;

//import java.util.ArrayList;

public class NflSMetRoadTripLimit extends NflScheduleMetric {

	public NflSMetRoadTripLimit() {
	}

	public NflSMetRoadTripLimit(String theName) {
		super(theName);
	}
	
	@Override
	public boolean computeMetric(NflSchedule schedule) {
       computeMetric(schedule,"RoadTripLimit");

	   return true;
	}
}
