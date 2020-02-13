package com.nflscheduling;

import java.util.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;


//import java.io.IOException;

public class NflScheduler {

   // Algorithm Top Level is in the static main function
   // curSchedule holds the partial schedule (so far)
	
   // Instance data

   public ArrayList<NflSchedule> schedules;        // schedule[i].teams holds the partial schedule
	   
   public NflSchedule curSchedule;                 // curSchedule.teams holds the partial schedule
                                                   // Each team has an array of scheduled games - 1 per week
                                                   // Byes are scheduled games marked with isBye=true
                                                   // Holds arrays of allGames, unscheduledGames, unscheduledByes
   
  
   public ArrayList<NflRestrictedGame> restrictedGames; // The restrictedGames are games forced into fixed/specified weeks
                                                        // Some are pre-defined in file nflforcedgames.csv
   
   public ArrayList<NflResource> resources;
   public ArrayList<NflGame> games;    // base class instances of games - to be turned in NflGameSchedule instances
                                       // loaded from nflgames.csv within function loadGames()
   
   public ArrayList<NflTeam> teams;    // base class instances of teams - to be turned into NflTeamSchedule instances
                                       // loaded from nflteams.csv within function loadTeams()
   
   public int reschedWeekNum = 0;      // TBD: Not set or used, except as a final logging output (in this file)
                                       // could be removed
      
   // reschedLog - log of each reschedule event - when fail to complete a week and need to backtrack
   //            - currently 2 log lines for each reschedule - one line to indicate the level of backtrack, second line provides more detailed reschedule history info
   //            - ought to divide reschedLog lines by 2 to get number of failures requiring backtracking - when logging "Rescheduled Weeks:"
   // 
   
   public ArrayList<String> reschedLog;

   public int reschedAttemptsSameWeek;       // retries in same week before giving up and going back 1 week
                                                    // unschedule the failed week, demote a game and retry the week
   
   public int reschedAttemptsOneWeekBack;    // retries 1 week back before going back multiple weeks
                                                    // unschedule the failed week and previous week, demote a game and retry the previous week
   
   public int reschedAttemptsMultiWeeksBack; // retries multiple weeks back before giving up completely
                                                    // unschedule the failed week and series of weeks, demote a game and retry the earliest unsched week
   public String terminationReason;
   
   //  Demotion Scheme: TBD document it
   //  Promotion scheme is not used - promotionInfo
   
   /* Issues identified by Ted
    * Can't have a back to back matchup with only a bye in between, has to have at least one game for both teams 
    *    Streamline by checking for - if (!usgame.game.findAttribute("division")) {
    *    Only divisional games can have repeat matchups

    *    Basic functionality in NflGMetNoRepeatedMatchup, turned on sched metric for alert if repeated matchup
    *    Could be a hole, due to a forced game where a bye could slip in: 
    *    forcedGame  Hole  NewGame scheduled - then bye slips into hole, could fix within bye scheduling
    * Divisional teams shouldn't play each other twice in the first 5 weeks of the season, they should wait until at least week 6 for a rematch 
    *    probably need a new game metric to push for this, then a schedule metric
    *           if (!usgame.game.findAttribute("division")) {
    *           NflGMetDivisionalSeparation
    * Byes should start no later than week 5. byes in weeks 4,12, 13 can be optional - now works, Bye resource can specify this
    *                   
    */

   public Random rnd = new Random();
   
   // logging of algorithm progress and histories
   public int lowestWeekNum = 1000;         
   public int iterNum = 0;
   
   // partialSchedules
   //    Array season weeks - summarizes the partial schedule for each scheduled week so far
   //    Each partial schedule entry has the fingerprint (# summary) sum of all the weighted fingerprints for each week
   //    Also has iterNum, weekNum, unscheduledTeams (?)
   
   public NflPartialScheduleEntry[] partialSchedules;
   
   // fingerPrintMap
   //    Keeps track of each unique full or partial scheduled week as a partial schedule fingerprint and a repeat count
   //    one entry for every unique scheduled week (full or partial)
   //    if we encounter the same schedule more than once, we increment the count in the existing entry, fail the week
   //    So iterations (iterNum) - reschedule events (reschedLog/2) (failed+repeated fp) = unique partial schedule fingerprints (in collection)
   //    TBD: validate that last calculation - understand it deeply
   //    Iterations = all attempts to schedule a week
   //    Reschedule events - all weeks that don't completely schedule or completely schedule but have repeated fingerPrint
   //    unique partial schedule fingerprints = fully scheduled weeks with a unique fingerprint - repeats are treated as failures
   //    Create Log File: logPartialScheduleResults.csv
   //    iteration start (scheduleUnrestrictedWeek): iterNum++
   //       Successfully schedule a week: call logPartialScheduleHistory
   //           Add new or updated entry (count increment) to fingerPrintMap - may not increase the number of entries
   //           Fail the week if it has a repeated FP
   //       Fail to complete the weeks schedule: call logPartialScheduleHistory
   //           Don't add new or updated entry to fingerPrintMap, unnecessarily calculates partialScheduleEntry
   //           and unnecessarily updates schedule.latestScheduleFingerPrint

   //public HashMap<Double, Integer> fingerPrintMap = new HashMap<Double, Integer>();
   public HashMap<Double, NflPartialScheduleEntry> fingerPrintMap;

   public int fpSkipCount = 0;   // Counts number of repeated encounters of a FingerPrint after completing a fully scheduled week
                                        // Each encounter is failed, causing a backtrack rescheduling attempt
   
   // unscheduledTeams - after failure of a week, the unscheduledTeams in that week are given first priority during the next weekly schedule
   // TBD: study and rethink if this really makes any real sense
   
   public ArrayList<NflTeamSchedule> unscheduledTeams = new ArrayList<NflTeamSchedule>();

   public BufferedWriter briefLogBw = null;
   public FileWriter briefLogFw = null;
   public BufferedWriter partialScheduleLogBw = null;
   public FileWriter partialScheduleLogFw = null;
   public BufferedWriter schedAttemptsLogBw = null;
   public FileWriter schedAttemptsLogFw = null;
   
   
   public boolean init() {
      loadParams();                                 // load from nflparams.csv: NflDefs.numberOfWeeks, NflDefs.numberOfTeams
      // resched limit params are hard-coded in here, TBD: should get from a file
      games = new ArrayList<NflGame>();
      teams = new ArrayList<NflTeam>();
      resources = new ArrayList<NflResource>();

      loadTeams(teams); // base teams created globally in NflScheduler
      loadGames(games); // base games created globally in NflScheduler
      loadResources();
      partialSchedules = new NflPartialScheduleEntry[NflDefs.numberOfWeeks];

      restrictedGames = new ArrayList<NflRestrictedGame>();
      if (!loadForcedGames(restrictedGames))
      {
         System.out.println("ERROR loading restricted games");
         System.exit(1);
      }

      schedules = new ArrayList<NflSchedule>();
      
      // ----------- debug output -----------------------------
      System.out.println("Creating new nflScheduler");
      System.out.println("numberOfWeeks: " + NflDefs.numberOfWeeks);
      System.out.println("games size is: " + games.size());
      System.out.println("restrictedGames size is: " + restrictedGames.size());
      // will need to create each nflGame object and put into the array

      // Dump loaded and distributed team data
      System.out.println("games length is: " + games.size());
      System.out.println("restrictedGames length is: " + restrictedGames.size());

      return true;
   }
   
   public boolean scheduleInit() {
      curSchedule = new NflSchedule();
	  curSchedule.init(teams, games, resources);
	      
	  // Debug output for initial schedule basic structural info
	  // ------------------------------------------------------
	  //System.out.println("curSchedule.teams size is: " + curSchedule.teamSchedules.size());
	      
	  //for (int i=0; i < curSchedule.teamSchedules.size(); i++) {
	     //NflTeamSchedule teamSchedule = curSchedule.teamSchedules.get(i);
	     //System.out.println("team: " + i + ", " + teamSchedule.team.teamName);
	  //}

	  // Dump the resource data
	  //System.out.println("attr limit length is: " + curSchedule.resourceSchedules.size());
	  //System.out.println("attr limit (1) intervals length is: " + resources.get(0).weeklyLimit.length);
	  //System.out.println("attr limit[1].weeklyLimit[1] is: " + resources.get(0).weeklyLimit.length);
	  //System.out.println("attr limit[1].weeklyLimit[numberOfWeeks] is: " + resources.get(0).weeklyLimit.length);
	  // ------------------------------------------------------

	  return true;
   }
   
   public boolean generateSchedules() {
      //---------- Schedule Initialization --------------------
      // create next curSchedule
	  // initialize unscheduledGames of the curSchedule from all the modeled games
      int scheduleAttempts;
      openSchedAttemptsLogFile();

	  for (scheduleAttempts = 1; scheduleAttempts < NflDefs.scheduleAttempts; scheduleAttempts++) {
         rnd = new Random();

	     scheduleInit();
	
         // Schedule games that are restricted - according to the restrictedGames
         scheduleForcedGames(restrictedGames, curSchedule);
		  
         reschedLog = new ArrayList<String>();
         openBriefLogFile();
         openPartialScheduleLogFile();
         iterNum = 0;
         fingerPrintMap = new HashMap<Double, NflPartialScheduleEntry>();
         fpSkipCount = 0;

         // Schedule the remaining unrestricted games
         scheduleUnrestrictedGames(curSchedule);
	
         closeBriefLogFile();
         closePartialScheduleLogFile();
         String savedScheduleFileName = new String();

         if (curSchedule.unscheduledGames.size() == 0) {
            curSchedule.computeMetrics();
            terminationReason = "Schedule Metric: " + curSchedule.score + " Alerts: " + curSchedule.alerts.size() + " hard violations: " + curSchedule.hardViolationCount + " vios: " + curSchedule.hardViolations;
            // if (curSchedule.alerts.size() <= NflDefs.alertLimit && curSchedule.hardViolationCount <= 1) {
            if (curSchedule.hardViolationCount <= NflDefs.hardViolationLimit && 
                curSchedule.alerts.size() <= NflDefs.alertLimit) {
               schedules.add(curSchedule);
               savedScheduleFileName = "curSchedule" + schedules.size() + ".csv";
               writeScheduleCsv(curSchedule, savedScheduleFileName);
               terminationReason += ", " + savedScheduleFileName;
            }
         }
         
	     System.out.println("Schedule: " + scheduleAttempts + ", iterations: " + iterNum + ", " + terminationReason);
	     
	     logSchedAttempt(scheduleAttempts, curSchedule, iterNum, lowestWeekNum, savedScheduleFileName);
	      
         if (schedules.size() >= NflDefs.savedScheduleLimit) {
            break; // hit the limit of saved schedules
         }
      }
	  
      closeSchedAttemptsLogFile();
	  	  
      return true;
   }

   public static void main(String[] args) {
      // Prints "Hello, World" in the terminal window.
      System.out.println("Hello, World");

      // ---------- Scheduler Initialization ------------------
      NflScheduler scheduler = new NflScheduler();
      
      scheduler.init();
 
      // ------------ Scheduling -------------
      
      scheduler.generateSchedules();
   }

   NflScheduler() {
   }

   // load the full set of games into the static global list of games
   // from the file nflgames.csv

   public boolean loadParams() {
      String csvFile = "nflparams.csv";
      BufferedReader br = null;
      String line = "";
      String cvsSplitBy = ",";

      try {
         br = new BufferedReader(new FileReader(csvFile));
         while ((line = br.readLine()) != null) {
            // use comma as separator
            String[] token = line.split(cvsSplitBy);
            
            if (token.length == 2) {
         	   if (token[0].equalsIgnoreCase("NumberOfWeeks")) {
                   NflDefs.numberOfWeeks = Integer.parseInt(token[1]);
                   if (NflDefs.numberOfWeeks <= 0 || NflDefs.numberOfWeeks > 30) {
                       System.out.println("loadParams: NumberOfWeeks invalid" + NflDefs.numberOfWeeks);
                   }
                   System.out.println("loadParams: NumberOfWeeks set to " + NflDefs.numberOfWeeks);
                }
         	   else if (token[0].equalsIgnoreCase("NumberOfTeams")) {
                   NflDefs.numberOfTeams = Integer.parseInt(token[1]);
                   if (NflDefs.numberOfTeams <= 0 || NflDefs.numberOfTeams > 50) {
                      System.out.println("loadParams: NumberOfTeams invalid" + NflDefs.numberOfTeams);
                   }
                   System.out.println("loadParams: NumberOfTeams set to " + NflDefs.numberOfTeams);
                }
         	   else if (token[0].equalsIgnoreCase("reschedAttemptsMultiWeeksBackLimit")) {
                   NflDefs.reschedAttemptsMultiWeeksBackLimit = Integer.parseInt(token[1]);
                   if (NflDefs.reschedAttemptsMultiWeeksBackLimit <= 0 || NflDefs.reschedAttemptsMultiWeeksBackLimit > 100) {
                      System.out.println("loadParams: reschedAttemptsMultiWeeksBackLimit invalid" + NflDefs.reschedAttemptsMultiWeeksBackLimit);
                   }
                   System.out.println("loadParams: reschedAttemptsMultiWeeksBackLimit set to " + NflDefs.reschedAttemptsMultiWeeksBackLimit);
                }
         	   else if (token[0].equalsIgnoreCase("reschedAttemptsOneWeekBackLimit")) {
                   NflDefs.reschedAttemptsOneWeekBackLimit = Integer.parseInt(token[1]);
                   if (NflDefs.reschedAttemptsOneWeekBackLimit <= 0 || NflDefs.reschedAttemptsOneWeekBackLimit > 100) {
                      System.out.println("loadParams: reschedAttemptsOneWeekBackLimit invalid" + NflDefs.reschedAttemptsOneWeekBackLimit);
                   }
                   System.out.println("loadParams: reschedAttemptsOneWeekBackLimit set to " + NflDefs.reschedAttemptsOneWeekBackLimit);
                }
         	   else if (token[0].equalsIgnoreCase("reschedAttemptsSameWeekLimit")) {
                   NflDefs.reschedAttemptsSameWeekLimit = Integer.parseInt(token[1]);
                   if (NflDefs.reschedAttemptsSameWeekLimit <= 0 || NflDefs.reschedAttemptsSameWeekLimit > 100) {
                      System.out.println("loadParams: reschedAttemptsSameWeekLimit invalid" + NflDefs.reschedAttemptsSameWeekLimit);
                   }
                   System.out.println("loadParams: reschedAttemptsSameWeekLimit set to " + NflDefs.reschedAttemptsSameWeekLimit);
                }
         	   else if (token[0].equalsIgnoreCase("scheduleAttempts")) {
                   NflDefs.scheduleAttempts = Integer.parseInt(token[1]);
                   if (NflDefs.scheduleAttempts <= 0 || NflDefs.scheduleAttempts > 2000) {
                      System.out.println("loadParams: scheduleAttempts invalid" + NflDefs.scheduleAttempts);
                   }
                   System.out.println("loadParams: scheduleAttempts set to " + NflDefs.scheduleAttempts);
                }
         	   else if (token[0].equalsIgnoreCase("savedScheduleLimit")) {
                   NflDefs.savedScheduleLimit = Integer.parseInt(token[1]);
                   if (NflDefs.savedScheduleLimit <= 0 || NflDefs.savedScheduleLimit > 100) {
                      System.out.println("loadParams: savedScheduleLimit invalid" + NflDefs.savedScheduleLimit);
                   }
                   System.out.println("loadParams: savedScheduleLimit set to " + NflDefs.savedScheduleLimit);
                }
         	   else if (token[0].equalsIgnoreCase("alertLimit")) {
                   NflDefs.alertLimit = Integer.parseInt(token[1]);
                   if (NflDefs.alertLimit < 0 || NflDefs.alertLimit > 100) {
                      System.out.println("loadParams: alertLimit invalid" + NflDefs.alertLimit);
                   }
                   System.out.println("loadParams: alertLimit set to " + NflDefs.alertLimit);
                }
         	    else if (token[0].equalsIgnoreCase("hardViolationLimit")) {
                   NflDefs.hardViolationLimit = Integer.parseInt(token[1]);
                   if (NflDefs.hardViolationLimit < 0 || NflDefs.hardViolationLimit > 100) {
                      System.out.println("loadParams: hardViolationLimit invalid" + NflDefs.hardViolationLimit);
                   }
                   System.out.println("loadParams: hardViolationLimit set to " + NflDefs.hardViolationLimit);
                }
            }
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         if (br != null) {
            try {
               br.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

     return true;
   }
   
   public boolean loadTeams(ArrayList<NflTeam> teams) {
      String csvFile = "nflteams.csv";
      BufferedReader br = null;
      String line = "";
      String cvsSplitBy = ",";

      try {
         br = new BufferedReader(new FileReader(csvFile));
         while ((line = br.readLine()) != null) {
            // use comma as separator
            String[] token = line.split(cvsSplitBy);
            
            if (token.length >= 1) {
               String teamName = token[0];
               NflTeam team = new NflTeam(teamName);
               teams.add(team);
               
               if (token.length > 1) {
                   team.conference = token[1];
                }    
                if (token.length > 2) {
                   team.division = token[2];
                }
                if (token.length > 3) {
                    double timezone = Double.parseDouble(token[3]);
                    team.timezone = timezone;
                 }    
                 if (token.length > 4) {
                    String stadium = token[4];
                    team.stadium = stadium;
                 }
            }
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         if (br != null) {
            try {
               br.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

     return true;
   }
   
   public boolean loadGames(ArrayList<NflGame> games) {
      String csvFile = "nflgames.csv";
      BufferedReader br = null;
      String line = "";
      String cvsSplitBy = ",";

      try {
         br = new BufferedReader(new FileReader(csvFile));
         while ((line = br.readLine()) != null) {
            // use comma as separator
            String[] token = line.split(cvsSplitBy);
            NflGame game = new NflGame();
            game.homeTeam = token[0];
            if (token[1].equalsIgnoreCase("bye")) {
               game.isBye = true;
            }
            else {
               game.awayTeam = token[1];
            }

            if (token.length > 2)
               game.attribute.add(token[2]);
            if (token.length > 3)
               game.attribute.add(token[3]);
            if (token.length > 4)
               game.attribute.add(token[4]);
            
            if (game.findAttribute("division")) {
               game.isDivisional = true;
            }
            if (game.findAttribute("international")) {
               game.isInternational = true;
            }

            //game.weekNum = 0;  // not scheduled yet

            games.add(game);

            //System.out.println("line token length: " + token.length);
            //System.out.println("Game: " + game.homeTeam + ":" + game.awayTeam);
            //for (int i=0; i < game.attribute.size(); i++)
            //   System.out.println("   " + game.attribute.get(i));
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         if (br != null) {
            try {
               br.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

     return true;
   }
  
   public boolean clearGameWeeknums(ArrayList<NflGame> games) {
      for (int gi=0; gi < games.size(); gi++) {
         //NflGame game = games.get(gi);
         //game.weekNum = 0;
      }   
      
      return true;
   }

   // load the full set of restrictedGames into the static global list of restrictedGames
   // from the file nflforcedgames.csv
   // Buccaneers,4,Mexico

   public boolean loadForcedGames(ArrayList<NflRestrictedGame> weeks) {
      String csvFile = "nflforcedgames.csv";
      BufferedReader br = null;
      String line = "";
      String cvsSplitBy = ",";

      try {
         br = new BufferedReader(new FileReader(csvFile));
         while ((line = br.readLine()) != null) {
            // use comma as separator
            String[] token = line.split(cvsSplitBy);

            if (token.length < 3) {
               System.out.println("Error Loading restricted games: less than 3 elements specified for line: " + line);
               return false;
            }
            
            String teamName = token[0];
            int weekNum = Integer.parseInt(token[1]);
            String restriction = token[2];
            String otherTeamName = "";
            String stadium = "";

            if (token.length > 3) {
               otherTeamName = token[3];
            }
            
            if (token.length > 4) {
         	   stadium = token[4];
            }
             
            if (teamName.equalsIgnoreCase("all"))
            {
                // for (int ti=0; ti < curSchedule.teams.size(); ti++) {
                for (int ti=0; ti < teams.size(); ti++) {
                  // NflTeamSchedule teamSchedule = curSchedule.teams.get(ti);
                  // NflRestrictedGame restrictedGame = new NflRestrictedGame(teamSchedule.team.teamName, weekNum, restriction, otherTeamName, stadium);
                  NflTeam team = teams.get(ti);
                  NflRestrictedGame restrictedGame = new NflRestrictedGame(team.teamName, weekNum, restriction, otherTeamName, stadium);
                  weeks.add(restrictedGame);

                  //System.out.println("line token length: " + token.length);
                  //System.out.println("Restricted game: week: " + weekNum + ", Team: " + team.teamName + ", restriction: " + restriction);
                  //System.out.println("Restricted game: week: " + weekNum + ", Team: " + teamSchedule.team.teamName + ", restriction: " + restriction);
               }
            }
            else {
               NflRestrictedGame restrictedGame = new NflRestrictedGame(teamName, weekNum, restriction, otherTeamName, stadium);
               weeks.add(restrictedGame);
               //System.out.println("line token length: " + token.length);
               //System.out.println("Week: " + restrictedGame.weekNum + ", Team: " + restrictedGame.teamName + ":");
            }
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
         if (br != null) {
            try {
               br.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

      return true;
   }

   // load the set of attribute limits from file nflattrlimits.csv into the base schedule
   // GiantsJetsStadium,0,1

   public boolean loadResources () {
      String csvFile = "nflresources.csv";
      BufferedReader br = null;
      String line = "";
      String cvsSplitBy = ",";

      try {
         br = new BufferedReader(new FileReader(csvFile));
         while ((line = br.readLine()) != null) {
            // use comma as separator
            String[] token = line.split(cvsSplitBy);

            String attrName = token[0];
            NflResource resource = null;
            for (int al=0; al < resources.size(); al++) {
               NflResource aLim = resources.get(al);
               if (aLim.resourceName.equalsIgnoreCase(attrName)) {
                  // System.out.println("aLim.attrName: " + aLim.resourceName + " == " + " attrname: " + attrName + ", so reusing");
                  resource = aLim;
                  break;
               }
            }

            if (resource == null) {
                //System.out.println("creating a new nflAttrLimit " + token[0]);
                resource = new NflResource();
                resources.add(resource);
            }

            resource.resourceName = token[0];
            // resource.weekNum = Integer.parseInt(token[1]);
            int weekNum = Integer.parseInt(token[1]);
            // System.out.println("    weekNum " + resource.weekNum);
            // System.out.println("    weekNum " + weekNum);
            // System.out.println("    weeklyLimit size " + resource.weeklyLimit.length);
            //attrLimit.weeklyLimit.set(attrLimit.weekNum-1,Integer.parseInt(token[2]));
            resource.weeklyLimit[weekNum-1] = Integer.parseInt(token[2]);
            resource.weeklyMinimum[weekNum-1] = 0;
            if (token.length > 3) {
               resource.weeklyMinimum[weekNum-1] = Integer.parseInt(token[3]);
            }

            // System.out.println("line token length: " + token.length);
            // System.out.println("AttrLimit: " + resource.resourceName + " : " + resource.weekNum + " : " + resource.weeklyLimit[resource.weekNum-1] + " : " + resource.weeklyMinimum[resource.weekNum-1]);
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         if (br != null) {
            try {
               br.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

     return true;
   }

   // To Do
   // Byes
   // Done: For testing - quit after scheduling restricted games - so can just see the restricted schedule
   // Done: Restricted byes - update the restrictedgames.csv with Ted's byes - rename the file to forcedgames.csv
   // Byes are stored in games - (1) add to games
   // Byes are the hometeam in a game, and have the isBye attribute set
   // When scheduling a restricted bye, look for the games that are byes with the hometeam matching - should only be one
   // test by quiting after scheduling restricted games, and see that all the byes made it onto the schedule
   // 
   
   public boolean scheduleForcedGames(ArrayList<NflRestrictedGame> restrictedGames, NflSchedule schedule) {      
      // for each restricted game
      // Currently support (only) the hometeam and division restrictions for a given week
      // Also support an optional named opponent - otherwise the scheduler will choose the opponent
      // 
      // Create a list of candidate games based on hometeam, opponent (if named), division restrictions
      
      
      for (int wi=0; wi < restrictedGames.size(); wi++) {
         NflRestrictedGame restrictedGame = restrictedGames.get(wi);
         int resWeekNum = restrictedGame.weekNum;
         String resTeamName = restrictedGame.teamName;
         NflTeamSchedule resTeam = schedule.findTeam(resTeamName);
         
         if (resTeam == null) {
            System.out.println("ERROR scheduling restricted game: can't find restricted team: " + resTeamName);
            return false;
         }

         String restriction = restrictedGame.restriction;
         
         if (!restriction.equalsIgnoreCase("division") && 
             !restriction.equalsIgnoreCase("hometeam") &&
             !restriction.equalsIgnoreCase("bye")) {
             System.out.println("ERROR scheduling restricted game: unrecognized restriction: " + restriction);
             return false;
         }
         
         String resOtherTeamName = restrictedGame.otherTeam;
         
         NflTeamSchedule resOtherTeam = null;
         if (!resOtherTeamName.isEmpty()) {
             resOtherTeam = schedule.findTeam(resOtherTeamName);
             if (resOtherTeam == null) {
                 System.out.println("ERROR scheduling restricted game: can't find restricted resOtherTeam: " + resOtherTeamName);
                 return false;
             }
         }
 
         String resStadium = restrictedGame.stadium;
         
         //System.out.println("Scheduling Restricted game for team: " + resTeamName + ", weekNum: " + resWeekNum + ", restriction: " + restriction + ", otherTeam: " + resOtherTeamName);

         // Validate that not already scheduled - may have been scheduled due to opponent being scheduled in that week
         
         if (resTeam.scheduledGames[resWeekNum-1] != null) {
            //System.out.println("Info: Restricted Game Scheduling: game already scheduled in week: " + resWeekNum + " for restricted team: " + resTeamName);
            continue;
         }
         
         if (resOtherTeam != null && resOtherTeam.scheduledGames[resWeekNum-1] != null) {
             //System.out.println("Info: Restricted Game Scheduling: game already scheduled in week: " + resWeekNum + " for restricted other team: " + resOtherTeamName);
             continue;
         }
         
         // handle forced byes here, then continue to the next forced game/bye
         if (restriction.equalsIgnoreCase("bye")) {
             NflGameSchedule usBye = null;
             for (int bi=0; bi < schedule.unscheduledByes.size(); bi++) {
                 usBye = schedule.unscheduledByes.get(bi);
                 // NflTeamSchedule homeTeam = usBye.homeTeamSchedule;

                 if (!resTeamName.equalsIgnoreCase(usBye.game.homeTeam)) {
                    continue;
                 }
                 
                 placeGameInSchedule(usBye, resWeekNum, schedule);
                 usBye.restrictedGame = true;
                 //System.out.println("scheduled restricted bye, weekNum: " + resWeekNum + " home team: " + usBye.game.homeTeam);

                 break;
             }
             
             if (usBye == null || !usBye.restrictedGame) {
                 // System.out.println("ERROR: unable to find and schedule restricted bye, weekNum: " + resWeekNum + " home team: " + resTeamName);
             }
             
             continue;
         }

         // Make a list of qualifying unscheduled games
         //
         // Find unscheduled games for the restricted game team
         //    that are qualified such that
         //   1a) restricted game "HomeTeam" attribute constraint is satisfied in the unscheduled game
         //   1b) other restricted game attribute constraints are present in the unscheduled game e.g. "Mexico"
         //   Furthermore
         //   2)  restricted game attribute constraints that match Global resource constraints have remaining capacity
         //        e.g. GiantsJetsStadium or Bye
         //   TBD: 3) other team is not already scheduled for that week - with another team - otherteam.scheduleGames
         //   Done: when schedule / place a game - must remove that game from both unscheduled lists and place in scheduled array
         // if qualified - add to list

         ArrayList<NflGameSchedule> qualifiedGames = new ArrayList<NflGameSchedule>();

         for (int gi=0; gi < schedule.unscheduledGames.size(); gi++) {
            NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
            NflTeamSchedule homeTeam = usgame.homeTeamSchedule;
            NflTeamSchedule awayTeam = usgame.awayTeamSchedule;
            
            // Validate that the restricted Team must be either the hometeam or the awayTeam (if division)
            if (resTeam != homeTeam && resTeam != awayTeam) {
                continue;
            }
            
            boolean qualified = true;
            
            // "hometeam" Handling - ensure the restricted team is the home team
            // also ensure that the opponent (if named) is the away team of the game
            if (restriction.equalsIgnoreCase("hometeam")) {
               if (!resTeamName.equalsIgnoreCase(usgame.game.homeTeam)) {
                  qualified = false;
                  //System.out.println("   Set qualified false 1a: " + resTeamName);
                  continue;
               }
               
               if (resOtherTeam != null && !resOtherTeamName.equalsIgnoreCase(usgame.game.awayTeam)) {
                   qualified = false;
                   //System.out.println("   Set qualified false 1b: " + resOtherTeamName);
                   continue;
               }
            }
            
            // "division" Handling - ensure the game is a divisional game - tagged with "division" attribute
            // NOTE: already validated that the restricted team is in this game
            else if (restriction.equalsIgnoreCase("division")) {
               if (!usgame.game.findAttribute("division")) {
                  qualified = false;
                  //System.out.println("   Set qualified false 2: " + resTeamName + ", division attribute not found for game: hometeam: " + usgame.game.homeTeam + ", awayTeam: " + usgame.game.awayTeam);
                  continue;
               }
            }

            // Verify that neither the hometeam or awayteam are already scheduled in resWeekNum
            if (homeTeam.scheduledGames[resWeekNum-1] != null) {
            	   qualified = false;
            }
            else if (awayTeam != null && awayTeam.scheduledGames[resWeekNum-1] != null) {
         	   qualified = false;
            }
         
            if (!qualified) {
               continue;
            }

            // unscheduled game is qualified so far
            
            // Verify that stadiums with a global resource have remaining capacity in the restricted weeknum
            
            if (homeTeam.team.stadium != null) {
                String stadiumName = homeTeam.team.stadium;
                
                NflResourceSchedule resourceSchedule = schedule.findResource(stadiumName);
                if (resourceSchedule != null && !resourceSchedule.hasCapacity(resWeekNum)) {
                    qualified = false;
                }
            }

            if (!qualified) {
               continue;
            }

            qualifiedGames.add(usgame);
            //System.out.println("   Qualifying game for team: " + usgame.homeTeam + " vs " + usgame.awayTeam);
            
            // computeMetrics on the game
         }

         // All games have been checked for qualification
         if (qualifiedGames.size() == 0) {
            System.out.println("ERROR: no qualified games found for the teamweek: " + resWeekNum + ", " + resTeamName + ", " + resOtherTeamName + ", " + restriction);
            return false;
         }
         
   	     for(NflGameSchedule usgame: qualifiedGames) {
            usgame.computeMetric(resWeekNum, schedule, qualifiedGames,"StadiumResource");
            //System.out.println("Info: Unrestricted Candidate game, weekNum: " + weekNum + " home team: " + usgame.game.homeTeam + " away team: " + usgame.game.awayTeam + ", score: " + usgame.score);
	     }
	  
         // sort the collection and choose the best game to schedule
         //candidateGames.sort(NflGameSchedule.GameScheduleComparator);
   	     
         NflGameSchedule chosenGame = chooseBestScoringGame(qualifiedGames);

         chosenGame.stadium = resStadium;
         
         placeGameInSchedule(chosenGame, resWeekNum, schedule);
         
         chosenGame.restrictedGame = true;
         //System.out.println("scheduled restricted game, weekNum: " + resWeekNum + " home team: " + chosenGame.game.homeTeam + " away team: " + chosenGame.game.awayTeam + ", score: " + chosenGame.score);
         
         // schedule.schedule
         // Probably just keep the game in the original game list - but mark as scheduled
         // Then would need to check for scheduled when processing through the list
         // This would simplify scheduling for the 2 teams, and simplify unscheduling

         // must process the resources, decrement the availabilities
         // Set the scheduled week, game.weekNum > 0 indicates scheduled

         // so need a schedule function, and an unschedule function - probably in the nflSchedule class
      }
      
      return true;
   }

   // place usGame in schedule for both the homeTeam and the awayTeam
   // at the index of weekNum in the scheduleGames array of each Team
   // set the game.weeknum = weeknum for temporary working purposes (reevaluate the usefulness of that)
   
   public boolean placeGameInSchedule(NflGameSchedule usGame, int weekNum, NflSchedule schedule) {
      // find the hometeam and the awayteam
       
      NflTeamSchedule homeTeam = usGame.homeTeamSchedule;
      NflTeamSchedule awayTeam = usGame.awayTeamSchedule;
            
      if (homeTeam == null) {
         System.out.println("ERROR: can't find home team: " + usGame.game.homeTeam);
         return false;
      }
      
      if (!usGame.isBye && awayTeam == null) {
         System.out.println("ERROR: can't find away team: " + usGame.game.awayTeam);
         return false;
      }
      
      //System.out.println("Placing game for home team: " + usGame.game.homeTeam + ", and away Team: " + usGame.game.awayTeam + ", in week: " + weekNum);
      
      // Validate that nothing is scheduled in that week for either team
      
      if (homeTeam.scheduledGames[weekNum-1] != null) {
          System.out.println("ERROR: game unexpectedly scheduled in week: " + weekNum + " for home team: " + usGame.game.homeTeam + " for away team: " + usGame.game.awayTeam + ", isBye: " + usGame.isBye + ", isRestricted: " + usGame.restrictedGame);
          NflGameSchedule xGame = homeTeam.scheduledGames[weekNum-1];
          System.out.println("Instead: scheduled in week: " + xGame.weekNum + " for home team: " + xGame.game.homeTeam + " for away team: " + xGame.game.awayTeam+ ", isBye: " + xGame.isBye + ", isRestricted: " + xGame.restrictedGame);
         
         //System.out.println("Rescheduled Weeks: " + reschedLog.size() + ", Reschedule Week Iterations: " + iterNum);
         return false;
      }
  
      if (!usGame.isBye && awayTeam.scheduledGames[weekNum-1] != null) { 
         System.out.println("ERROR: game unexpectedly scheduled in week: " + weekNum + " for away team: " + usGame.game.awayTeam);
         return false;
      }

      // add the game to the scheduled array for each team at the weeknum-1 index
      homeTeam.scheduledGames[weekNum-1] = usGame;   
      if (!usGame.isBye) {
         awayTeam.scheduledGames[weekNum-1] = usGame;
      }
      
      // remove the game from the unscheduled arraylist for the schedule
      
      if (usGame.isBye) {
         schedule.unscheduledByes.remove(usGame);
      } else {
         schedule.unscheduledGames.remove(usGame);
      }
    	  
      usGame.weekNum = weekNum;
      
      // Consume resource usage in weeknum

      if (!usGame.isBye) {
         if (homeTeam.team.stadium != null) {
            String stadiumName = homeTeam.team.stadium;
          
            NflResourceSchedule resourceSchedule = schedule.findResource(stadiumName);
            if (resourceSchedule != null && resourceSchedule.hasCapacity(weekNum)) {
            	resourceSchedule.usage[weekNum-1] += 1;
            }
         }
      }
      else {
         // reduce the bye resource count
    	  NflResourceSchedule byeResourceSchedule = schedule.findResource("bye");
         if (byeResourceSchedule != null && byeResourceSchedule.hasCapacity(weekNum)) {
        	 byeResourceSchedule.usage[weekNum-1] += 1;
         }
      }
      
      return true;
   }
   
   public boolean writeScheduleCsv(NflSchedule schedule, String fileName) {
      //System.out.println("Entered writeScheduleCsv");
      BufferedWriter bw = null;
      FileWriter fw = null;

      try {
         fw = new FileWriter(fileName);
         bw = new BufferedWriter(fw);
         
         // write the header to the file
         // team, week 1 opponent, week 2 opponent
         bw.write("Team");
         //bw.write(",Week 1,Week 2,Week 3,Week 4,Week 5,Week 6,Week 7,Week 8,Week 9");
         //bw.write(",Week 10,Week 11,Week 12,Week 13,Week 14,Week 15,Week 16,Week 17\n");
         for (int wi=1; wi <= NflDefs.numberOfWeeks; wi++) {
        	    bw.write(",Week " + wi);
         }
         bw.write("\n");
         
         // handle byes
         
         // loop through the teams in the schedule
         // start line with teamname
         for (int ti=0; ti < schedule.teamSchedules.size(); ti++) {
            NflTeamSchedule teamSchedule = schedule.teamSchedules.get(ti);
            // loop through the scheduledGames array
            // append "," to the line
            // get game from the array
            // if null, append 0 to the line
            // else if team is home - append the away teamname
            // else if team is away - append "at " home team name

            bw.write(teamSchedule.team.teamName);
            //int scheduledGameCount = 0;
            for (int gi=0; gi < teamSchedule.scheduledGames.length; gi++) {
               NflGameSchedule gameSchedule = teamSchedule.scheduledGames[gi];
               
               if (gameSchedule == null) {
                   bw.write(",0");
               }
               //else if (gameSchedule.game.findAttribute("bye")) {

               else if (gameSchedule.isBye) {
                   bw.write(",Bye");
               }
               else if (gameSchedule.game.homeTeam.equalsIgnoreCase(teamSchedule.team.teamName)) {
                   bw.write("," + gameSchedule.game.awayTeam);
                   //bw.write("," + gameSchedule.game.awayTeam + "." + gameSchedule.weekScheduleSequence);
               }
               else {
                   bw.write(",at " + gameSchedule.game.homeTeam);
                   //bw.write(",at " + gameSchedule.game.homeTeam + "." + gameSchedule.weekScheduleSequence);
               }
               
               if (gameSchedule != null && gameSchedule.stadium != null && gameSchedule.stadium.equalsIgnoreCase("london")) {
                   bw.write(" (Lon)");
               }
               else if (gameSchedule != null && gameSchedule.stadium != null && gameSchedule.stadium.equalsIgnoreCase("mexico")) {
                   bw.write(" (Mex)");
               }
               
               if (gameSchedule != null) {
                  //scheduledGameCount++;
               }
            }
            bw.write("\n");
            //System.out.println("team: " + ti + ", " + teamSchedule.team.teamName + ", scheduledGames: " + scheduledGameCount);
         }
         
         // Write out the schedule score
         bw.write("\nScore," + schedule.score + "\n");

         // Write out Bye Counts per week
         bw.write("\nByes");
  	     for (int wi=1; wi <= NflDefs.numberOfWeeks; wi++) {
 		    int byeCountsThisWeek = schedule.byeCounts(wi);
            bw.write("," + byeCountsThisWeek);
  	     }

         
         // Write out Divisional Game Counts per week
         
         int first8weeksDivisionalTotal = 0;
         int second8weeksDivisionalTotal = 0;
         
         bw.write("\nDivisional Games");
  	     for (int wi=1; wi <= NflDefs.numberOfWeeks; wi++) {
 		    int divisionalGameCount = schedule.divisionalGameCount(wi);
            bw.write("," + divisionalGameCount);
            if (wi <= 8) {
            	first8weeksDivisionalTotal += divisionalGameCount;
            }
            else if (wi <= 16) {
            	second8weeksDivisionalTotal += divisionalGameCount;
            }
  	     }
  	     
  	     // write out the 8 week totals summaries
  	     bw.write("\n8 week totals,,,,,,,," + first8weeksDivisionalTotal + ",,,,,,,," + second8weeksDivisionalTotal);
  	     
         // Write out Alerts
         if (!schedule.alerts.isEmpty()) {
            bw.write("\nAlerts\n");
            for(NflScheduleAlert alert: schedule.alerts) {
                //bw.write(alert.alertDescr + "," + alert.weekNum + "," + alert.homeTeam + "," + alert.awayTeam + "\n");
                bw.write(alert.alertDescr + "\n");
            }
         }
         bw.write("\n");

         //System.out.println("reschedWeekNum: " + reschedWeekNum);
         
         //System.out.println("Weeks Schedule Attempts: " + iterNum + "  (Iterations: Failed+Successful unique)");
         if (!reschedLog.isEmpty()) {
             //System.out.println(reschedLog.get(reschedLog.size()-1));
             //System.out.println("Failed Weeks:            " + reschedLog.size()/2 + "  (Incomplete, Identical.Complete.skip)");
         }
         
         //System.out.println("Unique,Successful Weeks:   " + fingerPrintMap.size() + "   (Unique FP collection size)");
         //System.out.println("Repeated,Successful Weeks: " + fpSkipCount + "   (Reschedule Non-Unique FP skip count)");
         //Set set = fingerPrintMap.entrySet();
         //Iterator iterator = set.iterator();
/*
         while(iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry)iterator.next();
            System.out.println("FP: "+ mentry.getKey() + " Count: " + mentry.getValue());
         }
*/

         if (reschedAttemptsMultiWeeksBack < NflDefs.reschedAttemptsMultiWeeksBackLimit) {
             //System.out.println("Schedule completed");
         }
         else {
             //System.out.println("Schedule did not complete, lowestWeekNum: " + lowestWeekNum);
         }
         //System.out.println("    unscheduled games: " + curSchedule.unscheduledGames.size());
         //System.out.println("    score: " + curSchedule.score);
         
         //System.out.println("Done");

      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         try {

            if (bw != null)
               bw.close();

            if (fw != null)
               fw.close();

         } catch (IOException ex) {
            ex.printStackTrace();

         }
      }
      
      return true;
   }

/*
  ScheduleWeek(weekNum, direction, schedule)
  Evaluate all unscheduled games in the schedule - determine if candidate
  Not a candidate - if one of the teams is already scheduled for the week
  For each qualifying candidate game - wrap it in a candidate data structure
  And add to a candidate list
  Then walk through the candidate list and evaluate all the metrics
  Then sort the candidates and start choosing games
  And placing them on the schedule
 ScheduleWeeksInSequence(weekNumStart, weekNumEnd, schedule)
  Determine direction from weekNumStart - weekNumEnd sign

 */
   
   public boolean scheduleUnrestrictedWeek(NflSchedule schedule, int weekNum) {
      NflGameSchedule game;
      //NflGameSchedule bye;
	   //int schedTeamsInWeek;
      int schedSeqNum = 0;
      boolean status = true;
      
      // TBD:Byes - determine # of bye pairs to attempt in this week
      //    byeCapacity 
      schedule.determineNumByesForWeek(weekNum);
      //System.out.println("byesToScheduleThisWeek: " + schedule.byesToScheduleThisWeek + ", for week: " + weekNum);

      iterNum++;
	   //unscheduledTeams.clear();
	   //if (fingerPrintMap.containsKey(partialScheduleEntry.fingerPrint)) {
		//   count = fingerPrintMap.get(partialScheduleEntry.fingerPrint);

      while ((schedule.scheduledTeamsInWeek(weekNum)) < NflDefs.numberOfTeams) {
         game = scheduleNextUnrestrictedGameInWeek(schedule, weekNum);
         if (game == null) {
	         //System.out.println("Scheduler: failed to schedule all teams in week: " + weekNum + " scheduled:" + schedTeamsInWeek + " games, teams: " + NflDefs.numberOfTeams + ", " + schedule.teams.size());
	         // attempt a reschedule of the week - making the first scheduled game a low/lower priority - somehow
	         // maybe exit false, fixup, and reenter, some number of times
	         // trying to find and demote the culprit preventing a full week of scheduling
	         // maybe have a demotion penalty for the gameSchedule that was first
	         // keep adding more demotion penalty to the first scheduled game everytime you can't schedule the week
	         // if there is a key culprit or 2, they will accumulate enough demotion penalty to push them way down
	         // Only retry - X number of times, or some other termination criteria 

            status = false;
            
      	   // determine unscheduled teams
      	   for (NflTeamSchedule teamSchedule: schedule.teamSchedules) {
               NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum-1];
      	      if (gameSched == null) {
      	         unscheduledTeams.add(teamSchedule);
      	      }
            }
      	    
	         break;
         }
                  
         game.weekScheduleSequence = ++schedSeqNum;
	   }
	  
	   // log the brief, 1-line scheduled results of weekNum
	   // weekNum, iternum, scheduled teams, unscheduled teams in csv
	   briefLogWeek(schedule, weekNum);
	   logPartialScheduleHistory(schedule, weekNum);

	   Integer SchedFingerPrintCount = 0;
	   // if completed week & finger print logged & fp repeated and not the last week
	   // set false/failed - maybe b/c don't want to continue with a repeated fp
	   // Log the fp in the logPartialScheduleResults.csv file
	  
	   if (status == true && fingerPrintMap.containsKey(schedule.latestScheduleFingerPrint)) {
		   NflPartialScheduleEntry partialScheduleEntry = fingerPrintMap.get(schedule.latestScheduleFingerPrint);
         SchedFingerPrintCount = partialScheduleEntry.count;
         if (SchedFingerPrintCount > 1 && weekNum < NflDefs.numberOfWeeks - 1) {
            status = false;
            fpSkipCount++;
         }
      }

      return status;
   }
   
   // possibly modularize this function to facilitate scheduling a game and/or scheduling a bye
   public NflGameSchedule scheduleNextUnrestrictedGameInWeek(NflSchedule schedule, int weekNum) {

	  
	   /////////////////
	   // Bye Scheduling
	   /////////////////
      if (schedule.byesToScheduleThisWeek >= 2 && schedule.unscheduledByes.size() >= 2) {
         //NflGameSchedule lastByeGame = scheduleNextUnrestrictedByes(schedule, weekNum);
         NflGameSchedule lastByeGame = scheduleNextUnrestrictedByes2(schedule, weekNum);
         
        return lastByeGame;
     }

      // Make a collection of unscheduled games
	   // where none of the teams are scheduled for this week

      ArrayList<NflGameSchedule> candidateGames = new ArrayList<NflGameSchedule>();

      for (int gi=0; gi < schedule.unscheduledGames.size(); gi++) {
         NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
         NflTeamSchedule homeTeam = usgame.homeTeamSchedule;
         NflTeamSchedule awayTeam = usgame.awayTeamSchedule;
         
         usgame.unscheduledByes.clear();   // clear the byes so byes can be set accurately during this scheduling pass
         
         // Validate that neither team from the game is already scheduled for this week
          
         if (homeTeam.scheduledGames[weekNum-1] != null) {
            //System.out.println("Info: schedUnrest: team already scheduled in week: " + weekNum + " home team: " + homeTeam.team.teamName + " away team: " + awayTeam.team.teamName + ", skip");
            continue;
         }
         else if (!usgame.isBye && awayTeam.scheduledGames[weekNum-1] != null) {
            //System.out.println("Info: schedUnrest: team already scheduled in week: " + weekNum + " away team: " + awayTeam.team.teamName + " home team: " + homeTeam.team.teamName + ", skip");
            continue;
         }

         // Verify that the game is not a repeated matchup - either before or after this week - unacceptable
         // Verify first that the next week would not cause a repeated matchup
         if (weekNum+1 <= NflDefs.numberOfWeeks) {
		      NflGameSchedule nextWeeksGame = homeTeam.scheduledGames[weekNum]; // NOTE: weekNum starts at 1, must correct for index
		      if (nextWeeksGame != null && !nextWeeksGame.isBye) {
			      if (nextWeeksGame.game.awayTeam.equalsIgnoreCase(usgame.game.homeTeam) &&
                   nextWeeksGame.game.homeTeam.equalsIgnoreCase(usgame.game.awayTeam)) {
				      continue;
			      }
		      }
	      }

         // Verify next that the previous week would not cause a repeated matchup
	      if (weekNum > 1) {
		      NflGameSchedule nextWeeksGame = homeTeam.scheduledGames[weekNum-2];  // NOTE: weekNum starts at 1, must correct for index
		      if (nextWeeksGame != null && !nextWeeksGame.isBye) {
			      if (nextWeeksGame.game.awayTeam.equalsIgnoreCase(usgame.game.homeTeam) &&
                   nextWeeksGame.game.homeTeam.equalsIgnoreCase(usgame.game.awayTeam)) {
				      continue;
			      }
		      }
	      }

         // Verify that global attrlimit matches have remaining capacity in the restricted weeknum
         // for any unscheduled game attributes that have a global resource: e.g. bye, GiantsJetsStadium
                 
         boolean resourcePass = true;
         if (homeTeam.team.stadium != null) {
            String stadiumName = homeTeam.team.stadium;
              
            NflResourceSchedule resourceSchedule = schedule.findResource(stadiumName);
            if (resourceSchedule != null && !resourceSchedule.hasCapacity(weekNum)) {
               resourcePass = false;
            }
         }
          
         // clear the bye information from the game, array list and flag ?? TBD
         usgame.unscheduledByes.clear();
          
         if (!resourcePass) {
            continue;   // resource capacity would be exceeded - byes, or JetsGiantsStadium
         }

         candidateGames.add(usgame);
      }
      
      if (candidateGames.isEmpty() ) {
         //System.out.println("Info: No candidate games in week: " + weekNum);
         return null;
      }
      
      //System.out.println("Info: schedUnrest: candidate count: " + candidateGames.size());
      
	   for(NflGameSchedule usgame: candidateGames) {
         usgame.computeMetrics(weekNum, schedule, candidateGames);
         //System.out.println("Info: Unrestricted Candidate game, weekNum: " + weekNum + " home team: " + usgame.game.homeTeam + " away team: " + usgame.game.awayTeam + ", score: " + usgame.score);
	   }
	  
	   /////////////////
	   // Bye Scheduling
      /////////////////
      /*
      if (schedule.byesToScheduleThisWeek >= 2 && schedule.unscheduledByes.size() >= 2) {
    	   //NflGameSchedule lastByeGame = scheduleNextUnrestrictedByes(schedule, weekNum);
    	   NflGameSchedule lastByeGame = scheduleNextUnrestrictedByes2(schedule, weekNum);
          
         return lastByeGame;
      }
      */

      //////////////////
      // Game Scheduling
      //////////////////
      
      NflGameSchedule chosenGame = null;
      
      if (!unscheduledTeams.isEmpty()) {
    	   NflTeamSchedule unscheduledTeam = unscheduledTeams.remove(0);
         chosenGame = chooseBestScoringGame(candidateGames, unscheduledTeam);
      }

      if (chosenGame == null) {
         chosenGame = chooseBestScoringGame(candidateGames);
      }
      
      if (!placeGameInSchedule(chosenGame, weekNum, schedule)) {
         return null;
      }
      
      // just chose one game, need to keep choosing until week is full
      // or no more games can be chosen
      // algorithm should be aware if a week is not full - and report
      
      return chosenGame;
   }
   
   public boolean findMaxEvenRelatedUnscheduledByes(ArrayList<NflGameSchedule> sortedUnscheduledByes, ArrayList<NflGameSchedule> byesToSchedule, int weekNum) {
	   byesToSchedule.clear();
	   
	   NflGameSchedule primaryUnscheduledBye = sortedUnscheduledByes.get(0);
	   
	   byesToSchedule.add(primaryUnscheduledBye);
      for (NflGameSchedule bye: primaryUnscheduledBye.opponentByes) {
    	   // if bye is unscheduled and isn't already in the collection, add it to the bye collection to schedule
         if (bye.weekNum == 0 && bye.homeTeamSchedule.scheduledGames[weekNum-1] == null) {
        	   if (!byesToSchedule.contains(bye)) {
        	      byesToSchedule.add(bye);
        	   }
         }
      }
	   
      int byeCount = byesToSchedule.size();
       
      // Ensure ByeCount is for even pairs of byes
      // 1 is ok, otherwise should be 2, 4, 6, 8, etc
      if (byeCount > 1 && byeCount % 2 != 0) {
    	   byesToSchedule.remove(0);
      }

      return true;
   }

   // public static NflGameSchedule scheduleNextUnrestrictedByes(NflSchedule schedule, int weekNum) {
   // TBD write a new version of scheduleNextUnrestrictedByes
   // TBD to schedule all the byes for the week, from the unscheduled bye with the most unscheduled opponent byes
   // TBD must sort the unscheduleByes first for unscheduled, then with most unscheduled opponent byes
   // TBD then work through the sorted list, scheduling an even number of byes - 
   // TBD starting with the primary bye then it's unscheduled opponent byes
   
   public NflGameSchedule scheduleNextUnrestrictedByes2(NflSchedule schedule, int weekNum) {
	   NflGameSchedule lastScheduledBye = null;
      ArrayList<NflGameSchedule> byesToSchedule = new ArrayList<NflGameSchedule>();

	   while (schedule.byesToScheduleThisWeek > 0) {
		   scoreUnscheduledByes(schedule.unscheduledByes, weekNum);
	      // Prefer games with multiple unscheduled byes vs 1 or 0
	      Collections.sort(schedule.unscheduledByes, NflGameSchedule.GameScheduleByeComparatorByByeCandidateScore);

	      findMaxEvenRelatedUnscheduledByes(schedule.unscheduledByes, byesToSchedule, weekNum);
	      for (NflGameSchedule byeToSchedule: byesToSchedule) {
	         //System.out.println("Scheduling Bye(2) in week: " + weekNum + " home team: " + byeToSchedule.homeTeamSchedule.team.teamName);
	         placeGameInSchedule(byeToSchedule, weekNum, schedule);
	         schedule.byesToScheduleThisWeek--;
	         lastScheduledBye = byeToSchedule;
	         if (schedule.byesToScheduleThisWeek <= 0) {
	            break;
	         }
	      }
	   }

	   return lastScheduledBye;
   }

   public boolean scoreUnscheduledByes(ArrayList<NflGameSchedule> unscheduledByes, int weekNum) {
       for (NflGameSchedule unscheduledBye: unscheduledByes) {
          unscheduledBye.byeCandidateScore = 0;
          // if the unscheduled bye can't be scheduled this week because a (forced) game is already scheduled, this bye is not a candidate
          if (unscheduledBye.homeTeamSchedule.scheduledGames[weekNum-1] != null) {
        	  continue;
          }
       
          unscheduledBye.byeCandidateScore = 1;   // The root bye is schedulable, now we will count the schedulable opponents
          for (NflGameSchedule opponentBye: unscheduledBye.opponentByes) {
              // if the opponent is already scheduled or
        	  // if the opponent bye can't be scheduled this week because a (forced) game is already scheduled, this bye is not a candidate
              if (opponentBye.weekNum > 0 ||
                  opponentBye.homeTeamSchedule.scheduledGames[weekNum-1] != null) {
            	  continue;
              }
              unscheduledBye.byeCandidateScore += 1;   // For the schedulable opponent bye, add it to the score
          }
       }
       
	   return true;
   }
   
   
   public NflGameSchedule scheduleNextUnrestrictedByes(NflSchedule schedule, int weekNum) {
      
      // analyze and find bye matchup candidates, i.e. unscheduled games where both teams have unscheduled byes
      // choose a candidate bye matchup
      // maybe we choose the game that has the bye matchup and schedule the byes instead of the game
      // we could put the game into the bestCandidate games as the only game, and indicate that it is the byes to be scheduled
      // maybe set a flag
      NflGameSchedule lastByeGame = null;
      
      for(NflGameSchedule usgame: schedule.allGames) {
         // find unscheduledBye(s) that match the hometeam and/or the away team
         // and store them on the candidateByeGame - for later selection as possible bye matchup - this week
         
         usgame.unscheduledByes.clear();
         
         for (NflGameSchedule unscheduledBye: schedule.unscheduledByes) {
            // if the team for the unscheduled Bye is not in a scheduled game this week
            // and it is either the home team or the away team of the unscheduled game
            // make it a camdidate bye

            if (unscheduledBye.homeTeamSchedule.scheduledGames[weekNum-1] == null) {
               if (unscheduledBye.homeTeamSchedule == usgame.homeTeamSchedule) {
                  usgame.unscheduledByes.add(unscheduledBye);
               }
               else if (unscheduledBye.homeTeamSchedule == usgame.awayTeamSchedule) {
                  usgame.unscheduledByes.add(unscheduledBye);
               }
            }
         }
      }

      // Prefer games with multiple unscheduled byes vs 1 or 0
      Collections.sort(schedule.allGames, NflGameSchedule.GameScheduleComparatorByUnscheduledByes);

      //System.out.println("Info: Unrestricted Candidate game, weekNum: " + weekNum + " home team: " + usgame.game.homeTeam + " away team: " + usgame.game.awayTeam + ", score: " + usgame.score);

      // Choose the "best" candidateByeGame based on the sorting 
      // probably could choose among ties randomly
      // if there is only 1 or 0 unscheduled byes in the chosen game
      // Choose unscheduled byes randomly from the unscheduledByes collection
      // Then "place" the 2 byes on the schedule
      
      NflGameSchedule chosenMatchup = null;
      for (NflGameSchedule byeMatchup: schedule.allGames) {
         chosenMatchup = byeMatchup;
         break;
      }
      
      if (chosenMatchup != null && chosenMatchup.unscheduledByes.size() == 2) {
         //System.out.println("Entered matchup bye scheduling for week: " + weekNum + " unscheduled bye count: " + schedule.unscheduledByes.size() + ", byesToScheduleThisWeek: " + schedule.byesToScheduleThisWeek);

         for (NflGameSchedule byeGame: chosenMatchup.unscheduledByes) {
            //System.out.println("Scheduling Bye(1.1) in week: " + weekNum + " home team: " + byeGame.homeTeamSchedule.team.teamName);
            placeGameInSchedule(byeGame, weekNum, schedule);
            schedule.byesToScheduleThisWeek--;
            lastByeGame = byeGame;
         }   
      }
      else {
         //System.out.println("Entered non-matchup bye scheduling for week: " + weekNum + " unscheduled bye count: " + schedule.unscheduledByes.size() + ", byesToScheduleThisWeek: " + schedule.byesToScheduleThisWeek);
         ArrayList<NflGameSchedule> unmatchedByes = new ArrayList<NflGameSchedule>();
         for (NflGameSchedule unscheduledBye: schedule.unscheduledByes) {
             //System.out.println("  unscheduled bye for " + unscheduledBye.game.homeTeam);
             if (unscheduledBye.homeTeamSchedule.scheduledGames[weekNum-1] != null) {
            	 continue;
             }
             unmatchedByes.add(unscheduledBye);
             if (unmatchedByes.size() == 2) {
            	 break;
             }
         }
         if (unmatchedByes.size() < 2) {
             System.out.println("ERROR: failed to schedule 2 bye games in weekNum: " + weekNum  + " scheduled: " + unmatchedByes.size());
         }
         for (NflGameSchedule unscheduledBye: unmatchedByes) {
             //System.out.println("Scheduling Bye(1.2) in week: " + weekNum + " home team: " + unscheduledBye.homeTeamSchedule.team.teamName);
             placeGameInSchedule(unscheduledBye, weekNum, schedule);
             schedule.byesToScheduleThisWeek--;
             lastByeGame = unscheduledBye;
         }
         //System.out.println("Exitted non-matchup bye scheduling: " + weekNum + " unscheduled bye count: " + schedule.unscheduledByes.size());
       }
	  
 	  return lastByeGame;
   }
   
   public NflGameSchedule chooseBestScoringGame(ArrayList<NflGameSchedule> candidateGames, NflTeamSchedule unscheduledTeam) {
       //System.out.println("Entered chooseBestScoringGame " + " unscheduledTeam: " + unscheduledTeam.team.teamName + ", candidateGame size: " + candidateGames.size());
       ArrayList<NflGameSchedule> candidateTeamGames = new ArrayList<NflGameSchedule>();
       for(NflGameSchedule usgame: candidateGames) {
    	   if (usgame.homeTeamSchedule == unscheduledTeam) {
    		   candidateTeamGames.add(usgame);
    	   }
    	   else if (usgame.awayTeamSchedule == unscheduledTeam) {
    		   candidateTeamGames.add(usgame);
    	   }
       }
       //System.out.println("candidateTeamGames size: " + candidateTeamGames.size());
       NflGameSchedule chosenGame = null;
       
       if (candidateTeamGames.size() > 0) {
           chosenGame = chooseBestScoringGame(candidateTeamGames);	   
       }
       return chosenGame;	   
   }
   
   public NflGameSchedule chooseBestScoringGame(ArrayList<NflGameSchedule> candidateGames) {
	   NflGameSchedule chosenGame = null;
       ArrayList<NflGameSchedule> bestCandidateGames = new ArrayList<NflGameSchedule>();
	   double bestScore = 1000000000;
		  
	   Collections.sort(candidateGames, NflGameSchedule.GameScheduleComparatorByScore);

       // Choose the best games into a collection and choose randomly
       for(NflGameSchedule usgame: candidateGames) {
	      //NflTeamSchedule homeTeam = usgame.homeTeamSchedule;
	      //NflTeamSchedule awayTeam = usgame.awayTeamSchedule;
	          
	      //System.out.println("Info: Post Sort Candidate game, weekNum: " + weekNum + " home team: " + homeTeam.team.teamName + " away team: " + awayTeam.team.teamName + ", score: " + usgame.score);
	          
	      // lowest score is best
	      if (usgame.score > bestScore) {
	         break;
	      }
	          
	      bestScore = usgame.score;
	          
	      bestCandidateGames.add(usgame);
	   }
	      
	   int numBestCandidates = bestCandidateGames.size();
	   chosenGame = bestCandidateGames.get(0);
	      
	   if (numBestCandidates > 1) {
          // randomly choose int between 0 and numBestCandidates-1
	    	  
	      int randomNextInt = rnd.nextInt(numBestCandidates);

	      //System.out.println("   Qualified games range: 0  , " + (numBestCandidates-1));
	      //System.out.println("   Chose game randomNextInt: " + randomNextInt);

	      chosenGame = bestCandidateGames.get(randomNextInt);
	   }
	      
	   return chosenGame;
	      
   }


   public boolean scheduleUnrestrictedGames(NflSchedule schedule) {
	   // schedule backwards from week numberOfWeeks to week 1
	   // for each week
	   // step through each team and then it's unscheduled games
	   // evaluate and score each game as a candidate for scheduling
	   //   mfgMetric base class - then instances for each rule
	   //   each instance applies to a game at a week for a team
	   //   Then the 2 metrics for the same game, same week, 2 teams can be combined 
	   //   to better know the best game to choose to schedule first for the week
	   //   Maybe keep the metric info around ??? for back tracking and rescheduling ???
	   //   Team --> 
	   // Individual penalties for each rule/metric, and weightings
	   // Have a candidate object to wrap the game in - which holds the penalties
	   //    nflCandidateGame: game, rule penalties, overall penalty for my team, overall penalty for both teams
	   // lowest penalty means the best game to schedule in this week
	   // choosing a game would have a combined penalty for both teams
	   // so maybe choose the game with lowest combined penalty
	   // after all teams unscheduled games are scored
	   // choose the games in order of lowest penalty

	   // For each week
	   // collect games (NflGameSchedule) from unscheduled list, and 
	   // remove games if either team already have a game schedule for the current week

	   // then grade each remaining game
	   // then sort the games by best grade
	   // place the best game

	   // start over - because 2 teams were just scheduled and their games will need to be removed
	   // start just randomly choosing a game - in order to get some kind of a schedule 

	   boolean status = true;
	   
	   reschedAttemptsOneWeekBack = 0;
	   reschedAttemptsMultiWeeksBack = 0;

	   int reschedAttemptedMaxWeek = 0;
	   int goBackToWeekNum = 0;

	   for (int weekNum=NflDefs.numberOfWeeks; weekNum >= 1; weekNum--) {
		   reschedAttemptsSameWeek = 0;

		   //System.out.println("\nscheduleUnrestrictedGames: for next week: " + weekNum);
		   initPromotionInfo(schedule);
		   unscheduledTeams.clear();

		   while (!scheduleUnrestrictedWeek(schedule, weekNum)) {
			   // Backtracking logic
			   // if continue - not ready to terminate
			   // unschedule weekNum, add a demotion penalty to the first scheduled game and increment a demotion count (0 to 1 for first time)
			   // unschedule weekNum - NEW: choose a different game to demote, start with the lowest scored scheduled game

			   // Clear demotion penalties of other games - unless demotion count > 1 (demoted twice) don't clear demotion penalty (a culprit)
			   // keep the demotion

			   if (reschedAttemptsMultiWeeksBack >= NflDefs.reschedAttemptsMultiWeeksBackLimit) {
				   //if (true) {
				   //if (iterNum >= 50) {
				   // Done - no more rescheduling attempts left to try
		
/*
				   System.out.println("Scheduler: Failed to schedule all unrestricted games in week: " + weekNum + " remaining games are:");
				   for (int gi=0; gi < schedule.unscheduledGames.size(); gi++) {
					   NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
					   NflTeamSchedule homeTeam = usgame.homeTeamSchedule;
					   NflTeamSchedule awayTeam = usgame.awayTeamSchedule;

					   //System.out.println("Scheduler: Unscheduled: home team " + usgame.game.homeTeam + ", away team: " + usgame.game.awayTeam);
				   }
*/
				   terminationReason = "Failed to schedule all unrestricted games in week: " + weekNum + ", low Week: " + lowestWeekNum;
				   status = false;
				   break;
			   }
			   
			   else if (reschedAttemptsOneWeekBack >= NflDefs.reschedAttemptsOneWeekBackLimit) {
				   // Now back track multiple weeks - single week back tracks exhausted
				   unscheduledTeams.clear();

				   goBackToWeekNum = Math.min(reschedAttemptedMaxWeek+5,NflDefs.numberOfWeeks-1);
				   reschedAttemptedMaxWeek = goBackToWeekNum;
				   reschedAttemptsMultiWeeksBack++;
				   reschedAttemptsOneWeekBack = 0;
				   reschedAttemptsSameWeek=0;
				   reschedLog.add("back tracking multiple weeks\n");
				   //System.out.println("back tracking multiple weeks\n");

			   }
			   else if (reschedAttemptsSameWeek >= NflDefs.reschedAttemptsSameWeekLimit) {
				   // Now back track a single week - cur week reschedules exhausted
				   unscheduledTeams.clear();

				   goBackToWeekNum = Math.min(weekNum+1,NflDefs.numberOfWeeks-1);
				   reschedAttemptedMaxWeek = Math.max(goBackToWeekNum,reschedAttemptedMaxWeek);
				   reschedAttemptsOneWeekBack++;
				   reschedAttemptsSameWeek=0;
				   reschedLog.add("back tracking a single week\n");
				   //System.out.println("back tracking a single week\n");
			   }
			   else {
				   // Now just reschedule the cur week until it's reschedules are exhausted

				   goBackToWeekNum = weekNum;
				   reschedAttemptedMaxWeek = Math.max(goBackToWeekNum,reschedAttemptedMaxWeek);
				   reschedAttemptsSameWeek++;
				   reschedLog.add("rescheduling the current week\n");
				   //System.out.println("rescheduling the current week\n");
			   }

			   boolean shouldClearHistory = true;
			   //System.out.println("Info: Resched: unscheduling weeks: " + weekNum + " to " + goBackToWeekNum);

			   for (int wn = weekNum; wn <= goBackToWeekNum; wn++) {
				   //weekNum = wn;
				   if (!unscheduleUnrestrictedWeek(schedule, wn, shouldClearHistory)) {
					   System.out.println("unscheduleUnrestrictedWeek - failed for week: " + wn);
					   return false;
				   }

				   //shouldClearHistory = false;
			   }
			   
			   // System.out.println("Iteration: " + iterNum + ", reschedAttemptsSameWeek: " + reschedAttemptsSameWeek + " for week: " + weekNum + ", reschedAttemptsOneWeekBack: " + reschedAttemptsOneWeekBack + ", reschedAttemptsMultiWeeksBack: " + reschedAttemptsMultiWeeksBack + ", reschedAttemptedMaxWeek: " + reschedAttemptedMaxWeek + ", goBackToWeekNum: " + goBackToWeekNum + "\n");
			   while (!updateDemotionInfo(schedule, goBackToWeekNum)) {
				   if (!unscheduleUnrestrictedWeek(schedule, goBackToWeekNum, shouldClearHistory)) {
					   System.out.println("unscheduleUnrestrictedWeek - failed for week: " + goBackToWeekNum);
					   return false;
				   }
				   goBackToWeekNum++;
				   if (goBackToWeekNum >= NflDefs.numberOfWeeks) {
		              System.out.println("updateDemotionInfo - backed up to week: " + goBackToWeekNum);
					  return false;
				   }
			   }

			   
			   //if (!updateDemotionInfo(schedule, goBackToWeekNum)) {
                  //System.out.println("updateDemotionInfo - failed");
				   //return false;
			   //}
			   
			   //if (!updatePromotionInfo(schedule)) {
               //  System.out.println("updatePromotionInfo - failed");
	           //   return false;
			   //}

			   reschedLog.add("reschedAttemptsSameWeek: " + reschedAttemptsSameWeek + " for week: " + weekNum + ", reschedAttemptsOneWeekBack: " + reschedAttemptsOneWeekBack + ", reschedAttemptsMultiWeeksBack: " + reschedAttemptsMultiWeeksBack + ", reschedAttemptedMaxWeek: " + reschedAttemptedMaxWeek + ", goBackToWeekNum: " + goBackToWeekNum + "\n");

			   weekNum = goBackToWeekNum;

			   //System.out.println("\nscheduleUnrestrictedGames: rescheduling for week: " + weekNum + ", reschedAttemptsSameWeek: " + reschedAttemptsSameWeek);
		   }

		   if (weekNum < lowestWeekNum) {
			   lowestWeekNum = weekNum;
		   }
		   
		   if (status == false) {
			   break;
		   }
	   }

	   return status;
   }
   
   public boolean unscheduleUnrestrictedWeek(NflSchedule schedule, int weekNum, boolean clearHistory) {
      //int unscheduledGameCount = 0;
      for (NflTeamSchedule teamSchedule: schedule.teamSchedules) {
          NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum-1];
          if (gameSched != null && !gameSched.restrictedGame) {
             if (!unscheduleGame(gameSched, weekNum, schedule)) {
                System.out.println("unscheduleUnrestrictedWeek - failed to unschedule game");
                return false;
             }
             
             if (clearHistory) {
            	if (gameSched.demotionPenalty > 0)
    		    //System.out.println("Clearing history: during week: " + weekNum + " DemotionPenalty was " + gameSched.demotionPenalty + ", weekSequence: " + gameSched.weekScheduleSequence + " for game: " + gameSched.game.homeTeam + " : " + gameSched.game.awayTeam);
                //gameSched.weekScheduleSequence = 0;
            	gameSched.demotionCount = 0;
            	gameSched.demotionPenalty = 0;
             }
             //unscheduledGameCount++;
          }
       }
       //System.out.println("unscheduleUnrestrictedWeek: unscheduled: " + unscheduledGameCount + ", for week: " + weekNum + ", totalUnscheduled: " + schedule.unscheduledGames.size());

      
	   return true;
   }
   
/*
 * Not used
   public static NflGameSchedule scheduleNextUnrestrictedByeInWeek(NflSchedule schedule, int weekNum) {
	   NflGameSchedule bye = null;
	   
	   // Get list of all teams without Byes
	   // Collect all the unscheduled games where both teams don't have scheduled byes
	   // Collect this weeks scheduled games where both teams don't have byes
	   // Find teams without byes that are not scheduled this week
	   
	   // First schedule
	   
	   // is there a pair of teams not scheduled for this week 
	   ArrayList<NflTeamSchedule> unscheduledTeams = new ArrayList<NflTeamSchedule>();
       schedule.unscheduledTeamsInWeek(weekNum, unscheduledTeams);
       
       // Ensure that all unscheduled teams do not have a bye - or we won't be able to complete the week with added byes
 	   for (int ti=0; ti < unscheduledTeams.size(); ti++) {
 	      NflTeamSchedule teamSchedule = unscheduledTeams.get(ti);
 	    	  if (!teamSchedule.hasScheduledBye()) {
 	    		  return null;
 	      }
 	   }

 	   // if there are unscheduledTeams in the week, construct a bye from 2 of those teams
       if (unscheduledTeams.size() >= 2) {
          bye = schedule.unscheduledByes.remove(0);
    	      bye.homeTeamSchedule = unscheduledTeams.get(0);
    	      bye.awayTeamSchedule = unscheduledTeams.get(1);
    	      bye.game.homeTeam = bye.homeTeamSchedule.team.teamName;
    	      bye.game.awayTeam = bye.awayTeamSchedule.team.teamName;
    	      // must place this bye on the schedule
       }
       else {
    	      // Must choose a pairing from the scheduled games/teams of this week - where both teams do not yet have a bye
    	      ArrayList<NflGameSchedule> candidateGames = new ArrayList<NflGameSchedule>();
    	      
    		  for (int ti=0; ti < teams.size(); ti++) {
                 NflTeamSchedule teamSchedule = schedule.teams.get(ti);
                 if (teamSchedule.scheduledGames[weekNum-1] != null) {
                    NflGameSchedule game = teamSchedule.scheduledGames[weekNum-1];
                    if (game.homeTeamSchedule.hasScheduledBye()) {
                       continue;
                    }
                    if (game.awayTeamSchedule.hasScheduledBye()) {
                       continue;
                    }
                    candidateGames.add(game);
    			 }
              }
    		  
    		  if (candidateGames.isEmpty()) {
    			  return null;
    		  }

    	      // unschedule a game from this week - and construct a bye from the matchup - and place it on the schedule
    		  // randomly choose a game from the candidateGames
    	      int randomNextInt = rnd.nextInt(candidateGames.size());
    	      NflGameSchedule game = candidateGames.get(randomNextInt);

    	      unscheduleGame(game, weekNum, schedule);
    	      
    	      // construct the bye
              bye = schedule.unscheduledByes.remove(0);
    	      bye.homeTeamSchedule = game.homeTeamSchedule;
    	      bye.awayTeamSchedule = game.awayTeamSchedule;
    	      bye.game.homeTeam = bye.homeTeamSchedule.team.teamName;
    	      bye.game.awayTeam = bye.awayTeamSchedule.team.teamName;
       }
       
       // place the chosen bye onto the schedule

       if (!placeGameInSchedule(bye, weekNum, schedule)) {
          System.out.println("failed to place unrestricted bye, weekNum: " + weekNum + " home team: " + bye.game.homeTeam + " away team: " + bye.game.awayTeam);
    	      return null;
       }
       System.out.println("scheduled unrestricted bye, weekNum: " + weekNum + " home team: " + bye.game.homeTeam + " away team: " + bye.game.awayTeam);

 	   return bye;
   }
*/
   
   public boolean unscheduleGame(NflGameSchedule gameSched, int weekNum, NflSchedule schedule) {
	  // remove from the scheduled games of both home and away teams (set null)
	  // add game back into the unscheduled list
	  // deduct any attribute usage - see/study place a game in a schedule
      NflTeamSchedule homeTeam = gameSched.homeTeamSchedule;
      NflTeamSchedule awayTeam = gameSched.awayTeamSchedule;
	     
	  if (homeTeam == null) {
         System.out.println("ERROR: can't find home team: " + gameSched.game.homeTeam);
	     return false;
	  }
	      
	  if (!gameSched.isBye && awayTeam == null) {
	     System.out.println("ERROR: can't find away team: " + gameSched.game.awayTeam);
	     return false;
	  }
	      
	   //System.out.println("Unscheduling game for home team: " + gameSched.game.homeTeam + ", and away Team: " + gameSched.game.awayTeam + ", in week: " + weekNum
	   //      + ", schedseq: " + gameSched.weekScheduleSequence + ", demotion Penalty: " + gameSched.demotionPenalty
	   //      + ", demotion count: " + gameSched.demotionCount + ",score: " + gameSched.score);
	      
	   // Validate that the game is scheduled in that week for both team
	      
	  if (homeTeam.scheduledGames[weekNum-1] != gameSched) {
	     System.out.println("ERROR: game unexpectedly not scheduled in week: " + weekNum + " for home team: " + gameSched.game.homeTeam);
	     return false;
	  }
	  
	  if (!gameSched.isBye && awayTeam.scheduledGames[weekNum-1] != gameSched) {
	     System.out.println("ERROR: game unexpectedly not scheduled in week: " + weekNum + " for away team: " + gameSched.game.awayTeam);
	     return false;
	  }

	   // remove the game from the scheduled array for each team at the weeknum-1 index
	   homeTeam.scheduledGames[weekNum-1] = null;   
	   if (!gameSched.isBye) {
          awayTeam.scheduledGames[weekNum-1] = null;
	   }
	      	          
	   gameSched.weekNum = 0;
	      
       // add the game back to the unscheduled arraylist for the schedule
	   if (gameSched.isBye) {
		  //System.out.println("Unscheduling Bye in week: " + weekNum + " for home team: " + gameSched.game.homeTeam);
	      schedule.unscheduledByes.add(gameSched);
	   }
	   else {
          schedule.unscheduledGames.add(gameSched);
		  //schedule.allGames.add(gameSched);
	   }
       
	   if (!gameSched.isBye) {
		  
		  // Remove stadium resource usage in weeknum
	      if (homeTeam.team.stadium != null) {
	         String stadiumName = homeTeam.team.stadium;
	          
	         NflResourceSchedule resourceSchedule = schedule.findResource(stadiumName);
	         if (resourceSchedule != null) {
	        	 resourceSchedule.usage[weekNum-1] -= 1;
	         }
	      }
	   }
	   else {
	      
	      // Remove bye resource usage in weeknum
	      NflResourceSchedule byeResourceSchedule = schedule.findResource("bye");
	      if (byeResourceSchedule != null) {
	    	  byeResourceSchedule.usage[weekNum-1] -= 1;
	      }
	      //System.out.println("unscheduled bye, weekNum: " + weekNum + " home team: " + gameSched.game.homeTeam);
	   }
	   
      return true;
   }
   
   public boolean updateDemotionInfo(NflSchedule schedule, int weekNum) {
	  // loop through unscheduled games
	  // if demotionCount <= 1 demotion penalty = 0
	  // if demotionCount > 1, keep demotion penalty unmodified
	   
	  // Find the partialScheduleEntry for weekNum
	  // Find the "Game in Week" with the highest penalty, remove it and demote it
	   
	  NflPartialScheduleEntry partialScheduleEntry = partialSchedules[weekNum-1];
	  NflGameSchedule gameScheduledLast = null;
	  for (NflGameSchedule gameSched: partialScheduleEntry.gamesInWeek) {
		  if (gameScheduledLast == null || 
              gameSched.weekScheduleSequence > gameScheduledLast.weekScheduleSequence) {
			  gameScheduledLast = gameSched;
			  continue;
		  }		  
	  }
 
	  if (gameScheduledLast == null) {
		  return false;
	  }
	  
	  partialScheduleEntry.gamesInWeek.remove(gameScheduledLast);
	  
	  gameScheduledLast.demotionPenalty = 2;
	  		 		         
	  return true;
   }
	  
   /*
    * 
   public boolean updateDemotionInfo(NflSchedule schedule, int weekNum) {
	  // loop through unscheduled games
	  // if demotionCount <= 1 demotion penalty = 0
	  // if demotionCount > 1, keep demotion penalty unmodified
	   
	  // Find the partialScheduleEntry for weekNum
	  // Find the "Game in Week" with the highest penalty, remove it and demote it
	  for (int gi=0; gi < schedule.unscheduledGames.size(); gi++) {
         NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
		 
		 // The 1st scheduled game in the week is suspect, gets a demotion penalty and demotion count
		 if (usgame.weekScheduleSequence == 1) {
			 usgame.demotionCount++;
			 usgame.demotionPenalty = usgame.demotionCount*2;
		     //System.out.println("Demotion update1: during week: " + weekNum + " updateDemotionPenalty to " + usgame.demotionPenalty + ", weekSequence: " + usgame.weekScheduleSequence + " for game: " + usgame.game.homeTeam + " : " + usgame.game.awayTeam);
		 }
		 // any game that has been demoted more than once is suspect, and gets an increased demotion penalty
		 else if (usgame.demotionCount > 1) {
			 usgame.demotionPenalty = usgame.demotionCount*2;
		     //System.out.println("Demotion update2: during week: " + weekNum + " updateDemotionPenalty to " + usgame.demotionPenalty + ", weekSequence: " + usgame.weekScheduleSequence + " for game: " + usgame.game.homeTeam + " : " + usgame.game.awayTeam);
		 }
		 // all other games - have no demotion penalty and deserve an increased chance of scheduling into the next week
		 else {
			 //if (usgame.demotionPenalty > 0)
		     //    System.out.println("Demotion update3: during week: " + weekNum + " updateDemotionPenalty to zero from demotionPenalty of " + usgame.demotionPenalty + ", weekSequence: " + usgame.weekScheduleSequence + " for game: " + usgame.game.homeTeam + " : " + usgame.game.awayTeam);
			 usgame.demotionPenalty = 0;
			 usgame.demotionCount = 0;
		 }
		 
		 usgame.weekScheduleSequence = 0;
      }
		         
	  return true;
   }
	  
    */
   public boolean initPromotionInfo(NflSchedule schedule) {
	   for (int gi=0; gi < schedule.unscheduledGames.size(); gi++) {
          NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
	      if (unscheduledTeams.contains(usgame.homeTeamSchedule) || 
              unscheduledTeams.contains(usgame.awayTeamSchedule)) {
	    	  usgame.promotionScore = 0.0;
	      }
       }
		         
	   return true;
   }
   
   public boolean updatePromotionInfo(NflSchedule schedule) {
	   for (int gi=0; gi < schedule.unscheduledGames.size(); gi++) {
          NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
	      if (unscheduledTeams.contains(usgame.homeTeamSchedule) || 
              unscheduledTeams.contains(usgame.awayTeamSchedule)) {
	    	  usgame.promotionScore += -0.05;
	      }
       }
		         
	   return true;
   }

   public boolean openSchedAttemptsLogFile() {
      try {
         schedAttemptsLogFw = new FileWriter("logSchedAttempts.csv");
         schedAttemptsLogBw = new BufferedWriter(schedAttemptsLogFw);
		         
         // write the header to the file
         schedAttemptsLogBw.write("SchedAttempt,status,# hard vios,alerts,score,iters,low week,unsched games,sched name,hard vios\n");
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } 

      return true;
   }
		
   public boolean closeSchedAttemptsLogFile() {
      if (schedAttemptsLogBw != null) {
         try {
        	 schedAttemptsLogBw.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return true;
   }

   public boolean logSchedAttempt(int schedAttempts, NflSchedule sched, int iterNum, int lowestWeekNum, String savedScheduleName) {
      try {
         // write the schedule info to the file
    	 boolean schedComplete = sched.unscheduledGames.size() == 0;
         
         schedAttemptsLogBw.write(schedAttempts + "," + (schedComplete ? "complete" : "failed") + "," + 
        		                  (schedComplete ? sched.hardViolationCount : "") + "," + (schedComplete ? sched.alerts.size() : "") + "," +
        		                  (schedComplete ? sched.score : "") + "," + iterNum + "," + (schedComplete ? "" : lowestWeekNum) + "," +
        		                  (schedComplete ? "" : sched.unscheduledGames.size()) + "," + savedScheduleName + "," +
        		                  (schedComplete ? sched.hardViolations : "") + "\n");
         
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } 

      return true;
   }

   public boolean openBriefLogFile() {
      try {
         briefLogFw = new FileWriter("logBriefSchedResults.csv");
         briefLogBw = new BufferedWriter(briefLogFw);
		         
         // write the header to the file
         briefLogBw.write("Week,Sched Games,Sched Byes,Unsched Games,Unsched Byes,Unsched Teams,Team1,Team2,Team3,Team4\n");
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } 

      return true;
   }
	   
   public boolean closeBriefLogFile() {
      if (briefLogBw != null) {
         try {
            briefLogBw.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return true;
   }

   public boolean briefLogWeek(NflSchedule schedule, int weekNum) {
	   // for each team - check if game scheduled for this week - count them, keep a list of teams unscheduled
	   // count the byes too
	   // count the unscheduled games and the unscheduled byes
	   // write: weekNum, scheduled games, scheduled byes, unscheduled games, unscheduled byes, comma separated unscheduled teams 
	   ArrayList<NflTeamSchedule> unscheduledTeams = new ArrayList<NflTeamSchedule>();
	   ArrayList<NflGameSchedule> scheduledGames = new ArrayList<NflGameSchedule>();
	   ArrayList<NflGameSchedule> scheduledByes = new ArrayList<NflGameSchedule>();
	   ArrayList<NflGameSchedule> unscheduledGames = new ArrayList<NflGameSchedule>();
	   ArrayList<NflGameSchedule> unscheduledByes = new ArrayList<NflGameSchedule>();

	   for (NflTeamSchedule teamSchedule: schedule.teamSchedules) {
		   NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum-1];
		   if (gameSched != null) {
			   if (gameSched.isBye) {
				   scheduledByes.add(gameSched);
			   }
			   else {
				   if (!scheduledGames.contains(gameSched)) {
					   scheduledGames.add(gameSched);
				   }
			   }
		   }
		   else {
			   unscheduledTeams.add(teamSchedule);
		   }
	   }

	   // for (int gi=0; gi < schedule.unscheduledGames.size(); gi++) {
	   for (NflGameSchedule usgame: schedule.unscheduledGames) {
		   if (usgame.isBye) {
			   unscheduledByes.add(usgame);
		   }
		   else {
			   unscheduledGames.add(usgame);
		   }

		   //System.out.println("Scheduler: Unscheduled: home team " + usgame.game.homeTeam + ", away team: " + usgame.game.awayTeam);
	   }


	   if (briefLogBw != null) {
		   try {
			   briefLogBw.write(weekNum + "," + scheduledGames.size() + "," + scheduledByes.size() + "," + unscheduledGames.size() + "," + unscheduledByes.size() + "," + unscheduledTeams.size());
			   for (NflTeamSchedule unschedTeam: unscheduledTeams) {
				   briefLogBw.write("," + unschedTeam.team.teamName);
			   }
			   if (unscheduledTeams.size() == 0) {
				   briefLogBw.write(",0,0");
			   }

			   Collections.sort(scheduledGames, NflGameSchedule.GameScheduleComparatorBySchedSequence);
			   int gameLogLimit = 0;
			   for (NflGameSchedule schedGame: scheduledGames) {
				   if (schedGame.restrictedGame) {
					   continue;
				   }
				   String gameInfo = "S:" + schedGame.weekScheduleSequence + ":" + schedGame.game.homeTeam.substring(0, 3) + ":" + schedGame.game.awayTeam.substring(0,3) + ":" + schedGame.score + ":" + schedGame.demotionPenalty;
				   briefLogBw.write("," + gameInfo);
				   gameLogLimit++;
				   if (gameLogLimit >= 3) {
					   break;
				   }
			   }

			   Collections.sort(unscheduledGames, NflGameSchedule.GameScheduleComparatorByDemotion);
			   gameLogLimit = 0;
			   for (NflGameSchedule usGame: unscheduledGames) {
				   String gameInfo = "U:" + usGame.game.homeTeam.substring(0,3) + ":" + usGame.game.awayTeam.substring(0,3) + ":" + usGame.score + ":" + usGame.demotionPenalty + ":" + usGame.demotionCount;
				   briefLogBw.write("," + gameInfo);
				   gameLogLimit++;
				   if (gameLogLimit >= 3) {
					   break;
				   }
			   }
			   briefLogBw.write("\n");

		   } catch (IOException e) {
			   e.printStackTrace();
		   }
	   }

	   return true;
   }

	   
   public boolean openPartialScheduleLogFile() {
      try {
         partialScheduleLogFw = new FileWriter("logPartialScheduleResults.csv");
         partialScheduleLogBw = new BufferedWriter(partialScheduleLogFw);

         // write the header to the file
         //partialScheduleLogBw.write("FingerPrint,Week,Iteration,Unscheduled,BaseFP,Count \n");
         partialScheduleLogBw.write("FingerPrint,Week,Iteration,Unscheduled,BaseFP,Count,GamesInWeek,HighSeqNum \n");
	  } catch (FileNotFoundException e) {
         e.printStackTrace();
	  } catch (IOException e) {
		 e.printStackTrace();
	  } 

	  return true;
   }
   
   public boolean closePartialScheduleLogFile() {
      if (partialScheduleLogBw != null) {
         try {
            partialScheduleLogBw.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return true;
   }

   public boolean logPartialScheduleHistory(NflSchedule schedule, int weekNum) {
	   // Construct a record for the full partial schedule up to this week
	   // Use the partial schedule fingerprint from the previous week to add onto for this week using hashcodes and the weeknum
	   
	   // Create a partial schedule entry for weekNum
	   NflPartialScheduleEntry partialScheduleEntry = new NflPartialScheduleEntry();
	   	   
	   // use a local object to access the partial schedules entry for weekNum
	   // and populate it with current schedule state information for this week
	   
	   partialScheduleEntry.unscheduledTeams = 0;
	   partialScheduleEntry.fingerPrint = 0.0;
	   partialScheduleEntry.baseFingerPrint = 0.0;
	   partialScheduleEntry.count = 0;
	   partialScheduleEntry.gamesInWeek = new ArrayList<NflGameSchedule>();

	   // Initialize base fingerprint append new fingerprint info to - from the previous weeks fingerprint
	   // Unless this is the very last week in the schedule - where there is no previous week to get a fingerprint from 
	   
	   if (weekNum < NflDefs.numberOfWeeks) {
		   partialScheduleEntry.baseFingerPrint = partialSchedules[weekNum].fingerPrint;
		   partialScheduleEntry.fingerPrint = partialScheduleEntry.baseFingerPrint;
	   }

	   // Extend the partial schedule fingerprint from the previous weeks from the games of this week
	   // using the hash code of each game as a unique id of each scheduled game for this week
	   // Also keep track of the unscheduled games for this week
	          
	   for (NflTeamSchedule teamSchedule: schedule.teamSchedules) {
		   NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum-1];
		   if (gameSched != null) {
			   partialScheduleEntry.fingerPrint += (double) gameSched.hashCode()*Math.pow(NflDefs.numberOfWeeks-weekNum+1,2);
			   if (!gameSched.isBye && !partialScheduleEntry.gamesInWeek.contains(gameSched)) {
			      partialScheduleEntry.gamesInWeek.add(gameSched);
			   }
		   }
		   else {
			   partialScheduleEntry.unscheduledTeams++;
		   }
	   }
	   
	   // Update the partial schedule fingerprint in the partial schedule repository
	   // if All teams were scheduled for this week
	   
	   if (partialScheduleEntry.unscheduledTeams == 0) {
		  if (fingerPrintMap.containsKey(partialScheduleEntry.fingerPrint)) {
			 partialScheduleEntry = fingerPrintMap.get(partialScheduleEntry.fingerPrint);
		  }
		  else {
		     fingerPrintMap.put(partialScheduleEntry.fingerPrint, partialScheduleEntry);
		  }
		  
		  //partialScheduleEntry.iterNum = iterNum;
		  //partialScheduleEntry.weekNum = weekNum;
          partialScheduleEntry.count += 1;
   	      partialSchedules[weekNum-1] = partialScheduleEntry; // store the partial schedule entry for this week

		  try {
	         // write the partial schedule log entry to the csv file
             int highSeqNum = 0;
      	     for (NflGameSchedule gameInWeek: partialScheduleEntry.gamesInWeek) {
      	    	 if (gameInWeek.weekScheduleSequence > highSeqNum) {
      	    		 highSeqNum = gameInWeek.weekScheduleSequence;
      	    	 }
      	     }

			 partialScheduleLogBw.write(partialScheduleEntry.fingerPrint + "," + weekNum + "," + iterNum + "," + partialScheduleEntry.unscheduledTeams + "," + 
	                                    partialScheduleEntry.baseFingerPrint + "," + partialScheduleEntry.count + "," + partialScheduleEntry.gamesInWeek.size() +
	                                    "," +  highSeqNum + "\n");
		  } catch (IOException e) {
			 e.printStackTrace();
		  } 
	   }
       partialScheduleEntry.iterNum = iterNum;
       partialScheduleEntry.weekNum = weekNum;
       //partialScheduleEntry.count += 1;
	   partialSchedules[weekNum-1] = partialScheduleEntry; // store the partial schedule entry for this week
	   
	   schedule.latestScheduleFingerPrint = partialScheduleEntry.fingerPrint;

	   return true;
   }
   
   /*
    * 
   public boolean logPartialScheduleHistory(NflSchedule schedule, int weekNum) {
	   // Construct a record for the full partial schedule up to this week
	   // Use the partial schedule fingerprint from the previous week to add onto for this week using hashcodes and the weeknum
	   
	   // Create the partial schedule entry if not yet created for weekNum
	   if (partialSchedules[weekNum-1] == null) {
		   partialSchedules[weekNum-1] = new NflPartialScheduleEntry();
	   }
	   
	   // use a local object to access the partial schedules entry for weekNum
	   // and populate it with current schedule state information for this week
	   
	   NflPartialScheduleEntry partialScheduleEntry = partialSchedules[weekNum-1];
	   partialScheduleEntry.iterNum = iterNum;
	   partialScheduleEntry.weekNum = weekNum;
	   partialScheduleEntry.unscheduledTeams = 0;
	   partialScheduleEntry.fingerPrint = 0.0;
	   partialScheduleEntry.baseFingerPrint = 0.0;
	   partialScheduleEntry.count = 0;

	   // Initialize base fingerprint append new fingerprint info to - from the previous weeks fingerprint
	   // Unless this is the very last week in the schedule - where there is no previous week to get a fingerprint from 
	   
	   if (weekNum < NflDefs.numberOfWeeks) {
		   partialScheduleEntry.baseFingerPrint = partialSchedules[weekNum].fingerPrint;
		   partialScheduleEntry.fingerPrint = partialScheduleEntry.baseFingerPrint;
	   }

	   // Extend the partial schedule fingerprint from the previous weeks from the games of this week
	   // using the hash code of each game as a unique id of each scheduled game for this week
	   // Also keep track of the unscheduled games for this week
	          
	   for (NflTeamSchedule teamSchedule: schedule.teamSchedules) {
		   NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum-1];
		   if (gameSched != null) {
              partialScheduleEntry.fingerPrint += (double) gameSched.hashCode()*Math.pow(NflDefs.numberOfWeeks-weekNum+1,2);
		   }
		   else {
              partialScheduleEntry.unscheduledTeams++;
		   }
	   }
	   
	   // Update the partial schedule fingerprint in the partial schedule repository
	   // if All teams were scheduled for this week
	   
	   if (partialScheduleEntry.unscheduledTeams == 0) {
          Integer count = 1;

		  if (fingerPrintMap.containsKey(partialScheduleEntry.fingerPrint)) {
             NflPartialScheduleEntry partialSchedule = fingerPrintMap.get(partialScheduleEntry.fingerPrint);
             partialSchedule.count += 1;
             count = partialSchedule.count;
	         //count = fingerPrintMap.get(partialScheduleEntry.fingerPrint);
			 //count += 1;
		  }
		  
		  //fingerPrintMap.put(partialScheduleEntry.fingerPrint, count);
		  try {
	         // write the partial schedule log entry to the csv file
			 partialScheduleLogBw.write(partialScheduleEntry.fingerPrint + "," + weekNum + "," + iterNum + "," + partialScheduleEntry.unscheduledTeams + "," + partialScheduleEntry.baseFingerPrint + "," + count + "\n");
		  } catch (IOException e) {
			 e.printStackTrace();
		  } 
	   }
	   
	   schedule.latestScheduleFingerPrint = partialScheduleEntry.fingerPrint;

	   return true;
   }
    * 
    */
}



