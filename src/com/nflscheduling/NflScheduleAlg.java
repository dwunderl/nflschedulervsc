package com.nflscheduling;

import java.util.*;
//import java.io.IOException;

public class NflScheduleAlg {

   public NflScheduler scheduler;

   public NflSchedule curSchedule; // curSchedule.teams holds the partial schedule
   // Each team has an array of scheduled games - 1 per week
   // Byes are scheduled games marked with isBye=true
   // Holds arrays of allGames, unscheduledGames, unscheduledByes

   public String terminationReason;

   // Demotion Scheme: TBD document it
   // Promotion scheme is not used - promotionInfo
   
   public Random rnd = new Random();

   // logging of algorithm progress and histories
   public int iterNum = 0;

   // partialSchedules
   // Array season weeks - summarizes the partial schedule for each scheduled week
   // so far
   // Each partial schedule entry has the fingerprint (# summary) sum of all the
   // weighted fingerprints for each week
   // Also has iterNum, weekNum, unscheduledTeams (?)

   public NflPartialScheduleEntry[] partialSchedules;

   public HashMap<Double, NflPartialScheduleEntry> fingerPrintMap;

   public int fpSkipCount = 0; // Counts number of repeated encounters of a FingerPrint after completing a
                               // fully scheduled week
                               // Each encounter is failed, causing a backtrack rescheduling attempt

   // unscheduledTeams - after failure of a week, the unscheduledTeams in that week
   // are given first priority during the next weekly schedule
   // TBD: study and rethink if this really makes any real sense

   public ArrayList<NflTeamSchedule> unscheduledTeams = new ArrayList<NflTeamSchedule>();

   public int alreadyScheduledRejection;
   public int backToBackMatchRejection;
   public int resourceUnavailRejection;
   public int hardViolationRejection;
   public int highWaterMark;

   public boolean init(NflScheduler theScheduler) {
      scheduler = theScheduler;
      return true;
   }

   public boolean scheduleInit() {
      iterNum = 0;
      fingerPrintMap = new HashMap<Double, NflPartialScheduleEntry>();
      fpSkipCount = 0;
      partialSchedules = new NflPartialScheduleEntry[NflDefs.numberOfWeeks];
      rnd = new Random();

      return true;
   }

   public boolean scheduleForcedGames(ArrayList<NflRestrictedGame> restrictedGames, NflSchedule schedule) {
      return true;
   }

   public boolean scheduleUnrestrictedGames(final NflSchedule schedule) {
      return true;
   }

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
               if (resourceSchedule.resource.zone.equalsIgnoreCase("international")) {
                  usGame.isInternational = true;
               }
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

   public boolean openLogging() {
      return true;
   }

   public boolean closeLogging() {
      return true;
   }
}