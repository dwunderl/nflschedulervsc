package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetNoRepeatedMatchup extends NflGameMetric {

	NflGMetNoRepeatedMatchup(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}

	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
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
       hardViolation = false;
       
       boolean alertViolation = false;
       int firstMatchupWeek = weekNum;
       int lastMatchupWeek = weekNum;

       if (!gameSchedule.game.findAttribute("division")) {
    	   return true;
       }

	   NflTeamSchedule homeTeamSched = schedule.findTeam(gameSchedule.game.homeTeam);
	   NflTeamSchedule awayTeamSched = schedule.findTeam(gameSchedule.game.awayTeam);
       //System.out.println("Info: No repeated match up metric for game, weekNum: " + weekNum + " home team: " + homeTeamSched.team.teamName + " away team: " + awayTeamSched.team.teamName);
	   
	   // Check next weeks game for no repeated matchup
	   // be aware of byes - skip over

	   if (weekNum+1 <= NflDefs.numberOfWeeks) {
		  // Check next non-bye game for the current game's home team to make sure no repeat
	      for (int wi=weekNum+1; wi <= NflDefs.numberOfWeeks; wi++) {
	         NflGameSchedule nextWeeksHomeTeamGame = homeTeamSched.scheduledGames[wi-1]; // NOTE: weekNum starts at 1, must correct for index
             if (nextWeeksHomeTeamGame == null) {
            	 break;
             }
             
	    	 if (nextWeeksHomeTeamGame.isBye) {
	    		 continue;
	    	 }
	    	 
             if (nextWeeksHomeTeamGame.game.awayTeam.equalsIgnoreCase(gameSchedule.game.homeTeam) &&
            		 nextWeeksHomeTeamGame.game.homeTeam.equalsIgnoreCase(gameSchedule.game.awayTeam)) {
                score = 10.0;
                alertViolation = true;
                lastMatchupWeek = wi;
             }
             
             break;
	      }

		  
		  // Check next non-bye game for the current game's away team to make sure no repeat
	      for (int wi=weekNum+1; wi <= NflDefs.numberOfWeeks; wi++) {
			 NflGameSchedule nextWeeksAwayTeamGame = awayTeamSched.scheduledGames[wi-1]; // NOTE: weekNum starts at 1, must correct for index
             if (nextWeeksAwayTeamGame == null) {
         	    break;
             }
          
	    	 if (nextWeeksAwayTeamGame.isBye) {
	    		 continue;
	    	 }
	    	 
             if (nextWeeksAwayTeamGame.game.awayTeam.equalsIgnoreCase(gameSchedule.game.homeTeam) &&
        		 nextWeeksAwayTeamGame.game.homeTeam.equalsIgnoreCase(gameSchedule.game.awayTeam)) {
                score = 10.0;
                alertViolation = true;
                lastMatchupWeek = wi;
             }
             break;
	      }
	   }
	   	   
	   // Check previous weeks game for no repeated matchup
	   // be aware of byes - skip over
	   
	   if (weekNum > 1) {
	      // Check the previous non-bye game for the current game's home team to make sure no repeat
	      for (int wi=weekNum-1; wi >= 1; wi--) {
             NflGameSchedule prevWeeksHomeTeamGame = homeTeamSched.scheduledGames[wi-1]; // NOTE: weekNum starts at 1, must correct for index
             if (prevWeeksHomeTeamGame == null) {
            	 break;
             }
             
	    	 if (prevWeeksHomeTeamGame.isBye) {
	    		 continue;
	    	 }
	    	 
             if (prevWeeksHomeTeamGame.game.awayTeam.equalsIgnoreCase(gameSchedule.game.homeTeam) &&
                 prevWeeksHomeTeamGame.game.homeTeam.equalsIgnoreCase(gameSchedule.game.awayTeam)) {
                score = 10.0;
                alertViolation = true;
                firstMatchupWeek = wi;
             }
             break;
	      }
	      
	      // Check the previous non-bye game for the current game's away team to make sure no repeat
	      for (int wi=weekNum-1; wi >= 1; wi--) {
	         NflGameSchedule prevWeeksAwayTeamGame = awayTeamSched.scheduledGames[wi-1]; // NOTE: weekNum starts at 1, must correct for index
	         if (prevWeeksAwayTeamGame == null) {
	            break;
	         }
	             
		     if (prevWeeksAwayTeamGame.isBye) {
		        continue;
		     }
		    	 
	         if (prevWeeksAwayTeamGame.game.awayTeam.equalsIgnoreCase(gameSchedule.game.homeTeam) &&
	        		 prevWeeksAwayTeamGame.game.homeTeam.equalsIgnoreCase(gameSchedule.game.awayTeam)) {
	            score = 10.0;
                alertViolation = true;
                firstMatchupWeek = wi;
	         }
	         break;
		  }
	   }
	   
       if (alertViolation) {
    	   hardViolation = true;
	       if (gameSchedule.schedule.enableAlerts) {
			    NflScheduleAlert alert = new NflScheduleAlert();
			    String team1;
			    String team2;
			    if (gameSchedule.game.homeTeam.compareToIgnoreCase(gameSchedule.game.awayTeam) < 0) {
			    	team1 = gameSchedule.game.homeTeam;
			    	team2 = gameSchedule.game.awayTeam;
			    }
			    else {
			    	team1 = gameSchedule.game.awayTeam;
			    	team2 = gameSchedule.game.homeTeam;
			    }
			    alert.alertDescr = "Repeated Matchup: " + team1 + " vs " + team2 + " first matchup week: " + firstMatchupWeek + " last matchup week: " + lastMatchupWeek;
			    gameSchedule.schedule.addAlert(alert);
	       }
       }

	   //System.out.println("Info: No Repeated Matchup metric for game, weekNum: " + weekNum + " home team: " + gameSchedule.game.homeTeam + " away team: " + gameSchedule.game.awayTeam
       //           + ", score: " + score);
	   return true;
	}
}
