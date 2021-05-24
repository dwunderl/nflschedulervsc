package com.nflscheduling;

import java.util.ArrayList;

public class NflRestrictedGame {

   // ---
   // Instance data

   public String teamName; // "all" means all teams
   public int weekNum;
   public String otherTeam;
   public String restriction;
   public String stadium;

   public static boolean exists(String teamName, int weekNum, ArrayList<NflRestrictedGame> restrictedGames) {
      for (int rgi = 0; rgi < restrictedGames.size(); rgi++) {
         NflRestrictedGame restrictedGame = restrictedGames.get(rgi);
         if (restrictedGame.weekNum == weekNum) {
            if (restrictedGame.teamName.equalsIgnoreCase(teamName) || 
                restrictedGame.otherTeam.equalsIgnoreCase(teamName)) {
               return true;
            }
         }
      }
      return false;
   }

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
