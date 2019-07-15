package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetBalancedHomeAway extends NflGameMetric {

	public NflGMetBalancedHomeAway(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}

	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
	       // Determine the penalty incurred if my scheduled home vs away balance gets worse 
		   // aggregated for both teams
		   // Before home/away balance = absolute((home games scheduled)/(all games scheduled) - .50)
		   // After - update the home games scheduled and all games scheduled - and calculate
		   // Calc/accumulate before and after for both home and away teams
		   // if After Balance < Before Balance then score = -0.5
		   // if After Balance > Before Balance then score = +0.5
			   
		   score = 0.0;
		   
		   //String myHomeTeamName = gameSchedule.game.homeTeam;
		   //String myAwayTeamName = gameSchedule.game.awayTeam;
		   
	       //NflTeamSchedule homeTeamSched = schedule.findTeam(myHomeTeamName);
	       NflTeamSchedule homeTeamSched = gameSchedule.homeTeamSchedule;
	       //NflTeamSchedule awayTeamSched = schedule.findTeam(myAwayTeamName);
	       NflTeamSchedule awayTeamSched = gameSchedule.awayTeamSchedule;

	       int homeTeamScheduledGames = 0;
	       int homeTeamScheduledHomeGames = 0;
	       
	       int awayTeamScheduledGames = 0;
	       int awayTeamScheduledHomeGames = 0;
	       
	       for (int wi=1; wi <= NflDefs.numberOfWeeks; wi++) {
    		   NflGameSchedule homeTeamGame = homeTeamSched.scheduledGames[wi-1];
	    	   if (homeTeamGame != null && !homeTeamGame.isBye) {
	    		   homeTeamScheduledGames++;
	    		   //if (homeTeamGame.game.homeTeam.equalsIgnoreCase(myHomeTeamName)) {
		           if (homeTeamGame.homeTeamSchedule == homeTeamSched) {
	    			   homeTeamScheduledHomeGames++;
	    		   }
	    	   }
	    	   
    		   NflGameSchedule awayTeamGame = awayTeamSched.scheduledGames[wi-1];
	    	   if (awayTeamGame != null && !awayTeamGame.isBye) {
	    		   awayTeamScheduledGames++;
		           if (awayTeamGame.homeTeamSchedule == awayTeamSched) {
	    		   //if (awayTeamGame.game.homeTeam.equalsIgnoreCase(myAwayTeamName)) {
	    			   awayTeamScheduledHomeGames++;
	    		   }
	    	   }
	       }
	       
	       if (homeTeamScheduledGames == 0) {
	    	   // avoid divide by 0
	    	   homeTeamScheduledGames = 1;
	    	   homeTeamScheduledHomeGames++;
	       }
	       
	       if (awayTeamScheduledGames == 0) {
	    	   // avoid divide by 0
	    	   awayTeamScheduledGames = 1;
	    	   awayTeamScheduledHomeGames++;
	       }
       
	       double beforeHomeTeamBalanceMetric = Math.abs((double) homeTeamScheduledHomeGames/(double) homeTeamScheduledGames);
	       double afterHomeTeamBalanceMetric = Math.abs((double) (homeTeamScheduledHomeGames+1)/(double) (homeTeamScheduledGames+1));

	       double beforeAwayTeamBalanceMetric = Math.abs((double) awayTeamScheduledHomeGames/(double) awayTeamScheduledGames);
	       double afterAwayTeamBalanceMetric = Math.abs((double) (awayTeamScheduledHomeGames)/(double) (awayTeamScheduledGames+1));

	       boolean homeBalanceBetter = afterHomeTeamBalanceMetric <= 0.5;
	       boolean homeBalanceNeutral = beforeHomeTeamBalanceMetric < 0.5 && afterHomeTeamBalanceMetric > 0.5;
	       boolean homeBalanceWorse = beforeHomeTeamBalanceMetric >= 0.5;
	       
	       boolean awayBalanceBetter = afterAwayTeamBalanceMetric >= 0.5;
	       boolean awayBalanceNeutral = beforeAwayTeamBalanceMetric > 0.5 && afterHomeTeamBalanceMetric < 0.5;
	       boolean awayBalanceWorse = beforeAwayTeamBalanceMetric <= 0.5;
	       
	       if (homeBalanceBetter && awayBalanceBetter) {
	    	   score = -2.0;
	       }
	       else if (homeBalanceBetter && awayBalanceNeutral) {
	    	   score = -1.0;
	       }
	       else if (homeBalanceBetter && awayBalanceWorse) {
	    	   score = 0.0;
	       }
	       else if (homeBalanceNeutral && awayBalanceBetter) {
	    	   score = -1.0;
	       }
	       else if (homeBalanceNeutral && awayBalanceNeutral) {
	    	   score = 0.0;
	       }
	       else if (homeBalanceNeutral && awayBalanceWorse) {
	    	   score = 2.0;
	       }
	       else if (homeBalanceWorse && awayBalanceBetter) {
	    	   score = 0.0;
	       }
	       else if (homeBalanceWorse && awayBalanceNeutral) {
	    	   score = 1.0;
	       }
	       else if (homeBalanceWorse && awayBalanceWorse) {
	    	   score = 4.0;
	       }
	       
	       //score = (afterHomeTeamBalanceMetric - beforeHomeTeamBalanceMetric) + (afterAwayTeamBalanceMetric - beforeAwayTeamBalanceMetric);
	        
		   //System.out.println("Info: Balanced Home Away metric for game, weekNum: " + weekNum + " home team: " + homeTeamSched.team.teamName + " away team: " + awayTeamSched.team.teamName
		   // 		                + ", score: " + score);
		   //System.out.println("                          beforeHomeTeamBalanceMetric: " + beforeHomeTeamBalanceMetric + ", afterHomeTeamBalanceMetric: " + afterHomeTeamBalanceMetric 
		   //		                    + ", beforeAwayTeamBalanceMetric: " + beforeAwayTeamBalanceMetric + ", afterAwayTeamBalanceMetric: " + afterAwayTeamBalanceMetric);
		   return true;
		}
}
