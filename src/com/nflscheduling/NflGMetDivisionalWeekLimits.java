package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetDivisionalWeekLimits extends NflGameMetric {

	public NflGMetDivisionalWeekLimits(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}
	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
	  // Penalize if more than 8 division games (1), and then more than 11 divisional games (5)
      // If this is not a divisional game, then there can be no penalty, score=0, return
      // If this is a divisional game, count all the divisional games this week, and check the limits
      // and set the score if limits exceeded
	  
      score = 0.0;
		  
      if (!gameSchedule.game.findAttribute("division")) {
    	  return true;
      }
      
 	  int divisionalGameCount = 0;
 	  
      for (NflTeamSchedule teamSchedule: schedule.teamSchedules) {
         if (teamSchedule.scheduledGames[weekNum-1] != null) {
            NflGameSchedule scheduledGame = teamSchedule.scheduledGames[weekNum-1];
            if (scheduledGame.game.findAttribute("division")) {
               divisionalGameCount++;
            }
         }
      }
      
      // must divide divisionalGameCount by 2 since each game appears twice in the week, once for each of the 2 teams
      divisionalGameCount = divisionalGameCount/2;
      
      if (divisionalGameCount > 11) {
         score = 10.0;
      }
      else if (divisionalGameCount > 8) {
         score = 1.0;
      }
      
      //System.out.println("Info: Last game unschedulable metric for game, weekNum: " + weekNum + " home team: " + gameSchedule.game.homeTeam + " away team: " + gameSchedule.game.awayTeam
      //                    + ", unScheduledTeamCount: " + unScheduledTeamCount + ", remainingUnscheduledTeams: " + remainingTeam1 + ", " + remainingTeam2 + ", score: " + score);
	   
	  return true;
	}

}
