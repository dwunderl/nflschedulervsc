package com.nflscheduling;

public class NflScheduleAlgForOrBack extends NflScheduleAlg {
    

   //public int fWeekScheduled = 0;
   //public int bWeekScheduled = NflDefs.numberOfWeeks + 1;
   //public int remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;
   //public boolean fWeekSuccess = true;
   //public boolean bWeekSuccess = true;
   //public int numWeeksBack = 0;
   //public int weekNum = 0;
/*
   // Adapted version from ForwardAndBackward - to do ForwardOrBackward
   // And to be very harmonized with ForwardAndBackward
   // doesn't work well enough - yet
   // May need to adopt very similar backtracking - 1-week and multi-week like the original forward or backward

   @Override
   public boolean scheduleUnrestrictedGames(final NflSchedule schedule) {

      boolean status = true;

      reschedAttemptsMultiWeeksBack = 0;

      initPromotionInfo(schedule);
      // unscheduledTeams.clear();

      int bWeekLowest = 1000;
      int fWeekHighest = 0;

      sDir = NflDefs.schedulingDirection;
      fWeekScheduled = 0;
      bWeekScheduled = NflDefs.numberOfWeeks + 1;
      remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;

      while(remWeeksToSchedule > 0) {
         if (sDir == -1) {
            weekNum = bWeekScheduled - 1;
         }
         else if (sDir == 1) {
            weekNum = fWeekScheduled + 1;
         }

         if (bWeekScheduled < bWeekLowest) bWeekLowest = bWeekScheduled;
         if (fWeekScheduled > fWeekHighest) fWeekHighest = fWeekScheduled;
         
         initPromotionInfo(schedule);
         //unscheduledTeams.clear();

         logWeeklyDataWeekStart(schedule);

         if (scheduleUnrestrictedWeek(schedule, weekNum)) {
            if (sDir == -1) {
               bWeekSuccess = true;
               bWeekScheduled = weekNum;
            }
            else if (sDir == 1) {
               fWeekSuccess = true;
               fWeekScheduled = weekNum;
            }
            remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;

            logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.success);

            reschedAttemptsSameWeek = 0;
         }
         else {
            // failed to schedule weekNum for this direction

            if (sDir == -1) {
               bWeekSuccess = false;
            }
            else if (sDir == 1) {
               fWeekSuccess = false;
            }

            if (reschedAttemptsMultiWeeksBack >= NflDefs.reschedAttemptsMultiWeeksBackLimit) {
               // Exhaustion of retries
               if (sDir == -1) {
                  terminationReason = "Failed to schedule all unrestricted games in week: " + weekNum + ", low Week: " + bWeekLowest;
               }
               else if (sDir == 1) {
                  terminationReason = "Failed to schedule all unrestricted games in week: " + weekNum + ", high Week: " + fWeekHighest;
               }
               // TBD - need something other than lowestWeekNum to show progress - maybe 
               status = false;

               logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.failExhaustAllRetries);

               break;
            }
            else if (reschedAttemptsSameWeek >= NflDefs.reschedAttemptsSameWeekLimit) {
               // if fail, increase numWeeksBack
               // unschedule numWeeksBack for both directions
               // reset fWeekScheduled,bWeekScheduled, remWeeksToSchedule for next attempt

               if (!fWeekSuccess || !bWeekSuccess) {
                  numWeeksBack = rnd.nextInt(4) + 1; // random number between 1 and 4
                  //numWeeksBack++;
                  //if (numWeeksBack > 5) {
                  //   numWeeksBack = 1;
                  //}

                  // unschedule back by numWeeksBack according to the scheduling direction
                  boolean shouldClearHistory = true;

                  if (sDir == 1) {
                     int newfWeekScheduled = Math.max(fWeekScheduled - numWeeksBack, 0);
                     for (int wn = newfWeekScheduled + 1; wn <= fWeekScheduled + 1; wn++) {
                        if (!unscheduleUnrestrictedWeek(schedule, wn, shouldClearHistory)) {
                           return false;
                        }
                     }
                     fWeekScheduled = newfWeekScheduled;
                     updateDemotionInfo(schedule, fWeekScheduled + 1);
                  } else if (sDir == -1) {
                     int newbWeekScheduled = Math.min(bWeekScheduled + numWeeksBack, NflDefs.numberOfWeeks + 1);
                     for (int wn = bWeekScheduled - 1; wn <= newbWeekScheduled - 1; wn++) {
                        if (!unscheduleUnrestrictedWeek(schedule, wn, shouldClearHistory)) {
                           return false;
                        }
                     }
                     bWeekScheduled = newbWeekScheduled;
                     updateDemotionInfo(schedule, bWeekScheduled - 1);
                  }
                  reschedAttemptsMultiWeeksBack++;
                  reschedAttemptsSameWeek = 0;

                  remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;

                  logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.failMultiWeeksBack);

                  //unscheduledTeams.clear();

                  bWeekSuccess = true; // reset
                  fWeekSuccess = true; // reset
               }
            }
            else {

               boolean shouldClearHistory = true;
               unscheduleUnrestrictedWeek(schedule, weekNum, shouldClearHistory);
               updateDemotionInfo(schedule, weekNum); 

               logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.failRepeatSameWeek);
                           
               //unscheduledTeams.clear();
               reschedAttemptsSameWeek++;
            }
         }

         // write it out here
         writeWeeklyDataToFile();

         remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;
      }

      return status;
   }

*/
   public boolean logWeeklyDataWeekStart(NflSchedule schedule) {
      if (scheduler.reschedLogOn) {
         priorWeeklyData = weeklyData;
         weeklyData = new NflWeeklyData();
         weeklyData.init(schedule.unscheduledGames.get(1));
         weeklyData.weekNum = weekNum;
         weeklyData.schedulingDirection = sDir;
         if (priorWeeklyData != null) {
            weeklyData.priorSuccess = priorWeeklyData.success;
            weeklyData.priorNumWeeksBack = priorWeeklyData.numWeeksBack;
            weeklyData.priorWeekResult = priorWeeklyData.weekResult;
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
      }
      return true;
   }


    public boolean scheduleUnrestrictedGames(final NflSchedule schedule) {
         terminationReason = "Beginning of scheduleUnrestrictedGames: " + iterNum;

        // schedule backwards from week numberOfWeeks to week 1
        // for each week
        // step through each team and then it's unscheduled games
        // evaluate and score each game as a candidate for scheduling
        // mfgMetric base class - then instances for each rule
        // each instance applies to a game at a week for a team
        // Then the 2 metrics for the same game, same week, 2 teams can be combined
        // to better know the best game to choose to schedule first for the week
        // Maybe keep the metric info around ??? for back tracking and rescheduling ???
        // Team -->
        // Individual penalties for each rule/metric, and weightings
        // Have a candidate object to wrap the game in - which holds the penalties
        // nflCandidateGame: game, rule penalties, overall penalty for my team, overall
        // penalty for both teams
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
  
        // start over - because 2 teams were just scheduled and their games will need to
        // be removed
        // start just randomly choosing a game - in order to get some kind of a schedule
  
        boolean status = true;
  
        reschedAttemptsOneWeekBack = 0;
        reschedAttemptsMultiWeeksBack = 0;
  
        int goBackToWeekNum = 0;
        int reschedAttemptedMaxWeek = 0;
  
        int startWeek = 1;
        int endWeek = NflDefs.numberOfWeeks;
        NflWeeklyData.weekScheduleResultType weekResultType = NflWeeklyData.weekScheduleResultType.success;

        final int sDir = NflDefs.schedulingDirection;

        if (sDir == -1) {
           startWeek = NflDefs.numberOfWeeks;
           endWeek = 1;
           reschedAttemptedMaxWeek = 1;
        } else if (sDir == 1) {
           startWeek = 1;
           endWeek = NflDefs.numberOfWeeks;
           reschedAttemptedMaxWeek = NflDefs.numberOfWeeks;
        }
        fWeekScheduled = 0;
        bWeekScheduled = NflDefs.numberOfWeeks + 1;

        // for (int weekNum=startWeek; weekNum != endWeek; weekNum +=
        // NflDefs.schedulingDirection) {
        for (weekNum = startWeek; weekNum * sDir <= endWeek * sDir; weekNum += sDir) {
           reschedAttemptsSameWeek = 0;
           terminationReason = "Beginning of week loop: " + iterNum;

           // System.out.println("\nscheduleUnrestrictedGames: for next week: " + weekNum);
           initPromotionInfo(schedule);
           //unscheduledTeams.clear();
           logWeeklyDataWeekStart(schedule);
           fWeekSuccess = true;
           bWeekSuccess = true;

           while (!scheduleUnrestrictedWeek(schedule, weekNum)) {
              // Backtracking logic
              // if continue - not ready to terminate
              // unschedule weekNum, add a demotion penalty to the first scheduled game and
              // increment a demotion count (0 to 1 for first time)
              // unschedule weekNum - NEW: choose a different game to demote, start with the
              // lowest scored scheduled game
  
              // Clear demotion penalties of other games - unless demotion count > 1 (demoted
              // twice) don't clear demotion penalty (a culprit)
              // keep the demotion
  
              bWeekSuccess = false;
              fWeekSuccess = false;

              if (reschedAttemptsMultiWeeksBack >= NflDefs.reschedAttemptsMultiWeeksBackLimit) {
                 terminationReason = "Failed to schedule all unrestricted games in week: " + weekNum + ", low Week: "
                       + lowestWeekNum;
                 status = false;
                 break;
              } else if (reschedAttemptsOneWeekBack >= NflDefs.reschedAttemptsOneWeekBackLimit) {
                 // Now back track multiple weeks - single week back tracks exhausted
  
                 if (sDir == -1) {
                    goBackToWeekNum = Math.min(reschedAttemptedMaxWeek + 1, NflDefs.numberOfWeeks);
                 } else if (sDir == 1) {
                    goBackToWeekNum = Math.max(reschedAttemptedMaxWeek - 1, 1);
                 }
                 reschedAttemptedMaxWeek = goBackToWeekNum;
                 reschedAttemptsMultiWeeksBack++;
                 reschedAttemptsOneWeekBack = 0;
                 reschedAttemptsSameWeek = 0;
                 weekResultType = NflWeeklyData.weekScheduleResultType.failMultiWeeksBack;

                 terminationReason = "back tracking multiple weeks, iternum: " + iterNum;
              } else if (reschedAttemptsSameWeek >= NflDefs.reschedAttemptsSameWeekLimit) {
                 // Now back track a single week - cur week reschedules exhausted
  
                 if (sDir == -1) {
                    goBackToWeekNum = Math.min(weekNum + 1, NflDefs.numberOfWeeks);
                    reschedAttemptedMaxWeek = Math.max(goBackToWeekNum, reschedAttemptedMaxWeek);
                 } else if (sDir == 1) {
                    goBackToWeekNum = Math.max(weekNum - 1, 1);
                    reschedAttemptedMaxWeek = Math.min(goBackToWeekNum, reschedAttemptedMaxWeek);
                 }
                 weekResultType = NflWeeklyData.weekScheduleResultType.failOneWeekBack;
                 reschedAttemptsOneWeekBack++;
                 reschedAttemptsSameWeek = 0;
                 // scheduler.reschedLog.add("back tracking a single week\n");
                 terminationReason = "back tracking a single week, iternum: " + iterNum;
              } else {
                 // Now just reschedule the cur week until it's reschedules are exhausted
                 //unscheduledTeams.clear();

                 goBackToWeekNum = weekNum;
   
                 reschedAttemptsSameWeek++;
                 weekResultType = NflWeeklyData.weekScheduleResultType.failRepeatSameWeek;

                 //scheduler.reschedLog.add("rescheduling the current week\n");
                 terminationReason = "rescheduling the current week, iternum: " + iterNum;
              }
  
              final boolean shouldClearHistory = true;
  
              for (int wn = weekNum; wn * sDir >= goBackToWeekNum * sDir; wn -= sDir) {
                 if (!unscheduleUnrestrictedWeek(schedule, wn, shouldClearHistory)) {
                    System.out.println("unscheduleUnrestrictedWeek - failed for week: " + wn);
                    terminationReason = "unscheduleUnrestrictedWeek - failed in week: " + wn;
                    return false;
                 }
              }


              logWeeklyDataWeekScheduleAttempt(schedule, weekResultType);

              updateDemotionInfo(schedule, goBackToWeekNum);

              weekNum = goBackToWeekNum;
              if (sDir == -1) {
                 bWeekScheduled = weekNum+1;
              } else if (sDir == 1) {
                 fWeekScheduled = weekNum-1;
              }
              writeWeeklyDataToFile();
              logWeeklyDataWeekStart(schedule);
           }
  
           if (weekNum * sDir > lowestWeekNum * sDir) {
              lowestWeekNum = weekNum;
           }

           if (status == true) {
              if (sDir == -1) {
                 bWeekSuccess = true;
                 bWeekScheduled = weekNum;
              } else if (sDir == 1) {
                 fWeekSuccess = true;
                 fWeekScheduled = weekNum;
              }
           }
           
           logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.success);

           // write it out here
           writeWeeklyDataToFile();

           if (status == false) {
              break;
           }
        }
  
        return status;
     }  

}
