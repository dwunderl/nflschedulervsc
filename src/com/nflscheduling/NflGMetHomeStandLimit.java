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
       //NflTeamSchedule awayTeamSched = gameSchedule.awayTeamSchedule;
       
       if (gameSchedule.schedule.enableAlerts) {
  	      //System.out.println("HSL team: " + homeTeamSched.team.teamName + ", week: " + weekNum);
        }
  
	   // look left to earlier weeks
	   firstHomeWeek = weekNum;
	   for (int wi=weekNum-1; wi >= 1; wi--) {
	      if (homeTeamSched.scheduledGames[wi-1] == null) {
	         break;
	      }
	          
	      if (homeTeamSched.scheduledGames[wi-1].isBye) {
	         continue;
	      }
	          
          NflTeamSchedule prevWeekAwayTeamSched = homeTeamSched.scheduledGames[wi-1].awayTeamSchedule;
          NflGame prevWeekGame = homeTeamSched.scheduledGames[wi-1].game;

          if (gameSchedule.schedule.enableAlerts) {
      	     //System.out.println("   prevWeekAway team: " + prevWeekAwayTeamSched.team.teamName + ", week: " + wi + ", isInt: " + prevWeekGame.isInternational);
          }

		  if (prevWeekAwayTeamSched == homeTeamSched || prevWeekGame.isInternational) {
	         // last weeks game was an away game or an international (virtual away) game
	         break;
	      }
	          
          // I was also the home team last week

	      homeStandLength++;
	      firstHomeWeek = wi;
          if (gameSchedule.schedule.enableAlerts) {
       	     //System.out.println("   homeStandLength: " + homeStandLength + ", firstHomeWeek: " + firstHomeWeek);
           }
	   }

	   // look right to later scheduled weeks
	   lastHomeWeek = weekNum;
	   for (int wi=weekNum+1; wi <= NflDefs.numberOfWeeks; wi++) {
	      if (homeTeamSched.scheduledGames[wi-1] == null) {
	         break;
	      }
	           
		  if (homeTeamSched.scheduledGames[wi-1].isBye) {
		     continue;
	      }
		       
          NflTeamSchedule nextWeekAwayTeamSched = homeTeamSched.scheduledGames[wi-1].awayTeamSchedule;
          NflGame nextWeekGame = homeTeamSched.scheduledGames[wi-1].game;
          
          if (gameSchedule.schedule.enableAlerts) {
       	     //System.out.println("   nextWeekAway team: " + nextWeekAwayTeamSched.team.teamName + ", week: " + wi + ", isInt: " + nextWeekGame.isInternational);
           }

	      if (nextWeekAwayTeamSched == homeTeamSched || nextWeekGame.isInternational) {
		     // next weeks game is an away game or an international (virtual away) game
	         break;
	      }
		  
          // I was also the home team last week

	      homeStandLength++;
	      lastHomeWeek = wi;
          if (gameSchedule.schedule.enableAlerts) {
        	     //System.out.println("   homeStandLength: " + homeStandLength + ", lastHomeWeek: " + lastHomeWeek);
            }

	   }
	   
	   boolean alertViolation = false;
	   
	   //if (homeStandLength == 2) {
		  // score = 1.0;
		  //  score = 2.0;
	   // }
	   if (homeStandLength == 3) {
	      //System.out.println("homeStandLength == 3; lastHomeWeek: " + lastHomeWeek + ", firstHomeWeek: " + firstHomeWeek);
          score = 3.0;
	      if (firstHomeWeek == 1) {
	          //System.out.println("... found firstHomeWeek == 1");
		      // score = 1.0;
		      score = 4.0;
		      alertViolation = true;
		      hardViolation = true;
	      }
	      else if (lastHomeWeek == NflDefs.numberOfWeeks) {
	         // System.out.println("... found lastHomeWeek == " + NflDefs.numberOfWeeks);
		     // score = 1.0;
		     score = 4.0;
		     alertViolation = true;
		     hardViolation = true;
	      }
	   }
	   else if (homeStandLength > 4) {
		  // score = 1.0;
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

       //System.out.println("Info: homeStandLength metric for game, weekNum: " + weekNum + " home team: " + homeTeamSched.team.teamName + " away team: " + awayTeamSched.team.teamName
	   //		               + ", homeStandLength: " + homeStandLength + ", score: " + score);
		   
      return true;
   }
}
