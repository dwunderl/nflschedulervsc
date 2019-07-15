package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetBalancedDivisional extends NflGameMetric {

	public NflGMetBalancedDivisional(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}

	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
	    // Determine the penalty incurred if my games teams divisional balance gets worse 
		// aggregated for both teams
		//------------
		   // Before home/away balance = absolute((home games scheduled)/(all games scheduled) - .50)
		   // After - update the home games scheduled and all games scheduled - and calculate
		   // Calc/accumulate before and after for both home and away teams
		   // if After Balance < Before Balance then score = -0.5
		   // if After Balance > Before Balance then score = +0.5
			   
		   score = 0.0;
		   
		   int curGameDivisional = 0;
		   if (gameSchedule.game.findAttribute("division")) {
			   curGameDivisional = 1;
		   }
		   
	       NflTeamSchedule homeTeamSched = gameSchedule.homeTeamSchedule;
	       NflTeamSchedule awayTeamSched = gameSchedule.awayTeamSchedule;

	       int homeTeamScheduledGames = 0;
	       int homeTeamScheduledDivisionalGames = 0;
	       
	       int awayTeamScheduledGames = 0;
	       int awayTeamScheduledDivisionalGames = 0;
	       
	       for (int wi=1; wi <= NflDefs.numberOfWeeks-1; wi++) {
    		   NflGameSchedule homeTeamGame = homeTeamSched.scheduledGames[wi-1];
	    	   if (homeTeamGame != null && !homeTeamGame.isBye) {
	    		   homeTeamScheduledGames++;
		           if (homeTeamGame.game.findAttribute("division")) {
		        	   homeTeamScheduledDivisionalGames++;
	    		   }
	    	   }
	    	   
    		   NflGameSchedule awayTeamGame = awayTeamSched.scheduledGames[wi-1];
	    	   if (awayTeamGame != null && !awayTeamGame.isBye) {
	    		   awayTeamScheduledGames++;
		           if (awayTeamGame.game.findAttribute("division")) {
		        	   awayTeamScheduledDivisionalGames++;
	    		   }
	    	   }
	       }
	       
	       if (homeTeamScheduledGames == 0 || awayTeamScheduledGames == 0) {
	    	   // avoid divide by 0
	    	   return true;
	       }
	       
	       double beforeHomeTeamBalanceMetric = Math.abs((double) homeTeamScheduledDivisionalGames/(double) homeTeamScheduledGames - (1.0/3.0));
	       double afterHomeTeamBalanceMetric = Math.abs((double) (homeTeamScheduledDivisionalGames+curGameDivisional)/(double) (homeTeamScheduledGames+1) - (1.0/3.0));

	       double beforeAwayTeamBalanceMetric = Math.abs((double) awayTeamScheduledDivisionalGames/(double) awayTeamScheduledGames - (1.0/3.0));
	       double afterAwayTeamBalanceMetric = Math.abs((double) (awayTeamScheduledDivisionalGames+curGameDivisional)/(double) (awayTeamScheduledGames+1) - (1.0/3.0));
	       
	       score = (afterHomeTeamBalanceMetric - beforeHomeTeamBalanceMetric) + (afterAwayTeamBalanceMetric - beforeAwayTeamBalanceMetric);
	        
		   //System.out.println("Info: Balanced Divisional metric for game, weekNum: " + weekNum + " home team: " + homeTeamSched.team.teamName + " away team: " + awayTeamSched.team.teamName
		   // 		                + ", score: " + score);
		   //System.out.println("                          beforeHomeTeamBalanceMetric: " + beforeHomeTeamBalanceMetric + ", afterHomeTeamBalanceMetric: " + afterHomeTeamBalanceMetric 
		   //		                    + ", beforeAwayTeamBalanceMetric: " + beforeAwayTeamBalanceMetric + ", afterAwayTeamBalanceMetric: " + afterAwayTeamBalanceMetric);
		   return true;
		}
}
