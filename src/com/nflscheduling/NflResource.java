package com.nflscheduling;

//import java.util.*;

public class NflResource {

   // ---
   // Instance data

   public String resourceName;
   public int[] weeklyLimit;
   public int[] weeklyMinimum;

   NflResource() {
      //System.out.println("Creating an nflAttrLimit");
      weeklyLimit = new int[NflDefs.numberOfWeeks];
      weeklyMinimum = new int[NflDefs.numberOfWeeks];
   }
   
}

