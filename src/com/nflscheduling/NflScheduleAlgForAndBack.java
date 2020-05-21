package com.nflscheduling;

public class NflScheduleAlgForAndBack extends NflScheduleAlg {

   @Override
   public boolean scheduleUnrestrictedGames(final NflSchedule schedule) {

      int fWeekScheduled = 0;
      int bWeekScheduled = NflDefs.numberOfWeeks + 1;
      int remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;
      boolean fWeekSuccess = true;
      boolean bWeekSuccess = true;
      int numWeeksBack = 0;
      int weekNum = 0;
      boolean status = true;

      reschedAttemptsMultiWeeksBack = 0;

      initPromotionInfo(schedule);
      unscheduledTeams.clear();

      int bWeekLowest = 1000;
      int fWeekHighest = 0;

      sDir = -1;

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
         unscheduledTeams.clear();

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

            scheduler.writeReschedLogFile("Successful week: " + weekNum + ", sDir: " + sDir + ", fweeks: " + fWeekScheduled +
                                          ", bweeks: " + bWeekScheduled + ", remWeeks: " + remWeeksToSchedule +
                                          ", fWeekSucceess: " + fWeekSuccess + ", bWeekSucceess: " + bWeekSuccess +
                                          ", us games/byes: " + schedule.unscheduledGames.size() + "/" + schedule.unscheduledByes.size() +
                                          ", us Tms: " + unscheduledTeams.size() + "\n");

            sDir *= -1;  // reverse the scheduling direction
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
               terminationReason = "Failed to schedule all unrestricted games in week: " + weekNum + ", low Week: " + lowestWeekNum;
               // TBD - need something other than lowestWeekNum to show progress - maybe 
               status = false;
               scheduler.writeReschedLogFile("   Fail+Exhaust Multi week retries:Week: " + weekNum + ", sDir: " + sDir + 
                                             ", fWeekSucc: " + fWeekSuccess + ", bWeekSucc: " + bWeekSuccess + ", fWeekHighest: " + fWeekHighest + 
                                             ", bWeekLowest: " + bWeekLowest + 
                                             ", us games/byes: " + schedule.unscheduledGames.size() + "/" + schedule.unscheduledByes.size() +
                                             ", us Tms: " + unscheduledTeams.size() + "\n");
               break;
            }
            else if (reschedAttemptsSameWeek >= NflDefs.reschedAttemptsSameWeekLimit) {
               // if double fail, increase numWeeksBack
               // unschedule numWeeksBack for both directions
               // reset fWeekScheduled,bWeekScheduled, remWeeksToSchedule for next attempt

               if (!fWeekSuccess && !bWeekSuccess) {
                  numWeeksBack++;
                  if (numWeeksBack > 3) {
                     numWeeksBack = 1;
                  }

                  // unschedule both directions back by numWeeksBack
                  boolean shouldClearHistory = true;
                  int newfWeekScheduled = Math.max(fWeekScheduled-numWeeksBack,0);
                  for (int wn = newfWeekScheduled+1; wn <= fWeekScheduled+1; wn++) {
                     if (!unscheduleUnrestrictedWeek(schedule, wn, shouldClearHistory)) {
                        return false;
                     }
                  }
                  fWeekScheduled = newfWeekScheduled;
                  updateDemotionInfo(schedule, fWeekScheduled+1); 

                  int newbWeekScheduled = Math.min(bWeekScheduled+numWeeksBack,NflDefs.numberOfWeeks + 1);
                  for (int wn = bWeekScheduled-1; wn <= newbWeekScheduled-1; wn++) {
                     if (!unscheduleUnrestrictedWeek(schedule, wn, shouldClearHistory)) {
                        return false;
                     }
                  }
                  bWeekScheduled = newbWeekScheduled;
                  updateDemotionInfo(schedule, bWeekScheduled-1); 
                  reschedAttemptsMultiWeeksBack++;
                  reschedAttemptsSameWeek = 0;

                  remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;
                  scheduler.writeReschedLogFile("   Fail-exhaust same week: " + weekNum + ", both dirs, multi-week retry: " + numWeeksBack + ", sDir: " + sDir + ", fweeks: " + fWeekScheduled +
                                                ", bweeks: " + bWeekScheduled + ", remWeeks: " + remWeeksToSchedule +
                                                ", fWeekSucc: " + fWeekSuccess + ", bWeekSucc: " + bWeekSuccess +
                                                ", us games/byes: " + schedule.unscheduledGames.size() + "/" + schedule.unscheduledByes.size() +
                                                ", us Tms: " + unscheduledTeams.size() + "\n");
                  unscheduledTeams.clear();
                  sDir *= -1;  // reverse the scheduling direction
                  bWeekSuccess = true;
                  fWeekSuccess = true;
               }
               else if (!fWeekSuccess || !bWeekSuccess) {
                  // just one direction has currently failed and exhausted retries
                  // switch back to the other direction and try to make progress there
                  // No change to the scheduled week state

                  boolean shouldClearHistory = true;
                  unscheduleUnrestrictedWeek(schedule, weekNum, shouldClearHistory);
                  updateDemotionInfo(schedule, weekNum); 
                  scheduler.writeReschedLogFile("   Fail-exhaust same week: " + weekNum + ", in 1 dir, retry other way: " + ", sDir: " + sDir + ", fweeks: " + fWeekScheduled +
                                                ", bweeks: " + bWeekScheduled + ", remWeeks: " + remWeeksToSchedule +
                                                ", fWeekSucc: " + fWeekSuccess + ", bWeekSucc: " + bWeekSuccess +
                                                ", us games/byes: " + schedule.unscheduledGames.size() + "/" + schedule.unscheduledByes.size() +
                                                ", us Tms: " + unscheduledTeams.size() + "\n");
                  unscheduledTeams.clear();
                  reschedAttemptsSameWeek=0;

                  sDir *= -1;  // reverse the scheduling direction   
               }
            }
            else {

               boolean shouldClearHistory = true;
               unscheduleUnrestrictedWeek(schedule, weekNum, shouldClearHistory);
               updateDemotionInfo(schedule, weekNum); 
               scheduler.writeReschedLogFile("   Fail-under same week limit: " + weekNum + ", retry same week: " + ", sDir: " + sDir + ", fweeks: " + fWeekScheduled +
                                             ", bweeks: " + bWeekScheduled + ", remWeeks: " + remWeeksToSchedule +
                                             ", fWeekSucc: " + fWeekSuccess + ", bWeekSucc: " + bWeekSuccess +
                                             ", us games/byes: " + schedule.unscheduledGames.size() + "/" + schedule.unscheduledByes.size() +
                                             ", us Teams: " + unscheduledTeams.size() + "\n");               // repeat the same week, first unschedule 
               unscheduledTeams.clear();
               reschedAttemptsSameWeek++;
            }
         }

         remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;
      }

      return status;
   }
}