package com.nflscheduling;

//import java.util.ArrayList;

public class NflSMetNoRepeatedMatchup extends NflScheduleMetric {

	public NflSMetNoRepeatedMatchup() {
	}

	public NflSMetNoRepeatedMatchup(String theName) {
		super(theName);
	}
	
	@Override
	public boolean computeMetric(NflSchedule schedule) {
       computeMetric(schedule,"NoRepeatedMatchup");
	   return true;
	}
}
