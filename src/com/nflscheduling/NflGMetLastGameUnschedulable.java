package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetLastGameUnschedulable extends NflGameMetric {

	public NflGMetLastGameUnschedulable(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}
	
	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
	  // Penalize if the 2nd to last game scheduled would leave the last game unschedulable
	  
      score = 0.0;
		  
 	  int unScheduledTeamCount = 0;
 	  ArrayList<NflTeamSchedule> remainingUnscheduledTeams = new ArrayList<NflTeamSchedule>();
 	  
      for (NflTeamSchedule teamSchedule: schedule.teamSchedules) {
         if (teamSchedule.scheduledGames[weekNum-1] == null) {
            unScheduledTeamCount++;
            if (!gameSchedule.containsTeam(teamSchedule.team.teamName)) {
               remainingUnscheduledTeams.add(teamSchedule);
    	        }
         }
      }

      // If unScheduledGameCount == 4 then the 2nd to last game is about to be scheduled
      // we want to determine if we schedule this game, will it leave the last game unschedulable
      // If so - penalize this game choice - in an effort to leave the last game schedulable
      
      if (unScheduledTeamCount != 4) {
         return true;
      }
      
      if (remainingUnscheduledTeams.size() != 2) {
         return false;
      }
      
      score = 1.0;  // penalty in case I don't find a candidate game for the final 2 teams
      
      String remainingTeam1 = remainingUnscheduledTeams.get(0).team.teamName;
      String remainingTeam2 = remainingUnscheduledTeams.get(1).team.teamName;

      // we are attempting to schedule the 2nd to last game 
      // if we schedule this game will it leave 2 teams to schedule which don't have a remaining game
      
      for (NflGameSchedule candidateGameSchedule: candidateGames) {
         if (candidateGameSchedule == gameSchedule) {
            continue;  // don't check my game
         }
         
         if (candidateGameSchedule.containsTeam(remainingTeam1) &&
        		 candidateGameSchedule.containsTeam(remainingTeam2)) {
            score = 0.0;  // found a candidate game for the remaining 2 teams
         }
      }
      
      //System.out.println("Info: Last game unschedulable metric for game, weekNum: " + weekNum + " home team: " + gameSchedule.game.homeTeam + " away team: " + gameSchedule.game.awayTeam
      //                    + ", unScheduledTeamCount: " + unScheduledTeamCount + ", remainingUnscheduledTeams: " + remainingTeam1 + ", " + remainingTeam2 + ", score: " + score);
	   
	  return true;
	}

}
