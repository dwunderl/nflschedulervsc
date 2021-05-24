package com.nflscheduling;

//import java.util.*;
//import java.io.File;
//import java.io.IOException;
//import java.util.Map;
//import java.util.Scanner;
//import java.util.TreeMap;

public class NflTeamSchedule {
	
	public NflTeam team;
	public NflGameSchedule[] scheduledGames;
   public NflTeamWeek[] teamWeeks;
	public int homeGameRunLength = 0;
	public int awayGameRunLength = 0;
	public int scheduledGameCount = 0;
	public double score = 0.0;

	NflTeamSchedule(NflTeam theTeam) {
       team = theTeam;
       scheduledGames = new NflGameSchedule[NflDefs.numberOfWeeks];
    }
	
	public boolean hasScheduledBye() {
		for(int wi=1; wi <= scheduledGames.length; wi++) {
           NflGameSchedule game = scheduledGames[wi-1];
           if (game != null) {
              if (game.isBye) {
                 return true;
              }
           }
        }
		
        return false;
	}
}
