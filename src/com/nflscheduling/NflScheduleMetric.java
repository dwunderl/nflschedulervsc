package com.nflscheduling;

import java.util.ArrayList;

public class NflScheduleMetric {

   public String metricName;
   public double weight;
   public double score;   // combined and weighted 
   public boolean hardViolation = false;

   public NflScheduleMetric() {
   }
		
   NflScheduleMetric(String theName) {
      //System.out.println("Creating an nflGameMetric");
      metricName = theName;
   }

   // use @Override when inheriting from nflScheduleMetric for this function
   public boolean computeMetric(NflSchedule schedule) {
      return false;
   }
   
   // use @Override when inheriting from nflScheduleMetric for this function
   public boolean computeMetric(NflSchedule schedule, String gameMetricName) {
      ArrayList<NflGameSchedule> candidateGames = null;
      score = 0;
      hardViolation = false;
      
      for (int ti=1; ti <= NflDefs.numberOfTeams; ti++) {
         NflTeamSchedule teamSchedule = schedule.teamSchedules.get(ti-1);
         for (int wi=1; wi <= NflDefs.numberOfWeeks; wi++) {
            NflGameSchedule teamGame = teamSchedule.scheduledGames[wi-1];
            if (teamGame == null || teamGame.isBye) {
               continue;
            }
            
	   		NflGameMetric gameMetric = teamGame.findMetric(gameMetricName);

	   		gameMetric.computeMetric(wi, schedule, candidateGames);
	   		if (gameMetric.hardViolation) {
	   			hardViolation = true;
	   		}
            score += gameMetric.score;            
         }
      }

      return true;
   }
}
