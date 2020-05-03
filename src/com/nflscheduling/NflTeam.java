package com.nflscheduling;

//import java.util.*;

public class NflTeam {

   // ---
   // Instance data

   public String teamName;
   public double timezone;
   public String stadium;
   public String conference;
   public String division;
   
   // public NflGame[] scheduledGames;

   NflTeam(String name) {
      teamName = name;
      //scheduledGames = new NflGame[NflDefs.numberOfWeeks];
   }
}

