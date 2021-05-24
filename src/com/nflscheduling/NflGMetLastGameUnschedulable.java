package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetLastGameUnschedulable extends NflGameMetric {

	public NflGMetLastGameUnschedulable(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}
	
	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
	  // Penalize if the 2nd to last game scheduled would leave the last game unschedulable
     // NOTE: It Doesn't seem like this metric can accomplish it's goals
     // It's just looking at remaining teams to schedule - without knowing if a game exists for the remaining teams
     // That's where the teamweek candidates could give that info
	  
      score = 0.0;
		  
 	  int unScheduledTeamCount = 0;
 	  ArrayList<NflTeamSchedule> remainingUnscheduledTeams = new ArrayList<NflTeamSchedule>();
 	  
      for (NflTeamSchedule teamSchedule: schedule.teamSchedules) {
         if (teamSchedule.scheduledGames[weekNum-1] == null) {
            unScheduledTeamCount++;
            // if this game does not involve this team, save it as a remaining candidate team
            // Collecting remaining unscheduled teams if we were to schedule this game
            if (!gameSchedule.containsTeam(teamSchedule.team.teamName)) {
               remainingUnscheduledTeams.add(teamSchedule);
    	      }
         }
      }

      // If unScheduledGameCount == 4 then the 2nd to last game is about to be scheduled
      // we want to determine if we schedule this game, will it leave the last game unschedulable
      // If so - penalize this game choice - in an effort to leave the last game schedulable
      
      // When 2 games left to schedule in week there should be exactly 4 different teams
      if (unScheduledTeamCount != 4) {
         return true;
      }
      
      // Then if schedule this game, there should be 2 remaining teams
      if (remainingUnscheduledTeams.size() != 2) {
         return false;
      }
      
      // Now check the remaining candidate games to determine if a scheduleable game remains for 
      // the 2 remainingUnscheduledTeams

      score = 2.0;  // penalty in case I don't find a candidate game for the final 2 teams
      
      String remainingTeam1 = remainingUnscheduledTeams.get(0).team.teamName;
      String remainingTeam2 = remainingUnscheduledTeams.get(1).team.teamName;

      // if we schedule the current game it leaves 2 teams to schedule which do (score=0) or don't (score > 0) have a remaining game
      
      for (NflGameSchedule candidateGameSchedule: candidateGames) {
         if (candidateGameSchedule == gameSchedule) {
            continue;  // don't check my game
         }
         
         if (candidateGameSchedule.containsTeam(remainingTeam1) &&
        		 candidateGameSchedule.containsTeam(remainingTeam2)) {
            score = 0.0;  // found a candidate game for the remaining 2 teams
         }
      }

	   return true;
	}
}
