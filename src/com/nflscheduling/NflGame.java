package com.nflscheduling;

import java.util.*;

public class NflGame {

   // ---
   // Instance data

   public String  homeTeam;
   public String  awayTeam;
   public ArrayList<String> attribute;
   public boolean isBye;
   public boolean isDivisional;
   public boolean isInternational;

   NflGame() {
      attribute = new ArrayList<String>(2);
      isBye = false;
      isDivisional = false;
      isInternational = false;
   }

   public boolean findAttribute(String attrName) {
      //System.out.println("   findAttribute: passed attrName: " + attrName + " within game attribute list of size: " + attribute.size());
      for (int ai=0; ai < attribute.size(); ai++) {
         String myAttrName = attribute.get(ai);
         //System.out.println("      comparing passed attrName: with myAttrName: " + myAttrName);

         if (myAttrName.equalsIgnoreCase(attrName)) {
            return true;
         }
      }

      return false;
   }

}
