package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetHomeStandLimit extends NflGameMetric {

	NflGMetHomeStandLimit(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}

	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
	   // Determine the penalty incurred if my gameSchedule causes too long of a home stand for the home team
	   // no penalty for a 1 or 2 game home
	   // a modest penalty for a 3 game home stand
	   // a severe penalty for a home stand of 4 games or longer
	   // look left and right from weekNum
			   
       score = 0.0;
       hardViolation = false;
       
       if (gameSchedule.game.isInternational) {
    	   // this is considered a virtual away game for the home team
    	   // so this can't participate in a home stand
    	   
    	   return true;
       }
       
       int homeStandLength = 1;
       int firstHomeWeek;
       int lastHomeWeek;
		   
       NflTeamSchedule homeTeamSched = gameSchedule.homeTeamSchedule;
  
	   // look left to earlier weeks
	   firstHomeWeek = weekNum;
	   for (int wi=weekNum-1; wi >= 1; wi--) {
	      if (homeTeamSched.scheduledGames[wi-1] == null) {
		     // No game scheduled (yet) for the previous week
	         break;
	      }
	          
	      if (homeTeamSched.scheduledGames[wi-1].isBye) {
	         continue;  // skip over a bye week
		  }
		  
		  NflGame prevWeekGame = homeTeamSched.scheduledGames[wi-1].game;
          NflTeamSchedule prevWeekAwayTeamSched = homeTeamSched.scheduledGames[wi-1].awayTeamSchedule;

		  if (prevWeekAwayTeamSched == homeTeamSched || prevWeekGame.isInternational) {
	         // last weeks game was an away game or an international (virtual away) game
	         break;
	      }
	          
          // I was also the home team last week

	      homeStandLength++;
	      firstHomeWeek = wi;
	   }

	   // look right to later scheduled weeks
	   lastHomeWeek = weekNum;
	   for (int wi=weekNum+1; wi <= NflDefs.numberOfWeeks; wi++) {
	      if (homeTeamSched.scheduledGames[wi-1] == null) {
			// No game scheduled (yet) for the next week
	         break;
	      }
	           
		  if (homeTeamSched.scheduledGames[wi-1].isBye) {
		     continue; // skip over a bye week
	      }
		       
          NflTeamSchedule nextWeekAwayTeamSched = homeTeamSched.scheduledGames[wi-1].awayTeamSchedule;
          NflGame nextWeekGame = homeTeamSched.scheduledGames[wi-1].game;
          
	      if (nextWeekAwayTeamSched == homeTeamSched || nextWeekGame.isInternational) {
		     // next weeks game is an away game or an international (virtual away) game
	         break;
	      }
		  
          // I was also the home team last week

	      homeStandLength++;
	      lastHomeWeek = wi;
	   }
	   
	   boolean alertViolation = false;
	   
	   if (homeStandLength == 3) {
          score = 3.0;
	      if (firstHomeWeek == 1) {
		      score = 4.0;
		      alertViolation = true;
		      hardViolation = true;
	      }
	      else if (lastHomeWeek == NflDefs.numberOfWeeks) {
		     score = 4.0;
		     alertViolation = true;
		     hardViolation = true;
	      }
	   }
	   else if (homeStandLength >= 4) {
		  score = 6.0;
	      alertViolation = true;
	      hardViolation = true;
	   }

       if (alertViolation) {
	       if (gameSchedule.schedule.enableAlerts) {
			    NflScheduleAlert alert = new NflScheduleAlert();
			    alert.alertDescr = "Home stand too long: " + homeTeamSched.team.teamName + " from week: " + firstHomeWeek + " to week: " + lastHomeWeek + " Games: " + homeStandLength;
			    gameSchedule.schedule.addAlert(alert);
	       }
       }
		   
      return true;
   }
}
