package com.nflscheduling;

import java.util.*;

import com.nflscheduling.NflDefs.AlgorithmType;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

public class NflScheduler {

   // Algorithm Top Level is in the static main function
   // curSchedule holds the partial schedule (so far)
	
   // Instance data

   public ArrayList<NflSchedule> schedules;        // schedule[i].teamSchedules holds the partial schedule
                                                   // Each teamSchedule holds array of scheduledGames
                                                   // as well as team info (name, stadium, etc)

   public NflSchedule curSchedule;                 // curSchedule.teams holds the partial schedule
                                                   // Each team has an array of scheduled games - 1 per week
                                                   // Byes are scheduled games marked with isBye=true
                                                   // Holds arrays of allGames, unscheduledGames, unscheduledByes
   public NflScheduleByWeekAlg algorithm;

   public ArrayList<NflRestrictedGame> restrictedGames; // The restrictedGames are games forced into fixed/specified weeks
                                                        // Some are pre-defined in file nflforcedgames.csv
   
   public ArrayList<NflResource> resources;
   public ArrayList<NflGame> games;    // base class instances of games - to be turned in NflGameSchedule instances
                                       // loaded from nflgames.csv within function loadGames()
   
   public ArrayList<NflTeam> teams;    // base class instances of teams - to be turned into NflTeamSchedule instances
                                       // loaded from nflteams.csv within function loadTeams()

   public LinkedHashMap<String,NflGameMetric> gameMetrics = new LinkedHashMap<String,NflGameMetric>();
   public LinkedHashMap<String,NflScheduleMetric> scheduleMetrics = new LinkedHashMap<String,NflScheduleMetric>();

   public int reschedWeekNum = 0;      // TBD: Not set or used, except as a logging output (in this file)
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
   public int scheduleAttempts;

   public String terminationReason;
   
   //  Demotion Scheme: TBD document it
   //  Promotion scheme is not used - promotionInfo
   
   /* Issues identified by Ted
    * Can't have a back to back matchup with only a bye in between, has to have at least one game for both teams 
    * Streamline by checking for - if (!usgame.game.findAttribute("division")) {
    * Only divisional games can have repeat matchups

    * Basic functionality in NflGMetNoRepeatedMatchup, turned on sched metric for alert if repeated matchup
    * Could be a hole, due to a forced game where a bye could slip in: 
    * forcedGame  Hole  NewGame scheduled - then bye slips into hole, could fix within bye scheduling
    * Divisional teams shouldn't play each other twice in the first 5 weeks of the season, they should wait until at least week 6 for a rematch 
    * probably need a new game metric to push for this, then a schedule metric
    *      if (!usgame.game.findAttribute("division")) {
    *      NflGMetDivisionalSeparation
    * Byes should start no later than week 5. byes in weeks 4,12, 13 can be optional - now works, Bye resource can specify this             
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
   public BufferedWriter reschedLogBw = null;
   public FileWriter reschedLogFw = null;
   public boolean reschedLogOn = true;

   public boolean init() {
      loadParams();                                 // load from nflparams.csv: NflDefs.numberOfWeeks, NflDefs.numberOfTeams
      // resched limit params are hard-coded in here, TBD: should get from a file
      games = new ArrayList<NflGame>();
      teams = new ArrayList<NflTeam>();
      resources = new ArrayList<NflResource>();

      loadTeams(teams); // base teams created globally in NflScheduler
      loadGames(games); // base games created globally in NflScheduler
      loadResources();
      loadMetrics();

      if (NflDefs.algorithmType == AlgorithmType.Forward) {
         algorithm = new NflScheduleAlgForOrBack();
         NflDefs.schedulingDirection = 1;
         algorithm.init(this);
      }
      else if (NflDefs.algorithmType == AlgorithmType.Backward) {
         algorithm = new NflScheduleAlgForOrBack();
         NflDefs.schedulingDirection = -1;
         algorithm.init(this);
      }
      else if (NflDefs.algorithmType == AlgorithmType.ForwardAndBackward) {
         algorithm = new NflScheduleAlgForAndBack();
         algorithm.init(this);
      }
      
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

   public boolean generateSchedules() throws CloneNotSupportedException {
      // ---------- Schedule Initialization --------------------
      // create next curSchedule
      // initialize unscheduledGames of the curSchedule from all the modeled games

      openSchedAttemptsLogFile();
      openReschedLogFile();

      for (scheduleAttempts = 1; scheduleAttempts <= NflDefs.scheduleAttempts; scheduleAttempts++) {
         rnd = new Random();

         curSchedule = new NflSchedule();
         curSchedule.init(teams, games, resources, scheduleMetrics,gameMetrics);
         NflWeeklyData.init(curSchedule.allGames.get(0));  // init weekly logging data structure
         algorithm.curSchedule = curSchedule;
         algorithm.scheduleInit();

         // Schedule games that are restricted - according to the restrictedGames
         scheduleForcedGames(restrictedGames, algorithm.curSchedule);

         reschedLog = new ArrayList<String>();
         openBriefLogFile();
         openPartialScheduleLogFile();
 
         // Schedule the remaining unrestricted games
         algorithm.scheduleUnrestrictedGames(algorithm.curSchedule);

         closeBriefLogFile();
         closePartialScheduleLogFile();
         String savedScheduleFileName = new String();

         if (curSchedule.unscheduledGames.size() == 0) {
            curSchedule.computeMetrics();
            algorithm.terminationReason = "Schedule Metric: " + algorithm.curSchedule.score + " , Alerts: " + curSchedule.alerts.size()
                  + " , hard violations: " + curSchedule.hardViolationCount + " vios: " + curSchedule.hardViolations;
            if (curSchedule.hardViolationCount <= NflDefs.hardViolationLimit
                  && curSchedule.alerts.size() <= NflDefs.alertLimit) {
               schedules.add(curSchedule);
               savedScheduleFileName = "curSchedule" + schedules.size() + ".csv";
               writeScheduleCsv(curSchedule, savedScheduleFileName);
               algorithm.terminationReason += ", " + savedScheduleFileName;
            }
         }

         System.out.println("Schedule: " + scheduleAttempts + ", iterations: " + algorithm.iterNum + ", " + algorithm.terminationReason);

         logSchedAttempt(scheduleAttempts, curSchedule, algorithm.iterNum, algorithm.lowestWeekNum, savedScheduleFileName);

         if (schedules.size() >= NflDefs.savedScheduleLimit) {
            break; // hit the limit of saved schedules
         }
      }

      closeSchedAttemptsLogFile();
      closeReschedLogFile();

      return true;
   }

   public static void main(String[] args) throws CloneNotSupportedException {
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
               } else if (token[0].equalsIgnoreCase("NumberOfTeams")) {
                  NflDefs.numberOfTeams = Integer.parseInt(token[1]);
                  if (NflDefs.numberOfTeams <= 0 || NflDefs.numberOfTeams > 50) {
                     System.out.println("loadParams: NumberOfTeams invalid" + NflDefs.numberOfTeams);
                  }
                  System.out.println("loadParams: NumberOfTeams set to " + NflDefs.numberOfTeams);
               } else if (token[0].equalsIgnoreCase("ReschedLogOn")) {
                  NflDefs.reschedLogOn = Boolean.parseBoolean(token[1]);
                  System.out.println("loadParams: ReschedLogOn set to " + NflDefs.reschedLogOn);
               } else if (token[0].equalsIgnoreCase("AlgorithmType")) {
                  NflDefs.algorithmType = AlgorithmType.valueOf(token[1]);
                  System.out.println("loadParams: Algorithm set to " + NflDefs.algorithmType.toString());
               } else if (token[0].equalsIgnoreCase("reschedAttemptsMultiWeeksBackLimit")) {
                  NflDefs.reschedAttemptsMultiWeeksBackLimit = Integer.parseInt(token[1]);
                  if (NflDefs.reschedAttemptsMultiWeeksBackLimit <= 0
                        || NflDefs.reschedAttemptsMultiWeeksBackLimit > 100) {
                     System.out.println("loadParams: reschedAttemptsMultiWeeksBackLimit invalid"
                           + NflDefs.reschedAttemptsMultiWeeksBackLimit);
                  }
                  System.out.println("loadParams: reschedAttemptsMultiWeeksBackLimit set to "
                        + NflDefs.reschedAttemptsMultiWeeksBackLimit);
               } else if (token[0].equalsIgnoreCase("reschedAttemptsOneWeekBackLimit")) {
                  NflDefs.reschedAttemptsOneWeekBackLimit = Integer.parseInt(token[1]);
                  if (NflDefs.reschedAttemptsOneWeekBackLimit <= 0 || NflDefs.reschedAttemptsOneWeekBackLimit > 100) {
                     System.out.println("loadParams: reschedAttemptsOneWeekBackLimit invalid"
                           + NflDefs.reschedAttemptsOneWeekBackLimit);
                  }
                  System.out.println("loadParams: reschedAttemptsOneWeekBackLimit set to "
                        + NflDefs.reschedAttemptsOneWeekBackLimit);
               } else if (token[0].equalsIgnoreCase("reschedAttemptsSameWeekLimit")) {
                  NflDefs.reschedAttemptsSameWeekLimit = Integer.parseInt(token[1]);
                  if (NflDefs.reschedAttemptsSameWeekLimit <= 0 || NflDefs.reschedAttemptsSameWeekLimit > 100) {
                     System.out.println(
                           "loadParams: reschedAttemptsSameWeekLimit invalid" + NflDefs.reschedAttemptsSameWeekLimit);
                  }
                  System.out.println(
                        "loadParams: reschedAttemptsSameWeekLimit set to " + NflDefs.reschedAttemptsSameWeekLimit);
               } else if (token[0].equalsIgnoreCase("scheduleAttempts")) {
                  NflDefs.scheduleAttempts = Integer.parseInt(token[1]);
                  if (NflDefs.scheduleAttempts <= 0 || NflDefs.scheduleAttempts > 2000) {
                     System.out.println("loadParams: scheduleAttempts invalid" + NflDefs.scheduleAttempts);
                  }
                  System.out.println("loadParams: scheduleAttempts set to " + NflDefs.scheduleAttempts);
               } else if (token[0].equalsIgnoreCase("savedScheduleLimit")) {
                  NflDefs.savedScheduleLimit = Integer.parseInt(token[1]);
                  if (NflDefs.savedScheduleLimit <= 0 || NflDefs.savedScheduleLimit > 100) {
                     System.out.println("loadParams: savedScheduleLimit invalid" + NflDefs.savedScheduleLimit);
                  }
                  System.out.println("loadParams: savedScheduleLimit set to " + NflDefs.savedScheduleLimit);
               } else if (token[0].equalsIgnoreCase("alertLimit")) {
                  NflDefs.alertLimit = Integer.parseInt(token[1]);
                  if (NflDefs.alertLimit < 0 || NflDefs.alertLimit > 100) {
                     System.out.println("loadParams: alertLimit invalid" + NflDefs.alertLimit);
                  }
                  System.out.println("loadParams: alertLimit set to " + NflDefs.alertLimit);
               } else if (token[0].equalsIgnoreCase("hardViolationLimit")) {
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
            } else {
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

            // game.weekNum = 0; // not scheduled yet

            games.add(game);

            // System.out.println("line token length: " + token.length);
            // System.out.println("Game: " + game.homeTeam + ":" + game.awayTeam);
            // for (int i=0; i < game.attribute.size(); i++)
            // System.out.println(" " + game.attribute.get(i));
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
      for (int gi = 0; gi < games.size(); gi++) {
         // NflGame game = games.get(gi);
         // game.weekNum = 0;
      }

      return true;
   }

   // load the full set of restrictedGames into the static global list of
   // restrictedGames
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

            if (teamName.equalsIgnoreCase("all")) {
               // for (int ti=0; ti < curSchedule.teams.size(); ti++) {
               for (int ti = 0; ti < teams.size(); ti++) {
                  // NflTeamSchedule teamSchedule = curSchedule.teams.get(ti);
                  // NflRestrictedGame restrictedGame = new
                  // NflRestrictedGame(teamSchedule.team.teamName, weekNum, restriction,
                  // otherTeamName, stadium);
                  NflTeam team = teams.get(ti);
                  NflRestrictedGame restrictedGame = new NflRestrictedGame(team.teamName, weekNum, restriction,
                        otherTeamName, stadium);
                  weeks.add(restrictedGame);
               }
            } else {
               NflRestrictedGame restrictedGame = new NflRestrictedGame(teamName, weekNum, restriction,
                     otherTeamName, stadium);
               weeks.add(restrictedGame);
               // System.out.println("line token length: " + token.length);
               // System.out.println("Week: " + restrictedGame.weekNum + ", Team: " +
               // restrictedGame.teamName + ":");
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

   // load the set of attribute limits from file nflattrlimits.csv into the base
   // schedule
   // GiantsJetsStadium,0,1

   public boolean loadResources() {
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
            for (int al = 0; al < resources.size(); al++) {
               NflResource aLim = resources.get(al);
               if (aLim.resourceName.equalsIgnoreCase(attrName)) {
                  // System.out.println("aLim.attrName: " + aLim.resourceName + " == " + "
                  // attrname: " + attrName + ", so reusing");
                  resource = aLim;
                  break;
               }
            }

            if (resource == null) {
               // System.out.println("creating a new nflAttrLimit " + token[0]);
               resource = new NflResource();
               resources.add(resource);
            }

            resource.resourceName = token[0];
            // resource.weekNum = Integer.parseInt(token[1]);
            int weekNum = Integer.parseInt(token[1]);
            // System.out.println(" weekNum " + resource.weekNum);
            // System.out.println(" weekNum " + weekNum);
            // System.out.println(" weeklyLimit size " + resource.weeklyLimit.length);
            // attrLimit.weeklyLimit.set(attrLimit.weekNum-1,Integer.parseInt(token[2]));
            resource.weeklyLimit[weekNum - 1] = Integer.parseInt(token[2]);
            resource.weeklyMinimum[weekNum - 1] = 0;
            if (token.length > 3) {
               resource.weeklyMinimum[weekNum - 1] = Integer.parseInt(token[3]);
            }

            // System.out.println("line token length: " + token.length);
            // System.out.println("AttrLimit: " + resource.resourceName + " : " +
            // resource.weekNum + " : " + resource.weeklyLimit[resource.weekNum-1] + " : " +
            // resource.weeklyMinimum[resource.weekNum-1]);
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
   // Done: For testing - quit after scheduling restricted games - so can just see
   // the restricted schedule
   // Done: Restricted byes - update the restrictedgames.csv with Ted's byes -
   // rename the file to forcedgames.csv
   // Byes are stored in games - (1) add to games
   // Byes are the hometeam in a game, and have the isBye attribute set
   // When scheduling a restricted bye, look for the games that are byes with the
   // hometeam matching - should only be one
   // test by quiting after scheduling restricted games, and see that all the byes
   // made it onto the schedule
   //

   public boolean scheduleForcedGames(ArrayList<NflRestrictedGame> restrictedGames, NflSchedule schedule) {
      // for each restricted game
      // Currently support (only) the hometeam and division restrictions for a given
      // week
      // Also support an optional named opponent - otherwise the scheduler will choose
      // the opponent
      //
      // Create a list of candidate games based on hometeam, opponent (if named),
      // division restrictions

      for (int wi = 0; wi < restrictedGames.size(); wi++) {
         NflRestrictedGame restrictedGame = restrictedGames.get(wi);
         int resWeekNum = restrictedGame.weekNum;
         String resTeamName = restrictedGame.teamName;
         NflTeamSchedule resTeam = schedule.findTeam(resTeamName);

         if (resTeam == null) {
            System.out.println("ERROR scheduling restricted game: can't find restricted team: " + resTeamName);
            return false;
         }

         String restriction = restrictedGame.restriction;

         if (!restriction.equalsIgnoreCase("division") && !restriction.equalsIgnoreCase("hometeam")
               && !restriction.equalsIgnoreCase("bye")) {
            System.out.println("ERROR scheduling restricted game: unrecognized restriction: " + restriction);
            return false;
         }

         String resOtherTeamName = restrictedGame.otherTeam;

         NflTeamSchedule resOtherTeam = null;
         if (!resOtherTeamName.isEmpty()) {
            resOtherTeam = schedule.findTeam(resOtherTeamName);
            if (resOtherTeam == null) {
               System.out.println(
                     "ERROR scheduling restricted game: can't find restricted resOtherTeam: " + resOtherTeamName);
               return false;
            }
         }

         String resStadium = restrictedGame.stadium;

         // System.out.println("Scheduling Restricted game for team: " + resTeamName + ",
         // weekNum: " + resWeekNum + ", restriction: " + restriction + ", otherTeam: " +
         // resOtherTeamName);

         // Validate that not already scheduled - may have been scheduled due to opponent
         // being scheduled in that week

         if (resTeam.scheduledGames[resWeekNum - 1] != null) {
            // System.out.println("Info: Restricted Game Scheduling: game already scheduled
            // in week: " + resWeekNum + " for restricted team: " + resTeamName);
            continue;
         }

         if (resOtherTeam != null && resOtherTeam.scheduledGames[resWeekNum - 1] != null) {
            // System.out.println("Info: Restricted Game Scheduling: game already scheduled
            // in week: " + resWeekNum + " for restricted other team: " + resOtherTeamName);
            continue;
         }

         // handle forced byes here, then continue to the next forced game/bye
         if (restriction.equalsIgnoreCase("bye")) {
            NflGameSchedule usBye = null;
            for (int bi = 0; bi < schedule.unscheduledByes.size(); bi++) {
               usBye = schedule.unscheduledByes.get(bi);
               // NflTeamSchedule homeTeam = usBye.homeTeamSchedule;

               if (!resTeamName.equalsIgnoreCase(usBye.game.homeTeam)) {
                  continue;
               }

               usBye.candidateCount = 1;

               placeGameInSchedule(usBye, resWeekNum, schedule);
               usBye.restrictedGame = true;
               // System.out.println("scheduled restricted bye, weekNum: " + resWeekNum + "
               // home team: " + usBye.game.homeTeam);

               break;
            }

            if (usBye == null || !usBye.restrictedGame) {
               // System.out.println("ERROR: unable to find and schedule restricted bye,
               // weekNum: " + resWeekNum + " home team: " + resTeamName);
            }

            continue;
         }

         // Make a list of qualifying unscheduled games
         //
         // Find unscheduled games for the restricted game team
         // that are qualified such that
         // 1a) restricted game "HomeTeam" attribute constraint is satisfied in the
         // unscheduled game
         // 1b) other restricted game attribute constraints are present in the
         // unscheduled game e.g. "Mexico"
         // Furthermore
         // 2) restricted game attribute constraints that match Global resource
         // constraints have remaining capacity
         // e.g. GiantsJetsStadium or Bye
         // TBD: 3) other team is not already scheduled for that week - with another team
         // - otherteam.scheduleGames
         // Done: when schedule / place a game - must remove that game from both
         // unscheduled lists and place in scheduled array
         // if qualified - add to list

         ArrayList<NflGameSchedule> qualifiedGames = new ArrayList<NflGameSchedule>();

         for (int gi = 0; gi < schedule.unscheduledGames.size(); gi++) {
            NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
            NflTeamSchedule homeTeam = usgame.homeTeamSchedule;
            NflTeamSchedule awayTeam = usgame.awayTeamSchedule;

            // Validate that the restricted Team must be either the hometeam or the awayTeam
            // (if division)
            if (resTeam != homeTeam && resTeam != awayTeam) {
               continue;
            }

            boolean qualified = true;

            // "hometeam" Handling - ensure the restricted team is the home team
            // also ensure that the opponent (if named) is the away team of the game
            if (restriction.equalsIgnoreCase("hometeam")) {
               if (!resTeamName.equalsIgnoreCase(usgame.game.homeTeam)) {
                  qualified = false;
                  // System.out.println(" Set qualified false 1a: " + resTeamName);
                  continue;
               }

               if (resOtherTeam != null && !resOtherTeamName.equalsIgnoreCase(usgame.game.awayTeam)) {
                  qualified = false;
                  // System.out.println(" Set qualified false 1b: " + resOtherTeamName);
                  continue;
               }
            }

            // "division" Handling - ensure the game is a divisional game - tagged with
            // "division" attribute
            // NOTE: already validated that the restricted team is in this game
            else if (restriction.equalsIgnoreCase("division")) {
               if (!usgame.game.findAttribute("division")) {
                  qualified = false;
                  // System.out.println(" Set qualified false 2: " + resTeamName + ", division
                  // attribute not found for game: hometeam: " + usgame.game.homeTeam + ",
                  // awayTeam: " + usgame.game.awayTeam);
                  continue;
               }
            }

            // Verify that neither the hometeam or awayteam are already scheduled in
            // resWeekNum
            if (homeTeam.scheduledGames[resWeekNum - 1] != null) {
               qualified = false;
            } else if (awayTeam != null && awayTeam.scheduledGames[resWeekNum - 1] != null) {
               qualified = false;
            }

            if (!qualified) {
               continue;
            }

            // unscheduled game is qualified so far

            // Verify that stadiums with a global resource have remaining capacity in the
            // restricted weeknum

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
            // System.out.println(" Qualifying game for team: " + usgame.homeTeam + " vs " +
            // usgame.awayTeam);

            // computeMetrics on the game
         }

         // All games have been checked for qualification
         if (qualifiedGames.size() == 0) {
            System.out.println("ERROR: no qualified games found for the teamweek: " + resWeekNum + ", " + resTeamName
                  + ", " + resOtherTeamName + ", " + restriction);
            return false;
         }

         for (NflGameSchedule usgame : qualifiedGames) {
            usgame.computeMetric(resWeekNum, schedule, qualifiedGames, "StadiumResource");
            // System.out.println("Info: Unrestricted Candidate game, weekNum: " + weekNum +
            // " home team: " + usgame.game.homeTeam + " away team: " + usgame.game.awayTeam
            // + ", score: " + usgame.score);
         }

         // sort the collection and choose the best game to schedule
         // candidateGames.sort(NflGameSchedule.GameScheduleComparator);

         NflGameSchedule chosenGame = chooseBestScoringGame(qualifiedGames);

         chosenGame.stadium = resStadium;
         chosenGame.candidateCount = 1;

         placeGameInSchedule(chosenGame, resWeekNum, schedule);

         chosenGame.restrictedGame = true;
         // System.out.println("scheduled restricted game, weekNum: " + resWeekNum + "
         // home team: " + chosenGame.game.homeTeam + " away team: " +
         // chosenGame.game.awayTeam + ", score: " + chosenGame.score);

         // schedule.schedule
         // Probably just keep the game in the original game list - but mark as scheduled
         // Then would need to check for scheduled when processing through the list
         // This would simplify scheduling for the 2 teams, and simplify unscheduling

         // must process the resources, decrement the availabilities
         // Set the scheduled week, game.weekNum > 0 indicates scheduled

         // so need a schedule function, and an unschedule function - probably in the
         // nflSchedule class
      }

      return true;
   }
   
   public NflGameSchedule chooseBestScoringGame(final ArrayList<NflGameSchedule> candidateGames) {
      NflGameSchedule chosenGame = null;
      final ArrayList<NflGameSchedule> bestCandidateGames = new ArrayList<NflGameSchedule>();
      double bestScore = 1000000000;

      Collections.sort(candidateGames, NflGameSchedule.GameScheduleComparatorByScore);

      // Choose the best games into a collection and choose randomly
      for (final NflGameSchedule usgame : candidateGames) {
         // NflTeamSchedule homeTeam = usgame.homeTeamSchedule;
         // NflTeamSchedule awayTeam = usgame.awayTeamSchedule;
         // System.out.println("Info: Post Sort Candidate game, weekNum: " + weekNum + "
         // home team: " + homeTeam.team.teamName + " away team: " +
         // awayTeam.team.teamName + ", score: " + usgame.score);

         // lowest score is best
         if (usgame.score > bestScore) {
            break;
         }

         bestScore = usgame.score;

         bestCandidateGames.add(usgame);
      }

      final int numBestCandidates = bestCandidateGames.size();
      chosenGame = bestCandidateGames.get(0);

      if (numBestCandidates > 1) {
         // randomly choose int between 0 and numBestCandidates-1

         final int randomNextInt = rnd.nextInt(numBestCandidates);

         // System.out.println(" Qualified games range: 0 , " + (numBestCandidates-1));
         // System.out.println(" Chose game randomNextInt: " + randomNextInt);

         chosenGame = bestCandidateGames.get(randomNextInt);
      }

      return chosenGame;
   }

   // place usGame in schedule for both the homeTeam and the awayTeam
   // at the index of weekNum in the scheduleGames array of each Team
   // set the game.weeknum = weeknum for temporary working purposes (reevaluate the
   // usefulness of that)

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

      // System.out.println("Placing game for home team: " + usGame.game.homeTeam + ",
      // and away Team: " + usGame.game.awayTeam + ", in week: " + weekNum);

      // Validate that nothing is scheduled in that week for either team

      if (homeTeam.scheduledGames[weekNum - 1] != null) {
         if (!usGame.isBye) {
            System.out.println("ERROR: game unexpectedly scheduled in week: " + weekNum + " for home team: "
                  + usGame.game.homeTeam + " for away team: " + usGame.game.awayTeam + ", isBye: " + usGame.isBye
                  + ", isRestricted: " + usGame.restrictedGame + ", score: " + usGame.score);
            NflGameSchedule xGame = homeTeam.scheduledGames[weekNum - 1];
            System.out.println("Instead: scheduled in week: " + xGame.weekNum + " for home team: " + xGame.game.homeTeam
                  + " for away team: " + xGame.game.awayTeam + ", isBye: " + xGame.isBye + ", isRestricted: "
                  + xGame.restrictedGame);
         }

         return false;
      }

      if (!usGame.isBye && awayTeam.scheduledGames[weekNum - 1] != null) {
         System.out.println(
               "ERROR: game unexpectedly scheduled in week: " + weekNum + " for away team: " + usGame.game.awayTeam);
         return false;
      }

      // add the game to the scheduled array for each team at the weeknum-1 index
      homeTeam.scheduledGames[weekNum - 1] = usGame;
      if (!usGame.isBye) {
         awayTeam.scheduledGames[weekNum - 1] = usGame;
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
               resourceSchedule.usage[weekNum - 1] += 1;
            }
         }
      } else {
         // reduce the bye resource count
         NflResourceSchedule byeResourceSchedule = schedule.findResource("bye");
         if (byeResourceSchedule != null && byeResourceSchedule.hasCapacity(weekNum)) {
            byeResourceSchedule.usage[weekNum - 1] += 1;
         }
      }

      return true;
   }

   public boolean writeScheduleCsv(NflSchedule schedule, String fileName) {
      // System.out.println("Entered writeScheduleCsv");
      BufferedWriter bw = null;
      FileWriter fw = null;

      try {
         fw = new FileWriter(fileName);
         bw = new BufferedWriter(fw);

         // write the header to the file
         // team, week 1 opponent, week 2 opponent
         bw.write("Team");
         // bw.write(",Week 1,Week 2,Week 3,Week 4,Week 5,Week 6,Week 7,Week 8,Week 9");
         // bw.write(",Week 10,Week 11,Week 12,Week 13,Week 14,Week 15,Week 16,Week
         // 17\n");
         for (int wi = 1; wi <= NflDefs.numberOfWeeks; wi++) {
            bw.write(",Week " + wi);
         }
         bw.write("\n");

         // handle byes

         // loop through the teams in the schedule
         // start line with teamname
         for (int ti = 0; ti < schedule.teamSchedules.size(); ti++) {
            NflTeamSchedule teamSchedule = schedule.teamSchedules.get(ti);
            // loop through the scheduledGames array
            // append "," to the line
            // get game from the array
            // if null, append 0 to the line
            // else if team is home - append the away teamname
            // else if team is away - append "at " home team name

            bw.write(teamSchedule.team.teamName);
            // int scheduledGameCount = 0;
            for (int gi = 0; gi < teamSchedule.scheduledGames.length; gi++) {
               NflGameSchedule gameSchedule = teamSchedule.scheduledGames[gi];

               if (gameSchedule == null) {
                  bw.write(",0");
               }
               // else if (gameSchedule.game.findAttribute("bye")) {

               else if (gameSchedule.isBye) {
                  bw.write(",Bye");
               } else if (gameSchedule.game.homeTeam.equalsIgnoreCase(teamSchedule.team.teamName)) {
                  bw.write("," + gameSchedule.game.awayTeam);
                  // bw.write("," + gameSchedule.game.awayTeam + "." +
                  // gameSchedule.weekScheduleSequence);
               } else {
                  bw.write(",at " + gameSchedule.game.homeTeam);
                  // bw.write(",at " + gameSchedule.game.homeTeam + "." +
                  // gameSchedule.weekScheduleSequence);
               }

               if (gameSchedule != null && gameSchedule.stadium != null
                     && gameSchedule.stadium.equalsIgnoreCase("london")) {
                  bw.write(" (Lon)");
               } else if (gameSchedule != null && gameSchedule.stadium != null
                     && gameSchedule.stadium.equalsIgnoreCase("mexico")) {
                  bw.write(" (Mex)");
               }

               if (gameSchedule != null) {
                  // scheduledGameCount++;
               }
            }
            bw.write("\n");
            // System.out.println("team: " + ti + ", " + teamSchedule.team.teamName + ",
            // scheduledGames: " + scheduledGameCount);
         }

         // Write out the schedule score
         bw.write("\nScore," + schedule.score + "\n");

         // Write out Bye Counts per week
         bw.write("\nByes");
         for (int wi = 1; wi <= NflDefs.numberOfWeeks; wi++) {
            int byeCountsThisWeek = schedule.byeCounts(wi);
            bw.write("," + byeCountsThisWeek);
         }

         // Write out Divisional Game Counts per week

         int first8weeksDivisionalTotal = 0;
         int second8weeksDivisionalTotal = 0;

         bw.write("\nDivisional Games");
         for (int wi = 1; wi <= NflDefs.numberOfWeeks; wi++) {
            int divisionalGameCount = schedule.divisionalGameCount(wi);
            bw.write("," + divisionalGameCount);
            if (wi <= 8) {
               first8weeksDivisionalTotal += divisionalGameCount;
            } else if (wi <= 16) {
               second8weeksDivisionalTotal += divisionalGameCount;
            }
         }

         // write out the 8 week totals summaries
         bw.write("\n8 week totals,,,,,,,," + first8weeksDivisionalTotal + ",,,,,,,," + second8weeksDivisionalTotal);

         // Write out Alerts
         if (!schedule.alerts.isEmpty()) {
            bw.write("\nAlerts\n");
            for (NflScheduleAlert alert : schedule.alerts) {
               // bw.write(alert.alertDescr + "," + alert.weekNum + "," + alert.homeTeam + ","
               // + alert.awayTeam + "\n");
               bw.write(alert.alertDescr + "\n");
            }
         }
         bw.write("\n");

         // System.out.println("Unique,Successful Weeks: " + fingerPrintMap.size() + "
         // (Unique FP collection size)");
         // System.out.println("Repeated,Successful Weeks: " + fpSkipCount + "
         // (Reschedule Non-Unique FP skip count)");
         // Set set = fingerPrintMap.entrySet();
         // Iterator iterator = set.iterator();
         /*
          * while(iterator.hasNext()) { Map.Entry mentry = (Map.Entry)iterator.next();
          * System.out.println("FP: "+ mentry.getKey() + " Count: " + mentry.getValue());
          * }
          */

         if (reschedAttemptsMultiWeeksBack < NflDefs.reschedAttemptsMultiWeeksBackLimit) {
            // System.out.println("Schedule completed");
         } else {
            // System.out.println("Schedule did not complete, lowestWeekNum: " +
            // lowestWeekNum);
         }
         // System.out.println(" unscheduled games: " +
         // curSchedule.unscheduledGames.size());
         // System.out.println(" score: " + curSchedule.score);

         // System.out.println("Done");

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

      // Validate that the game is scheduled in that week for both team

      if (homeTeam.scheduledGames[weekNum - 1] != gameSched) {
         System.out.println("ERROR: game unexpectedly not scheduled in week: " + weekNum + " for home team: "
               + gameSched.game.homeTeam);
         return false;
      }

      if (!gameSched.isBye && awayTeam.scheduledGames[weekNum - 1] != gameSched) {
         System.out.println("ERROR: game unexpectedly not scheduled in week: " + weekNum + " for away team: "
               + gameSched.game.awayTeam);
         return false;
      }

      // remove the game from the scheduled array for each team at the weeknum-1 index
      homeTeam.scheduledGames[weekNum - 1] = null;
      if (!gameSched.isBye) {
         awayTeam.scheduledGames[weekNum - 1] = null;
      }

      gameSched.weekNum = 0;

      // add the game back to the unscheduled arraylist for the schedule
      if (gameSched.isBye) {
         // System.out.println("Unscheduling Bye in week: " + weekNum + " for home team:
         // " + gameSched.game.homeTeam);
         schedule.unscheduledByes.add(gameSched);
      } else {
         schedule.unscheduledGames.add(gameSched);
         // schedule.allGames.add(gameSched);
      }

      if (!gameSched.isBye) {

         // Remove stadium resource usage in weeknum
         if (homeTeam.team.stadium != null) {
            String stadiumName = homeTeam.team.stadium;

            NflResourceSchedule resourceSchedule = schedule.findResource(stadiumName);
            if (resourceSchedule != null) {
               resourceSchedule.usage[weekNum - 1] -= 1;
            }
         }
      } else {

         // Remove bye resource usage in weeknum
         NflResourceSchedule byeResourceSchedule = schedule.findResource("bye");
         if (byeResourceSchedule != null) {
            byeResourceSchedule.usage[weekNum - 1] -= 1;
         }
         // System.out.println("unscheduled bye, weekNum: " + weekNum + " home team: " +
         // gameSched.game.homeTeam);
      }

      return true;
   }

   public boolean openSchedAttemptsLogFile() {
      try {
         schedAttemptsLogFw = new FileWriter("logSchedAttempts.csv");
         schedAttemptsLogBw = new BufferedWriter(schedAttemptsLogFw);

         // write the header to the file
         schedAttemptsLogBw.write(
               "SchedAttempt,status,# hard vios,alerts,score,iters,low week,unsched games,sched name,hard vios\n");
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

   public boolean openReschedLogFile() {
      if (reschedLogOn) {
         try {
            reschedLogFw = new FileWriter("reschedLog.csv");
            reschedLogBw = new BufferedWriter(reschedLogFw);

            // write the header to the file
            reschedLogBw.write("Resched Log\n");
         } catch (FileNotFoundException e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return true;
   }

   public boolean writeReschedLogFile(String s) {
      if (reschedLogOn) {
         if (reschedLogBw != null) {
            try {
               reschedLogBw.write(s);
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

      return true;
   }

   public boolean closeReschedLogFile() {
      if (reschedLogOn) {
         if (reschedLogBw != null) {
            try {
               reschedLogBw.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

      return true;
   }

   public boolean logSchedAttempt(int schedAttempts, NflSchedule sched, int iterNum,
                                  int lowestWeekNum, String savedScheduleName) {
      try {
         // write the schedule info to the file
         boolean schedComplete = sched.unscheduledGames.size() == 0;

         schedAttemptsLogBw.write(schedAttempts + "," + (schedComplete ? "complete" : "failed") + ","
               + (schedComplete ? sched.hardViolationCount : "") + "," + (schedComplete ? sched.alerts.size() : "")
               + "," + (schedComplete ? sched.score : "") + "," + iterNum + "," + (schedComplete ? "" : lowestWeekNum)
               + "," + (schedComplete ? "" : sched.unscheduledGames.size()) + "," + savedScheduleName + ","
               + (schedComplete ? sched.hardViolations : "") + "\n");

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
         briefLogBw
               .write("Week,Sched Games,Sched Byes,Unsched Games,Unsched Byes,Unsched Teams,Team1,Team2,Team3,Team4\n");
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
      // for each team - check if game scheduled for this week - count them, keep a
      // list of teams unscheduled
      // count the byes too
      // count the unscheduled games and the unscheduled byes
      // write: weekNum, scheduled games, scheduled byes, unscheduled games,
      // unscheduled byes, comma separated unscheduled teams
      ArrayList<NflTeamSchedule> unscheduledTeams = new ArrayList<NflTeamSchedule>();
      ArrayList<NflGameSchedule> scheduledGames = new ArrayList<NflGameSchedule>();
      ArrayList<NflGameSchedule> scheduledByes = new ArrayList<NflGameSchedule>();
      ArrayList<NflGameSchedule> unscheduledGames = new ArrayList<NflGameSchedule>();
      ArrayList<NflGameSchedule> unscheduledByes = new ArrayList<NflGameSchedule>();

      for (NflTeamSchedule teamSchedule : schedule.teamSchedules) {
         NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum - 1];
         if (gameSched != null) {
            if (gameSched.isBye) {
               scheduledByes.add(gameSched);
            } else {
               if (!scheduledGames.contains(gameSched)) {
                  scheduledGames.add(gameSched);
               }
            }
         } else {
            unscheduledTeams.add(teamSchedule);
         }
      }

      // for (int gi=0; gi < schedule.unscheduledGames.size(); gi++) {
      for (NflGameSchedule usgame : schedule.unscheduledGames) {
         if (usgame.isBye) {
            unscheduledByes.add(usgame);
         } else {
            unscheduledGames.add(usgame);
         }

         // System.out.println("Scheduler: Unscheduled: home team " +
         // usgame.game.homeTeam + ", away team: " + usgame.game.awayTeam);
      }

      if (briefLogBw != null) {
         try {
            briefLogBw.write(weekNum + "," + scheduledGames.size() + "," + scheduledByes.size() + ","
                  + unscheduledGames.size() + "," + unscheduledByes.size() + "," + unscheduledTeams.size());
            for (NflTeamSchedule unschedTeam : unscheduledTeams) {
               briefLogBw.write("," + unschedTeam.team.teamName);
            }
            if (unscheduledTeams.size() == 0) {
               briefLogBw.write(",0,0");
            }

            Collections.sort(scheduledGames, NflGameSchedule.GameScheduleComparatorBySchedSequence);
            int gameLogLimit = 0;
            for (NflGameSchedule schedGame : scheduledGames) {
               if (schedGame.restrictedGame) {
                  continue;
               }
               String gameInfo = "S:" + schedGame.weekScheduleSequence + ":"
                     + schedGame.game.homeTeam.substring(0, 3) + ":" + schedGame.game.awayTeam.substring(0, 3) + ":"
                     + schedGame.score + ":" + schedGame.demotionPenalty;
               briefLogBw.write("," + gameInfo);
               gameLogLimit++;
               if (gameLogLimit >= 3) {
                  break;
               }
            }

            Collections.sort(unscheduledGames, NflGameSchedule.GameScheduleComparatorByDemotion);
            gameLogLimit = 0;
            for (NflGameSchedule usGame : unscheduledGames) {
               String gameInfo = "U:" + usGame.game.homeTeam.substring(0, 3) + ":"
                     + usGame.game.awayTeam.substring(0, 3) + ":" + usGame.score + ":" + usGame.demotionPenalty + ":"
                     + usGame.demotionCount;
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
         partialScheduleLogFw = new FileWriter("logPartialScheduleResults" + scheduleAttempts + ".csv");
         partialScheduleLogBw = new BufferedWriter(partialScheduleLogFw);
         partialScheduleLogBw.write("FingerPrint,Week,Iteration,Unscheduled,BaseFP,PSE Count,GamesInWeek,HighSeqNum,Unsched Games,Unsched Byes \n");
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

   public boolean loadMetrics() {
      String csvFile = "nflmetrics.csv";
      BufferedReader br = null;
      String line = "";
      String cvsSplitBy = ",";

      try {
         br = new BufferedReader(new FileReader(csvFile));
         while ((line = br.readLine()) != null) {
            // use comma as separator
            String[] token = line.split(cvsSplitBy);
            ///////////////////////////
            // token[0] metric type
            // token[1] metric name
            // tokem[2] metric weight
            // create an instance of the metric, add weight, add to map
            // then NflGameSchedule can find the metrics to use there, and weight
            // also NflSchedule can find the schedule metrics to use there + weight

            NflGameMetric gameMetric = null;
            NflScheduleMetric scheduleMetric = null;

            if (token[0].equalsIgnoreCase("GameMetric")) {
               if (token[1].equalsIgnoreCase("HomeStandLimit")) {
                  gameMetric = new NflGMetHomeStandLimit("HomeStandLimit",null);
               }
               else if (token[1].equalsIgnoreCase("RoadTripLimit")) {
                  gameMetric = new NflGMetRoadTripLimit("RoadTripLimit",null);
               }
               else if (token[1].equalsIgnoreCase("NoRepeatedMatchup")) {
                  gameMetric = new NflGMetNoRepeatedMatchup("NoRepeatedMatchup",null);
               }
               else if (token[1].equalsIgnoreCase("ConflictsInWeek")) {
                  gameMetric = new NflGMetConflictsInWeek("ConflictsInWeek",null);
               }
               else if (token[1].equalsIgnoreCase("LastGameUnschedulable")) {
                  gameMetric = new NflGMetLastGameUnschedulable("LastGameUnschedulable",null);
               }
               else if (token[1].equalsIgnoreCase("BalancedHomeAway")) {
                  gameMetric = new NflGMetBalancedHomeAway("BalancedHomeAway",null);
               }
               else if (token[1].equalsIgnoreCase("StadiumResource")) {
                  gameMetric = new NflGMetStadiumResource("StadiumResource",null);
               }
               else if (token[1].equalsIgnoreCase("DivisionalWeekLimits")) {
                  gameMetric = new NflGMetDivisionalWeekLimits("DivisionalWeekLimits",null);
               }
               else if (token[1].equalsIgnoreCase("DivisionalSeparation")) {
                  gameMetric = new NflGMetDivisionalSeparation("DivisionalSeparation",null);
               }
               else if (token[1].equalsIgnoreCase("BalancedDivisional")) {
                  gameMetric = new NflGMetBalancedDivisional("BalancedDivisional",null);
               }
               else if (token[1].equalsIgnoreCase("RemainingOpportunities")) {
                  gameMetric = new NflGMetRemainingOpportunities("RemainingOpportunities",null);
               }
               else {
                  System.out.println("Unrecognized game Metric: " + token[1]);
                  continue;
               }
               gameMetric.weight = Double.parseDouble(token[2]);
               if (gameMetric.weight > 0.0) {
                  gameMetrics.put(gameMetric.metricName, gameMetric);
               }
            }
            else if (token[0].equalsIgnoreCase("ScheduleMetric")) {
               if (token[1].equalsIgnoreCase("NoRepeatedMatchup")) {
                  scheduleMetric = new NflSMetNoRepeatedMatchup("NoRepeatedMatchup");
               }
               else if (token[1].equalsIgnoreCase("RoadTripLimit")) {
                  scheduleMetric = new NflSMetRoadTripLimit("RoadTripLimit");
               }
               else if (token[1].equalsIgnoreCase("HomeStandLimit")) {
                  scheduleMetric = new NflSMetHomeStandLimit("HomeStandLimit");
               }
               else if (token[1].equalsIgnoreCase("DivisionalSeparation")) {
                  scheduleMetric = new NflSMetDivisionalSeparation("DivisionalSeparation");
               }
               else if (token[1].equalsIgnoreCase("DivisionalWeekLimits")) {
                  scheduleMetric = new NflSMetDivisionalWeekLimits("DivisionalWeekLimits");
               }
               else if (token[1].equalsIgnoreCase("DivisionalStart")) {
                  scheduleMetric = new NflSMetDivisionalStart("DivisionalStart");
               }
               else if (token[1].equalsIgnoreCase("DivisionalBalance")) {
                  scheduleMetric = new NflSMetDivisionalBalance("DivisionalBalance");
               }
               else {
                  System.out.println("Unrecognized schedule Metric: " + token[1]);
                  continue;
               }
               scheduleMetric.weight = Double.parseDouble(token[2]);
               if (scheduleMetric.weight > 0.0) {
                  scheduleMetrics.put(scheduleMetric.metricName, scheduleMetric);
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

      System.out.println("Number of game metrics is " + gameMetrics.size());
      System.out.println("Number of schedule metrics is " + scheduleMetrics.size());
      
      return true;
   }
}



