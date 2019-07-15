package com.nflscheduling;

//import java.util.*;
//import java.io.File;
//import java.io.IOException;

public class NflResourceSchedule {
	
	public NflResource resource;
	public NflSchedule schedule;
	public int[] usage;
	
	NflResourceSchedule(NflResource theResource, NflSchedule theSchedule) {
       resource = theResource;
       usage = new int[NflDefs.numberOfWeeks];
       schedule = theSchedule;
       //System.out.println("Construct NflResourceSchedule for : " + resource.resourceName + " , resource.weeknum: " + resource.weekNum);
       //System.out.println("Construct NflResourceSchedule for : " + resource.resourceName);
	   //for (int weekNum=NflDefs.numberOfWeeks; weekNum >= 1; weekNum--) {
	       //System.out.println("   weeknum: " + weekNum + " limit : " + resource.weeklyLimit[weekNum-1] + "  minimum : " + resource.weeklyMinimum[weekNum-1]);
	   //}

    }	
	
	public boolean hasCapacity(int weekNum) {
		if (usage[weekNum-1] < resource.weeklyLimit[weekNum-1]) {
			return true;
		}
		   
		return false;
	}
}
