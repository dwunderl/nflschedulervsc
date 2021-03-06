package com.nflscheduling;

public class NflDefs {
	
	public static int numberOfWeeks  = 17;
	public static int numberOfTeams  = 32;
	public static int reschedAttemptsMultiWeeksBackLimit = 10;
	public static int reschedAttemptsOneWeekBackLimit = 7;
	public static int reschedAttemptsSameWeekLimit = 7;
	public static int scheduleAttempts = 20;
	public static int savedScheduleLimit = 10;
	public static int alertLimit = 4;
	public static int hardViolationLimit = 0;
	public static int schedulingDirection = -1;
	public static boolean reschedLogOn = false;
	
	public enum AlgorithmType {Default,
							   Forward,
		                       Backward,
		                       ForwardAndBackward,
							   TeamWeek
							   }

	public static AlgorithmType algorithmType = AlgorithmType.Default;

	public NflDefs() {
	}

}
