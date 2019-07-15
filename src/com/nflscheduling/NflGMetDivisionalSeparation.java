package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetDivisionalSeparation extends NflGameMetric {

	public NflGMetDivisionalSeparation(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}

	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
		
       // ensure 1st Divisional game with a team is after week 5
       // if game is a divisional game, and weeknum > 5, incentivize the game if the second matchup game hasn't been scheduled yet
       // if game is a divisional game, and weeknum > 5, incentivize the game if the second matchup game hasn't been scheduled yet
	   // Otherwise a 0 score - no incentive
		
	   // Ensure there is not a weekNum-1 or weekNum+1 scheduled game that has the same teams
	   // as the gameSchedule linked to this gameMetric
	   // if no repeated matchups in either direction - then set the score to 0
	   // if a repeated matchup exists in either direction - set the score to 1
	

	   // for each team in my gameSchedule 
	   // if weekNum+1 is a valid week (NflDefs.numberOfWeeks)
       // check weekNum+1 schedule for TeamSchedule(s) homeTeam and then awayTeam
	   // if weekNum+1 has a game scheduled, ensure it's gameSchedule teams are not the same as mine
	   // if weekNum-1 is a valid week
       // if weekNum-1 has a game scheduled, ensure it's gameSchedule teams are not the same as mine
       
	   // check my home teams scheduled game for a repeated matchup
	   
       score = 0.0;
       
       // This metric only applies to divisional games
       if (!gameSchedule.game.findAttribute("division")) {
    	   return true;
       }

       // within week 5 it's too late to avoid the separation problem
       if (weekNum <= 5) {
    	   return true;
       }
       
       // beyond week 10 it's better to let nature take it's course, maybe the first divisional game will schedule naturally
       if (weekNum >= 10) {
    	   return true;
       }
       
       // has the other divisional game been scheduled yet - in a later week
       // if so - no problem - otherwise - incentivize this game with a negative penalty
       boolean divisionalPairScheduled = false;
       
       NflTeamSchedule teamSchedule = gameSchedule.homeTeamSchedule;
       for (int wi=weekNum+1; wi <= NflDefs.numberOfWeeks; wi++) {
          NflGameSchedule teamGame2 = teamSchedule.scheduledGames[wi-1];
       
          if (teamGame2 == null || teamGame2.isBye || !teamGame2.game.findAttribute("division")) {
	         continue;
	      }
       
          if (gameSchedule.game.awayTeam.equalsIgnoreCase(teamGame2.game.homeTeam) &&
              gameSchedule.game.homeTeam.equalsIgnoreCase(teamGame2.game.awayTeam)) {
        	  divisionalPairScheduled = true;
        	  break;
          }
       }
       
       if (divisionalPairScheduled) {
    	   return true;
       }
       
       // score incentivizes the game in a sliding scale where week 6 is the most urgent
       
       score = -(10-weekNum);
       
	   return true;
	}
}
