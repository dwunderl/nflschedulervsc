package com.nflscheduling;
import java.util.*;

public class NflTeamWeek {
	
	public NflTeam team;
    public ArrayList<NflGameSchedule> candidateGames;
    public NflRestrictedGame restrictedGame;

	NflTeamWeek(NflTeam theTeam) {
       team = theTeam;
       restrictedGame = null;
       candidateGames = new ArrayList<NflGameSchedule>();
    }
}
