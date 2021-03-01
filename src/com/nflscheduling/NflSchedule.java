package com.nflscheduling;

import java.util.*;
// import java.io.IOException;

public class NflSchedule {

   // ---
   // Instance data

   public ArrayList<NflTeamSchedule> teamSchedules;
   public ArrayList<NflResourceSchedule> resourceSchedules;
   public ArrayList<NflGameSchedule> allGames;
   public ArrayList<NflGameSchedule> unscheduledGames;
   public ArrayList<NflGameSchedule> unscheduledByes;
   public ArrayList<NflScheduleMetric> scheduleMetrics;
   public ArrayList<NflScheduleAlert> alerts;
 
   public double score = 0.0;
   public int hardViolationCount = 0;
   public String hardViolations = "";
   public boolean enableAlerts = false;
   public double latestScheduleFingerPrint;
   public static Random rnd = new Random();

   // Nfl Schedule Object
   //    Models an instance of a schedule
   //    Owns an array of team schedules
   //    Owns a list of unscheduled Games
   //    We model a scheduled game by ... each NflTeamSchedule holds a list of scheduledGames (NflGameSchedule)
   //    allGames
   
   NflSchedule() {
   }

   static double factorial(int N) 
   { 
       // Initialize result 
       double f = 1; 
 
       // Multiply f with 2, 3, ...N 
       for (int i = 2; i <= N; i++) 
           f *= i;
 
       return f; 
   } 

   public boolean init(ArrayList<NflTeam> teams, 
		               ArrayList<NflGame> games,
                     ArrayList<NflResource> resources,
                     LinkedHashMap<String,NflScheduleMetric> sMetricsToUse,
                     LinkedHashMap<String,NflGameMetric> gMetricsToUse) throws CloneNotSupportedException{
	   teamSchedules = new ArrayList<NflTeamSchedule>();
      allGames = new ArrayList<NflGameSchedule>();
      unscheduledGames = new ArrayList<NflGameSchedule>();
      unscheduledByes = new ArrayList<NflGameSchedule>();
      resourceSchedules = new ArrayList<NflResourceSchedule>();
      scheduleMetrics = new ArrayList<NflScheduleMetric>();
      alerts = new ArrayList<NflScheduleAlert>();
      enableAlerts = false;

	  createTeamSchedules(teams);
	  createGameSchedules(games,gMetricsToUse);
     createResourceSchedules(resources);
     createScheduleMetrics(sMetricsToUse);
	  populateOpponentByes();
	  return true;
   }
   
   public boolean createTeamSchedules(ArrayList<NflTeam> baseTeams) {
	  for (int ti=0; ti < baseTeams.size(); ti++) {
	     NflTeam team = baseTeams.get(ti);
	     
	     NflTeamSchedule teamSchedule = new NflTeamSchedule(team);
	     teamSchedules.add(teamSchedule);
	  }

      return true;
   }
   
   public boolean createGameSchedules(ArrayList<NflGame> baseGames, 
                                      LinkedHashMap<String,NflGameMetric> gameMetrics) 
                                                             throws CloneNotSupportedException {
      for (int gi=0; gi < baseGames.size(); gi++) {
         NflGame game = baseGames.get(gi);
			     
         NflGameSchedule gameSchedule = new NflGameSchedule(game, this);
         if (gameSchedule.isBye) {
            gameSchedule.initBye();
            unscheduledByes.add(gameSchedule);
         }
         else {
            gameSchedule.initGame(gameMetrics);
            unscheduledGames.add(gameSchedule);
            allGames.add(gameSchedule);
         }
      }

      return true;
   }
  
   public boolean createResourceSchedules(ArrayList<NflResource> baseResources) {
      for (int ri=0; ri < baseResources.size(); ri++) {
	     NflResource resource = baseResources.get(ri);    
	     NflResourceSchedule resourceSchedule = new NflResourceSchedule(resource, this);
	     resourceSchedules.add(resourceSchedule);
	  }

	  return true;
   }

   public boolean populateOpponentByes() {
	   // Want each bye to have a list of opponent byes, and some sense of multiplicity
	   // how to efficiently do this
	   // - Sort games by hometeam
	   // - loop through games in order (test this)
	   //     - When encounter a change in hometeam (test)
	   //         - currentBye = bye of hometeam (find it in unscheduled byes)
	   //     - find Bye of opponent team and add to opponent byes 
	   //     - (See if multiplicity can be implemented - e.g. broncos play chargers twice)
	   // - end loop : Each unscheduled Bye should have an array of opponent byes (with multiplicity)
	   // when we want to schedule byes we want to 
	   
	   // Prefer games with multiple unscheduled byes vs 1 or 0
       //Collections.sort(allGames, NflGameSchedule.GameScheduleComparatorByHomeTeam);
       // Choose the best games into a collection and choose randomly
       
       // String curHomeTeamName = null;
       // NflTeamSchedule curHomeTeam = null;
       // NflGameSchedule curBye = null;
       
       for(NflGameSchedule usgame: allGames) {
           NflGameSchedule homeTeamBye = null;
           NflGameSchedule awayTeamBye = null;

    	  // find hometeam bye
    	  // find awayteam bye
     	  for (NflGameSchedule byeGame: this.unscheduledByes) {
    		  if (byeGame.homeTeamSchedule == usgame.homeTeamSchedule) {
    			  homeTeamBye = byeGame;
    			  if (awayTeamBye != null) break;
    		  }
    		  else if(byeGame.homeTeamSchedule == usgame.awayTeamSchedule) {
    			  awayTeamBye = byeGame;
    			  if (homeTeamBye != null) break;
    		  }
    	  }

     	  homeTeamBye.opponentByes.add(awayTeamBye);
     	  awayTeamBye.opponentByes.add(homeTeamBye);
       }
              
	   return true;
   }

   /*
   public boolean createByeSchedules() {

      // TBD:Byes
      // create a bye/gameschedule for each pair of teams according NflDefs.numberOfTeams/2
      // keep a collection of the byes
      // create an empty NflGame for each bye gameSchedule - to hold the assigned teams
	  for (int ti=0; ti < teams.size()/2; ti++) {
		  NflGame game = new NflGame();
		  NflGameSchedule bye = new NflGameSchedule(game, this);
		  bye.initBye();
		  unscheduledByes.add(bye);
	  }

      return true;
   }
*/
   public NflTeamSchedule findTeam(String teamName) {
	  if (teamName == null) return null;
	  
      for (int ti=0; ti < teamSchedules.size(); ti++) {
         NflTeamSchedule teamSchedule = teamSchedules.get(ti);
         if (teamName.equalsIgnoreCase(teamSchedule.team.teamName)) {
            return teamSchedule;
         }
      }

      return null;
   }

   public NflResourceSchedule findResource(String attrName) {
      for (int ali=0; ali < resourceSchedules.size(); ali++) {
         NflResourceSchedule resourceSchedule = resourceSchedules.get(ali);
         if (attrName.equalsIgnoreCase(resourceSchedule.resource.resourceName)) {
            return resourceSchedule;
         }
      }

      return null;
   }

   public boolean resourceExists(String resourceName) {
      NflResourceSchedule resourceSchedule = findResource(resourceName);
      return resourceSchedule != null;
   }

   public boolean resourceHasCapacity(String resourceName, int weekNum) {
      NflResourceSchedule resourceSchedule = findResource(resourceName);
      if (resourceSchedule != null) {
          if (resourceSchedule.resource.weeklyLimit[weekNum-1] > 0) {
            return true;
         }
      }

      return false;
   }
   
   public int scheduledTeamsInWeek(int weekNum) {
	  int gameCount = 0;
	  for (int ti=0; ti < teamSchedules.size(); ti++) {
	     NflTeamSchedule teamSchedule = teamSchedules.get(ti);
	     if (teamSchedule.scheduledGames[weekNum-1] != null) {
            gameCount++;
	     }
	  }
	   
	  return gameCount;
   }

   public int unscheduledTeamsInWeek(int weekNum, ArrayList<NflTeamSchedule> unscheduledGames) {
	  int gameCount = 0;
	  for (int ti=0; ti < teamSchedules.size(); ti++) {
	     NflTeamSchedule teamSchedule = teamSchedules.get(ti);
	     if (teamSchedule.scheduledGames[weekNum-1] == null) {
	    	    if (!teamSchedule.hasScheduledBye()) {
               gameCount++;
               unscheduledGames.add(teamSchedule);
	    	    }
	     }
	  }
	   
	  return gameCount;
  }

   public boolean computeMetrics() {
      score = 0.0;
      hardViolationCount = 0;
      
      enableAlerts = true;
      for (int mi=0; mi < scheduleMetrics.size(); mi++) {
         NflScheduleMetric scheduleMetric = scheduleMetrics.get(mi);
         //System.out.println("Computing Metric: " + gameMetric.metricName + " for game: " + game.homeTeam + " : " + game.awayTeam);
         scheduleMetric.computeMetric(this);
         score += scheduleMetric.score;
         if (scheduleMetric.hardViolation) {
        	 hardViolationCount++;
        	 hardViolations = hardViolations + ";" + scheduleMetric.metricName;
         }
         //System.out.println("Computing ScheduleMetric: " + scheduleMetric.metricName + " score: " + scheduleMetric.score);
      }
      
      return true;
   }
   
   public int byeCapacityAvail(int weekNum) {
	   NflResourceSchedule byeResource = findResource("Bye");
       int byeAvail = byeResource.resource.weeklyLimit[weekNum-1] - byeResource.usage[weekNum-1];
       return byeAvail;
   }
   
   /*
    * 	AdjustedMin = 
		max(
			remByesToSched - (RemByeMax - MaxByes) - ForcedByes(usage),    
			MinByes - ForcedByes(usage))
	AdjustedMax = 
		Min(
			remByesToSched - (RemByeMin - MinByes) - ForcedByes(usage),
			MaxByes - ForcedByes(usage))

    * 
    */
   
   public int byeCount(int weekNum) {
	   int byeCount = 0;
	   
       for (NflTeamSchedule teamSchedule: teamSchedules) {
          if (teamSchedule.scheduledGames[weekNum-1] != null) {
             NflGameSchedule scheduledGame = teamSchedule.scheduledGames[weekNum-1];
             if (scheduledGame.isBye) {
            	 byeCount++;
             }
          }
       }
        
       return byeCount;
   }

   public int forcedCount(int weekNum) {
	   int forcedCount = 0;
	   
       for (NflTeamSchedule teamSchedule: teamSchedules) {
          if (teamSchedule.scheduledGames[weekNum-1] != null) {
             NflGameSchedule scheduledGame = teamSchedule.scheduledGames[weekNum-1];
             if (scheduledGame.game.homeTeam.equalsIgnoreCase(teamSchedule.team.teamName) && !scheduledGame.isBye) {
               if (scheduledGame.restrictedGame) {
                  forcedCount++;
                }
             }
          }
       }
        
       return forcedCount;
   }

   //chosenCounts
   public int chosenCount(int weekNum) {
	   int chosenCount = 0;
	   
       for (NflTeamSchedule teamSchedule: teamSchedules) {
          if (teamSchedule.scheduledGames[weekNum-1] != null) {
             NflGameSchedule scheduledGame = teamSchedule.scheduledGames[weekNum-1];
             if (scheduledGame.game.homeTeam.equalsIgnoreCase(teamSchedule.team.teamName) && !scheduledGame.isBye) {
               if (!scheduledGame.restrictedGame) {
                  chosenCount++;
                }
             }
          }
       }
        
       return chosenCount;
   }

   //choices
   public double choicesCount(int weekNum) {
      double choicesCount = 1;
      int chosenGameCount = 0;
	   
       for (NflTeamSchedule teamSchedule: teamSchedules) {
          if (teamSchedule.scheduledGames[weekNum-1] != null) {
             NflGameSchedule scheduledGame = teamSchedule.scheduledGames[weekNum-1];
             if (scheduledGame.game.homeTeam.equalsIgnoreCase(teamSchedule.team.teamName) && !scheduledGame.isBye) {
               if (!scheduledGame.restrictedGame) {
                  choicesCount *= scheduledGame.candidateCount;
                  chosenGameCount++;
                  System.out.println("Week:" + weekNum + "team: "  + teamSchedule.team.teamName + 
                  " candidateCount:" + scheduledGame.candidateCount + ", chosenGameCount:" + chosenGameCount);
                }
             }
          }
       }
       System.out.println("Week:" + weekNum + ", choicesCount: " + choicesCount + ", chosenGameCount: " + chosenGameCount + 
       ", factorial(chosenGameCount): " + factorial(chosenGameCount) + ", cc/fact: " + choicesCount/factorial(chosenGameCount));
       return choicesCount/factorial(chosenGameCount);
   }

   public int divisionalGameCount(int weekNum) {
	   int divisionalGameCount = 0;
	   
       for (NflTeamSchedule teamSchedule: teamSchedules) {
          if (teamSchedule.scheduledGames[weekNum-1] != null) {
             NflGameSchedule scheduledGame = teamSchedule.scheduledGames[weekNum-1];
             if (scheduledGame.game.findAttribute("division")) {
                divisionalGameCount++;
             }
          }
       }
    
       // must divide divisionalGameCount by 2 since each game appears twice in the week, once for each of the 2 teams
       divisionalGameCount = divisionalGameCount/2;
    
       return divisionalGameCount;
   }
   
   public boolean addAlert(NflScheduleAlert newAlert) {
       for(NflScheduleAlert alert: alerts) {
    	  if (alert.alertDescr.equalsIgnoreCase(newAlert.alertDescr)) {
    		  // newAlert is already in the collection, don't duplicate
    		  return true;
    	  }
       }
	   alerts.add(newAlert);

	   return true;
   }

   public boolean createScheduleMetrics(LinkedHashMap<String, NflScheduleMetric> sMetricsToUse) throws CloneNotSupportedException {
      for (Map.Entry<String, NflScheduleMetric> scheduleMetricEntry : sMetricsToUse.entrySet()) {
         NflScheduleMetric scheduleMetric = scheduleMetricEntry.getValue();
         NflScheduleMetric newScheduleMetric = (NflScheduleMetric) scheduleMetric.clone();
         scheduleMetrics.add(newScheduleMetric);
      }
      return true;
   }
}
