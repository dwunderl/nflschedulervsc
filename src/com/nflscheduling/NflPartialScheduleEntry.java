package com.nflscheduling;

import java.util.ArrayList;

public class NflPartialScheduleEntry {
	
    public double fingerPrint = 0.0;
    public double baseFingerPrint = 0.0;
    public int weekNum;
    public int iterNum = 0;
    public int unscheduledTeams = 0;
    public ArrayList<NflGameSchedule> gamesInWeek = null;
    public int count = 0;

	public NflPartialScheduleEntry() {
	}
}
