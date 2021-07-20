package com.nflscheduling;

//import java.util.*;

public class NflResource {

   // ---
   // Instance data

   public String resourceName;
   public String zone;

   public int[] weeklyLimit;
   public int[] weeklyMinimum;

   NflResource() {
      weeklyLimit = new int[NflDefs.numberOfWeeks];
      weeklyMinimum = new int[NflDefs.numberOfWeeks];
      weeklyMinimum = new int[NflDefs.numberOfWeeks];
   }
   
}

