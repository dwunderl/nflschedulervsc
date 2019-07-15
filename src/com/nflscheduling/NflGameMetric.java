package com.nflscheduling;
import java.util.*;

public class NflGameMetric {

   // ---
   // Instance data

   public String metricName;
   public NflGameSchedule gameSchedule;
   public int weekNum;
   public double weight;
   public double  scoreHomeTeam;
   public double  scoreAwayTeam;
   public double  score;   // combined and weighted 
   public boolean isScheduleMetric = true;
   public boolean hardViolation = false;

   NflGameMetric(String theName, NflGameSchedule theGameSchedule) {
      //System.out.println("Creating an nflGameMetric");
      metricName = theName;
      gameSchedule = theGameSchedule;
      weight = 1.0;
   }

   // use @Override when inheriting from nflGameMetric for this function
   public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
      return false;
   }
   
   // use @Override when inheriting from nflGameMetric for this function
   public boolean isScheduleMetric() {
      return isScheduleMetric;
   }
}
