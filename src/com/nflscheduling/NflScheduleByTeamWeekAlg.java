package com.nflscheduling;

import java.util.*;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;

public class NflScheduleByTeamWeekAlg extends NflScheduleAlg {

   public BufferedWriter teamWeekLogBw = null;
   public FileWriter teamWeekLogFw = null;

   @Override
   public boolean init(NflScheduler theScheduler) {
      scheduler = theScheduler;
      return true;
   }

   @Override
   public boolean scheduleInit() {
      iterNum = 0;
      fingerPrintMap = new HashMap<Double, NflPartialScheduleEntry>();
      fpSkipCount = 0;
      partialSchedules = new NflPartialScheduleEntry[NflDefs.numberOfWeeks];
      rnd = new Random();
      // fully populate the teamweeks to have all games for the team of the teamweek
      populateTeamWeeks();
      return true;
   }

   @Override
   public boolean scheduleUnrestrictedGames(final NflSchedule schedule) {
      // tbd - next
      return true;
   }

   @Override
   public boolean scheduleForcedGames(ArrayList<NflRestrictedGame> restrictedGames, NflSchedule schedule) {

      // For each restricted game specification
      //    validate the parameters: resTeam, resOtherTeam, restriction, resStadium (TBD)
      //    validate resTeam and/or resOtherTeam not scheduled for resWeek
      //    Handle forced byes
      //    Handle Fully specified game (home and away teams)
      //    Handle Home team only specified
      //    Handle Divisional games home team only specified

      for (final NflRestrictedGame restrictedGame : restrictedGames) {
         int resWeekNum = restrictedGame.weekNum;
         String resTeamName = restrictedGame.teamName;
         NflTeamSchedule resTeam = schedule.findTeam(resTeamName);
         String resOtherTeamSpec = restrictedGame.otherTeamSpec;
         NflTeamSchedule resOtherTeam = schedule.findTeam(resOtherTeamSpec);
         String resStadium = restrictedGame.stadium;

         // Validate resTeam
         if (resTeam == null) {
            System.out.println("ERROR scheduling restricted game: can't find restricted team: " + resTeamName);
            return false;
         }

         // Validate other team specification
         if (!resOtherTeamSpec.equalsIgnoreCase("division") && 
             !resOtherTeamSpec.equalsIgnoreCase("anyTeam") && 
             !resOtherTeamSpec.equalsIgnoreCase("bye")) {
               if (resOtherTeam == null) {
                  System.out.println("ERROR scheduling restricted game: can't find restricted resOtherTeam: " + resOtherTeamSpec);
                  return false;
               }
               else {
                  System.out.println("ERROR scheduling restricted game: unrecognized restriction: " + resOtherTeamSpec);
                  return false;
               }
         }

         if (resStadium.length() > 0) {
            // validate that there is capacity for this week - if capacity is defined for it
            NflResourceSchedule resourceSchedule = schedule.findResource(resStadium);
            if (resourceSchedule != null && !resourceSchedule.hasCapacity(resWeekNum)) {
               System.out.println("ERROR scheduling restricted game: insufficient capacity for resStadium: " + resStadium);
               return false;
            }
         }

         // Validate that not already scheduled - may have been scheduled due to opponent
         // being scheduled in that week

         if (resTeam.scheduledGames[resWeekNum - 1] != null) {
            System.out.println("WARNING scheduling restricted game: home team: " + resTeamName + " already acheduled in week: " + resWeekNum);
            continue;
         }
         if (resOtherTeam != null && resOtherTeam.scheduledGames[resWeekNum - 1] != null) {
            System.out.println("WARNING scheduling restricted game: away team: " + resOtherTeamSpec + " already acheduled in week: " + resWeekNum);
            continue;
         }

         // Byes - handle forced byes here, then continue to the next forced game/bye
         if (resOtherTeamSpec.equalsIgnoreCase("bye")) {
            for (final NflGameSchedule usBye : schedule.unscheduledByes) {
               if (!resTeamName.equalsIgnoreCase(usBye.game.homeTeam)) {
                  continue;
               }
               // Found the unscheduled bye
               scheduler.placeGameInSchedule(usBye, resWeekNum, schedule);
               NflTeamWeek teamWeek = usBye.homeTeamSchedule.teamWeeks[resWeekNum-1];
               teamWeek.candidateGames.clear();  // no candidates remain - where this bye is scheduled
               teamWeek.restrictedGame = restrictedGame;
               // clear the schedule bye from all teamweeks for resTeam
               for (NflTeamWeek tw : resTeam.teamWeeks) {
                  tw.candidateGames.remove(usBye);
               }
               usBye.restrictedGame = true;
               break;
            }
            
            continue;  // Forced bye has been handled and scheduled
         }
         // Fully specified game (home and away teams)
         else if (resOtherTeam != null) {
            for (final NflGameSchedule usGame : schedule.unscheduledGames) {
               if (resTeam != usGame.homeTeamSchedule) {
                  continue;
               }
               if (resOtherTeam != usGame.awayTeamSchedule) {
                  continue;
               }
               // Found the exact specified unscheduled game
               scheduler.placeGameInSchedule(usGame, resWeekNum, schedule);

               // Update teamweeks for the hometeam
               NflTeamWeek teamWeek = usGame.homeTeamSchedule.teamWeeks[resWeekNum-1];
               teamWeek.candidateGames.clear();  // no more candidates where game is now scheduled
               teamWeek.restrictedGame = restrictedGame;
               // clear the scheduled game from all teamweeks for resTeam
               for (NflTeamWeek tw : resTeam.teamWeeks) {
                  tw.candidateGames.remove(usGame);
               }

               // Update teamweeks for the awayteam
               teamWeek = usGame.awayTeamSchedule.teamWeeks[resWeekNum-1];
               teamWeek.candidateGames.clear();
               teamWeek.restrictedGame = restrictedGame;
               // clear the scheduled game from all teamweeks for resOtherTeam
               for (NflTeamWeek tw : resOtherTeam.teamWeeks) {
                  tw.candidateGames.remove(usGame);
               }
               usGame.restrictedGame = true;
               break;
            }
            continue;  // Forced game has been handled and scheduled
         }
         // Home team only specified - specified team must be home - remove away games for this team - (vertical only)
         else if (resOtherTeamSpec.equalsIgnoreCase("anyTeam")) {
            // Find the teamweek for the resTeam and resWeekNum
            NflTeamWeek teamWeek = resTeam.teamWeeks[resWeekNum-1];
            // clear the candidate games where resTeam is awayGame
            teamWeek.candidateGames.removeIf(g -> (g.awayTeamSchedule == resTeam));
            teamWeek.candidateGames.removeIf(g -> (g.isBye == true));
            continue;  // Forced home game only has been handled and away game candidates removed
         }
         // Division Games, one specified team, remove non-divisional games for this team (home or away) (vertical only)
         else if (resOtherTeamSpec.equalsIgnoreCase("division")) {
            // Find the teamweek for the resTeam and resWeekNum
            NflTeamWeek teamWeek = resTeam.teamWeeks[resWeekNum-1];
            // clear the candidate games where resTeam is awayGame
            teamWeek.candidateGames.removeIf(g -> (g.game.isDivisional == true));
            teamWeek.candidateGames.removeIf(g -> (g.isBye == true));
            continue;  // Forced home game only has been handled and away game candidates removed
         }
      }
      return true;
   }

   public boolean populateTeamWeeks() {
      // walk through each game - distribute that game to all team weeks of home team and away team
      for (final NflGameSchedule usgame : curSchedule.unscheduledGames) {
         for (final NflTeamWeek homeTeamWeek : usgame.homeTeamSchedule.teamWeeks) {
            homeTeamWeek.candidateGames.add(usgame);
         }
         for (final NflTeamWeek awayTeamWeek : usgame.awayTeamSchedule.teamWeeks) {
            awayTeamWeek.candidateGames.add(usgame);
         }
      }

      for (final NflGameSchedule usBye : curSchedule.unscheduledByes) {
         for (final NflTeamWeek homeTeamWeek : usBye.homeTeamSchedule.teamWeeks) {
            homeTeamWeek.candidateGames.add(usBye);
         }
      }

      return true;
   }

   public boolean openLogging() {
      return true;
   }

   public boolean closeLogging() {
      return true;
   }

   public boolean openTeamWeekLogFile() {
      try {
         teamWeekLogFw = new FileWriter("logTeamWeekSchedResults" + scheduler.scheduleAttempts + ".csv");
         teamWeekLogBw = new BufferedWriter(teamWeekLogFw);

         // write the header to the file
         teamWeekLogBw.write("Week,Team,CandNumBefore,CandNumAfter,Operation,Home,Away,Forced\n");
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return true;
   }

   public boolean closeTeamWeekLogFile() {
      if (teamWeekLogBw != null) {
         try {
            teamWeekLogBw.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return true;
   }

}