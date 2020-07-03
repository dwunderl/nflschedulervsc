package com.nflscheduling;

import java.util.LinkedHashMap;

public class NflWeeklyData {
    public int iterNum;
    public int weekNum;
    public boolean success = true;
    public weekScheduleResultType weekResult = weekScheduleResultType.success;

    public int schedulingDirection = 1;
    public int scheduledByes = 0;
    public int scheduledGames = 0;
    public int unscheduledTeams = 0;
    public int unscheduledGames = 0;
    public int unscheduledByes = 0;
    public int forwardWeekScheduled;
    public int backwardWeekScheduled;
    public int alreadyScheduledRejection = 0;
    public int backToBackMatchRejection = 0;
    public int resourceUnavailRejection = 0;
    public int hardViolationRejection = 0;
    public int numWeeksBack = 0;

    public boolean priorSuccess;
    public weekScheduleResultType priorWeekResult;
    public int priorNumWeeksBack;
    public int hardViolationCount = 0;
    public double score = 0.0f;
    public int demotionPenalty = 0;
    public static LinkedHashMap<String,Float> gameMetrics;
    public boolean homeStandHardViolation = false;
    public boolean roadTripHardViolation = false;
    public boolean repeatedMatchUpHardViolation = false;
    public int fpSkipCount = 0;

    public enum weekScheduleResultType {
        success,
        failRepeatSameWeek,
        failOneWeekBack,
        failMultiWeeksBack,
        failChangeDir,
        failExhaustAllRetries
    }

    public static boolean init(NflGameSchedule gameSchedule) {
        gameMetrics = new LinkedHashMap<String,Float>();
        for (int mi = 0; mi < gameSchedule.metrics.size(); mi++) {
            NflGameMetric gameMetric = gameSchedule.metrics.get(mi);
            gameMetrics.put(gameMetric.metricName,0.0f);
        }

        return true;
    }
}

