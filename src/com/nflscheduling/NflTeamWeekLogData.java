package com.nflscheduling;

//import java.util.LinkedHashMap;

public class NflTeamWeekLogData {
    // teamWeekLogBw.write("Week,Team,CandNumBefore,CandNumAfter,Operation,Home,Away,Forced\n");

    public int weekNum;
    public String team;
    public int candNumBefore;
    public int candNumAfter;
    public String operation;
    public String homeTeam;
    public String awayTeam;
    public boolean isForced;

    public OperationType operationType = OperationType.schedule;

    public enum OperationType {
        schedule,
        constrain,
        ripple,
        unschedule
    }
  
    public static boolean init() {
        return true;
    }
}

