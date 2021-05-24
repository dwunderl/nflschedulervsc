package com.nflscheduling;

public class NflScheduleAlgForOrBack extends NflScheduleByWeekAlg {
    
   // Adapted version from ForwardAndBackward - to do ForwardOrBackward
   // And to be very harmonized with ForwardAndBackward
   // doesn't work well enough - yet
   // May need to adopt very similar backtracking - 1-week and multi-week like the original forward or backward

   @Override
   public boolean scheduleUnrestrictedGames(final NflSchedule schedule) {

      boolean status = true;
      sDir = NflDefs.schedulingDirection;
      fWeekScheduled = 0;
      bWeekScheduled = NflDefs.numberOfWeeks + 1;
      bWeekLowest = NflDefs.numberOfWeeks+1;
      fWeekHighest = 0;

      int goBackToWeekNum = 0;
      int reschedAttemptedMaxWeek = 1;
      reschedAttemptsOneWeekBack = 0;
      reschedAttemptsMultiWeeksBack = 0;
      remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;
      if (sDir == -1)     reschedAttemptedMaxWeek = 1;
      else if (sDir == 1) reschedAttemptedMaxWeek = NflDefs.numberOfWeeks;

      NflWeeklyData.weekScheduleResultType weekResultType = NflWeeklyData.weekScheduleResultType.success;

      while(remWeeksToSchedule > 0) {
         if (sDir == -1) {
            weekNum = bWeekScheduled - 1;
            if (bWeekScheduled < bWeekLowest) 
               bWeekLowest = bWeekScheduled;
         }
         else if (sDir == 1) {
            weekNum = fWeekScheduled + 1;
            if (fWeekScheduled > fWeekHighest)
               fWeekHighest = fWeekScheduled;
         }

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
            weekResultType = NflWeeklyData.weekScheduleResultType.success;
            reschedAttemptsSameWeek = 0;
         }
         else {
            // failed to schedule weekNum for this direction

            if (sDir == -1)     bWeekSuccess = false;
            else if (sDir == 1) fWeekSuccess = false;

            if (reschedAttemptsMultiWeeksBack >= NflDefs.reschedAttemptsMultiWeeksBackLimit) {
               // Exhaustion of all retries - terminate the scheduling algorithm at this point
               if (sDir == -1)
                  terminationReason = "Failed to schedule all unrestricted games in week: " + weekNum + ", low Week: " + bWeekLowest;
               else if (sDir == 1)
                  terminationReason = "Failed to schedule all unrestricted games in week: " + weekNum + ", high Week: " + fWeekHighest;
               status = false;
               weekResultType = NflWeeklyData.weekScheduleResultType.failExhaustAllRetries;
               break;
            }
            else if (reschedAttemptsOneWeekBack >= NflDefs.reschedAttemptsOneWeekBackLimit) {
               // Now back track multiple weeks - single week back tracks exhausted

               if (sDir == -1)
                  goBackToWeekNum = Math.min(reschedAttemptedMaxWeek + 1, NflDefs.numberOfWeeks);
               else if (sDir == 1)
                  goBackToWeekNum = Math.max(reschedAttemptedMaxWeek - 1, 1);

               reschedAttemptedMaxWeek = goBackToWeekNum;
               reschedAttemptsMultiWeeksBack++;
               reschedAttemptsOneWeekBack = 0;
               reschedAttemptsSameWeek = 0;
               weekResultType = NflWeeklyData.weekScheduleResultType.failMultiWeeksBack;
            }
            else if (reschedAttemptsSameWeek >= NflDefs.reschedAttemptsSameWeekLimit) {
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
            }
            else {
               // reschedule the current week - no back tracking
               goBackToWeekNum = weekNum;
               reschedAttemptsSameWeek++;
               weekResultType = NflWeeklyData.weekScheduleResultType.failRepeatSameWeek;
            }
            // unschedule for failed week (and backtrack weeks) + reset weekNum + state for the reschedule
            // unschedule from weekNum to goBackToWeekNum according to sDir

            boolean shouldClearHistory = true;
            for (int wn = weekNum; wn*sDir >= goBackToWeekNum*sDir; wn-=sDir) {
               if (!unscheduleUnrestrictedWeek(schedule, wn, shouldClearHistory)) {
                  return false;
               }
            }
            updateDemotionInfo(schedule, goBackToWeekNum);
            if (sDir == -1) 
               bWeekScheduled = goBackToWeekNum+1;
            else if (sDir == 1) 
               fWeekScheduled = goBackToWeekNum-1;
         }
         logWeeklyDataWeekScheduleAttempt(schedule, weekResultType);
         writeWeeklyDataToFile();
         remWeeksToSchedule = bWeekScheduled - fWeekScheduled - 1;
      }

      return status;
   }

}
