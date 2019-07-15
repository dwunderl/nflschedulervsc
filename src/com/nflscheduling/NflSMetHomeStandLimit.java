package com.nflscheduling;

public class NflSMetHomeStandLimit extends NflScheduleMetric {

	public NflSMetHomeStandLimit() {
	}

	public NflSMetHomeStandLimit(String theName) {
		super(theName);
	}
	
	@Override
	public boolean computeMetric(NflSchedule schedule) {
       computeMetric(schedule,"HomeStandLimit");
       
	   return true;
	}
}
