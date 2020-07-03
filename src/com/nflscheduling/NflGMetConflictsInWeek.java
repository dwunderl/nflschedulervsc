package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetConflictsInWeek extends NflGameMetric {

	NflGMetConflictsInWeek(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}
	
	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
	  // Penalize for the number of game conflicts for this week
      // find the number of unscheduled games that match with my home team or away team
	  // Then normalize that number with the total number of unscheduled games - use as penalty
		   
      score = 0.0;
      int conflictsInWeek = 0;
       
 	  for(NflGameSchedule otherUSgame: candidateGames) {
         if (otherUSgame == gameSchedule) {
        	continue;   // don't count myself as a conflict
         }
         
         if (otherUSgame.game.homeTeam.equalsIgnoreCase(gameSchedule.game.homeTeam) ||
             otherUSgame.game.homeTeam.equalsIgnoreCase(gameSchedule.game.awayTeam) ||
             otherUSgame.game.awayTeam.equalsIgnoreCase(gameSchedule.game.homeTeam) ||
             otherUSgame.game.awayTeam.equalsIgnoreCase(gameSchedule.game.awayTeam)) {
        	 
            conflictsInWeek++;
         }
      }
 	  
 	  int candidateSizeWithoutMe = candidateGames.size() - 1;
 	  if (candidateSizeWithoutMe == 0) {
 	      candidateSizeWithoutMe = 1;
 	  }
 	  score = (double) conflictsInWeek/(double) candidateSizeWithoutMe;
	   
	  return true;
	}
}
