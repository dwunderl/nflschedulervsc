package com.nflscheduling;

public class NflAttrLimit {

   // ---
   // Instance data

   public String attrName;
   public int weekNum;
   public int[] weeklyLimit;

   NflAttrLimit() {
      System.out.println("Creating an nflAttrLimit");
      weeklyLimit = new int[NflDefs.numberOfWeeks];
   }
}

