package com.nflscheduling;

public class NflRestrictedGame {

   // ---
   // Instance data

   public String teamName; // "all" means all teams
   public int weekNum;
   public String otherTeam;
   public String restriction;
   public String stadium;

   NflRestrictedGame() {
      System.out.println("Creating an nflRestrictedGame");
   }

   NflRestrictedGame(String theTeamName, int theWeekNum, String theRestriction, String theOtherTeam, String theStadium) {
      teamName = theTeamName;
      weekNum = theWeekNum;
      restriction = theRestriction;
      otherTeam = theOtherTeam;
      stadium = theStadium;
   }
}
