package com.nflscheduling;

public class NflScheduleAlgForAndBack extends NflScheduleAlg {

   @Override
   public boolean scheduleUnrestrictedGames(final NflSchedule schedule) {

      boolean status = true;

      reschedAttemptsMultiWeeksBack = 0;
      bWeekLowest = NflDefs.numberOfWeeks+1;
      fWeekHighest = 0;
      int smallestUnschedGapWeeks = NflDefs.numberOfWeeks;

      sDir = -1;
      fWeekScheduled = 0;
      bWeekScheduled = NflDefs.numberOfWeeks + 1;
      remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;

      while(remWeeksToSchedule > 0) {
         if (sDir == -1) {
            weekNum = bWeekScheduled - 1;
            if (bWeekScheduled < bWeekLowest) bWeekLowest = bWeekScheduled;
         }
         else if (sDir == 1) {
            weekNum = fWeekScheduled + 1;
            if (fWeekScheduled > fWeekHighest) fWeekHighest = fWeekScheduled;
         }
         if (remWeeksToSchedule < smallestUnschedGapWeeks) smallestUnschedGapWeeks = remWeeksToSchedule;

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
            reschedAttemptsSameWeek = 0;
            logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.success);
            NflDefs.schedulingDirection = (sDir *= -1);  // reverse the scheduling direction
            numWeeksBack = 0;
         }
         else {
            // failed to schedule weekNum for this direction
            if (sDir == -1)     bWeekSuccess = false;
            else if (sDir == 1) fWeekSuccess = false;

            if (reschedAttemptsMultiWeeksBack >= NflDefs.reschedAttemptsMultiWeeksBackLimit) {
               // Exhaustion of retries
               terminationReason = "Failed to schedule all unrestricted games in week: " + weekNum + 
                                   ", high Week forward: " + fWeekHighest + ", low Week backward: " + bWeekLowest + 
                                   ", smallest gap: " + smallestUnschedGapWeeks;
               status = false;
               logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.failExhaustAllRetries);
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
                  logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.failMultiWeeksBack);
                  NflDefs.schedulingDirection = (sDir *= -1);  // reverse the scheduling direction
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
                  reschedAttemptsSameWeek=0;
                  logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.failChangeDir);
                  NflDefs.schedulingDirection = (sDir *= -1);  // reverse the scheduling direction
               }
            }
            else {
               boolean shouldClearHistory = true;
               unscheduleUnrestrictedWeek(schedule, weekNum, shouldClearHistory);
               updateDemotionInfo(schedule, weekNum); 
               reschedAttemptsSameWeek++;
               logWeeklyDataWeekScheduleAttempt(schedule, NflWeeklyData.weekScheduleResultType.failRepeatSameWeek);
            }
         }
         writeWeeklyDataToFile();
         remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;
      }

      return status;
   }

}