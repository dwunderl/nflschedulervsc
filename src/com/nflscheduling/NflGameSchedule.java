package com.nflscheduling;
import java.util.*;

public class NflGameSchedule {

   // ---
   // Instance data

   public NflGame game;
   public int weekNum;
   public double  score;   // combined for all metrics 
   public int hardViolationCount;
   public ArrayList<NflGameMetric> metrics;
   public boolean restrictedGame;
   public String stadium;
   public int weekScheduleSequence;
   public double demotionPenalty;
   public int demotionCount;
   public double promotionScore;

   public ArrayList<NflResource> scheduledResources;
   public NflSchedule schedule;
   public NflTeamSchedule homeTeamSchedule;
   public NflTeamSchedule awayTeamSchedule;
   public ArrayList<NflGameSchedule> unscheduledByes;
   public ArrayList<NflGameSchedule> opponentByes;
   public int byeCandidateScore;

   public boolean isBye;
   public int candidateCount;

   NflGameSchedule(NflGame theGame, NflSchedule theSchedule) {
      //System.out.println("Creating an nflGameMetric");
      game = theGame;
      weekNum = 0;
      restrictedGame = false;
      weekScheduleSequence = 0;
      demotionCount = 0;
      demotionPenalty = 0;
      promotionScore = 0;
      schedule = theSchedule;
      isBye = theGame.isBye;
   }
   
   public boolean initGame() {
      homeTeamSchedule = schedule.findTeam(game.homeTeam);
      awayTeamSchedule = schedule.findTeam(game.awayTeam);
      stadium = homeTeamSchedule.team.stadium;
      unscheduledByes = new ArrayList<NflGameSchedule>();
      metrics = new ArrayList<NflGameMetric>();
	      
      NflGMetNoRepeatedMatchup metricNRM = new NflGMetNoRepeatedMatchup("NoRepeatedMatchup", this);
      metrics.add(metricNRM);
      NflGMetConflictsInWeek metricCIW = new NflGMetConflictsInWeek("ConflictsInWeek", this);
      metrics.add(metricCIW);
      NflGMetRoadTripLimit metricRTL = new NflGMetRoadTripLimit("RoadTripLimit", this);
      metrics.add(metricRTL);
      NflGMetHomeStandLimit metricHSL = new NflGMetHomeStandLimit("HomeStandLimit", this);
      metrics.add(metricHSL);
      NflGMetLastGameUnschedulable metricLGUS = new NflGMetLastGameUnschedulable("LastGameUnschedulable", this);
      metrics.add(metricLGUS);
      NflGMetBalancedHomeAway metricBalHA = new NflGMetBalancedHomeAway("BalancedHomeAway", this);
      metrics.add(metricBalHA);
      NflGMetStadiumResource metricStdRes = new NflGMetStadiumResource("StadiumResource", this);
      metrics.add(metricStdRes);
      NflGMetDivisionalWeekLimits metricDivWkLim = new NflGMetDivisionalWeekLimits("DivisionalWeekLimits", this);
      metrics.add(metricDivWkLim);
      NflGMetDivisionalSeparation metricDivSep = new NflGMetDivisionalSeparation("DivisionalSeparation", this);
      metrics.add(metricDivSep);
      
      // scheduling success is severely degraded with this new metric
      NflGMetBalancedDivisional metricBalDiv = new NflGMetBalancedDivisional("BalancedDivisional", this);
      metrics.add(metricBalDiv);

      //NflGMetRemainingOpportunities metricRO = new NflGMetRemainingOpportunities("RemainingOpportunities", this);
      //metrics.add(metricRO);

      candidateCount = 0;
      
      return true;
   }
   
   public boolean initBye() {
      homeTeamSchedule = schedule.findTeam(game.homeTeam);
	   awayTeamSchedule = schedule.findTeam(game.awayTeam);
	   metrics = new ArrayList<NflGameMetric>();
      opponentByes = new ArrayList<NflGameSchedule>();
      isBye = true;
      candidateCount = 0;

      return true;
   }

   public boolean containsTeam(String teamName) {
	   if (teamName.equalsIgnoreCase(game.homeTeam) ||
          teamName.equalsIgnoreCase(game.awayTeam)) {
		   return true;
	   }
	   
	   return false;
   }

   public boolean computeMetrics(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
      score = 0.0;
      hardViolationCount = 0;

      for (int mi = 0; mi < metrics.size(); mi++) {
         NflGameMetric gameMetric = metrics.get(mi);
         gameMetric.computeMetric(weekNum, schedule, candidateGames);
         score += gameMetric.score * gameMetric.weight;
         if (gameMetric.hardViolation) {
            hardViolationCount++;
         }
      }

      // score += demotionPenalty + promotionScore;
      score += demotionPenalty;
      // score += promotionScore;

      // if (demotionPenalty != 0.0)
      // System.out.println("Computed Metric: " + score + ", including demotionPenalty
      // of " + demotionPenalty + ", weekSequence: " + weekScheduleSequence + " for
      // game: " + game.homeTeam + " : " + game.awayTeam);

      return true;
   }
   
   public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames, String metricName) {
      score = 0.0;
      if (metricName == null) return true;
		  
      for (int mi=0; mi < metrics.size(); mi++) {
         NflGameMetric gameMetric = metrics.get(mi);
         if (gameMetric.metricName.equalsIgnoreCase(metricName)) {
            gameMetric.computeMetric(weekNum, schedule, candidateGames);
            score += gameMetric.score;
            break;
         }
         //System.out.println("Computing Metric: " + gameMetric.metricName + " for game: " + game.homeTeam + " : " + game.awayTeam);
      }
	      		 
      return true;
   }

   public NflGameMetric findMetric(String metricName) {
      NflGameMetric requestedMetric = null;
      for (int mi=0; mi < metrics.size(); mi++) {
    	  NflGameMetric gameMetric = metrics.get(mi);
          if (gameMetric.metricName.equalsIgnoreCase(metricName)) {
        	  requestedMetric = gameMetric;
        	  break;
          }
      }
      
      return requestedMetric;
   }
   
   
   
   /*Comparator for sorting the list by score */
   public static Comparator<NflGameSchedule> GameScheduleComparatorByHomeTeam = new Comparator<NflGameSchedule>() {

      public int compare(NflGameSchedule gs1, NflGameSchedule gs2) {
          String homeTeam1Name = gs1.homeTeamSchedule.team.teamName;
          String homeTeam2Name = gs2.homeTeamSchedule.team.teamName;
       
	     //ascending order
	     int returnStatus = homeTeam1Name.compareTo(homeTeam2Name);

	     return returnStatus;
       }
    };
   
   /*Comparator for sorting the list by score */
   public static Comparator<NflGameSchedule> GameScheduleComparatorByScore = new Comparator<NflGameSchedule>() {

      public int compare(NflGameSchedule gs1, NflGameSchedule gs2) {
         Double gs1Score = gs1.score;
         Double gs2Score = gs2.score;
         
         Integer gs1HardViolationCount = gs1.hardViolationCount;
         Integer gs2HardViolationCount = gs2.hardViolationCount;

   	    //ascending order by hard violation count - prefer less hard violation counts
        int returnStatus = gs1HardViolationCount.compareTo(gs2HardViolationCount);

         if (returnStatus == 0) {
            //ascending order, prefer lower score (penalty)
	        returnStatus = gs1Score.compareTo(gs2Score);
	        //System.out.println("Sort Compare status : " + returnStatus + " for game1: " + gs1.game.homeTeam + " : " + gs1.game.awayTeam + ", score: " + gs1.score
            //	                                                      + ", vs game2: " + gs2.game.homeTeam + " : " + gs2.game.awayTeam + ", score: " + gs2.score);
         }

	     return returnStatus;
       }
    };
   
    /*Comparator for sorting the list by schedule Seqeunce */
    public static Comparator<NflGameSchedule> GameScheduleComparatorBySchedSequence = new Comparator<NflGameSchedule>() {

       public int compare(NflGameSchedule gs1, NflGameSchedule gs2) {
          Integer gs1SchedSequence = gs1.weekScheduleSequence;
          Integer gs2SchedSequence = gs2.weekScheduleSequence;
        
 	     //ascending order
  	     int returnStatus = gs1SchedSequence.compareTo(gs2SchedSequence);
 	     //System.out.println("Sort Compare status : " + returnStatus + " for game1: " + gs1.game.homeTeam + " : " + gs1.game.awayTeam + ", score: " + gs1.score
         //	                                                      + ", vs game2: " + gs2.game.homeTeam + " : " + gs2.game.awayTeam + ", score: " + gs2.score);

 	     return returnStatus;
        }
     };
     
     /*Comparator for sorting the list by demotion info */
     public static Comparator<NflGameSchedule> GameScheduleComparatorByDemotion = new Comparator<NflGameSchedule>() {

        public int compare(NflGameSchedule gs1, NflGameSchedule gs2) {
           Double gs1DemotionPenalty = gs1.demotionPenalty;
           Double gs2DemotionPenalty = gs2.demotionPenalty;
         
  	     //descending order
  	     int returnStatus = gs2DemotionPenalty.compareTo(gs1DemotionPenalty);
  	     //System.out.println("Sort Compare status : " + returnStatus + " for game1: " + gs1.game.homeTeam + " : " + gs1.game.awayTeam + ", score: " + gs1.score
          //	                                                      + ", vs game2: " + gs2.game.homeTeam + " : " + gs2.game.awayTeam + ", score: " + gs2.score);

  	     return returnStatus;
         }
      };
      
      /*Comparator for sorting the list by demotion info */
      public static Comparator<NflGameSchedule> GameScheduleComparatorByUnscheduledByes = new Comparator<NflGameSchedule>() {

         public int compare(NflGameSchedule gs1, NflGameSchedule gs2) {
            Double gs1score = gs1.score;
            Double gs2score = gs2.score;
            Integer gs1UnschedByeCount = gs1.unscheduledByes.size();
            Integer gs2UnschedByeCount = gs2.unscheduledByes.size();
            Integer gs1WeekNum = gs1.weekNum;
            Integer gs2WeekNum = gs2.weekNum;
            
          
            int returnStatus = 0;
            
      	    //descending order by unschedule bye count - prefer more unscheduled byes
            returnStatus = gs2UnschedByeCount.compareTo(gs1UnschedByeCount);
            
            if (returnStatus == 0) {
            	// prefer unscheduled game over scheduled game
                returnStatus = gs1WeekNum.compareTo(gs2WeekNum);
            }
            
            if (returnStatus == 0) {
            	// then, descending order by score (penalty) - prefer a higher penalty (less attractive game)
                returnStatus = gs2score.compareTo(gs1score);
            }

            return returnStatus;
         }
      };
      
      public static Comparator<NflGameSchedule> GameScheduleByeComparatorByByeCandidateScore = new Comparator<NflGameSchedule>() {

          public int compare(NflGameSchedule gsbye1, NflGameSchedule gsbye2) {
          	 Integer gsBye1CandidateScore = gsbye1.byeCandidateScore;
         	 Integer gsBye2CandidateScore = gsbye2.byeCandidateScore;
        	 
             int returnStatus = 0;

       	    // Descending order by byeCandidateScore
             returnStatus = gsBye2CandidateScore.compareTo(gsBye1CandidateScore);
                          
             return returnStatus;
          }
       };

}
