package com.nflscheduling;

public class NflTeamWeek {
	
	public NflTeam team;
	public NflGameSchedule[] candidateGames;

	NflTeamWeek(NflTeam theTeam) {
       team = theTeam;
       candidateGames = new NflGameSchedule[NflDefs.numberOfWeeks];
    }
}
