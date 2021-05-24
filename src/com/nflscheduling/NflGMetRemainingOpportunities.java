package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetRemainingOpportunities extends NflGameMetric {

	NflGMetRemainingOpportunities(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}

	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
		
      // Incentivize for lower number of remaining opportunities/games vs other teams
      // For each of my teams determine the minimum number of remaining games
      // if my number of remaining games is less than the maximum number of remaining games from any other teams/game combinations
      
      // First - Find team with most unscheduled games ==> mostUnscheduledGames - could be done globally by the algorithm for this week
      // Then for my game - find the team with the minimum unscheduled games ==> myUnscheduledGames
      // score = -(maxUnscheduledGames/myUnscheduledGames - 1.0) a negative penalty = an incentive
      // where the teams with the max unscheduled games get no incentive
      // and teams with min unscheduled games get the max incentive - and those in middle get some incentive

      score = 0.0;
      
      // Find game/teams with most unscheduled games - save that as maxUnscheduledGames
      
      int maxMinRemainingGameCount = 0;
      int myMinRemainingGameCount = 0;
      
 	  for(NflGameSchedule usGame: candidateGames) {	       
 	     NflTeamSchedule homeTeamSched = schedule.findTeam(usGame.game.homeTeam);
 	     NflTeamSchedule awayTeamSched = schedule.findTeam(usGame.game.awayTeam);

 	     int homeTeamUSgameCount = 0;
 	     int awayTeamUSgameCount = 0;
 	       
 	     for (int wi=1; wi <= NflDefs.numberOfWeeks; wi++) {
 	        NflGameSchedule homeGame = homeTeamSched.scheduledGames[wi-1];
            if (homeGame == null || homeGame.isBye) {
 	    	   homeTeamUSgameCount++;
 	    	}
 	    	      
 	        NflGameSchedule awayGame = awayTeamSched.scheduledGames[wi-1];
 	        if (awayGame == null || awayGame.isBye) {
	           awayTeamUSgameCount++;
	    	}
 	     }

		 int minRemainingGameCount = Math.min(homeTeamUSgameCount, awayTeamUSgameCount);
		     maxMinRemainingGameCount = Math.max(minRemainingGameCount, maxMinRemainingGameCount);
		   
	     if (usGame == gameSchedule) {
		    myMinRemainingGameCount = minRemainingGameCount;
	     }
 	  }

 	  score = 1.0*(1.0 - (double) maxMinRemainingGameCount/ (double) myMinRemainingGameCount);
      //System.out.println("Info: Remaining Opportunities metric for game, weekNum: " + weekNum + " home team: " + gameSchedule.game.homeTeam + " away team: " + gameSchedule.game.awayTeam
      //	                   + ", myRemainingGameCount: " + myRemainingGameCount + ", maxRemainingGameCount: " + maxRemainingGameCount + ", score: " + score);
	   
	  return true;
	}
}
