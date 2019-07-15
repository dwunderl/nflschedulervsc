package com.nflscheduling;

//import java.util.ArrayList;

public class NflSMetDivisionalWeekLimits extends NflScheduleMetric {

	public NflSMetDivisionalWeekLimits() {
	}

	public NflSMetDivisionalWeekLimits(String theName) {
		super(theName);
	}
	
	@Override
	public boolean computeMetric(NflSchedule schedule) {
	   score = 0;
	   hardViolation = false;
	   
	   for (int wi=1; wi <= NflDefs.numberOfWeeks-1; wi++) {
		  int divisionalGameCount = schedule.divisionalGameCount(wi);
	      
	      String limitViolated = "";
	      
	      if (divisionalGameCount > 11) {
	         score = 2.0;
	         limitViolated = " > 11";
	         hardViolation = true;
	      }
	      else if (divisionalGameCount > 8) {
	         score = 1.0;
	         limitViolated = " > 8";
	      }
	      else if (divisionalGameCount < 2) {
	    	  score = 1.0;
		      limitViolated = " < 2";
		      hardViolation = true;
	      }
	      
	      if (!limitViolated.isEmpty()) {
	         NflScheduleAlert alert = new NflScheduleAlert();
	         //alert.alertDescr = metricName;
	         //alert.weekNum = wi;
	         //alert.homeTeam = limitViolated;
	         //alert.awayTeam = Integer.toString(divisionalGameCount);
	         //schedule.alerts.add(alert);
			 alert.alertDescr = "Divisional Week Limit: " + limitViolated + " in Week: " + wi + " divisional game count: " + divisionalGameCount;
			 schedule.addAlert(alert);
	      }
	   }

	   return true;
	}

}
