package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetRoadTripLimit extends NflGameMetric {

	public NflGMetRoadTripLimit(String theName, NflGameSchedule theGame) {
		super(theName, theGame);
	}
	
	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
       // Determine the penalty incurred if my gameSchedule causes too long of a road trip for the away team
       // no penalty for a 1 or 2 game road trip
       // a modest penalty for a 3 game road trip
       // a severe penalty for a road trip of 4 games or longer
       // look left and right from weekNum
		   
	    score = 0.0;
	   
       NflTeamSchedule homeTeamSched = gameSchedule.homeTeamSchedule;
       NflTeamSchedule awayTeamSched = gameSchedule.awayTeamSchedule;
              
       // TBD
       // Modularize the road trip checking to a passed NflTeamSchedule
       // Because if international, then want to treat the home team as having an away game
       // Therefore, check both home and away team for road trip violations
       
      scoreRoadTripLength(weekNum, awayTeamSched);
       
      if (gameSchedule.isInternational) {
         scoreRoadTripLength(weekNum, homeTeamSched);  // this game is a virtual away game for the home team
      }
	   
	   return true;
	}
	
	public boolean scoreRoadTripLength(int weekNum, NflTeamSchedule awayTeamSched) {
	   int roadTripLength = 1;
	   int firstRoadWeek;
	   int lastRoadWeek;

	   score = 0.0;
	   hardViolation = false;
	   
       // left to earlier weeks
       firstRoadWeek = weekNum;
       for (int wi=weekNum-1; wi >= 1; wi--) {
          if (awayTeamSched.scheduledGames[wi-1] == null) {
            // No game scheduled (yet) for the previous week
             break;
          }
          
          if (awayTeamSched.scheduledGames[wi-1].isBye) {
             continue;  // skip over a bye week
          }
          
          NflTeamSchedule prevWeekHomeTeamSched = awayTeamSched.scheduledGames[wi-1].homeTeamSchedule;
          NflGameSchedule prevWeekGame = awayTeamSched.scheduledGames[wi-1];
          
          if (prevWeekHomeTeamSched == awayTeamSched && !prevWeekGame.isInternational) {
             // last weeks game was a home game that was not an international (virtual away) game
             // so, treat a previous week international home game as an away game
             break;
          }
          
          // I was also the away team last week
          
          roadTripLength++;
          firstRoadWeek = wi;
       }

       // look right to later scheduled weeks
       lastRoadWeek = weekNum;
       for (int wi = weekNum + 1; wi <= NflDefs.numberOfWeeks; wi++) {
          if (awayTeamSched.scheduledGames[wi - 1] == null) {
            // No game scheduled (yet) for the next week
            break;
          }

          if (awayTeamSched.scheduledGames[wi - 1].isBye) {
             continue;  // skip over a bye week
          }

          NflTeamSchedule nextWeekHomeTeamSched = awayTeamSched.scheduledGames[wi - 1].homeTeamSchedule;
          NflGameSchedule nextWeekGame = awayTeamSched.scheduledGames[wi - 1];

          if (nextWeekHomeTeamSched == awayTeamSched && !nextWeekGame.isInternational) {
             // next weeks game is a home game that is not an international (virtual away) game
             // so, treat a next week international home game as an away game
             break;
          }

          roadTripLength++;
          lastRoadWeek = wi;
       }
       
	   boolean alertViolation = false;
	   
      if (roadTripLength == 3) {
         score += 3.0;
         if (firstRoadWeek == 1) {
            score = 4.0;
    	      alertViolation = true;
    	      hardViolation = true;
         }
         else if (lastRoadWeek == NflDefs.numberOfWeeks) {
            score = 4.0;
    	      alertViolation = true;
    	      hardViolation = true;
         }
      }
      else if (roadTripLength > 3) {
         score += 6.0;
 	      alertViolation = true;
 	      hardViolation = true;
      }
       
      if (alertViolation) {
	      if (gameSchedule.schedule.enableAlerts) {
			   NflScheduleAlert alert = new NflScheduleAlert();
			   alert.alertDescr = "Road trip too long: " + awayTeamSched.team.teamName + " from week: " + firstRoadWeek + " to week: " + lastRoadWeek + " Games: " + roadTripLength;
			   gameSchedule.schedule.addAlert(alert);
	      }
      }

      return true;
	}
}
