package com.nflscheduling;

import java.util.ArrayList;

public class NflGMetHomeGameInLast3weeks extends NflGameMetric {

	public NflGMetHomeGameInLast3weeks(String theName, NflGameSchedule theGame) {
		super(theName, theGame);
	}
	
	@Override
	public boolean computeMetric(int weekNum, NflSchedule schedule, ArrayList<NflGameSchedule> candidateGames) {
	   return false;
	}
}
