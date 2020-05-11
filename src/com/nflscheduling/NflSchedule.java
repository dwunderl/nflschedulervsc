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
   public int byesToScheduleThisWeek;
   public double latestScheduleFingerPrint;
   public static Random rnd = new Random();

   // Nfl Schedule Object
   //    Models an instance of a schedule
   //    Owns an array of team schedules
   //    Owns a list of unscheduled Games
   //    We model a scheduled game by ... each NflTeamSchedule holds a list of scheduledGames (NflGameSchedule)
   //    allGames
   
   NflSchedule() {
      //System.out.println("Creating an nflSchedule");
   }

   public boolean init(ArrayList<NflTeam> teams, 
		               ArrayList<NflGame> games,
		               ArrayList<NflResource> resources) {
	   
	   
	   teamSchedules = new ArrayList<NflTeamSchedule>();
      allGames = new ArrayList<NflGameSchedule>();
      unscheduledGames = new ArrayList<NflGameSchedule>();
      unscheduledByes = new ArrayList<NflGameSchedule>();
      resourceSchedules = new ArrayList<NflResourceSchedule>();
      scheduleMetrics = new ArrayList<NflScheduleMetric>();
      alerts = new ArrayList<NflScheduleAlert>();
      enableAlerts = false;

      NflSMetNoRepeatedMatchup metricNRM = new NflSMetNoRepeatedMatchup("NoRepeatedMatchup");
      scheduleMetrics.add(metricNRM);
      NflSMetRoadTripLimit metricRTL = new NflSMetRoadTripLimit("RoadTripLimit");
      scheduleMetrics.add(metricRTL);
      NflSMetHomeStandLimit metricHSL = new NflSMetHomeStandLimit("HomeStandLimit");
      scheduleMetrics.add(metricHSL);
      NflSMetDivisionalSeparation metricDS = new NflSMetDivisionalSeparation("DivisionalSeparation");
      scheduleMetrics.add(metricDS);
      NflSMetDivisionalWeekLimits metricDWL = new NflSMetDivisionalWeekLimits("DivisionalWeekLimits");
      scheduleMetrics.add(metricDWL);
      NflSMetDivisionalStart metricDivStart = new NflSMetDivisionalStart("DivisionalStart");
      scheduleMetrics.add(metricDivStart);
      NflSMetDivisionalBalance metricDivBal = new NflSMetDivisionalBalance("DivisionalBalance");
      scheduleMetrics.add(metricDivBal);
      
      //NflSMetBalancedHomeAway metricBalHA = new NflSMetBalancedHomeAway("Balanced Home Away", this);
      //scheduleMetrics.add(metricBalHA);

	  createTeamSchedules(teams);
	  createGameSchedules(games);
	  createResourceSchedules(resources);
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
   
   public boolean createGameSchedules(ArrayList<NflGame> baseGames) {
      for (int gi=0; gi < baseGames.size(); gi++) {
         NflGame game = baseGames.get(gi);
			     
         NflGameSchedule gameSchedule = new NflGameSchedule(game, this);
         if (gameSchedule.isBye) {
            gameSchedule.initBye();
            unscheduledByes.add(gameSchedule);
         }
         else {
            gameSchedule.initGame();
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

    	   // Add awayteam bye to hometeam bye opponentByes
    	   // Add hometeam bye to awwayteam bye opponentByes
     	  homeTeamBye.opponentByes.add(awayTeamBye);
     	  awayTeamBye.opponentByes.add(homeTeamBye);
       }
              
      // debug dump of the fully populate byes with their opponent byes
       /*
 	  for (NflGameSchedule byeGame: this.unscheduledByes) {
 	      System.out.println("Bye for team: " + byeGame.homeTeamSchedule.team.teamName + ", weekNum: " + byeGame.weekNum + ", opponentBye length: " + byeGame.opponentByes.size());
 	 	  for (NflGameSchedule opponentByeGame: byeGame.opponentByes) {
 	 	      System.out.println("   Opponent Bye for team: " + opponentByeGame.homeTeamSchedule.team.teamName + ", weekNum: " + opponentByeGame.weekNum + ", opponentBye length: " + opponentByeGame.opponentByes.size());
 		  }
	  }
	  */

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
   
   public int byeCounts(int weekNum) {
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

   public boolean determineNumByesForWeek(int weekNum) {
	   // byesToScheduleThisWeek - set it
	   // remaining byeCapacity in resource "Bye" from weeknum back
	   
	   int remainingByesToSchedule = unscheduledByes.size();
	   this.byesToScheduleThisWeek = 0;

	   if (remainingByesToSchedule == 0) {
		   return true;
	   }

	   int remainingByeCapacity = 0;
	   int remainingByeMin = 0;
	   NflResourceSchedule byeResourceSchedule = findResource("Bye");
	   
       if (byeResourceSchedule == null) {
          return true;
       }
       
       // Short circuit (temporary): exactly specified number for this week
       // TBD: Doesnt account for forced byes in the schedule
   
       // determine remaining bye capacity based on the specified bye resource max 
       // reduced by the number of byes already scheduled
       
      int sDir = NflDefs.schedulingDirection;
      int weekEnd = NflDefs.numberOfWeeks;

      if (sDir == -1) {
         weekEnd = 1;
      }
      else if (sDir == 1) {
         weekEnd = NflDefs.numberOfWeeks;
      }
      
      // Accumulated over the remaining unscheduled weeks
      for (int wi=weekNum; wi*sDir <= weekEnd*sDir; wi += sDir) {
	      remainingByeCapacity += byeResourceSchedule.resource.weeklyLimit[wi-1] - byeResourceSchedule.usage[wi-1];
	      remainingByeMin += byeResourceSchedule.resource.weeklyMinimum[wi-1] - byeResourceSchedule.usage[wi-1];
	   }
	   
	   // Determine number of byes already scheduled (forced) in this week
	   // Loop through all of the teamschedules, and check
	   
	   int byesScheduledThisWeek = 0;
	   
	   for (NflTeamSchedule teamSched: teamSchedules) {
		   NflGameSchedule gameInThisWeek = teamSched.scheduledGames[weekNum-1];
		   if (gameInThisWeek != null && gameInThisWeek.isBye) {
              byesScheduledThisWeek++;
		   }
	   }
	   
	   // Determine the min and the max possible byes for this week
      int min = Math.max(byeResourceSchedule.resource.weeklyMinimum[weekNum-1] - byesScheduledThisWeek, 0);
	   int max = Math.max(byeResourceSchedule.resource.weeklyLimit[weekNum-1] - byesScheduledThisWeek, 0);
	   
      int adjustedMin = Math.max(byeResourceSchedule.resource.weeklyMinimum[weekNum-1] - byesScheduledThisWeek, 
                                  remainingByesToSchedule - (remainingByeCapacity - byeResourceSchedule.resource.weeklyLimit[weekNum-1]));
	   int adjustedMax = Math.min(byeResourceSchedule.resource.weeklyLimit[weekNum-1] - byesScheduledThisWeek, 
                                  remainingByesToSchedule - (remainingByeMin - byeResourceSchedule.resource.weeklyMinimum[weekNum-1]));

	   if (remainingByesToSchedule > remainingByeCapacity) {
	       System.out.println("   ERROR determineNumByesForWeek: For week: " + weekNum + " insufficient bye capacity: " + remainingByeCapacity + ", remainingByesToSchedule: " + remainingByesToSchedule);
	       //ERROR determineNumByesForWeek: For week: 4 insufficient bye capacity: 6.0, remainingByesToSchedule: 8.0
	   }
	   
	   // TBD: May need to adjust min so that downstream has capacity for the remaining byes
	   // i.e. maybe consider remainingByeCapacity (Max), remainingByeMin (don't compute yet), remainingByesToSchedule, 
	   // remainingByeCapacity (Max) - remainingByeMin vs remainingByesToSchedule
	   if (remainingByesToSchedule >= remainingByeCapacity) {
		   byesToScheduleThisWeek = max;
		   return true;
	   }
	   
	    // double excessAvailRatio = (remainingByeCapacity-remainingByesToSchedule)/remainingByeCapacity;
       double randomNum  = rnd.nextDouble();
       double useMaxByeCapacityProb = randomNum;
       //double useMaxByeCapacityProb = excessAvailRatio*randomNum;
       
       //useMaxByeCapacityProb = 1.0 - useMaxByeCapacityProb;
       //useMaxByeCapacityProb = randomNum;

       //byesToScheduleThisWeek = (int) (useMaxByeCapacityProb*(max - min) + min);
       byesToScheduleThisWeek = (int) (useMaxByeCapacityProb*(adjustedMax - adjustedMin) + adjustedMin);
	   
	   for (int bc=min; bc <= max; bc += 2) {
		   if (byesToScheduleThisWeek <= bc) {
			   byesToScheduleThisWeek = bc;
			   break;
		   }
	   }
	   
	   if (byesToScheduleThisWeek > 0) {
          // System.out.println("   For week: " + weekNum + " byesToScheduleThisWeek: " + byesToScheduleThisWeek);
	   }

	   return true;
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

}
