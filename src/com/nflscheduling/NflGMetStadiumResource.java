package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetStadiumResource extends NflGameMetric {

	public NflGMetStadiumResource(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}
	
	// metric = -(Stadium capacity - Remaining Home Games - Away Games Scheduled This week + home games scheduled this week + 1)
	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
	  // Penalize if scheduling this game would prevent/hinder the fitting of all future home games into a constrained home stadium of one of my teams
	  
      score = 0.0;

      if (this.gameSchedule.isBye) {
         return true;
      }
      
      // determine if one of my teams has a constrained home stadium - no scoring if none exists
      // is either stadium an NflResource - capture the resource
      NflTeamSchedule homeTeamSched = schedule.findTeam(gameSchedule.game.homeTeam);
      NflTeamSchedule awayTeamSched = schedule.findTeam(gameSchedule.game.awayTeam);
      
      NflResourceSchedule stadiumResourceSchedule = null;
      
      if (homeTeamSched.team.stadium != null) {          
    	  stadiumResourceSchedule = schedule.findResource(homeTeamSched.team.stadium);
      }
      
      if (awayTeamSched.team.stadium != null) {          
    	  stadiumResourceSchedule = schedule.findResource(awayTeamSched.team.stadium);
      }
      
      if (stadiumResourceSchedule == null) {
         return true;
      }
     
      // determine the remaining capacity of the stadium
      // walk through the remaining unscheduled weeks from weeknum=1 to the current weeknum for the NflResource - add up the (weeklyLimit-usage) for all weeks

      int sDir = NflDefs.schedulingDirection;
      int remainingCapacityOfStadium = 0;
      for (int wi=weekNum; (sDir == -1) ? wi >= 1 : wi <= NflDefs.numberOfWeeks; wi+=sDir) {
    	   remainingCapacityOfStadium += stadiumResourceSchedule.usage[wi-1];
      }
      
      // determine the remaining unscheduled home games for the stadium
      
      int remainingHomeGames = 0;
	   for(NflGameSchedule usGame: candidateGames) {
		   if (usGame.stadium == null) continue;
		  
		   if (usGame.stadium.equalsIgnoreCase(stadiumResourceSchedule.resource.resourceName)) {
			   remainingHomeGames++;
		   }
	   }
	  
      // determine the home/away games scheduled (in this week) for the teams having this as their home stadium stadium (count my game)
      int awayGamesScheduledThisWeek = 0;
      int homeGamesScheduledThisWeek = 0;
      
      // For the current game being evaluated - which uses the stadium
      // include this current candidate game in the count of games scheduled for this week - for conditional evaluation purposes
      if (homeTeamSched.team.stadium != null &&
          homeTeamSched.team.stadium.equalsIgnoreCase(stadiumResourceSchedule.resource.resourceName)) {
          homeGamesScheduledThisWeek++;
      }
      else {
         awayGamesScheduledThisWeek++;
      }
      
      // Find the teams that use the stadium
      // then if they have a game already scheduled this week - count as home or away
      for (NflTeamSchedule teamSched: schedule.teamSchedules) {
	     if (teamSched.team.stadium == null) continue;
    	    if (teamSched.team.stadium.equalsIgnoreCase(stadiumResourceSchedule.resource.resourceName)) {
            NflGameSchedule thisWeeksGame = teamSched.scheduledGames[weekNum-1];
    	      if (thisWeeksGame != null) {
    	        if (thisWeeksGame.game.homeTeam.equalsIgnoreCase(teamSched.team.teamName)) {
    	    	     homeGamesScheduledThisWeek++;
    	        }
    	        else {
    	           awayGamesScheduledThisWeek++;
    	        }
    	      }
    	   }
      }
      
      // Calculate the metric penalty
  	  // metric = -(Stadium capacity - Remaining Home Games - Away Games Scheduled This week + home games scheduled this week + 1)
      
      if (homeGamesScheduledThisWeek <= 0) {
          score = remainingCapacityOfStadium - remainingHomeGames - awayGamesScheduledThisWeek + homeGamesScheduledThisWeek + 1;
          score = score*(-1);  // flip the sign - so negative capacity shows as a positive penalty and positive capacity is a good score
      }
      //System.out.println("Stadium Resource metric game, weekNum: " + weekNum + " home team: " + gameSchedule.game.homeTeam + " away team: " + gameSchedule.game.awayTeam + ", score: " + score);

 	  return true;
	}
}
