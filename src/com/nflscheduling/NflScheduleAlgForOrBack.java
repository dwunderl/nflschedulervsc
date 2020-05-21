package com.nflscheduling;

public class NflScheduleAlgForOrBack extends NflScheduleAlg {
    
    @Override
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

        // for (int weekNum=startWeek; weekNum != endWeek; weekNum +=
        // NflDefs.schedulingDirection) {
        for (int weekNum = startWeek; weekNum * sDir <= endWeek * sDir; weekNum += sDir) {
           reschedAttemptsSameWeek = 0;
           terminationReason = "Beginning of week loop: " + iterNum;
System.out.println("0:Week: " + weekNum + ", sDir: " + sDir );

           // System.out.println("\nscheduleUnrestrictedGames: for next week: " + weekNum);
           initPromotionInfo(schedule);
           unscheduledTeams.clear();
  
           while (!scheduleUnrestrictedWeek(schedule, weekNum)) {
              // Backtracking logic
              // if continue - not ready to terminate
              // unschedule weekNum, add a demotion penalty to the first scheduled game and
              // increment a demotion count (0 to 1 for first time)
              // unschedule weekNum - NEW: choose a different game to demote, start with the
              // lowest scored scheduled game
System.out.println("1:Week: " + weekNum + ", sDir: " + sDir );
  
              // Clear demotion penalties of other games - unless demotion count > 1 (demoted
              // twice) don't clear demotion penalty (a culprit)
              // keep the demotion
  
              if (reschedAttemptsMultiWeeksBack >= NflDefs.reschedAttemptsMultiWeeksBackLimit) {
                 terminationReason = "Failed to schedule all unrestricted games in week: " + weekNum + ", low Week: "
                       + lowestWeekNum;
                 status = false;
                 break;
              } else if (reschedAttemptsOneWeekBack >= NflDefs.reschedAttemptsOneWeekBackLimit) {
                 // Now back track multiple weeks - single week back tracks exhausted
                 unscheduledTeams.clear();
  
                 if (sDir == -1) {
                    goBackToWeekNum = Math.min(reschedAttemptedMaxWeek + 1, NflDefs.numberOfWeeks);
                 } else if (sDir == 1) {
                    goBackToWeekNum = Math.max(reschedAttemptedMaxWeek - 1, 1);
                 }
                 reschedAttemptedMaxWeek = goBackToWeekNum;
                 reschedAttemptsMultiWeeksBack++;
                 reschedAttemptsOneWeekBack = 0;
                 reschedAttemptsSameWeek = 0;
                 scheduler.reschedLog.add("back tracking multiple weeks\n");
                 terminationReason = "back tracking multiple weeks, iternum: " + iterNum;
                 // System.out.println("back tracking multiple weeks\n");
              } else if (reschedAttemptsSameWeek >= NflDefs.reschedAttemptsSameWeekLimit) {
                 // Now back track a single week - cur week reschedules exhausted
                 unscheduledTeams.clear();
  
                 if (sDir == -1) {
                    goBackToWeekNum = Math.min(weekNum + 1, NflDefs.numberOfWeeks);
                    reschedAttemptedMaxWeek = Math.max(goBackToWeekNum, reschedAttemptedMaxWeek);
                 } else if (sDir == 1) {
                    goBackToWeekNum = Math.max(weekNum - 1, 1);
                    reschedAttemptedMaxWeek = Math.min(goBackToWeekNum, reschedAttemptedMaxWeek);
                 }
  
                 reschedAttemptsOneWeekBack++;
                 reschedAttemptsSameWeek = 0;
                 scheduler.reschedLog.add("back tracking a single week\n");
                 terminationReason = "back tracking a single week, iternum: " + iterNum;

                 // System.out.println("back tracking a single week\n");
              } else {
                 // Now just reschedule the cur week until it's reschedules are exhausted
  
                 goBackToWeekNum = weekNum;
   
                 reschedAttemptsSameWeek++;
                 scheduler.reschedLog.add("rescheduling the current week\n");
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
  
              updateDemotionInfo(schedule, goBackToWeekNum);
  
              scheduler.reschedLog.add("reschedAttemptsSameWeek: " + reschedAttemptsSameWeek + " for week: " + weekNum
                    + ", reschedAttemptsOneWeekBack: " + reschedAttemptsOneWeekBack + ", reschedAttemptsMultiWeeksBack: "
                    + reschedAttemptsMultiWeeksBack + ", reschedAttemptedMaxWeek: " + reschedAttemptedMaxWeek
                    + ", goBackToWeekNum: " + goBackToWeekNum + "\n");
  
              weekNum = goBackToWeekNum;
           }
  
           if (weekNum * sDir > lowestWeekNum * sDir) {
              lowestWeekNum = weekNum;
           }
  
           if (status == false) {
              break;
           }
        }
  
        return status;
     }  
}