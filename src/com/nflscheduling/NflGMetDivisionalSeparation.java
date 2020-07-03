package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetDivisionalSeparation extends NflGameMetric {

	public NflGMetDivisionalSeparation(String theName, NflGameSchedule theGameSchedule) {
		super(theName, theGameSchedule);
	}

	@Override
    public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {

      // ensure at least one of a divisional pair is scheduled outside of week 5

      // if division game and within week 5 and matchup game already scheduled within week 5
      //     reject scheduling the matchup within week 5 - hard violation
      // if division game and within week 5 and matchup game already scheduled outside of week 5
      //     no problem - ok to schedule here
      // if division game and within week 5 and matchup game not yet scheduled
      //     no problem - ok to schedule here
      // if division game and outside of week 5 and matchup game not yet scheduled
      //     incentivize to schedule - to ensure at least one game of pair schedules outside of week 5

       score = 0.0;

       // This metric only applies to divisional games
       if (!gameSchedule.game.findAttribute("division")) {
          return true;
       }

       // has the other divisional game been scheduled yet - in a later week
       // if so - no problem - otherwise - incentivize this game with a negative
       // penalty

      int divisionalPairWeekScheduled = 0;

      NflTeamSchedule teamSchedule = gameSchedule.homeTeamSchedule;
      for (int wi = 1; wi <= NflDefs.numberOfWeeks; wi++) {
          NflGameSchedule teamGame2 = teamSchedule.scheduledGames[wi - 1];

          if (teamGame2 == null || teamGame2.isBye || !teamGame2.game.findAttribute("division")) {
             continue;
          }

          if (gameSchedule.game.awayTeam.equalsIgnoreCase(teamGame2.game.homeTeam)
                && gameSchedule.game.homeTeam.equalsIgnoreCase(teamGame2.game.awayTeam)) {
             divisionalPairWeekScheduled = wi;
             break;
          }
       }

       if (divisionalPairWeekScheduled > 5) {
          // Not possible to schedule pair within week 5
          return true;
       }
       else if (divisionalPairWeekScheduled > 0) {
          if (weekNum <= 5) {
             // the candidate would schedule in week 5 thus the pair would be within week 5
             // That's a hard violation - not allowable
             hardViolation = true;
             score = 5;
          }
       }
       else if (weekNum > 5) {
          // Neither games of the divisional pair are inside of 5
          // incentivize to schedule at least one of the games outside of week 5

          score = -1;
       }

       return true;
    }
}
