package com.nflscheduling;

import java.util.*;
import java.io.IOException;

public class NflScheduleAlg {

   public NflScheduler scheduler;

   public NflSchedule curSchedule; // curSchedule.teams holds the partial schedule
   // Each team has an array of scheduled games - 1 per week
   // Byes are scheduled games marked with isBye=true
   // Holds arrays of allGames, unscheduledGames, unscheduledByes

   // public ArrayList<String> reschedLog;
   public int fWeekScheduled = 0;
   public int bWeekScheduled = NflDefs.numberOfWeeks + 1;
   public int remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;
   public boolean fWeekSuccess = true;
   public boolean bWeekSuccess = true;
   public int bWeekLowest = NflDefs.numberOfWeeks + 1;
   public int fWeekHighest = 0;
   public int numWeeksBack = 0;
   public int weekNum = 0;

   public int reschedAttemptsSameWeek; // retries in same week before giving up and going back 1 week
                                       // unschedule the failed week, demote a game and retry the week

   public int reschedAttemptsOneWeekBack; // retries 1 week back before going back multiple weeks
                                          // unschedule the failed week and previous week, demote a game and retry the
                                          // previous week

   public int reschedAttemptsMultiWeeksBack; // retries multiple weeks back before giving up completely
                                             // unschedule the failed week and series of weeks, demote a game and retry
                                             // the earliest unsched week
   public int scheduleAttempts;

   public String terminationReason;

   // Demotion Scheme: TBD document it
   // Promotion scheme is not used - promotionInfo

   /*
    * Issues identified by Ted Can't have a back to back matchup with only a bye in
    * between, has to have at least one game for both teams Streamline by checking
    * for - if (!usgame.game.findAttribute("division")) { Only divisional games can
    * have repeat matchups
    * 
    * Basic functionality in NflGMetNoRepeatedMatchup, turned on sched metric for
    * alert if repeated matchup Could be a hole, due to a forced game where a bye
    * could slip in: forcedGame Hole NewGame scheduled - then bye slips into hole,
    * could fix within bye scheduling Divisional teams shouldn't play each other
    * twice in the first 5 weeks of the season, they should wait until at least
    * week 6 for a rematch probably need a new game metric to push for this, then a
    * schedule metric if (!usgame.game.findAttribute("division")) {
    * NflGMetDivisionalSeparation Byes should start no later than week 5. byes in
    * weeks 4,12, 13 can be optional - now works, Bye resource can specify this
    */

   public Random rnd = new Random();

   // logging of algorithm progress and histories
   public int lowestWeekNum = 1000;
   public int iterNum = 0;

   // partialSchedules
   // Array season weeks - summarizes the partial schedule for each scheduled week
   // so far
   // Each partial schedule entry has the fingerprint (# summary) sum of all the
   // weighted fingerprints for each week
   // Also has iterNum, weekNum, unscheduledTeams (?)

   public NflPartialScheduleEntry[] partialSchedules;

   // fingerPrintMap
   // Keeps track of each unique full or partial scheduled week as a partial
   // schedule fingerprint and a repeat count
   // one entry for every unique scheduled week (full or partial)
   // if we encounter the same schedule more than once, we increment the count in
   // the existing entry, fail the week
   // So iterations (iterNum) - reschedule events (reschedLog/2) (failed+repeated
   // fp) = unique partial schedule fingerprints (in collection)
   // TBD: validate that last calculation - understand it deeply
   // Iterations = all attempts to schedule a week
   // Reschedule events - all weeks that don't completely schedule or completely
   // schedule but have repeated fingerPrint
   // unique partial schedule fingerprints = fully scheduled weeks with a unique
   // fingerprint - repeats are treated as failures
   // Create Log File: logPartialScheduleResults.csv
   // iteration start (scheduleUnrestrictedWeek): iterNum++
   // Successfully schedule a week: call logPartialScheduleHistory
   // Add new or updated entry (count increment) to fingerPrintMap - may not
   // increase the number of entries
   // Fail the week if it has a repeated FP
   // Fail to complete the weeks schedule: call logPartialScheduleHistory
   // Don't add new or updated entry to fingerPrintMap, unnecessarily calculates
   // partialScheduleEntry
   // and unnecessarily updates schedule.latestScheduleFingerPrint

   public HashMap<Double, NflPartialScheduleEntry> fingerPrintMap;

   public int fpSkipCount = 0; // Counts number of repeated encounters of a FingerPrint after completing a
                               // fully scheduled week
                               // Each encounter is failed, causing a backtrack rescheduling attempt

   // unscheduledTeams - after failure of a week, the unscheduledTeams in that week
   // are given first priority during the next weekly schedule
   // TBD: study and rethink if this really makes any real sense

   public ArrayList<NflTeamSchedule> unscheduledTeams = new ArrayList<NflTeamSchedule>();
   public int byesToScheduleThisWeek;
   public int byesScheduledThisWeek;
   public int sDir;
   public NflWeeklyData weeklyData = new NflWeeklyData();
   public NflWeeklyData priorWeeklyData = null;

   public int alreadyScheduledRejection;
   public int backToBackMatchRejection;
   public int resourceUnavailRejection;
   public int hardViolationRejection;

   public boolean init(NflScheduler theScheduler) {
      scheduler = theScheduler;
      sDir = NflDefs.schedulingDirection;
      return true;
   }

   public boolean scheduleInit() {
      lowestWeekNum = (sDir == -1) ? 1000 : 0;
      iterNum = 0;
      fingerPrintMap = new HashMap<Double, NflPartialScheduleEntry>();
      fpSkipCount = 0;
      partialSchedules = new NflPartialScheduleEntry[NflDefs.numberOfWeeks];
      rnd = new Random();
      writeWeeklyDataHeaderToFile();

      return true;
   }

   /*
    * ScheduleWeek(weekNum, direction, schedule) Evaluate all unscheduled games in
    * the schedule - determine if candidate Not a candidate - if one of the teams
    * is already scheduled for the week For each qualifying candidate game - wrap
    * it in a candidate data structure And add to a candidate list Then walk
    * through the candidate list and evaluate all the metrics Then sort the
    * candidates and start choosing games And placing them on the schedule
    * ScheduleWeeksInSequence(weekNumStart, weekNumEnd, schedule) Determine
    * direction from weekNumStart - weekNumEnd sign
    * 
    */

   public boolean scheduleUnrestrictedWeek(final NflSchedule schedule, final int weekNum) {
      NflGameSchedule game;
      // NflGameSchedule bye;
      // int schedTeamsInWeek;
      int schedSeqNum = 0;
      boolean status = true;

      alreadyScheduledRejection = 0;
      backToBackMatchRejection = 0;
      resourceUnavailRejection = 0;
      hardViolationRejection = 0;

      determineNumByesForWeek(weekNum, schedule);

      iterNum++;

      boolean gamesToSchedule = false;

      while ((schedule.scheduledTeamsInWeek(weekNum)) < NflDefs.numberOfTeams) {
         gamesToSchedule = true;
         game = scheduleNextUnrestrictedGameInWeek(schedule, weekNum);
         if (game == null) {
            // System.out.println("Scheduler: failed to schedule all teams in week: " +
            // weekNum + " scheduled:" + schedTeamsInWeek + " games, teams: " +
            // NflDefs.numberOfTeams + ", " + schedule.teams.size());
            // attempt a reschedule of the week - making the first scheduled game a
            // low/lower priority - somehow
            // maybe exit false, fixup, and reenter, some number of times
            // trying to find and demote the culprit preventing a full week of scheduling
            // maybe have a demotion penalty for the gameSchedule that was first
            // keep adding more demotion penalty to the first scheduled game everytime you
            // can't schedule the week
            // if there is a key culprit or 2, they will accumulate enough demotion penalty
            // to push them way down
            // Only retry - X number of times, or some other termination criteria

            status = false;

            // determine unscheduled teams
            unscheduledTeams.clear();
            for (final NflTeamSchedule teamSchedule : schedule.teamSchedules) {
               final NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum - 1];
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
         final NflPartialScheduleEntry partialScheduleEntry = fingerPrintMap.get(schedule.latestScheduleFingerPrint);
         SchedFingerPrintCount = partialScheduleEntry.count;
         // if (SchedFingerPrintCount > 1 && weekNum < NflDefs.numberOfWeeks - 1) {

         if (gamesToSchedule && SchedFingerPrintCount > 1) {
            status = false;
            fpSkipCount++;
         }
      }

      return status;
   }

   public boolean scheduleUnrestrictedGames(final NflSchedule schedule) {
      return true;
   }

   // possibly modularize this function to facilitate scheduling a game and/or
   // scheduling a bye
   public NflGameSchedule scheduleNextUnrestrictedGameInWeek(final NflSchedule schedule, final int weekNum) {
      /////////////////
      // Bye Scheduling
      /////////////////
      if (byesToScheduleThisWeek - byesScheduledThisWeek >= 2 && schedule.unscheduledByes.size() >= 2) {
         // NflGameSchedule lastByeGame = scheduleNextUnrestrictedByes(schedule,
         // weekNum);
         final NflGameSchedule lastByeGame = scheduleNextUnrestrictedByes2(schedule, weekNum);

         return lastByeGame;
      }

      // Make a collection of unscheduled games
      // where none of the teams are scheduled for this week

      final ArrayList<NflGameSchedule> candidateGames = new ArrayList<NflGameSchedule>();

      for (int gi = 0; gi < schedule.unscheduledGames.size(); gi++) {
         final NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
         final NflTeamSchedule homeTeam = usgame.homeTeamSchedule;
         final NflTeamSchedule awayTeam = usgame.awayTeamSchedule;

         usgame.unscheduledByes.clear(); // clear the byes so byes can be set accurately during this scheduling pass

         // Validate that neither team from the game is already scheduled for this week

         if (homeTeam.scheduledGames[weekNum - 1] != null) {
            // System.out.println("Info: schedUnrest: team already scheduled in week: " +
            // weekNum + " home team: " + homeTeam.team.teamName + " away team: " +
            // awayTeam.team.teamName + ", skip");
            alreadyScheduledRejection++;
            continue;
         } else if (!usgame.isBye && awayTeam.scheduledGames[weekNum - 1] != null) {
            // System.out.println("Info: schedUnrest: team already scheduled in week: " +
            // weekNum + " away team: " + awayTeam.team.teamName + " home team: " +
            // homeTeam.team.teamName + ", skip");
            alreadyScheduledRejection++;
            continue;
         }

         // Verify that the game is not a repeated matchup - either before or after this
         // week - unacceptable
         // Verify first that the next week would not cause a repeated matchup
         if (weekNum + 1 <= NflDefs.numberOfWeeks) {
            final NflGameSchedule nextWeeksGame = homeTeam.scheduledGames[weekNum]; // NOTE: weekNum starts at 1, must
                                                                                    // correct for index
            if (nextWeeksGame != null && !nextWeeksGame.isBye) {
               if (nextWeeksGame.game.awayTeam.equalsIgnoreCase(usgame.game.homeTeam)
                     && nextWeeksGame.game.homeTeam.equalsIgnoreCase(usgame.game.awayTeam)) {
                  backToBackMatchRejection++;
                  continue;
               }
            }
         }

         // Verify next that the previous week would not cause a repeated matchup
         if (weekNum > 1) {
            final NflGameSchedule nextWeeksGame = homeTeam.scheduledGames[weekNum - 2]; // NOTE: weekNum starts at 1,
                                                                                        // must correct for index
            if (nextWeeksGame != null && !nextWeeksGame.isBye) {
               if (nextWeeksGame.game.awayTeam.equalsIgnoreCase(usgame.game.homeTeam)
                     && nextWeeksGame.game.homeTeam.equalsIgnoreCase(usgame.game.awayTeam)) {
                  backToBackMatchRejection++;
                  continue;
               }
            }
         }

         // Verify that global attrlimit matches have remaining capacity in the
         // restricted weeknum
         // for any unscheduled game attributes that have a global resource: e.g. bye,
         // GiantsJetsStadium

         boolean resourcePass = true;
         if (homeTeam.team.stadium != null) {
            final String stadiumName = homeTeam.team.stadium;
            final NflResourceSchedule resourceSchedule = schedule.findResource(stadiumName);
            if (resourceSchedule != null && !resourceSchedule.hasCapacity(weekNum)) {
               resourcePass = false;
            }
         }

         // clear the bye information from the game, array list and flag ?? TBD
         usgame.unscheduledByes.clear();

         if (!resourcePass) {
            resourceUnavailRejection++;
            continue; // resource capacity would be exceeded - byes, or JetsGiantsStadium
         }

         candidateGames.add(usgame);
      }

      if (candidateGames.isEmpty()) {
         return null;
      }

      for (final NflGameSchedule usgame : candidateGames) {
         usgame.computeMetrics(weekNum, schedule, candidateGames, this);
      }

      int candidateGameCountBefore = candidateGames.size();
      candidateGames.removeIf(gs -> (gs.hardViolationCount > 0));
      hardViolationRejection += candidateGameCountBefore - candidateGames.size();

      int scheduledTeamsInWeek = schedule.scheduledTeamsInWeek(weekNum);
      int remainingGamesToScheduleInWeek = (NflDefs.numberOfTeams - scheduledTeamsInWeek) / 2;
      if (candidateGames.size() < remainingGamesToScheduleInWeek) {
         // Not enough candidate games to finish this week
         if (schedule.unscheduledGames.size() <= (NflDefs.numberOfTeams - byesScheduledThisWeek) / 2) {
            // Don't repeat the same week -
            // there is not enough unscheduled games to draw a different set of candidates
            // from
            reschedAttemptsSameWeek = NflDefs.reschedAttemptsSameWeekLimit + 1;
         }
         return null;
      }

      if (candidateGames.isEmpty()) {
         return null;
      }

      //////////////////
      // Game Scheduling
      //////////////////

      NflGameSchedule chosenGame = null;

      if (!unscheduledTeams.isEmpty()) {
         final NflTeamSchedule unscheduledTeam = unscheduledTeams.remove(0);
         chosenGame = chooseBestScoringGame(candidateGames, unscheduledTeam);
      }

      if (chosenGame == null) {
         // Why would chosenGame be null ?
         // There wasn't a game for the selected unscheduledTeam ?
         // Possibly candidate games eliminated, repeated matchup, stadium, other
         chosenGame = scheduler.chooseBestScoringGame(candidateGames);
      }

      chosenGame.candidateCount = candidateGames.size();

      // TBD: also need a function to write out the weeklyData csv line
      // TBD: also need a function write out the weeklyData header

      logWeeklyDataFromScheduledGame(chosenGame);

      if (!scheduler.placeGameInSchedule(chosenGame, weekNum, schedule)) {
         return null;
      }

      // just chose one game, need to keep choosing until week is full
      // or no more games can be chosen
      // algorithm should be aware if a week is not full - and report

      return chosenGame;
   }

   public boolean findMaxEvenRelatedUnscheduledByes(final ArrayList<NflGameSchedule> sortedUnscheduledByes,
         final ArrayList<NflGameSchedule> byesToSchedule, final int weekNum) {
      byesToSchedule.clear();

      final NflGameSchedule primaryUnscheduledBye = sortedUnscheduledByes.get(0);

      byesToSchedule.add(primaryUnscheduledBye);
      for (final NflGameSchedule bye : primaryUnscheduledBye.opponentByes) {
         // if bye is unscheduled and isn't already in the collection, add it to the bye
         // collection to schedule
         if (bye.weekNum == 0 && bye.homeTeamSchedule.scheduledGames[weekNum - 1] == null) {
            if (!byesToSchedule.contains(bye)) {
               byesToSchedule.add(bye);
            }
         }
      }

      final int byeCount = byesToSchedule.size();

      // Ensure ByeCount is for even pairs of byes
      // 1 is ok, otherwise should be 2, 4, 6, 8, etc
      if (byeCount > 1 && byeCount % 2 != 0) {
         byesToSchedule.remove(0);
      }

      return true;
   }

   public NflGameSchedule chooseBestScoringGame(final ArrayList<NflGameSchedule> candidateGames,
         final NflTeamSchedule unscheduledTeam) {
      // System.out.println("Entered chooseBestScoringGame " + " unscheduledTeam: " +
      // unscheduledTeam.team.teamName + ", candidateGame size: " +
      // candidateGames.size());
      final ArrayList<NflGameSchedule> candidateTeamGames = new ArrayList<NflGameSchedule>();
      for (final NflGameSchedule usgame : candidateGames) {
         if (usgame.homeTeamSchedule == unscheduledTeam) {
            candidateTeamGames.add(usgame);
         } else if (usgame.awayTeamSchedule == unscheduledTeam) {
            candidateTeamGames.add(usgame);
         }
      }

      NflGameSchedule chosenGame = null;

      if (candidateTeamGames.size() > 0) {
         chosenGame = scheduler.chooseBestScoringGame(candidateTeamGames);
      }

      return chosenGame;
   }

   // public static NflGameSchedule scheduleNextUnrestrictedByes(NflSchedule
   // schedule, int weekNum)
   // TBD write a new version of scheduleNextUnrestrictedByes
   // TBD to schedule all the byes for the week, from the unscheduled bye with the
   // most unscheduled opponent byes
   // TBD must sort the unscheduleByes first for unscheduled, then with most
   // unscheduled opponent byes
   // TBD then work through the sorted list, scheduling an even number of byes -
   // TBD starting with the primary bye then it's unscheduled opponent byes

   public NflGameSchedule scheduleNextUnrestrictedByes2(final NflSchedule schedule, final int weekNum) {
      NflGameSchedule lastScheduledBye = null;
      final ArrayList<NflGameSchedule> byesToSchedule = new ArrayList<NflGameSchedule>();

      while (byesToScheduleThisWeek - byesScheduledThisWeek > 0) {
         scoreUnscheduledByes(schedule.unscheduledByes, weekNum);
         // Prefer games with multiple unscheduled byes vs 1 or 0
         Collections.sort(schedule.unscheduledByes, NflGameSchedule.GameScheduleByeComparatorByByeCandidateScore);

         findMaxEvenRelatedUnscheduledByes(schedule.unscheduledByes, byesToSchedule, weekNum);
         for (final NflGameSchedule byeToSchedule : byesToSchedule) {
            // System.out.println("Scheduling Bye(2) in week: " + weekNum + " home team: " +
            // byeToSchedule.homeTeamSchedule.team.teamName);

            scheduler.placeGameInSchedule(byeToSchedule, weekNum, schedule);
            byesScheduledThisWeek++;
            lastScheduledBye = byeToSchedule;
            if (byesToScheduleThisWeek - byesScheduledThisWeek <= 0) {
               break;
            }
         }
      }

      return lastScheduledBye;
   }

   public boolean scoreUnscheduledByes(final ArrayList<NflGameSchedule> unscheduledByes, final int weekNum) {
      for (final NflGameSchedule unscheduledBye : unscheduledByes) {
         unscheduledBye.byeCandidateScore = 0;
         // if the unscheduled bye can't be scheduled this week because a (forced) game
         // is already scheduled, this bye is not a candidate
         if (unscheduledBye.homeTeamSchedule.scheduledGames[weekNum - 1] != null) {
            continue;
         }

         unscheduledBye.byeCandidateScore = 1; // The root bye is schedulable, now we will count the schedulable
                                               // opponents
         for (final NflGameSchedule opponentBye : unscheduledBye.opponentByes) {
            // if the opponent is already scheduled or
            // if the opponent bye can't be scheduled this week because a (forced) game is
            // already scheduled, this bye is not a candidate
            if (opponentBye.weekNum > 0 || opponentBye.homeTeamSchedule.scheduledGames[weekNum - 1] != null) {
               continue;
            }
            unscheduledBye.byeCandidateScore += 1; // For the schedulable opponent bye, add it to the score
         }
      }

      return true;
   }

   public boolean briefLogWeek(final NflSchedule schedule, final int weekNum) {
      // for each team - check if game scheduled for this week - count them, keep a
      // list of teams unscheduled
      // count the byes too
      // count the unscheduled games and the unscheduled byes
      // write: weekNum, scheduled games, scheduled byes, unscheduled games,
      // unscheduled byes, comma separated unscheduled teams
      final ArrayList<NflTeamSchedule> unscheduledTeams = new ArrayList<NflTeamSchedule>();
      final ArrayList<NflGameSchedule> scheduledGames = new ArrayList<NflGameSchedule>();
      final ArrayList<NflGameSchedule> scheduledByes = new ArrayList<NflGameSchedule>();
      final ArrayList<NflGameSchedule> unscheduledGames = new ArrayList<NflGameSchedule>();
      final ArrayList<NflGameSchedule> unscheduledByes = new ArrayList<NflGameSchedule>();

      for (final NflTeamSchedule teamSchedule : schedule.teamSchedules) {
         final NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum - 1];
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
      for (final NflGameSchedule usgame : schedule.unscheduledGames) {
         if (usgame.isBye) {
            unscheduledByes.add(usgame);
         } else {
            unscheduledGames.add(usgame);
         }

         // System.out.println("Scheduler: Unscheduled: home team " +
         // usgame.game.homeTeam + ", away team: " + usgame.game.awayTeam);
      }

      if (scheduler.briefLogBw != null) {
         try {
            scheduler.briefLogBw.write(weekNum + "," + scheduledGames.size() + "," + scheduledByes.size() + ","
                  + unscheduledGames.size() + "," + unscheduledByes.size() + "," + unscheduledTeams.size());
            for (final NflTeamSchedule unschedTeam : unscheduledTeams) {
               scheduler.briefLogBw.write("," + unschedTeam.team.teamName);
            }
            if (unscheduledTeams.size() == 0) {
               scheduler.briefLogBw.write(",0,0");
            }

            Collections.sort(scheduledGames, NflGameSchedule.GameScheduleComparatorBySchedSequence);
            int gameLogLimit = 0;
            for (final NflGameSchedule schedGame : scheduledGames) {
               if (schedGame.restrictedGame) {
                  continue;
               }
               final String gameInfo = "S:" + schedGame.weekScheduleSequence + ":"
                     + schedGame.game.homeTeam.substring(0, 3) + ":" + schedGame.game.awayTeam.substring(0, 3) + ":"
                     + schedGame.score + ":" + schedGame.demotionPenalty;
               scheduler.briefLogBw.write("," + gameInfo);
               gameLogLimit++;
               if (gameLogLimit >= 3) {
                  break;
               }
            }

            Collections.sort(unscheduledGames, NflGameSchedule.GameScheduleComparatorByDemotion);
            gameLogLimit = 0;
            for (final NflGameSchedule usGame : unscheduledGames) {
               final String gameInfo = "U:" + usGame.game.homeTeam.substring(0, 3) + ":"
                     + usGame.game.awayTeam.substring(0, 3) + ":" + usGame.score + ":" + usGame.demotionPenalty + ":"
                     + usGame.demotionCount;
               scheduler.briefLogBw.write("," + gameInfo);
               gameLogLimit++;
               if (gameLogLimit >= 3) {
                  break;
               }
            }
            scheduler.briefLogBw.write("\n");

         } catch (final IOException e) {
            e.printStackTrace();
         }
      }

      return true;
   }

   public boolean logPartialScheduleHistory(final NflSchedule schedule, final int weekNum) {
      // Construct a record for the full partial schedule up to this week
      // Use the partial schedule fingerprint from the previous week to add onto for
      // this week using hashcodes and the weeknum

      // Create a partial schedule entry for weekNum
      NflPartialScheduleEntry partialScheduleEntry = new NflPartialScheduleEntry();

      // use a local object to access the partial schedules entry for weekNum
      // and populate it with current schedule state information for this week

      partialScheduleEntry.unscheduledTeams = 0;
      partialScheduleEntry.fingerPrint = 0.0;
      partialScheduleEntry.baseFingerPrint = 0.0;
      partialScheduleEntry.count = 0;
      partialScheduleEntry.gamesInWeek = new ArrayList<NflGameSchedule>();

      // Initialize base fingerprint append new fingerprint info to - from the
      // previous weeks fingerprint
      // Unless this is the very last week in the schedule - where there is no
      // previous week to get a fingerprint from

      if (sDir == -1) {
         if (weekNum < NflDefs.numberOfWeeks) {
            // Going backward, there is no previous partial schedule until weekNum == 16-
            // To base the partial schedule off of : weekNum == 17 is first partial
            partialScheduleEntry.baseFingerPrint = partialSchedules[weekNum].fingerPrint;
            partialScheduleEntry.fingerPrint = partialScheduleEntry.baseFingerPrint;
         }
      } else if (sDir == 1) {
         if (weekNum > 1) {
            // Going forward, there is no previous partial schedule until weekNum == 2+
            // To base the partial schedule off of : weekNum == 1 is first partial
            partialScheduleEntry.baseFingerPrint = partialSchedules[weekNum - 2].fingerPrint;
            partialScheduleEntry.fingerPrint = partialScheduleEntry.baseFingerPrint;
         }
      }

      // Extend the partial schedule fingerprint from the previous weeks from the
      // games of this week
      // using the hash code of each game as a unique id of each scheduled game for
      // this week
      // Also keep track of the unscheduled games for this week

      for (final NflTeamSchedule teamSchedule : schedule.teamSchedules) {
         final NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum - 1];
         if (gameSched != null) {
            partialScheduleEntry.fingerPrint += (double) gameSched.hashCode()
                  * Math.pow(NflDefs.numberOfWeeks - weekNum + 1, 2);
            if (!gameSched.isBye && !partialScheduleEntry.gamesInWeek.contains(gameSched)) {
               partialScheduleEntry.gamesInWeek.add(gameSched);
            }
         } else {
            partialScheduleEntry.unscheduledTeams++;
         }
      }

      // Update the partial schedule fingerprint in the partial schedule repository
      // if All teams were scheduled for this week

      if (partialScheduleEntry.unscheduledTeams == 0) {
         if (fingerPrintMap.containsKey(partialScheduleEntry.fingerPrint)) {
            partialScheduleEntry = fingerPrintMap.get(partialScheduleEntry.fingerPrint);
         } else {
            fingerPrintMap.put(partialScheduleEntry.fingerPrint, partialScheduleEntry);
         }

         // partialScheduleEntry.iterNum = iterNum;
         // partialScheduleEntry.weekNum = weekNum;
         partialScheduleEntry.count += 1;
         partialSchedules[weekNum - 1] = partialScheduleEntry; // store the partial schedule entry for this week
      }

      try {
         // write the partial schedule log entry to the csv file
         int highSeqNum = 0;
         for (final NflGameSchedule gameInWeek : partialScheduleEntry.gamesInWeek) {
            if (gameInWeek.weekScheduleSequence > highSeqNum) {
               highSeqNum = gameInWeek.weekScheduleSequence;
            }
         }

         scheduler.partialScheduleLogBw.write(partialScheduleEntry.fingerPrint + "," + weekNum + "," + iterNum + ","
               + partialScheduleEntry.unscheduledTeams + "," + partialScheduleEntry.baseFingerPrint + ","
               + partialScheduleEntry.count + "," + partialScheduleEntry.gamesInWeek.size() + "," + highSeqNum + ","
               + schedule.unscheduledGames.size() + "," + schedule.unscheduledByes.size() + "\n");
      } catch (final IOException e) {
         e.printStackTrace();
      }

      partialScheduleEntry.iterNum = iterNum;
      partialScheduleEntry.weekNum = weekNum;
      // partialScheduleEntry.count += 1;
      partialSchedules[weekNum - 1] = partialScheduleEntry; // store the partial schedule entry for this week

      schedule.latestScheduleFingerPrint = partialScheduleEntry.fingerPrint;

      return true;
   }

   public boolean updateDemotionInfo(final NflSchedule schedule, final int weekNum) {
      // loop through unscheduled games
      // if demotionCount <= 1 demotion penalty = 0
      // if demotionCount > 1, keep demotion penalty unmodified

      // Find the partialScheduleEntry for weekNum
      // Find the "Game in Week" with the highest penalty, remove it and demote it

      final NflPartialScheduleEntry partialScheduleEntry = partialSchedules[weekNum - 1];
      NflGameSchedule gameScheduledLast = null;
      for (final NflGameSchedule gameSched : partialScheduleEntry.gamesInWeek) {
         if (gameScheduledLast == null || gameSched.weekScheduleSequence > gameScheduledLast.weekScheduleSequence) {
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
    * public boolean updateDemotionInfo(NflSchedule schedule, int weekNum) { //
    * loop through unscheduled games // if demotionCount <= 1 demotion penalty = 0
    * // if demotionCount > 1, keep demotion penalty unmodified
    * 
    * // Find the partialScheduleEntry for weekNum // Find the "Game in Week" with
    * the highest penalty, remove it and demote it for (int gi=0; gi <
    * schedule.unscheduledGames.size(); gi++) { NflGameSchedule usgame =
    * schedule.unscheduledGames.get(gi);
    * 
    * // The 1st scheduled game in the week is suspect, gets a demotion penalty and
    * demotion count if (usgame.weekScheduleSequence == 1) {
    * usgame.demotionCount++; usgame.demotionPenalty = usgame.demotionCount*2;
    * //System.out.println("Demotion update1: during week: " + weekNum +
    * " updateDemotionPenalty to " + usgame.demotionPenalty + ", weekSequence: " +
    * usgame.weekScheduleSequence + " for game: " + usgame.game.homeTeam + " : " +
    * usgame.game.awayTeam); } // any game that has been demoted more than once is
    * suspect, and gets an increased demotion penalty else if (usgame.demotionCount
    * > 1) { usgame.demotionPenalty = usgame.demotionCount*2;
    * //System.out.println("Demotion update2: during week: " + weekNum +
    * " updateDemotionPenalty to " + usgame.demotionPenalty + ", weekSequence: " +
    * usgame.weekScheduleSequence + " for game: " + usgame.game.homeTeam + " : " +
    * usgame.game.awayTeam); } // all other games - have no demotion penalty and
    * deserve an increased chance of scheduling into the next week else { //if
    * (usgame.demotionPenalty > 0) //
    * System.out.println("Demotion update3: during week: " + weekNum +
    * " updateDemotionPenalty to zero from demotionPenalty of " +
    * usgame.demotionPenalty + ", weekSequence: " + usgame.weekScheduleSequence +
    * " for game: " + usgame.game.homeTeam + " : " + usgame.game.awayTeam);
    * usgame.demotionPenalty = 0; usgame.demotionCount = 0; }
    * 
    * usgame.weekScheduleSequence = 0; }
    * 
    * return true; }
    * 
    */
   public boolean initPromotionInfo(final NflSchedule schedule) {
      for (int gi = 0; gi < schedule.unscheduledGames.size(); gi++) {
         final NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
         if (unscheduledTeams.contains(usgame.homeTeamSchedule) || unscheduledTeams.contains(usgame.awayTeamSchedule)) {
            usgame.promotionScore = 0.0;
         }
      }

      return true;
   }

   public boolean updatePromotionInfo(final NflSchedule schedule) {
      for (int gi = 0; gi < schedule.unscheduledGames.size(); gi++) {
         final NflGameSchedule usgame = schedule.unscheduledGames.get(gi);
         if (unscheduledTeams.contains(usgame.homeTeamSchedule) || unscheduledTeams.contains(usgame.awayTeamSchedule)) {
            usgame.promotionScore += -0.05;
         }
      }

      return true;
   }

   public boolean unscheduleUnrestrictedWeek(final NflSchedule schedule, final int weekNum,
         final boolean clearHistory) {
      // int unscheduledGameCount = 0;
      for (final NflTeamSchedule teamSchedule : schedule.teamSchedules) {
         final NflGameSchedule gameSched = teamSchedule.scheduledGames[weekNum - 1];
         if (gameSched != null && !gameSched.restrictedGame) {
            if (!scheduler.unscheduleGame(gameSched, weekNum, schedule)) {
               System.out.println("unscheduleUnrestrictedWeek - failed to unschedule game");
               return false;
            }

            if (clearHistory) {
               if (gameSched.demotionPenalty > 0)
                  // System.out.println("Clearing history: during week: " + weekNum + "
                  // DemotionPenalty was " + gameSched.demotionPenalty + ", weekSequence: " +
                  // gameSched.weekScheduleSequence + " for game: " + gameSched.game.homeTeam + "
                  // : " + gameSched.game.awayTeam);
                  // gameSched.weekScheduleSequence = 0;
                  gameSched.demotionCount = 0;
               gameSched.demotionPenalty = 0;
            }
            // unscheduledGameCount++;
         }
      }

      return true;
   }

   public boolean determineNumByesForWeek(int weekNum, NflSchedule schedule) {
      // byesToScheduleThisWeek - set it
      // remaining byeCapacity in resource "Bye" from weeknum back

      int remainingByesToSchedule = schedule.unscheduledByes.size();

      byesToScheduleThisWeek = 0;
      byesScheduledThisWeek = 0;

      if (remainingByesToSchedule == 0) {
         return true;
      }

      int remainingByeCapacity = 0;
      int remainingByeMin = 0;
      NflResourceSchedule byeResourceSchedule = schedule.findResource("Bye");

      if (byeResourceSchedule == null) {
         return true;
      }

      int weekEnd = NflDefs.numberOfWeeks;

      if (sDir == -1) {
         weekEnd = 1;
         weekEnd = fWeekScheduled + 1; // TBD forwardAndBackward - must work for single
      } else if (sDir == 1) {
         weekEnd = NflDefs.numberOfWeeks;
         weekEnd = bWeekScheduled - 1; // TBD forwardAndBackward - must work for single
      }

      // Accumulated over the remaining unscheduled weeks
      for (int wi = weekNum; wi * sDir <= weekEnd * sDir; wi += sDir) {
         remainingByeCapacity += byeResourceSchedule.resource.weeklyLimit[wi - 1] - byeResourceSchedule.usage[wi - 1];
         remainingByeMin += byeResourceSchedule.resource.weeklyMinimum[wi - 1] - byeResourceSchedule.usage[wi - 1];
      }

      // Determine number of byes already scheduled (forced) in this week
      // Loop through all of the teamschedules, and check

      int byesScheduledThisWeek = 0;

      for (NflTeamSchedule teamSched : schedule.teamSchedules) {
         NflGameSchedule gameInThisWeek = teamSched.scheduledGames[weekNum - 1];
         if (gameInThisWeek != null && gameInThisWeek.isBye) {
            byesScheduledThisWeek++;
         }
      }

      // Determine the min and the max possible byes for this week
      int min = Math.max(byeResourceSchedule.resource.weeklyMinimum[weekNum - 1] - byesScheduledThisWeek, 0);
      int max = Math.max(byeResourceSchedule.resource.weeklyLimit[weekNum - 1] - byesScheduledThisWeek, 0);

      int adjustedMin = Math.max(byeResourceSchedule.resource.weeklyMinimum[weekNum - 1] - byesScheduledThisWeek,
            remainingByesToSchedule - (remainingByeCapacity - byeResourceSchedule.resource.weeklyLimit[weekNum - 1]));
      int adjustedMax = Math.min(byeResourceSchedule.resource.weeklyLimit[weekNum - 1] - byesScheduledThisWeek,
            remainingByesToSchedule - (remainingByeMin - byeResourceSchedule.resource.weeklyMinimum[weekNum - 1]));

      if (remainingByesToSchedule > remainingByeCapacity) {
         System.out.println("   ERROR determineNumByesForWeek: For week: " + weekNum + " insufficient bye capacity: "
               + remainingByeCapacity + ", remainingByesToSchedule: " + remainingByesToSchedule);
      }

      // TBD: May need to adjust min so that downstream has capacity for the remaining
      // byes
      // i.e. maybe consider remainingByeCapacity (Max), remainingByeMin (don't
      // compute yet), remainingByesToSchedule,
      // remainingByeCapacity (Max) - remainingByeMin vs remainingByesToSchedule
      if (remainingByesToSchedule >= remainingByeCapacity) {
         byesToScheduleThisWeek = max;
         return true;
      }

      // double excessAvailRatio =
      // (remainingByeCapacity-remainingByesToSchedule)/remainingByeCapacity;
      double randomNum = rnd.nextDouble();
      double useMaxByeCapacityProb = randomNum;
      // double useMaxByeCapacityProb = excessAvailRatio*randomNum;

      // useMaxByeCapacityProb = 1.0 - useMaxByeCapacityProb;
      // useMaxByeCapacityProb = randomNum;

      // byesToScheduleThisWeek = (int) (useMaxByeCapacityProb*(max - min) + min);
      byesToScheduleThisWeek = (int) (useMaxByeCapacityProb * (adjustedMax - adjustedMin) + adjustedMin);

      for (int bc = min; bc <= max; bc += 2) {
         if (byesToScheduleThisWeek <= bc) {
            byesToScheduleThisWeek = bc;
            break;
         }
      }

      return true;
   }

   public boolean logWeeklyDataFromScheduledGame(NflGameSchedule scheduledGame) {
      if (scheduler.reschedLogOn) {
         for (int mi = 0; mi < scheduledGame.metrics.size(); mi++) {
            NflGameMetric gameMetric = scheduledGame.metrics.get(mi);

            // accumulate the score into the metric map element

            float metricScore = NflWeeklyData.gameMetrics.get(gameMetric.metricName);
            metricScore += gameMetric.score * gameMetric.weight;
            NflWeeklyData.gameMetrics.put(gameMetric.metricName, metricScore);

            if (gameMetric.hardViolation) {
               weeklyData.hardViolationCount++;
            }
         }
         weeklyData.score += scheduledGame.score;
         weeklyData.demotionPenalty += scheduledGame.demotionPenalty;
      }
      return true;
   }

   public boolean writeWeeklyDataHeaderToFile() {
      if (scheduler.reschedLogOn) {
         scheduler.writeReschedLogFile("iter,week,success,retry,sDirection" + ",sByes,sGames,usTeams,usGames,usByes"
               + ",fWeekSched,bWeekSched,cAlreadySched,cBackToBack,cResUnavail,cHardVios"
               + ",cHShardVio,cRThardVio,cB2BhardVio,sHardVios,score");

         NflGameSchedule gameSchedule = curSchedule.unscheduledGames.get(1);
         for (int mi = 0; mi < gameSchedule.metrics.size(); mi++) {
            NflGameMetric gameMetric = gameSchedule.metrics.get(mi);
            scheduler.writeReschedLogFile("," + gameMetric.metricName);
         }
         scheduler.writeReschedLogFile(",nWeeksBack,pSuccess,pRetry,pNWeeksBack,demotionPenalty,fpSkipCnt");
         scheduler.writeReschedLogFile("\n");
      }
      return true;
   }

   public boolean writeWeeklyDataToFile() {
      if (scheduler.reschedLogOn) {
         scheduler.writeReschedLogFile(weeklyData.iterNum + "," + weeklyData.weekNum + "," + weeklyData.success + ","
               + weeklyData.weekResult.toString() + "," + weeklyData.schedulingDirection + ",");
         scheduler.writeReschedLogFile(weeklyData.scheduledByes + "," + weeklyData.scheduledGames + ","
               + weeklyData.unscheduledTeams + "," + weeklyData.unscheduledGames + "," + weeklyData.unscheduledByes
               + "," + weeklyData.forwardWeekScheduled + "," + weeklyData.backwardWeekScheduled + ","
               + weeklyData.alreadyScheduledRejection + "," + weeklyData.backToBackMatchRejection + ","
               + weeklyData.resourceUnavailRejection + "," + weeklyData.hardViolationRejection + ","
               + weeklyData.homeStandHardViolation + "," + weeklyData.roadTripHardViolation + ","
               + weeklyData.repeatedMatchUpHardViolation + "," + weeklyData.hardViolationCount + ","
               + weeklyData.score);
         for (Map.Entry<String, Float> entry : NflWeeklyData.gameMetrics.entrySet()) {
            Float accumScore = entry.getValue();
            scheduler.writeReschedLogFile("," + accumScore.toString());
         }
         scheduler.writeReschedLogFile("," + weeklyData.numWeeksBack + "," + weeklyData.priorSuccess + ","
               + weeklyData.priorWeekResult.toString() + "," + weeklyData.priorNumWeeksBack + ","
               + weeklyData.demotionPenalty + "," + weeklyData.fpSkipCount);
         scheduler.writeReschedLogFile("\n");
      }
      return true;
   }

   public boolean logWeeklyDataWeekStart(NflSchedule schedule) {
      if (scheduler.reschedLogOn) {
         priorWeeklyData = weeklyData;
         weeklyData = new NflWeeklyData();
         weeklyData.iterNum = iterNum;
         weeklyData.weekNum = weekNum;
         weeklyData.schedulingDirection = sDir;
         if (priorWeeklyData != null) {
            weeklyData.priorSuccess = priorWeeklyData.success;
            weeklyData.priorNumWeeksBack = priorWeeklyData.numWeeksBack;
            weeklyData.priorWeekResult = priorWeeklyData.weekResult;
         }

         for (Map.Entry<String, Float> gameMetric : NflWeeklyData.gameMetrics.entrySet()) {
            NflWeeklyData.gameMetrics.replace(gameMetric.getKey(), 0.0f);
         }
      }
      return true;
   }

   public boolean logWeeklyDataWeekScheduleAttempt(NflSchedule schedule,
         NflWeeklyData.weekScheduleResultType weekScheduleResult) {
      if (scheduler.reschedLogOn) {
         if (weekScheduleResult == NflWeeklyData.weekScheduleResultType.success) {
            weeklyData.success = true;
         } else {
            weeklyData.success = false;
         }
         weeklyData.weekResult = weekScheduleResult;
         weeklyData.backwardWeekScheduled = bWeekScheduled;
         weeklyData.forwardWeekScheduled = fWeekScheduled;
         weeklyData.numWeeksBack = 0;
         if (weekScheduleResult == NflWeeklyData.weekScheduleResultType.failMultiWeeksBack) {
            weeklyData.numWeeksBack = numWeeksBack;
         }
         weeklyData.scheduledByes = byesScheduledThisWeek;
         weeklyData.scheduledGames = NflDefs.numberOfTeams / 2 - unscheduledTeams.size() / 2
               - byesScheduledThisWeek / 2;
         weeklyData.unscheduledByes = schedule.unscheduledByes.size();
         weeklyData.unscheduledGames = schedule.unscheduledGames.size();
         weeklyData.unscheduledTeams = unscheduledTeams.size();

         weeklyData.alreadyScheduledRejection = alreadyScheduledRejection;
         weeklyData.backToBackMatchRejection = backToBackMatchRejection;
         weeklyData.resourceUnavailRejection = resourceUnavailRejection;
         weeklyData.hardViolationRejection = hardViolationRejection;
         weeklyData.fpSkipCount = fpSkipCount;
      }
      return true;
   }

   public boolean logWeeklyDataHardViolation(NflGameMetric gameMetric) {
      if (gameMetric.metricName.equalsIgnoreCase("HomeStandLimit")) {
         weeklyData.homeStandHardViolation = true;
      } else if (gameMetric.metricName.equalsIgnoreCase("RoadTripLimit")) {
         weeklyData.roadTripHardViolation = true;
      } else if (gameMetric.metricName.equalsIgnoreCase("NoRepeatedMatchup")) {
         weeklyData.repeatedMatchUpHardViolation = true;
      }
      return true;
   }

}