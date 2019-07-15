package com.nflscheduling;

public class NflSMetDivisionalBalance extends NflScheduleMetric {

	public NflSMetDivisionalBalance() {
	}

	public NflSMetDivisionalBalance(String theName) {
		super(theName);
	}
	
	@Override
	public boolean computeMetric(NflSchedule schedule) {
	   score = 0;
	   
	   int first8weeksDivisionalTotal = 0;
	   int second8weeksDivisionalTotal = 0;
	   int divisionalTotal = 0;
	   
	   for (int wi=1; wi <= NflDefs.numberOfWeeks-1; wi++) {
		  int divisionalGameCount = schedule.divisionalGameCount(wi);
		  
		  if (wi <= 8) {
	         first8weeksDivisionalTotal += divisionalGameCount;
          }
		  else if (wi <= 16) {
			  second8weeksDivisionalTotal += divisionalGameCount;
          }
		  
		  divisionalTotal += divisionalGameCount;
	   }
	   
       double deviationPercent = Math.abs(((double) (first8weeksDivisionalTotal - second8weeksDivisionalTotal))/((double) divisionalTotal)*100.0);
       
       if (deviationPercent > 10.0) {
	      NflScheduleAlert alert = new NflScheduleAlert();
	      //alert.alertDescr = metricName;
	      //alert.weekNum = (int) deviationPercent;
	      //alert.homeTeam = Integer.toString(first8weeksDivisionalTotal);
	      //alert.awayTeam = Integer.toString(second8weeksDivisionalTotal);
	      //schedule.alerts.add(alert);
          alert.alertDescr = "Divisional Games out of baance: " + " 1st 8 week total: " + first8weeksDivisionalTotal + " 2nd 8 week total: " + second8weeksDivisionalTotal + " deviation: " + deviationPercent + " %";
          schedule.addAlert(alert);
	   }

	   return true;
	}
}
