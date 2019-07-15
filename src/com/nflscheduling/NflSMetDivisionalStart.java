package com.nflscheduling;

public class NflSMetDivisionalStart extends NflScheduleMetric {

	public NflSMetDivisionalStart() {
	}

	public NflSMetDivisionalStart(String theName) {
		super(theName);
	}
	
	@Override
	public boolean computeMetric(NflSchedule schedule) {
      score = 0;
      hardViolation = false;
      
      for (int ti=1; ti <= NflDefs.numberOfTeams; ti++) {
         NflTeamSchedule teamSchedule = schedule.teamSchedules.get(ti-1);
         // Find the first divisional game for this team
         for (int wi=1; wi <= NflDefs.numberOfWeeks; wi++) {
             NflGameSchedule teamGame = teamSchedule.scheduledGames[wi-1];
             
             if (teamGame == null || teamGame.isBye) {
                continue;
             }
             
             if (teamGame.game.findAttribute("division")) {
            	 if (wi > 7) {
            		hardViolation = true;
                     // first divisional is too late in the season - alert
  	    		    NflScheduleAlert alert = new NflScheduleAlert();
  	    		    //alert.alertDescr = metricName;
  	    		    //alert.weekNum = wi;
  	    		    //alert.homeTeam = teamGame.game.homeTeam;
  	    		    //alert.awayTeam = teamGame.game.awayTeam;
  	    		    //schedule.alerts.add(alert);
  	 			    alert.alertDescr = "Divisional games late start in week: " + wi + " for team: " + teamSchedule.team.teamName;
  				    schedule.addAlert(alert);
            	 }
            	 break; // move on to next team
             }
         }
      }

      return true;
	}

}
