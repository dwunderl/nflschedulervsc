package com.nflscheduling;
public class NflResourceSchedule {
	
	public NflResource resource;
	public NflSchedule schedule;
	public int[] usage;
	
	NflResourceSchedule(NflResource theResource, NflSchedule theSchedule) {
       resource = theResource;
       usage = new int[NflDefs.numberOfWeeks];
       schedule = theSchedule;
    }	
	
	public boolean hasCapacity(int weekNum) {
		if (usage[weekNum-1] < resource.weeklyLimit[weekNum-1]) {
			return true;
		}
		   
		return false;
	}
}
