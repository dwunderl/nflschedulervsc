package com.nflscheduling;

//import java.util.ArrayList;

public class NflSMetDivisionalSeparation extends NflScheduleMetric {

	public NflSMetDivisionalSeparation() {
	}

	public NflSMetDivisionalSeparation(String theName) {
		super(theName);
	}
	
	@Override
	public boolean computeMetric(NflSchedule schedule) {
      score = 0;
      hardViolation = false;
      
      for (int ti=1; ti <= NflDefs.numberOfTeams; ti++) {
         NflTeamSchedule teamSchedule = schedule.teamSchedules.get(ti-1);
         for (int wi1=1; wi1 <= 4; wi1++) {
            NflGameSchedule teamGame1 = teamSchedule.scheduledGames[wi1-1];
            
            if (teamGame1 == null || teamGame1.isBye || !teamGame1.game.findAttribute("division")) {
	           continue;
	        }

	        for (int wi2=wi1+1; wi2 <= 5; wi2++) {
		       NflGameSchedule teamGame2 = teamSchedule.scheduledGames[wi2-1];
		       
	           if (teamGame2 == null || teamGame2.isBye || !teamGame2.game.findAttribute("division")) {
			      continue;
			   }
	           
	           if (teamGame1.game.awayTeam.equalsIgnoreCase(teamGame2.game.homeTeam) &&
	               teamGame1.game.homeTeam.equalsIgnoreCase(teamGame2.game.awayTeam)) {
	               score+= 2.0;
	    		   NflScheduleAlert alert = new NflScheduleAlert();
	    		   alert.alertDescr = metricName;
	    		   alert.weekNum = wi2;
	    		   alert.homeTeam = teamGame2.game.homeTeam;
	    		   alert.awayTeam = teamGame2.game.awayTeam;
	    		   schedule.alerts.add(alert);
	    		   //hardViolation = true;
	           }
	        }
         }
      }

      return true;
	}
}
